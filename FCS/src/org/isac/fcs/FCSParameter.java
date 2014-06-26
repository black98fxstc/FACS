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
 * Encapsulates information about one of the measurments recorded for each event
 * in and <code>FCS</code> data file.
 * <p>
 * All the methods for accessing attributes in this class concatinate a prefix,
 * the parameter index and a suffix. For example <code>getAttribute("$P", "B")</code>
 * called on the first parameter returns the value of the <code>$P1B</code> keyword.
 *
 * @author Wayne A. Moore
 * @version 1.2
 */
public class FCSParameter
{
  /**
   * The natrual logarithim of <code>10</code>
   */
  private static final double ln10 = Math.log(10);
  /**
   * The number of bits of data stored for this parameter.
   * Corresponds to the <code>$PnB</code> keyword.
   */
  private int bits = 0;

  /**
   * The maximum (exclulsive) range of the data values stored for this
   * parameter.
   * Corresponds to the <code>$PnR</code> keyword.
   */
  private int range = 0;

  /**
   * The index of this data parameter. Corresponds to the <code>n</code> in the
   * standard <code>$PnX</code> keywords
   */
  private int index;

  /**
   * The analog linear gain of data recorded for this parameter.
   * Corresponds to the <code>$PnG</code> keyword.
   */
  private double gain = Double.NaN;

  /**
   * For logarithmic data gives the number of decades for binary data stored for this parameter.
   * Corresponds to the first component of the <code>$PnE</code> keyword.
   */
  private double decades = Double.NaN;

  /**
   * The minimum scaled data value of this parameter.
   * For logarithmic data corresponds to the second component of the <code>$PnE</code> keyword.
   * For linear data returns <code>0</code> for convenience.
   */
  private double minimum = Double.NaN;

  /**
   * The maximum (exclusive) scaled data value of this parameter. This is a
   * convenience method and is computed from the <code>$PnE</code> or
   * <code>$PnG</code> keywords as appropriate.
   */
  private double maximum = Double.NaN;

  /**
   * The <code>FCSTextSegment</code> from which to retrieve attribute values for
   * this parameter
   */
  private FCSTextSegment textSegment;

  /**
   * Constructs an <code>FCSParameter</code> that gets it's attributes from the
   * specified <code>FCSTextSegment</code>
   *
   * @param textSegment FCSTextSegment
   */
  FCSParameter(
      FCSTextSegment textSegment)
  {
    this.textSegment = textSegment;
  }

  /**
   * Gets the value of an attribute as an <code>int</code>
   *
   * @param prefix The prefix <code>String</code> of the keyword desired
   * @param postfix The suffix <code>String</code> of the keyword desired.
   * @return The <code>int</code> value of the keyword specified or
   *   <code>Integer.MIN_VALUE</code> if the attribute does not exist.
   */
  public int getInteger(
      String prefix,
      String postfix)
  {
    try
    {
      return Integer.parseInt(getAttribute(prefix, postfix));
    }
    catch (Exception ex)
    {
      return Integer.MIN_VALUE;
    }
  }

  /**
   * Gets the value of an attribute as an <code>float</code>
   *
   * @param prefix The prefix <code>String</code> of the keyword desired
   * @param postfix The suffix <code>String</code> of the keyword desired.
   * @return The <code>int</code> value of the keyword specified or
   *   <code>Float.NaN</code> if the attribute does not exist.
   */
  public float getFloat(
      String prefix,
      String postfix)
  {
    try
    {
      return Float.parseFloat(getAttribute(prefix, postfix));
    }
    catch (Exception ex)
    {
      return Float.NaN;
    }
  }

  /**
   * Gets the value of an attribute as an <code>double</code>
   *
   * @param prefix The prefix <code>String</code> of the keyword desired
   * @param postfix The suffix <code>String</code> of the keyword desired.
   * @return The <code>int</code> value of the keyword specified or
   *   <code>Double.NaN</code> if the attribute does not exist.
   */
  public double getDouble(
      String prefix,
      String postfix)
  {
    try
    {
      return Double.parseDouble(getAttribute(prefix, postfix));
    }
    catch (Exception ex)
    {
      return Double.NaN;
    }
  }

  /**
   * Gets the value of an attribute as an <code>String</code>
   *
   * @param prefix The prefix <code>String</code> of the keyword desired
   * @param postfix The suffix <code>String</code> of the keyword desired.
   * @return The <code>String</code> value of the keyword specified or
   *   <code>null</code> if the attribute does not exist.
   */
  public String getAttribute(
      String prefix,
      String postfix)
  {
    return textSegment.getAttribute(prefix + getIndex() + postfix);
  }

  /**
   * Sets the value of the specified attribute to a <code>String</code> value
   *
   * @param prefix The prefix <code>String</code> of the keyword desired
   * @param postfix The suffix <code>String</code> of the keyword desired.
   * @param value The <code>String</code> value of the attribute
   * @throws FCSException If the FCS version is not supported of the file is malformed
   */
  public void setAttribute(
      String prefix,
      String postfix,
      String value)
      throws FCSException
  {
    textSegment.setAttribute(prefix + getIndex() + postfix, value);
  }

  /**
   * Sets the value of the specified attribute to a <code>double</code> value
   *
   * @param prefix The prefix <code>String</code> of the keyword desired
   * @param postfix The suffix <code>String</code> of the keyword desired.
   * @param value The double value of the attribute
   * @throws FCSException If the FCS version is not supported of the file is malformed
   */
  public void setAttribute(
      String prefix,
      String postfix,
      double value)
      throws FCSException
  {
    textSegment.setAttribute(prefix + getIndex() + postfix, String.valueOf((float)value));
  }

  /**
   * Sets the value of the specified attribute to a <code>double</code> value
   *
   * @param prefix The prefix <code>String</code> of the keyword desired
   * @param postfix The suffix <code>String</code> of the keyword desired.
   * @param value The double value of the attribute
   * @throws FCSException If the FCS version is not supported of the file is malformed
   */
  public void setAttribute(
      String prefix,
      String postfix,
      int value)
      throws FCSException
  {
    textSegment.setAttribute(prefix + getIndex() + postfix, String.valueOf(value));
  }

  /**
   * Gets the number of bits stored for each data value for this parameter.
   * This corresponds to the <code>$PnB</code> standard keyword.
   *
   * @return The <code>int</code> number of bits stored for each data value
   */
  public int getBits()
  {
    if (bits == 0)
      bits = getInteger("$P", "B");

    return bits;
  }

  /**
   * Sets the number of bits stored for each data value for this parameter.
   * This corresponds to the <code>$PnB</code> standard keyword.
   *
   * @param bits The <code>int</code> number of bits stored
   * @throws FCSException If the FCS version is not supported of the file is malformed
   */
  public void setBits(
      int bits)
      throws FCSException
  {
    this.bits = bits;
    setAttribute("$P", "B", String.valueOf(bits));
  }

  /**
   * Sets the maximum value (exclusive) for data values stored for this
   * parameter.
   * This corresponds to the <code>$PnR</code> standard keyword.
   *
   * @param range The <code>int</code> maximum value (exclusive) for stored data
   * @throws FCSException If the FCS version is not supported of the file is malformed
   */
  public void setRange(
      int range)
      throws FCSException
  {
    this.range = range;
    setAttribute("$P", "R", String.valueOf(range));
  }

  /**
   * Gets the maximum value (exclusive) for data values stored for this
   * parameter.
   * This corresponds to the <code>$PnR</code> standard keyword.
   *
   * @return The <code>int</code> maximum value (exclusive) for data values
   */
  public int getRange()
  {
    if (range == 0)
    {
      range = 2 << getBits();
      try
      {
        range = getInteger("$P", "R");
      }
      catch (NumberFormatException ex)
      {
        ;
      }
    }

    return range;
  }

  /**
   * Tests whether or not the data for this parameter was logarithmic
   *
   * @return <code>true</code> if the data are logarithmic, <code>false</code>
   *   if the data are linear
   */
  public boolean isLog()
  {
    double d = getDecades();
    return !Double.isNaN(d) && d > 0;
  }

  /**
   * Sets the <code>int</code> index of this parameter within the dataset.
   * Note that the <code>FCS</code> standard defined indecies as 1 origin.
   *
   * @param index The <code>int</code> index of this parameter
   */
  void setIndex(int index)
  {
    this.index = index;
  }

  /**
   * Gets the <code>int</code> index of this parameter. Note that the
   * <code>FCS</code> standard defined indecies as 1 origin.
   *
   * @return The <code>int</code> index of this parameter
   */
  public int getIndex()
  {
    return index;
  }

  /**
   * Gets the analog linear gain for data value for this parameter.
   * This corresponds to the <code>$PnG</code> standard keyword.
   *
   * @return The <code>double</code> analog linear gain for this parameter
   */
  public double getGain()
  {
    if (Double.isNaN(gain))
    {
      String value = getAttribute("$P", "G");
      if (value == null)
        gain = 1;
      else
        gain = Double.parseDouble(value);
    }
    return gain;
  }

  /**
   * Sets the <code>double</code> analog linear gain for data stored for this
   * parameter.
   * This corresponds to the <code>$PnG</code> standard keyword.
   *
   * @param gain The <code>double</code> analog linear gain
   * @throws FCSException If the FCS version is not supported of the file is malformed
   */
  public void setGain(
      double gain)
      throws FCSException
  {
    this.gain = gain;
    float g = (float)gain;
    setAttribute("$P", "G", String.valueOf(g));
    maximum = Double.NaN;
  }

  /**
   * Gets the <code>double</code> number of decades of the scale values of
   * stored logarithmic data.
   * This corresponds to the first component of the <code>$PnE</code> keyword.
   *
   * @return The <code>double</double> number of scale decades
   */
  public double getDecades()
  {
    if (Double.isNaN(decades))
    {
      String e = getAttribute("$P", "E");
      if (e != null && e.length() > 0)
        decades = Double.parseDouble(e.substring(0, e.indexOf(',')));
    }
    return decades;
  }

  /**
   * Sets the <code>double</code> number of decades of the scale values of
   * stored logarithmic data.
   * This corresponds to the first component of the <code>$PnE</code> keyword.
   *
   * @param decades The <code>double</code> number of scale decades
   * @throws FCSException If the FCS version is not supported of the file is malformed
   */
  public void setDecades(
      double decades)
      throws FCSException
  {
    double m = getMinimum();
    if (Double.isNaN(m) || m == 0)
      setLogScale(decades, 1);
    else
      setLogScale(decades, m);
  }

  /**
   * Sets the number of scale decades and the minimum scale value for stored
   * logarithmic data.
   * This corresponds to the <code>$PnE</code> keyword.
   *
   * @param decades The <code>double</code> number of scale decades
   * @param minimum The <code>double</code> minimum scale value
   * @throws FCSException If the FCS version is not supported of the file is malformed
   */
  public void setLogScale(
      double decades,
      double minimum)
      throws FCSException
  {
    this.decades = decades;
    this.minimum = minimum;
    float d = (float)decades;
    float m = (float)minimum;
    setAttribute("$P", "E", d + "," + m);
    maximum = Double.NaN;
  }

  /**
   * Gets the <code>double</code> minimum scaled value for the data
   * stored for this parameter.
   * This is a convinience method not defined in the <code>FCS</code> standard
   * and is calculated from the <code>$PnG</code> or <code>$PnE</code> keywords
   * as appropriate.
   *
   * @return The <code>double</code> minimum scaled value of the data
   */
  public double getMinimum()
  {
    if (Double.isNaN(minimum))
    {
      if (isLog())
      {
        String e = getAttribute("$P", "E");
        int i = e.indexOf(',');
        minimum = Double.parseDouble(e.substring(i + 1, e.length()));
        if (minimum == 0)  // CellQuest not compliant with FCS standard
          minimum = 1;
      }
      else
        minimum = 0;
    }
    return minimum;
  }

  /**
   * Sets the <code>double</code> minimum scaled value for data stored for this
   * parameter.
   * This corresponds to the second component of the <code>$PnE</code> standard keyword.
   *
   * @param minimum The <code>double</code> minimum scale value
   * @throws FCSException If the FCS version is not supported of the file is malformed
   */
  public void setMinimum(
      double minimum)
      throws FCSException
  {
    this.minimum = minimum;
    float m = (float)getMinimum();
    float d = (float)getDecades();
    setAttribute("$P", "E", d + "," + m);
    maximum = Double.NaN;
  }

  /**
   * Gets the <code>double</code> maximum scaled value (exclusive) for the data
   * stored for this parameter.
   * This is a convinience method not defined in the <code>FCS</code> standard
   * and is calculated from the <code>$PnG</code> or <code>$PnE</code> keywords
   * as appropriate.
   *
   * @return The <code>double</code> maximum scaled value of the data
   */
  public double getMaximum()
  {
    if (Double.isNaN(maximum))
    {
      if (isLog())
        maximum = getMinimum() * Math.exp(ln10 * getDecades());
      else
        maximum = getRange() / getGain();
    }
    return maximum;
  }
}
