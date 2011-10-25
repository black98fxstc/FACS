package edu.stanford.facs.desk;

import java.io.*;
import java.util.*;

/**
 * Title:        EDesk Utilities
 * Description:
 * Copyright:    Copyright (c) 2000 by The Board of Trustees of the Leland Stanford Jr. University
 * Company:      Stanford University
 * @author Wayne A. Moore
 * @version 1.0
 */

public class Warehouse
{
  private final FacsDesk eDesk;
  private BitSet convertedSamples;

  public final static Comparator compare_tapes = new Comparator()
  {
    // sort descending stringified integers
    public int compare(Object o1, Object o2)
    {
      String s1 = (String)o1;
      String s2 = (String)o2;
      int i1, i2;
      if (s1.startsWith("WH"))
        i1 = Integer.parseInt(s1.substring(2, 5));
      else
        i1 = Integer.parseInt(s1.substring(1, 5));
      if (s2.startsWith("WH"))
        i2 = Integer.parseInt(s2.substring(2, 5));
      else
        i2 = Integer.parseInt(s2.substring(1, 5));
      if (i1 < i2)
        return -1;
      if (i1 > i2)
        return 1;
      return 0;
    }
  };

  public final static Comparator compare_ids = new Comparator()
  {
    // sort descending stringified integers
    public int compare(Object o1, Object o2)
    {
      String s1 = (String)o1;
      String s2 = (String)o2;
      int i1 = Integer.parseInt(s1);
      int i2 = Integer.parseInt(s2);
      if (i1 < i2)
        return -1;
      if (i1 > i2)
        return 1;
      return 0;
    }
  };

  void import_tapes()
      throws DeskException
  {
    File whouse = eDesk.getSiteFile("WHouse");
    if (whouse == null)
      return;
    String[] tapes = whouse.list();
    if (tapes == null)
      return;
    Arrays.sort(tapes, compare_tapes);
    for (int i = 0; i < tapes.length; ++i)
      import_tape(tapes[i]);
  }

  void import_tape(
      String volume)
      throws DeskException
  {
    File whtape = new File(eDesk.getSiteFile("WHouse"), volume);
    import_samples(whtape);
    import_drawers(whtape);
  }

  void import_samples(
      File whtape)
  {
    try
    {
      if (convertedSamples == null)
      {
        convertedSamples = (BitSet)eDesk.getSiteObject("converted-samples.dat");
        if (convertedSamples == null)
          convertedSamples = new BitSet();
      }

      File csampl = new File(whtape, "CSAMPL");
      if (!csampl.exists())
        return;

      File[] files = csampl.listFiles();
      for (int i = 0; i < files.length; ++i)
      {
        int id = Integer.parseInt(files[i].getName());
        File cache = eDesk.getCacheData(id);
        if (convertedSamples.get(id))
        {
          if (!files[i].delete())
            System.out.println("Cannot delete " + files[i].getPath());
          continue;
        }
        if (!cache.exists())
          cache.getParentFile().mkdirs();
        if (!files[i].renameTo(cache))
          System.out.println("Cannot rename " + files[i].getPath());
      }
      csampl.delete();
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
  }

  void import_drawers(
      File whtape)
      throws DeskException
  {
    try
    {
      File csdata = new File(whtape, "CSDATA");
      if (!csdata.exists())
        return;

      String[] archive = csdata.list();
      Arrays.sort(archive, compare_ids);
      for (int i = 0; i < archive.length; ++i)
      {
        File file = new File(csdata, archive[i]);
        DeskInputStream dis = new DeskInputStream(file);
        DataDrawer drawer = DataDrawer.parse(dis, true);
        dis.close();
        File cache = eDesk.getCacheArchive(drawer.protocol);
        if (!cache.exists())
        {
          if (!cache.exists())
            cache.getParentFile().mkdirs();
          if (!file.renameTo(cache))
            System.out.println("Cannot rename " + file.getPath());
        }
        else
        {
          dis = new DeskInputStream(cache);
          DataDrawer cached = DataDrawer.parse(dis, true);
          dis.close();
          if (cached.archive > drawer.archive)
          {
            if (!file.delete())
              System.out.println("Cannot delete " + file.getPath());
          }
          else
          {
            if (!file.renameTo(cache))
              System.out.println("Cannot rename " + file.getPath());
          }
        }
      }
      csdata.delete();
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
  }

  void update_pending_faults()
  {
    try
    {
      List pending = (List)eDesk.getSiteObject(eDesk.PENDING_FAULTS);
      if (pending == null)
        pending = new ArrayList();
      File temp = eDesk.getSiteFile("faults.tmp");
      File log = eDesk.getSiteFile(eDesk.FAULT_LOG);
      if (!temp.exists() && log.exists())
        log.renameTo(temp);
      if (temp.exists())
        pending.addAll(eDesk.getWarehouseFaults(temp));
      ListIterator li = pending.listIterator();
      while (li.hasNext())
      {
        FacsDesk.WarehouseFault wf = (FacsDesk.WarehouseFault)li.next();
        if (eDesk.getCacheData(wf.warehouse).exists())
          li.remove();
      }
      eDesk.setSiteObject(eDesk.PENDING_FAULTS, pending);
      if (temp.exists())
        temp.delete();
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
  }

  void update_tape_requests()
  {
    List requests = new ArrayList();
    int[] tapeIndex = (int[])eDesk.getSiteObject(eDesk.TAPE_INDEX);

    List faults = (List)eDesk.getSiteObject(eDesk.PENDING_FAULTS);
    if (faults == null)
      faults = new ArrayList();
    faults.addAll(eDesk.getWarehouseFaults());

    ListIterator li = faults.listIterator();
    while (li.hasNext())
    {
      FacsDesk.WarehouseFault wf = (FacsDesk.WarehouseFault)li.next();
      if (eDesk.getCacheData(wf.warehouse).exists())
        li.remove();
    }
    while (li.hasPrevious())
    {
      FacsDesk.WarehouseFault wf = (FacsDesk.WarehouseFault)li.previous();
      for (int tape = 0; tape < tapeIndex.length; ++tape)
      {
        if (tapeIndex[tape] == 0)
          continue;
        else if (wf.warehouse <= tapeIndex[tape])
        {
          Integer volume = new Integer(tape);
          if (!requests.contains(volume))
            requests.add(volume);
          break;
        }
      }
    }

    File whouse = eDesk.getSiteFile("WHouse");
    li = requests.listIterator();
    while (li.hasNext())
    {
      String volume = String.valueOf(((Integer)li.next()).intValue());
      volume = "WH000".substring(0, 5 - volume.length()) + volume;
      File tape = new File(whouse, volume);
      if (tape.exists())
        tape.delete();
    }
  }

  Warehouse(
      FacsDesk eDesk)
  {
    this.eDesk = eDesk;
  }

  Warehouse(
      String site)
      throws DeskException
  {
    this(new FacsDesk(site));
  }

  public static void main(String[] args)
  {
    try
    {
      Warehouse warehouse = new Warehouse(args[0]);
      if (args.length < 2)
        warehouse.import_tapes();
      else
        warehouse.import_tape(args[1]);

      System.out.println("Warehouse exiting normally");
      System.exit(0);
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
  }
}
