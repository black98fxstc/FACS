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
 * An <code>Exception</code> subclass used to signal errors in processing
 * <code>FCS</code> files.
 *
 * @author Wayne A. Moore
 * @version 1.2
 */
public class FCSException
    extends Exception
{

  /**
   * Construct an <code>FCSException</code> with a <code>String</code> detail
   * message
   *
   * @param msg The <code>String</code> detail message
   */
  public FCSException(String msg)
  {
    super(msg);
  }

  /**
   * Construct an <code>FCSException</code> the encapsulates an underlysing
   * <code>Exception</code> cause
   *
   * @param cause The underlying <code>Exception</code> cause
   */
  public FCSException(Exception cause)
  {
    super(cause);
  }
}
