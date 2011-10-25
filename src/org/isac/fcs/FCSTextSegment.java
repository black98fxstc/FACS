/*
    Utilities for reading and writing Flow Cytometry Standard (FCS) data files
    as defined by the International Society for Analytical Cytology (ISAC)

    Copyright (C) 2000,2005 by The Board of Trustees of
    the Leland Stanford Jr. University.

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.isac.fcs;

/**
 * Title:        FCS Utilities
 * Description:
 * Copyright:    Copyright (c) 2000 by The Board of Trustees of the Leland Stanford Jr. University
 * Company:      Stanford University
 * @author Wayne A. Moore
 * @version 1.0
 */

import java.io.*;
import java.util.*;
import java.text.*;

public class FCSTextSegment
{
  char delimiter;
  Hashtable textAttributes;
  boolean modified = true;

  public FCSTextSegment()
  {
    delimiter = '\t';
    textAttributes = new Hashtable(100);
  }

  private FCSTextSegment(FCSTextSegment copy)
  {
    this.delimiter = copy.delimiter;
    this.textAttributes = (Hashtable)copy.textAttributes.clone();
  }

  public char getDelimiter()
  {
    return delimiter;
  }

  public void setDelimiter(
      char delimiter)
  {
    this.delimiter = delimiter;
  }

  public Set getAttributeNames ()
  {
    return textAttributes.keySet();
  }
  public void removeAttribute(
      String keyword)
  {
    keyword = keyword.toUpperCase();

    if (textAttributes.get(keyword) == null)
      return;

    textAttributes.remove(keyword);
    modified = true;
  }

  public void setAttribute(
      String keyword,
      String value)
      throws FCSException
  {
    if (keyword.length() > FCS.MAX_KEYWORD)
      throw new FCSException("Keyword is too long");
    if (value.length() > FCS.MAX_VALUE)
      throw new FCSException("Value is too long");

    keyword = keyword.toUpperCase();
    if (value == null || value.length() == 0)
      value = " ";

    String oldValue = (String)textAttributes.get(keyword);
    if (oldValue != null && oldValue.equals(value))
      return;

    textAttributes.put(keyword, value);
    modified = true;
  }

  public void setAttribute(
      String prefix,
      int index,
      String suffix,
      String value)
      throws FCSException
  {
    setAttribute(prefix + index + suffix, value);
  }

  public void setAttribute(
      String keyword,
      int value)
      throws FCSException
  {
    setAttribute(keyword, String.valueOf(value));
  }

  public void setAttribute(
      String prefix,
      int index,
      String suffix,
      int value)
      throws FCSException
  {
    setAttribute(prefix + index + suffix, String.valueOf(value));
  }

  public void setAttribute(
      String prefix,
      int index,
      String suffix,
      double value)
      throws FCSException
  {
    setAttribute(prefix + index + suffix, String.valueOf(value));
  }

  public void setAttribute(
      String keyword,
      long value)
      throws FCSException
  {
    setAttribute(keyword, String.valueOf(value));
  }

  public void setAttribute(
      String keyword,
      double value)
      throws FCSException
  {
    setAttribute(keyword, String.valueOf((float)value));
  }

  public void setAttribute(
      String keyword,
      double value1,
      double value2)
      throws FCSException
  {
    setAttribute(keyword,
        String.valueOf((float)value1) + "," +
        String.valueOf((float)value2));
  }

  private static String[] months =
      {"JAN", "FEB", "MAR", "APR", "MAY", "JUN",
      "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};
  public void setAttribute(
      String keyword,
      Date date)
      throws FCSException
  {
    GregorianCalendar cal = new GregorianCalendar();
    cal.setTime(date);
    String value = String.valueOf(cal.get(cal.DAY_OF_MONTH));
    if (value.length() < 2)
      value = "0" + value;
    value += "-" + months[cal.get(cal.MONTH)] + "-" + String.valueOf(cal.get(cal.YEAR));

    setAttribute(keyword, value);
  }

  public String getAttribute(
      String keyword)
  {
    return (String)textAttributes.get(keyword.toUpperCase());
  }

  public String getAttribute(
      String prefix,
      int index,
      String suffix)
  {
    return (String)textAttributes.get(prefix + index + suffix);
  }

  public int getIntegerAttribute(
      String keyword)
      throws NumberFormatException
  {
    String value = getAttribute(keyword);
    return Integer.valueOf(value.trim()).intValue();
  }

  public int getIntegerAttribute(
      String prefix,
      int index,
      String suffix)
      throws NumberFormatException
  {
    String value = getAttribute(prefix + index + suffix);
    return Integer.valueOf(value.trim()).intValue();
  }

  public long getLongAttribute(
      String keyword)
      throws NumberFormatException
  {
    String value = getAttribute(keyword);
    return Long.valueOf(value.trim()).intValue();
  }

  public float getFloatAttribute(
      String keyword)
      throws NumberFormatException
  {
    String value = getAttribute(keyword);
    return Float.valueOf(value.trim()).floatValue();
  }

  public float getFloatAttribute(
      String prefix,
      int index,
      String suffix)
      throws NumberFormatException
  {
    String value = getAttribute(prefix + index + suffix);
    return Float.valueOf(value.trim()).floatValue();
  }

  public Date getDateAttribute(
      String keyword)
      throws ParseException
  {
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
    String value = getAttribute(keyword);
    return dateFormat.parse(value);
  }

  public void writeTo(
      OutputStream out)
      throws IOException
  {
    out.write(delimiter);
    Enumeration k = textAttributes.keys();
    while (k.hasMoreElements())
    {
      String keyword = (String)k.nextElement();
      String value = (String)textAttributes.get(keyword);
      for (int i = 0, n = keyword.length(); i < n; ++i)
      {
        char ctr = keyword.charAt(i);
        if (ctr == delimiter)
        {
          out.write(delimiter);
          out.write(delimiter);
        }
        else
          out.write(ctr);
      }
      out.write(delimiter);
      for (int i = 0, n = value.length(); i < n; ++i)
      {
        char ctr = value.charAt(i);
        if (ctr == delimiter)
        {
          out.write(delimiter);
          out.write(delimiter);
        }
        else
          out.write(ctr);
      }
      out.write(delimiter);
    }
  }

  public void readFrom(
      FCSTextSegment textSegment)
      throws FCSException
  {
    Iterator it = textSegment.textAttributes.entrySet().iterator();
    while (it.hasNext())
    {
      Map.Entry entry = (Map.Entry)it.next();
      setAttribute((String)entry.getKey(), (String)entry.getValue());
    }
  }

  public void readFrom(
      byte[] text_header)
      throws FCSException
  {
    int pos = 0, len = text_header.length;
    delimiter = (char)text_header[pos++];
    while (text_header[len - 1] != delimiter) // in case length is wrong (FACSDiVa)
      --len;
    StringBuffer keyword = new StringBuffer(), value = new StringBuffer();
    while (pos < len)
    {
      keyword.setLength(0);
      while (pos < len)
      {
        char c = (char)text_header[pos++];
        if (c != delimiter)
          keyword.append(c);
        else if (pos < len)
        {
          c = (char)text_header[pos];
          if (c == delimiter)
          {
            keyword.append(delimiter);
            pos++;
          }
          else
            break;
        }
        else
          break;
      }

      value.setLength(0);
      while (pos < len)
      {
        char c = (char)text_header[pos++];
        if (c != delimiter)
          value.append(c);
        else if (pos < len)
        {
          c = (char)text_header[pos];
          if (c == delimiter)
          {
            value.append(delimiter);
            pos++;
          }
          else
            break;
        }
        else
          break;
      }

      if (keyword.length() == 0 || value.length() == 0)
        throw new FCSException("Malformed FCS text segment");

      setAttribute(keyword.toString(), value.toString());
    }
  }

  public void writeTo(
      RandomAccessFile out)
      throws IOException
  {
    ByteArrayOutputStream baos = new ByteArrayOutputStream(20
        * textAttributes.size());
    writeTo(baos);
    out.write(baos.toByteArray());
  }

  /**
   * Returns the size in bytes of this <code>FCSTextSegment</code>.
   *
   * @return int
   * @throws IOException
   */
  public int size()
      throws IOException
  {
    ByteArrayOutputStream baos = new ByteArrayOutputStream(20
        * textAttributes.size());
    writeTo(baos);

    return baos.size();
  }

  /**
   * Creates and returns a copy of this object.
   *
   * @return a clone of this instance.
   * @throws CloneNotSupportedException if the object's class does not support
   *   the <code>Cloneable</code> interface. Subclasses that override the
   *   <code>clone</code> method can also throw this exception to indicate that
   *   an instance cannot be cloned.
   */
  public Object clone()
      throws CloneNotSupportedException
  {
    return new FCSTextSegment(this);
  }
}
