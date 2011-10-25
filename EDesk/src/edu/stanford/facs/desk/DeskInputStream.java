package edu.stanford.facs.desk;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;

/**
 * Utilities for reading FACS/Desk envelope and archive files
 * @author Wayne A. Moore
 * @version 1.0
 * <BR> &copy; 1997 by the Board of Trustees of Leland Stanford Junior University
 */

public class DeskInputStream
extends BufferedInputStream
{
  private int  ctr = -2;
  private StringBuffer  sb = new StringBuffer(80);

  public DeskInputStream (
    InputStream  is )
  throws IOException
  {
    super( is, 1024 );
  }

  public DeskInputStream (
    File  file )
  throws IOException
  {
    this( new FileInputStream( file ) );
  }

/**
 * Read a white space delimited "word" from the current line
 * @return the next word found
 * @exception java.io.IOException if the underlying stream does
 */
  public String  readWord ()
  throws IOException
  {
    if (ctr == -2)
      ctr = read();

    if (ctr == '\n' || ctr == -1)
      return  null;

    sb.setLength(0);
    while (ctr == ' ' || ctr == '\t')
      ctr = read();
    while (ctr != ' ' && ctr != '\t' && ctr != '\r' && ctr != '\n' && ctr != -1)
    {
      sb.append( (char) ctr );
      ctr = read();
    }
    while (ctr == ' ' || ctr == '\t' || ctr == '\r')
      ctr = read();

    if (sb.length() == 0)
      return  null;
    else
      return  sb.toString();
  }

/**
 * Read a white space delimited "integer" from the current line
 * @return integer value of the next word found
 * @exception java.io.IOException if the underlying stream does
 */
    public int  readInt ()
    throws IOException
    {
      return  Integer.parseInt( this.readWord() );
    }

/**
 * Read characters delimited by line breaks (i.e. CR and/or LF)
 * @return the characters found
 * @exception java.io.IOException if the underlying stream does
 */
  public String  readLine ()
  throws IOException
  {
    sb.setLength(0);

    if (ctr == -2)
      ctr = read();

    while (ctr != '\r' && ctr != '\n' && ctr != -1)
    {
      sb.append( (char) ctr );
      ctr = read();
    }
    while (ctr == '\r')
      ctr = read();
    if (ctr != -1)
      ctr = read();

    return  sb.toString();
  }

  public void  readToEnd ()
  {
    try
    {
      while (ctr >= 0)
        ctr = read();
    }
    catch (IOException  ex)
    {
      ex.printStackTrace();
    }
  }
}