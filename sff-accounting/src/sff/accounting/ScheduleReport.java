package sff.accounting;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.Map.*;

/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2004
 * </p>
 * <p>
 * Company: Stanford University
 * </p>
 * 
 * @author not attributable
 * @version 1.0
 */

abstract public class ScheduleReport
{
  SimpleDateFormat reportDate = new SimpleDateFormat("HH:mm");
  SimpleDateFormat titleDate = new SimpleDateFormat("EEEE, MMMM d, yyyy");
  SortedMap<Date, SortedMap<String, List<Reservation>>> perDay = new TreeMap<Date, SortedMap<String, List<Reservation>>>();

  class Reservation
    implements Comparable<Reservation>
  {
    String uid;
    String date;
    String time;
    String duration;
    String use;
    String operator;
    String repeatType;
    String onBehalfOf;
    String sterility;
    String lasers;
    String name;
    String machine;
    String canceled;
    Date startTime;
    Date endTime;

    /**
     * Compares this object with the specified object for order.
     * 
     * @param o
     *          the Object to be compared.
     * @return a negative integer, zero, or a positive integer as this object is
     *         less than, equal to, or greater than the specified object.
     * @todo Implement this java.lang.Comparable method
     */
    public int compareTo (Reservation o)
    {
      return this.startTime.compareTo(((Reservation)o).startTime);
    }

    public boolean equals (Reservation o)
    {
      return compareTo(o) == 0;
    }
  }

  public ScheduleReport (File file)
  {
    try
    {
      FileReader fr = new FileReader(file);
      BufferedReader br = new BufferedReader(fr);
      GregorianCalendar gc = new GregorianCalendar();
      SimpleDateFormat calDate = new SimpleDateFormat("M/d/yyyy");
      SimpleDateFormat calTime = new SimpleDateFormat("M/d/yyyy hh:mmaa");
      br.readLine();
      outer: for (;;)
      {
        Reservation r = new Reservation();
        StringBuffer sb = new StringBuffer();
        inner: for (int i = 1;; ++i)
        {
          sb.setLength(0);
          for (;;)
          {
            int c = br.read();
            if (c < 0)
              break outer;
            if (c == '\r' || (c == '\n'))
              break inner;
            if (c == '\t')
              break;
            sb.append((char)c);
          }

          String token = sb.toString();
          switch (i)
          {
          case 2:
            r.date = token;
            break;
          case 3:
            r.time = token;
            break;
          case 4:
            r.duration = token;
            break;
          case 5:
            r.onBehalfOf = token;
            break;
          case 6:
            r.use = token;
            break;
          case 7:
            r.lasers = token;
            break;
          case 8:
            r.sterility = token;
            break;
          case 10:
            r.operator = token;
            break;
          case 14:
            r.repeatType = token;
            break;
          case 21:
            r.machine = token;
            break;
          case 22:
            r.uid = token;
            break;
          case 23:
            r.name = token;
            break;
          case 25:
            r.canceled = token;
            break;
          default:
            break;
          }
        }
        if (!r.canceled.equals("No"))
          continue;
        if (r.repeatType.startsWith("Repeats"))
          continue;

        r.startTime = calTime.parse(r.date + " " + r.time);
        gc.setTime(r.startTime);
        gc.add(GregorianCalendar.MINUTE, Integer.parseInt(r.duration));
        r.endTime = gc.getTime();

        Date d = calDate.parse(r.date);
        SortedMap<String, List<Reservation>> machine = perDay.get(d);
        if (machine == null)
        {
          machine = new TreeMap<String, List<Reservation>>();
          perDay.put(d, machine);
        }

        List<Reservation> report = machine.get(r.machine);
        if (report == null)
        {
          report = new ArrayList<Reservation>();
          machine.put(r.machine, report);
        }
        report.add(r);
      }
      br.close();
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }

    Iterator<SortedMap<String, List<Reservation>>> i = perDay.values()
      .iterator();
    while (i.hasNext())
    {
      SortedMap<String, List<Reservation>> machineMap = i.next();
      Iterator<List<Reservation>> j = machineMap.values().iterator();
      while (j.hasNext())
      {
        List<Reservation> report = j.next();
        Collections.<Reservation>sort(report);
        for (int k = report.size() - 2; k >= 0; --k)
          if (report.get(k).equals(report.get(k + 1)))
            report.remove(k);
      }
    }
  }

  public void print (PrintWriter pw)
  {
    printFileHeader(pw);

    Iterator<Entry<Date,SortedMap<String,List<Reservation>>>> i = perDay.entrySet().iterator();
    while (i.hasNext())
    {
      Entry<Date,SortedMap<String,List<Reservation>>> day_entry = i.next();
      Date thisDate = day_entry.getKey();
      SortedMap<String,List<Reservation>> machineMap = day_entry.getValue();

      printDateHeader(pw, thisDate);

      Iterator<Entry<String,List<Reservation>>> j = machineMap.entrySet().iterator();
      while (j.hasNext())
      {
        Entry<String,List<Reservation>> machine_entry = j.next();
        String machine = machine_entry.getKey();

        printMachineHeader(pw, machine);

        List<Reservation> report = machine_entry.getValue();
        Iterator<Reservation> k = report.iterator();
        while (k.hasNext())
        {
          Reservation r = k.next();
          String sterility = "normal";
          if (r.sterility.indexOf("BL2") >= 0
            || r.sterility.indexOf("BSL-2") >= 0)
            sterility = "bl2";
          else if (r.sterility.indexOf("BL1") >= 0)
            sterility = "bl1";
          else if (r.sterility.equalsIgnoreCase("Sterile"))
            sterility = "sterile";

          String lasers = sterility;
          if (r.lasers.indexOf("405") >= 0 || r.lasers.indexOf("407") >= 0
            || r.lasers.indexOf("UV") >= 0)
            lasers = "lasers";

          String nozzle = sterility;
          if (r.use.indexOf("Nozzle") >= 0)
            nozzle = "nozzle";

          printReservation(pw, r, sterility, lasers, nozzle);
        }

        printMachineFooter(pw);
      }

      printDateFooter(pw);
    }

    printFileTrailer(pw);
  }

  /**
   * printDateFooter
   * 
   * @param pw
   *          PrintWriter
   */
  protected abstract void printDateFooter (PrintWriter pw);

  /**
   * printPageFooter
   * 
   * @param pw
   *          PrintWriter
   */
  protected abstract void printFileTrailer (PrintWriter pw);

  /**
   * printMachineFooter
   * 
   * @param pw
   *          PrintWriter
   */
  protected abstract void printMachineFooter (PrintWriter pw);

  /**
   * printReservation
   * 
   * @param pw
   *          PrintWriter
   * @param r
   *          Record
   */
  protected abstract void printReservation (PrintWriter pw,
    sff.accounting.ScheduleReport.Reservation r, String sterility,
    String lasers, String nozzle);

  /**
   * printMachineHeader
   * 
   * @param pw
   *          PrintWriter
   * @param machine
   *          String
   */
  protected abstract void printMachineHeader (PrintWriter pw, String machine);

  /**
   * printDateHeader
   * 
   * @param pw
   *          PrintWriter
   * @param thisDate
   *          Date
   */
  protected abstract void printDateHeader (PrintWriter pw, Date thisDate);

  /**
   * printPageHeader
   * 
   * @param pw
   *          PrintWriter
   */
  protected abstract void printFileHeader (PrintWriter pw);
}
