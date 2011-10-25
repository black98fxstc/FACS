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

import java.util.*;

/**
 * Constants used elsewhere in the FCS package.
 *
 * @author Wayne A. Moore
 * @version 1.2
 */
public interface FCS
{
  /**
   * The <code>byte array</code> corresponding to the string <code>"FCS"</code>, which must
   * start every legal FCS file.
   */
  public static final byte[] FCS = "FCS".getBytes();

  /**
   * The <code>byte array</code> corresponding to <code>"FCS2.0"</code>, which starts files conforming
   * to FCS V2.0
   */
  public static final byte[] FCS2 = "FCS2.0    ".getBytes();

  /**
   * The <code>byte array</code> corresponding to <code>"FCS3.0"</code>, which starts files conforming
   * to FCS V3.0
   */
  public static final byte[] FCS3 = "FCS3.0    ".getBytes();

  /**
   * The size of the standard FCS header in <code>byte</code>s.
   */
  public static final int HEADER_SIZE = 10 + 6 * 8;

  /**
   * The maximum header offset to the start or end of any segment in an FCS V2.0
   * file.
   *
   * <p>This limitation is due to the definition of the FCS header itself. FCS
   * V3.0 provides a way of bypassing this limitation.
   */
  public static final int MAX_V2_OFFSET = 100000000;

  /**
   * The maximum keyword size permitted in FCS V2.0 files in bytes.
   *
   * <p>This is an implementation limit not defined in the spec.
   */
  public static final int MAX_KEYWORD = 63;

  /**
   * The maximum value size permitted in FCS V2.0 files in bytes.
   *
   * <p>This is an implementation limit not defined in the spec.
   */
  public static final int MAX_VALUE = 8192;
}
