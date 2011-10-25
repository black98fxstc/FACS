package edu.stanford.facs.desk;

import java.io.*;
import java.text.*;
import java.util.*;

/**
 * Title:        EDesk Utilities
 * Description:
 * Copyright:    Copyright (c) 2000 by The Board of Trustees of the Leland Stanford Jr. University
 * Company:      Stanford University
 * @author Wayne A. Moore
 * @version 1.0
 */

public class DataDrawer
{
  public final String  title;
  public final String  author;
//  public final String  username;
  public final String  cytometer;
  public final Date  date;
  public final int  protocol;
  public final int  envelope;
  public final int  archive;
  public final boolean  collecting;
  public final FacsSample[]  sample;

  private static final FacsChannel[]  channel_array = new FacsChannel[0];
  private static final FacsSample[]  sample_array = new FacsSample[0];
  SimpleDateFormat  dateFormat = new SimpleDateFormat( "MMMM d, yyyy" );

  DataDrawer (
    String  title,
    String  author,
    String  cytometer,
    Date  date,
    int  protocol,
    int  envelope,
    int  archive,
    boolean  collecting,
    FacsSample[]  sample )
  {
    this.title = title;
    this.author = author;
    this.cytometer = cytometer;
    this.date = date;
    this.protocol = protocol;
    this.envelope = envelope;
    this.archive = archive;
    this.collecting = collecting;
    this.sample = sample;
  }

  public String  index ()
  {
    CharArrayWriter  caw = new CharArrayWriter();
    PrintWriter  pw = new PrintWriter( caw );
    index( pw );
    pw.flush();
    caw.close();
    return  caw.toString();
  }

  public void  index (
      PrintWriter  pw )
  {
    pw.println( "<html>" );
    pw.println( "<head>" );
    pw.println( "<style type=\"text/css\">" );
    pw.println( ".sample {");
    pw.println( "  background:  #CFCFFF; }");
    pw.println( ".parameter {");
    pw.println( "  background:  #FFFFAF; }");
    pw.println( "</style>" );
    pw.println( "</head>" );
    pw.println( "<body>" );
    pw.println( "<center>" );
    pw.println( "<table width=\"80%\">" );

    pw.print( "<tr>" );
    pw.print( "<th align=\"left\">" );
    pw.print( title  );
    pw.print( "</th>" );
    pw.print( "<th align=\"left\">" );
    pw.print( author );
    pw.print( "</th>" );
    pw.println( "</tr>" );

    pw.print( "<tr>" );
    pw.print( "<th align=\"left\">" );
    pw.print( cytometer + " " + Integer.toString( protocol ) );
    pw.print( "</th>" );
    pw.print( "<th align=\"left\">" );
    pw.print( dateFormat.format( date ) );
    pw.print( "</th>" );
    pw.println( "</tr>" );

    pw.print( "<tr>" );
    pw.print( "<td>" );
    pw.print( "&nbsp;" );
    pw.print( "</td>" );
    pw.println( "</tr>" );

    for (int  i = 0;  i < sample.length;  ++i)
    {
      pw.print( "<tr class=\"sample\">" );
      pw.print( "<td>" );
      pw.print( sample[i].coord );
      pw.print( "</td>" );
      pw.print( "<td>" );
      pw.print( sample[i].events );
      pw.print( "</td>" );
      pw.println( "</tr>" );

      pw.print( "<tr class=\"sample\">" );
      pw.print( "<td colspan=\"2\">" );
      pw.print( sample[i].label );
      pw.print( "</td>" );
      pw.println( "</tr>" );

      for (int  j = 0, n = sample[i].channel.length;  j < n;  ++j)
      {
        pw.print( "<tr class=\"parameter\">" );
        pw.print( "<td>" );
        pw.print( sample[i].channel[j].sensor );
        pw.print( "</td>" );
        pw.print( "<td>" );
        pw.print( sample[i].channel[j].reagent );
        pw.print( "</td>" );
        pw.println( "</tr>" );
      }

      pw.print( "<tr>" );
      pw.print( "<td>" );
      pw.print( "&nbsp;" );
      pw.print( "</td>" );
      pw.println( "</tr>" );
    }
    pw.println( "</table>" );
    pw.println( "</center>" );
    pw.println( "</body>" );
    pw.println( "</html>" );

    pw.flush();
  }

  public static DataDrawer  parse (
    File  file ,
    boolean  isArchive )
  throws DeskException, IOException
  {
    return  parse( new DeskInputStream( file ), isArchive );
  }

  public static DataDrawer  parse (
    File  file )
  throws DeskException, IOException
  {
    DeskInputStream  dis = new DeskInputStream( file );
    DataDrawer dd = parse( dis, file.getName().endsWith(".ED_WH") );
    dis.close();
    return  dd;
  }

  public static DataDrawer  parse (
    DeskInputStream  drawer )
  throws DeskException, IOException
  {
    return  parse( drawer, false );
  }

  public static DataDrawer  parse (
    DeskInputStream  drawer,
    boolean  isArchive )
  throws DeskException, IOException
  {
    String  line, token;
    String  title = null, author = null, cytometer = null;
    String  coord, sample, reagent, sensor, range;
    Date  date = null;
    float low, high;
    int  warehouse = 0, envelope = 0, protocol = 0, archive = 0;
    int  rank, count;
    boolean  collecting = false, log;
    byte  type, bits;
    List  samples = new ArrayList();

    try
    {
      line = drawer.readLine();
      if (!line.equals("CSdata V1"))
	throw new DeskException("bad format");

      while (true)
      {
	token = drawer.readWord();
	if (token == null)
	  break;
	if (token.equals("title"))
	  title = drawer.readLine();
	else if (token.equals("protocol"))
	{
	  protocol = Integer.parseInt( drawer.readWord() );
	  cytometer = drawer.readLine();
	  if (cytometer.equals( "FacStar" ))
	    cytometer = "FACStar";
	}
	else if (token.equals("author"))
	  author = drawer.readLine();
	else if (token.equals("date"))
	{
	  line = drawer.readLine();
	  int yr = Integer.parseInt( line.substring(0,2) );
	  if (yr <= 50)
	    yr += 2000;
	  else
	    yr += 1900;
	  int mo = Integer.parseInt( line.substring(2,4) ) - 1;
	  int dy = Integer.parseInt( line.substring(4,6) );
	  date = new GregorianCalendar( yr, mo, dy ).getTime();
	}
	else if (token.equals("archive"))
	  archive = Integer.parseInt( drawer.readLine() );
	else
	  line = drawer.readLine();
      }
      drawer.readLine();

      while (true)
      {
	token = drawer.readWord();
	if (token == null)
	  break;
	if (!token.equals("CSampl"))
	  throw new DeskException("bad format");
	drawer.readLine();

	coord = drawer.readWord();
	int  cp = coord.indexOf( '<' );
	if (cp > 0)
	  coord = coord.substring(0,cp);
	warehouse = Integer.parseInt(drawer.readWord());
	rank = Integer.parseInt(drawer.readWord());
	count = Integer.parseInt(drawer.readWord());
	drawer.readLine();

	sample = drawer.readLine();

	List  channels = new ArrayList( rank );

	for (byte i = 0; i < rank; ++i)
	{
	  sensor = drawer.readWord();
	  bits = (byte) Integer.valueOf(drawer.readWord()).intValue();
	  range = drawer.readWord();
	  line = drawer.readLine();
	  if (range == null)
	  {
	    low  = (float) 0;
	    high = (float) (1 << bits);
	    range = Float.toString( low ) + "," + Float.toString( high );
	    log = false;
	  }
	  else
	  {
	    StringTokenizer  st = new StringTokenizer( range, "," );
	    low  = Float.valueOf( st.nextToken() ).floatValue();
	    high = Float.valueOf( st.nextToken() ).floatValue();
	    if (line.length() == 0)
	      log = false;
	    else
	    {
	      log = true;
	    }
	  }
	  reagent = drawer.readLine();
	  channels.add( new FacsChannel( sensor, reagent, bits, low, high, log ) );
	}
	samples.add(
	  new FacsSample( coord, sample, count, warehouse,
	  (FacsChannel[]) channels.toArray( channel_array ) ) );

	if (isArchive)
	  continue;

	while (true)
	{
	  token = drawer.readWord();
	  if (token == null)
	    break;
	  if (token.equals("CScond"))
	  {
	    line = drawer.readLine();
	    line = drawer.readLine();
	    line = drawer.readLine();
	    line = drawer.readLine();
	  }
	  else
	    throw new DeskException("bad format");
	}
	drawer.readLine();
      }
    }
    catch (DeskException  de)
    {
      de.printStackTrace();
      drawer.readToEnd();
      throw  de;
    }

    return  new DataDrawer(
      title, author, cytometer, date, protocol,
      envelope, archive, collecting,
      (FacsSample[]) samples.toArray( sample_array ) );
  }
}