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

import java.io.*;
import java.util.*;

/**
 * An <code>abstract</code> superclass used in implementing input and output
 * iterators for reading data from <code>FCSFiles</code>.
 *
 * @author Wayne A. Moore
 * @version 1.2
 */
public abstract class FCSHandler
{
  /**
   * The <code>FCSFile</code> that created this <code>FCSHandler</code>
   */
  protected FCSFile fcs;

  /**
   * The <code>int</code> 0 origin index of the next event to be read or written
   */
  protected int next_event = 0;

  /**
   * The <code>int</code> 0 origin index of the next parameter to be read or written
   */
  protected int next_parameter = 0;

  /**
   * The <code>int</code> 0 origin index of the next parameter to be read or written
   */
  protected int has_more_values = 0;

  /**
   * The <code>int</code> number of events to be read or written
   */
  protected int number_of_events;

  /**
   * The <code>int</code> number of parameters to be read or written
   */
  protected int number_of_parameters;

  /**
   * An <code>int</code> array of the number of bits to be read or written for
   * each parameter.
   * This corresponds to the the <code>$PnB</code> keywords for all the
   * parameters.
   */
  protected int[] bit_width;

  /**
   * The <code>byte[]</code> buffer holding data being read or written to the
   * underlying <code>RandomAccessFile</code>
   */
  private byte[] buffer = new byte[8192];

  /**
   * The <code>int</code> postion in the buffer to be used next.
   */
  private int buffer_position = 0;

  /**
   * The <code>int</code> number of <code>byte</code>s of the buffer that are
   * filled with data
   */
  private int buffer_fill;

  /**
   * Construct an <code>FCSHandler</code> for the referrenced
   * <code>FCSFile</code>
   *
   * @param fcs FCSFile
   * @throws FCSException If the FCS version is not supported of the file is malformed
   * @throws IOException If an Input or output exception occurs
   */
  FCSHandler(
      FCSFile fcs)
      throws FCSException, IOException
  {
    this.fcs = fcs;
    number_of_events = fcs.getTotal();
    number_of_parameters = fcs.getParameters();
    bit_width = new int[number_of_parameters];
    for (int i = 0; i < number_of_parameters; ++i)
      bit_width[i] = fcs.getParameter(i + 1).getBits();
  }

  /**
   * Reads the next byte value from the buffer as an <code>int</code>
   *
   * @throws IOException If an Input or output exception occurs
   * @return The <code>int</code> value of the next byte in the buffer
   */
  protected int readByte()
      throws IOException
  {
    if (buffer_position == 0)
      buffer_fill = fcs.random.read(buffer);
    if (buffer_fill < 0)
      return buffer_fill;

    int value = buffer[buffer_position++];
    if (buffer_position == buffer_fill)
      buffer_position = 0;

    return value & 0xFF;
  }

  /**
   * Writes the next <code>byte</code> into the buffer
   *
   * @param value The <code>byte</code> value to be written
   * @throws IOException If an Input or output exception occurs
   */
  protected void writeByte(
      byte value)
      throws IOException
  {
    buffer[buffer_position++] = (byte)value;
    if (buffer_position == buffer.length)
    {
      fcs.random.write(buffer);
      buffer_position = 0;
    }
  }

  /**
   * Writes data from the buffer to the underlying <code>RandomAccessFile</code>
   *
   * @throws IOException If an Input or output exception occurs
   */
  protected void flush()
      throws IOException
  {
    if (fcs.mutator)
      fcs.random.write(buffer, 0, buffer_position);
  }

  /**
   * Reads a bit field from the data stream
   *
   * @param num_bits The <code>int</code> number of bits to read
   * @throws IOException If an Input or output exception occurs
   * @return The <code>int</code> value of the bits read
   */
  protected abstract int readBits(
      int num_bits)
      throws IOException;

  /**
   * Writes a bit field to the data stream
   *
   * @param num_bits The <code>int</code> number of bits to be written
   * @param value The <code>int</code> value of the bit field to be written
   * @throws IOException If an Input or output exception occurs
   */
  protected abstract void writeBits(
      int num_bits,
      int value)
      throws IOException;

  /**
   * Reads an <code>int</code> from the data stream
   *
   * @throws IOException If an Input or output exception occurs
   * @return The <code>int</code> value read
   */
  public abstract int readInt()
      throws IOException;

  /**
   * Writes an <code>int</code> value to the data stream
   *
   * @param value int
   * @throws IOException If an Input or output exception occurs
   */
  public abstract void writeInt(
      int value)
      throws IOException;

  /**
   * Tests if there are more events to be read or written
   *
   * @return <code>true</code> if there are more events, <code>false</code>
   *   otherwise
   */
  public boolean hasMoreEvents()
  {
    return next_event < number_of_events;
  }

  /**
   * Tests if there are more parameters in this event to be read or written
   *
   * @return <code>true</code> if there are more parameters, <code>false</code>
   *   otherwise
   */
  public boolean hasMoreValues()
  {
    if (has_more_values++ < number_of_parameters)
      return true;

    has_more_values = 0;
    return false;
  }

  /**
   * Reads the next value from the data stream as an <code>int</code>
   *
   * @throws IOException If an Input or output exception occurs
   * @return The <code>int</code> data value
   */
  public int readValue()
      throws IOException
  {
    int value = readBits(bit_width[next_parameter++]);
    if (next_parameter == number_of_parameters)
    {
      next_parameter = 0;
      ++next_event;
    }
    return value;
  }

  /**
   * Writes the next data value to the stream
   *
   * @param value The <code>int</code> value to be written
   * @throws IOException If an Input or output exception occurs
   */
  public void writeValue(
      int value)
      throws IOException
  {
    writeBits(bit_width[next_parameter++], value);
    if (next_parameter == number_of_parameters)
    {
      next_parameter = 0;
      ++next_event;
    }
  }

  /**
   * Reads a <code>float</code> from the data stream
   *
   * @throws IOException If an Input or output exception occurs
   * @return The <code>float</code> value read
   */
  public float readFloat()
      throws IOException
  {
    return Float.intBitsToFloat(readInt());
  }

  /**
   * Writes and <code>float</code> value to the data stream
   *
   * @param value The <code>float</code> value to be written
   * @throws IOException If an Input or output exception occurs
   */
  public void writeFloat(
      float value)
      throws IOException
  {
    writeInt(Float.floatToIntBits(value));
  }

  /**
   * Gets the <code>int</code> 0 origin index of this event in the data
   *
   * @return The <code>int</code> index of this event
   */
  public int getEvent()
  {
    return next_event;
  }

  /**
   * Closes this <code>FCSHandler</code>. On files open for writing, flushes all
   * the buffered data, sets the total event count in the
   * <code>FCSTextSegment</code> and writes the <code>TEXT</code> segment and
   * the file header.
   *
   * @throws FCSException If the FCS version is not supported of the file is malformed
   * @throws IOException If an Input or output exception occurs
   */
  public void close()
      throws FCSException, IOException
  {
    if (fcs.mutator)
    {
      flush();

      fcs.setDataEnd(fcs.random.getFilePointer() - 1);

      if (next_parameter != 0)
        throw new FCSException("Incomplete event written");
      fcs.getTextSegment().setAttribute("$TOT", next_event);
    }
    fcs.close();
  }
}
