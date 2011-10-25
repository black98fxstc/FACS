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

/**
 * Implements <code>FCSHandler</code> for data stored in "little endian" byte
 * order.
 *
 * @author Wayne A. Moore
 * @version 1.2
 */
class LittleEndianHandler
    extends FCSHandler
{
  private int high_bit = 0;
  private long accumulator = 0;

  LittleEndianHandler(
      FCSFile fcs)
      throws FCSException, IOException
  {
    super(fcs);
  }

  protected int readBits(
      int num_bits)
      throws IOException
  {
    while (high_bit < num_bits)
    {
      accumulator = (accumulator | (readByte() << high_bit));
      high_bit += 8;
    }
    int value = (int)(accumulator & ((1 << num_bits) - 1));
    accumulator = accumulator >>> num_bits;
    high_bit -= num_bits;

    return value;
  }

  protected void writeBits(
      int num_bits,
      int value)
      throws IOException
  {
    accumulator |= (value & ((1 << num_bits) - 1)) << high_bit;
    high_bit += num_bits;
    while (high_bit >= 8)
    {
      writeByte((byte)(accumulator & 0xFF));
      accumulator >>>= 8;
      high_bit -= 8;
    }
  }

  public int readInt()
      throws IOException
  {
    if (++next_parameter == number_of_parameters)
    {
      next_parameter = 0;
      ++next_event;
    }

    return readByte() | readByte() << 8 | readByte() << 16 | readByte() << 24;
  }

  public void writeInt(int value)
      throws IOException
  {
    writeByte((byte)value);
    writeByte((byte)(value >> 8));
    writeByte((byte)(value >> 16));
    writeByte((byte)(value >> 24));

    if (++next_parameter == number_of_parameters)
    {
      next_parameter = 0;
      ++next_event;
    }
  }

  public void flush()
      throws IOException
  {
    if (high_bit != 0)
      writeByte((byte)(accumulator & 0xFF));
    super.flush();
  }
}
