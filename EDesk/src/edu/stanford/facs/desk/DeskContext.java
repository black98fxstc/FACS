package edu.stanford.facs.desk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Title:        EDesk Utilities
 * Description:
 * Copyright:    Copyright (c) 2000 by The Board of Trustees of the Leland Stanford Jr. University
 * Company:      Stanford University
 * @author Wayne A. Moore
 * @version 1.0
 */

public class DeskContext
implements Serializable
{
  public int  tape_index[];
  public int  volumes;
  public BitSet  envelope_seen, warehouse_reference, warehouse_request;
  public Map  user_checkpoint;
  private String  institution;
  private int  warehouse_maximum;
  transient FacsDesk  eDesk;

  private transient Map  author_uid;
//  private transient File  site_root;
//  private int  site_id;

  private Map  authorToUID()
  {
    if (author_uid == null)
    try
    {
      author_uid = new HashMap();
      File  f = eDesk.getSiteFile( "USERS.ED" );
      if (f.exists())
      {
	DeskInputStream  users = new DeskInputStream( f );
	while (true)
	{
	  String uid = users.readWord();
	  if (uid == null)
	    break;
	  String  author = users.readLine().trim();
	  author_uid.put( author, uid.toUpperCase() );
	}
	users.readToEnd();
      }
    }
    catch (IOException  ex)
    {
      ex.printStackTrace();
    }

    return  author_uid;
  }

  public FacsDesk  getEDesk ()
  {
    return  eDesk;
  }

  public Set  getUsers ()
  {
    Set  users = new HashSet( user_checkpoint.keySet() );
    users.addAll( authorToUID().values() );
    return  users;
  }

  public Set  getAuthors ()
  {
    return  authorToUID().keySet();
  }

  public boolean  isEnvelopeSeen (
    int  envelope )
  {
    return  envelope_seen.get( envelope );
  }

  public void  setEnvelopeSeen (
    int  envelope )
  {
    envelope_seen.set( envelope );
  }

  public boolean  isWarehouseReferenced (
    int  warehouse )
  {
    return  warehouse_reference.get( warehouse );
  }

  public void  setWarehouseMaximum (
    int  warehouse )
  {
    if (warehouse > warehouse_maximum)
      warehouse_maximum = warehouse;
  }

  public void  setWarehouseReferenced (
    int  warehouse )
  {
    setWarehouseMaximum( warehouse );
    warehouse_reference.set( warehouse );
  }

  public void  setWarehouseReferences (
    DataDrawer  drawer )
  {
    for (int  i = 0;  i < drawer.sample.length;  ++i)
      setWarehouseReferenced( drawer.sample[i].warehouse );
  }

  public void  setWarehouseRequested (
    DataDrawer  drawer )
  {
    for (int  i = 0;  i < drawer.sample.length;  ++i)
    {
      int  warehouse = drawer.sample[i].warehouse;
      setWarehouseReferenced( warehouse );
      if (!eDesk.getCacheData( warehouse ).exists())
	setWarehouseRequested( warehouse );
    }
  }

  public boolean  isWarehouseRequested (
    int  warehouse )
  {
    return  warehouse_request.get( warehouse );
  }

  public void  setWarehouseRequested (
    int  warehouse )
  {
    warehouse_request.set( warehouse );
  }

  public int  getWarehouseMaximum ()
  {
    return  warehouse_maximum;
  }

  public String  getUID (
    String  author )
  {
    String  uid = (String) authorToUID().get( author );
    if (uid == null)
    {
      authorToUID().put( author, FacsDesk.UNKNOWN_USER );
      return  FacsDesk.UNKNOWN_USER;
    }
    else
      return  uid;
  }

  public void  setUID (
    String  author,
    String  uid )
  {
    authorToUID().put( author, uid );
  }


  public int  getCheckpoint (
    String  user )
  {
    Integer  checkpoint = (Integer) user_checkpoint.get( user.toUpperCase() );
    if (checkpoint == null)
      return  0;
    else
      return  checkpoint.intValue();
  }

  public void  setCheckpoint (
    String  user,
    int  checkpoint )
  {
    user_checkpoint.put( user.toUpperCase(), new Integer( checkpoint ) );
  }

  public int  getTapeIndex (
    int  volume )
  {
    if (volume > tape_index.length)
      volume = tape_index.length - 1;

    int  index = tape_index[volume];
    while (index == 0 && volume > 0)
      index = tape_index[--volume];

    return  index;
  }

  public void  setTapeIndex (
    int  volume,
    int  index )
  {
    if (volume >= tape_index.length)
    {
      int[]  new_tapes = new int[ 2 * volume ];
      System.arraycopy( tape_index, 0, new_tapes, 0, tape_index.length );
      tape_index = new_tapes;
    }
    tape_index[volume] = index;
    setWarehouseMaximum( index );
  }

  public int  getTapeVolume (
    int  warehouse )
  {
    int  volume = tape_index.length;
    while (volume > 0)
      if (tape_index[--volume] >= warehouse)
	return  volume;
    return  0;
  }

  private void writeObject (
    ObjectOutputStream out )
  throws IOException
  {
    out.defaultWriteObject();
  }

  private void readObject (
    ObjectInputStream in )
  throws IOException, ClassNotFoundException
  {
    in.defaultReadObject();
  }

  private  DeskContext (
    FacsDesk  eDesk )
  throws DeskException
  {
    this.eDesk = eDesk;
    tape_index = new int[1024];
    volumes = 0;
    user_checkpoint = new HashMap( 500 );
    envelope_seen = new BitSet();
    warehouse_maximum = 0;
    warehouse_reference = new BitSet();
    warehouse_request = new BitSet();
  }

  private  DeskContext ()
  throws DeskException
  {   }

  public static DeskContext  getContext (
    String  site )
  throws DeskException
  {
    FacsDesk eDesk = new FacsDesk( site );
    File  siteRoot = eDesk.getSiteRoot();
    if (!siteRoot.exists())
      siteRoot.mkdir();
    File  saved_context = eDesk.getSiteFile( "context.dat" );
    if (!saved_context.exists())
      return  new DeskContext( eDesk );
    else
      try
      {
	ObjectInputStream  ois = new ObjectInputStream( new FileInputStream( saved_context ) );
	DeskContext  context = (DeskContext) ois.readObject();
	ois.close();
	context.eDesk = eDesk;
	return  context;
      }
      catch (Exception  ex)
      {
	ex.printStackTrace();
	throw  new DeskException( "error reading context" );
      }
  }

  public void  commit ()
  throws DeskException
  {
    try
    {
      File  save = eDesk.getSiteFile( "context.dat" );
      ObjectOutputStream  oos = new ObjectOutputStream( new FileOutputStream( save ) );
      oos.writeObject( this );
      oos.close();

      if (author_uid == null)
	return;

      StringBuffer  sb = new StringBuffer( 80 );
      Iterator  entries = author_uid.entrySet().iterator();
      List  lines = new ArrayList();
      while (entries.hasNext())
      {
	Map.Entry  entry = (Map.Entry) entries.next();
	sb.setLength( 0 );
	sb.append( (String) entry.getValue() );
	while (sb.length() < 13)
	  sb.append( ' ' );
	sb.append( entry.getKey() );
	lines.add( sb.toString() );
      }
      Collections.sort( lines );

      PrintWriter  pw = new PrintWriter( new FileWriter( eDesk.getSiteFile( "USERS.ED" ) ) );
      Iterator  line = lines.iterator();
      while (line.hasNext())
      {
	String  l = (String) line.next();
	if (l.startsWith( FacsDesk.UNKNOWN_USER ))
	  System.out.println( l );
	pw.println( l );
      }
      pw.close();
    }
    catch (Exception  ex)
    {
      ex.printStackTrace();
      throw  new DeskException( "error saving context" );
    }
  }
}