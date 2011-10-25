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
import java.nio.channels.*;
import java.nio.*;

/**
 * <code>FCSFile</code> encapsulates an instance of a flow cytometry standard
 * data file.
 *
 * @author Wayne A. Moore
 * @version 1.2
 */
public class FCSFile
{
  boolean headerModified = true;
  boolean crcModified = true;

  /**
   * <code>true</code> if the FCS file is being created or modified,
   * <code>false</code> otherwise
   */
  boolean mutator;

  /**
   * A <code>RandomAccessFile</code> used to read or write the underlying data
   * <code>File</code>
   */
  RandomAccessFile random;

  /**
   * The underlying <code>File</code> contining the data
   */
  private File file;

  /**
   * A <code>List</code> of <code>FCSParameter</code>s describing the
   * measurements stored in this data file
   */
  private ArrayList parameters;

  /**
   * The first 8 <code>bytes</code> of the HEADER as defined in the standard
   * contining version information
   */
  private byte[] version = new byte[10];

  /**
   * The <code>long</code> offset to the first <code>byte</code> of the primary (or only)
   * <code>TEXT</code> section. This implementation never generates supplemental
   * <code>TEXT</code> segments although it will read them if they exist.
   */
  private long textStart = -1;

  /**
   * The <code>long</code> offset to the last <code>byte</code> of the primary
   * (or only) <code>TEXT</code> segment.
   */
  private long textEnd = -1;

  /**
   * The <code>long</code> offset to the first <code>byte</code> of the
   * <code>DATA</code> segment.
   */
  private long dataStart = -1;

  /**
   * The <code>long</code> offset to the last <code>byte</code> of the
   * <code>DATA</code> segment.
   */
  private long dataEnd = -1;

  /**
   * The <code>long</code> offset to the first <code>byte</code> of the
   * <code>ANALYSIS</code> segment.
   */
  private long analysisStart = -1;

  /**
   * The <code>long</code> offset to the last <code>byte</code> of the
   * <code>ANALYSIS</code> segment.
   */
  private long analysisEnd = -1;

  /**
   * An internal <code>FCSTextSegment</code> that handles the <code>TEXT</code>
   * segment of the data file
   */
  private FCSTextSegment textSegment;

  /**
   * The power of 2 to which the <code>DATA</code> segment will be aligned
   * within the underlying file.
   */
  private int dataAlignment = 8;
  private byte[] header_value = new byte[8];

  public FCSFile()
  {
    System.arraycopy(FCS.FCS3, 0, version, 0, version.length);
  }

  /**
   * Constructs an <code>FCSFile</code> given the <code>String</code> path to
   * the data file.
   *
   * @param name The <code>String</code> path to the underlying data file
   */
  public FCSFile(
      String name)
  {
    this(new File(name));
  }

  /**
   * Constructs an <code>FCSFile</code> given a <code>File</code> referring to
   * the data file.
   *
   * @param file The <code>File</code> refferring to the underlying data file
   */
  public FCSFile(
      File file)
  {
    this();
    this.file = file;
  }

  /**
   * Reads and interprets a <code>byte</code> offset from an FCS header.
   *
   * <p>The standard defines offsets as a right justified 8 character decimal
   * encoded integer.
   *
   * @throws IOException If an Input or output exception occurs
   * @return int
   */
  private int readHeaderValue()
      throws IOException
  {
    int value = 0;

    random.readFully(header_value);
    for (int i = 0; i < 8; ++i)
    {
      char c = (char)header_value[i];
      if (c == ' ')
        continue;

      value *= 10;
      value += c - '0';
    }

    return value;
  }

  /**
   * Encodes and writes a <code>byte</code> offset to an FCS header.
   *
   * <p>The standard defines offsets as a right justified 8 character decimal
   * encoded integer.
   *
   * @param value long
   * @throws IOException If an Input or output exception occurs
   */
  private void writeHeaderValue(
      long value)
      throws IOException
  {
    int i = 8;
    while (i > 0)
    {
      header_value[--i] = (byte)(value % 10 + '0');
      value /= 10;
      if (value == 0)
        break;
    }
    while (i > 0)
      header_value[--i] = (byte)' ';

    random.write(header_value);
  }

  /**
   * Writes the <code>HEADER</code> segment to an FCS data file including the
   * version and offsets the the <code>TEXT</code> and <code>DATA</code>
   * segments.
   *
   * @throws FCSException If any segment offset is too large
   * @throws IOException If an input or output exception occurs
   */
  public void writeHeader()
      throws FCSException, IOException
  {
    if (!headerModified)
      return;

    createMutator();

    random.seek(0);
    random.write(version);

    if (getTextStart() < 0 || getTextEnd() < 0)
      throw new FCSException("Text segment offsets not defined");

    writeHeaderValue(getTextStart());
    writeHeaderValue(getTextEnd());

    if (getDataStart() < 0 || getDataEnd() < 0)
      throw new FCSException("Data segment offsets not defined");

    if (getDataStart() < FCS.MAX_V2_OFFSET
        && getDataEnd() < FCS.MAX_V2_OFFSET)
    {
      writeHeaderValue(getDataStart());
      writeHeaderValue(getDataEnd());
    }
    else if (Arrays.equals(version, FCS.FCS3))
    {
      writeHeaderValue(0);
      writeHeaderValue(0);
    }
    else
      throw new FCSException("Data segment is too long");

    if (getAnalysisStart() < 0 || getAnalysisEnd() < 0)
      throw new FCSException("Analysis segment offsets not defined");

    if (getAnalysisStart() < FCS.MAX_V2_OFFSET
        && getAnalysisEnd() < FCS.MAX_V2_OFFSET)
    {
      writeHeaderValue(getAnalysisStart());
      writeHeaderValue(getAnalysisEnd());
    }
    else if (Arrays.equals(version, FCS.FCS3))
    {
      writeHeaderValue(0);
      writeHeaderValue(0);
    }
    else
      throw new FCSException("Analysis segment is too long");

    headerModified = false;
  }

  /**
   * Writes the <code>FCSTextSegment</code> for this <code>FCSfile</code> to the
   * underlying <code>File</code>.
   *
   * @throws FCSException If the text segment is too long to fit into the space
   *   allocated by the file's header
   * @throws IOException If an input or output exception occurs
   */
  public void writeTextSegment()
      throws FCSException, IOException
  {
    if (!textSegment.modified)
      return;

    createMutator();

    if (textStart < FCS.HEADER_SIZE)
      setTextStart(FCS.HEADER_SIZE);
    if (textStart > FCS.HEADER_SIZE)
    {
      byte[] fill = new byte[(int)textStart - FCS.HEADER_SIZE];
      Arrays.fill(fill, (byte)' ');
      random.seek(FCS.HEADER_SIZE);
      random.write(fill);
    }

    random.seek(getTextStart());
    textSegment.writeTo(random);
    setTextEnd(random.getFilePointer() - 1);

    if (random.getFilePointer() > dataStart)
      throw new FCSException("Text segment is too large");

    byte[] fill = new byte[(int)(dataStart - random.getFilePointer())];
    Arrays.fill(fill, (byte)' ');
    random.write(fill);

    textSegment.modified = false;
  }

  /**
   * Writes a null <code>CRC</code> for this <code>FCSfile</code> to the
   * underlying <code>File</code>.
   *
   * @throws IOException If an input or output exception occurs
   */
  public void writeCRC()
      throws IOException, FCSException
  {
    if (!crcModified)
      return;

    if (Arrays.equals(version, FCS.FCS3))
    {
      createMutator();

      Arrays.fill(header_value, 0, 8, (byte)'0');
      random.seek(dataEnd + 1);
      random.write(header_value, 0, 8);
    }

    crcModified = false;
  }

  /**
   * Creates a <code>RandomAccessFile</code> referring to the underlying
   * <code>File</code> open for writing
   *
   * @throws IOException If the file cannot be opened
   */
  protected void createMutator()
      throws IOException, FCSException
  {
    if (file == null)
      throw new FCSException("File has not been set");

    if (random != null)
      if (mutator)
        return;
      else
        random.close();

    random = new RandomAccessFile(file, "rw");
    mutator = true;
  }

  /**
   * Creates a <code>RandomAccessFile</code> referring to the underlying
   * <code>File</code> open for reading.
   *
   * @throws FCSException If the file is not and FCS file or the FCS version is
   *   not supported
   * @throws IOException If the file cannot be opened
   */
  protected void createAccessor()
      throws FCSException, IOException
  {
    if (file == null)
      throw new FCSException("File has not been set");

    if (random == null)
    {
      random = new RandomAccessFile(file, "r");
      mutator = false;

      random.readFully(version);
      for (int i = 0; i < FCS.FCS.length; ++i)
        if (version[i] != FCS.FCS[i])
          throw new FCSException("Not an FCS file");
      if (!Arrays.equals(version, FCS.FCS2) && !Arrays.equals(version, FCS.FCS3))
        throw new FCSException("Unsupported version " + new String(version));

      textStart = readHeaderValue();
      textEnd = readHeaderValue();
      dataStart = readHeaderValue();
      dataEnd = readHeaderValue();
      analysisStart = readHeaderValue();
      analysisEnd = readHeaderValue();

      headerModified = false;
    }
  }

  /**
   * If it exists, closes the <code>RandomAccessFile</code> used bye this
   * <code>FCSFile</code>
   *
   * @throws IOException If an input or output exception occurs
   */
  public void close()
      throws IOException, FCSException
  {
    if (random == null)
      return;

    if (mutator)
    {
      writeCRC();
      writeTextSegment();
      writeHeader();
    }

    random.close();
    random = null;
    mutator = false;
  }

  public long align (long offset)
  {
    long alignment = (1 << getDataAlignment()) - 1;
    return (offset + alignment) & ~alignment;
  }

  /**
   * Returns a channel that can be used to access the underlying file,
   * either for high performance reading of the data or
   * for copying or enriching existing <code>FCS</code> files.
   *
   * @param writable boolean
   * @return FileChannel
   * @throws IOException
   * @throws FCSException
   */
  public FileChannel getChannel(boolean writable)
      throws IOException, FCSException
  {
    if (writable)
      createMutator();
    else
      createAccessor();

    return random.getChannel();
  }

  /**
   * Sets the <code>FCSTextSegment</code> for this file to be a copy of the
   * argument
   *
   * @param textSegment The <code>FCSTextSegment</code> to be copied
   * @throws FCSException If the supplied <code>FCSTextSegment</code> does so
   */
  void setTextSegment(
      FCSTextSegment textSegment)
      throws FCSException
  {
    this.textSegment = new FCSTextSegment();
    this.textSegment.readFrom(textSegment);
  }

  /**
   * Gets the <code>FCSTextSegment</code> corresponding to this
   * <code>FCSFile</code>. Will open the underlying <code>File</code> and read
   * it's header and <code>TEXT</code> segment as necessary.
   *
   * @throws FCSException If the FCS version is not supported or the file is
   *   malformed
   * @throws IOException If an input or output exception occurs
   * @return FCSTextSegment
   */
  public FCSTextSegment getTextSegment()
      throws FCSException, IOException
  {
    if (textSegment == null)
    {
      textSegment = new FCSTextSegment();
      if (file != null && file.exists() && getTextStart() >= 0)
      {
        createAccessor();
        random.seek(getTextStart());
        byte[] text_header = new byte[(int)(getTextEnd() - getTextStart() + 1)];
        random.readFully(text_header);

        textSegment.readFrom(text_header);

        if (Arrays.equals(version, FCS.FCS3))
        {
          if (getDataStart() == 0 && getDataEnd() == 0)
          {
            setDataStart(Long.parseLong(getAttribute("$BEGINDATA").trim()));
            setDataEnd(Long.parseLong(getAttribute("$ENDDATA").trim()));
          }

          long sTextStart = Long.parseLong(getAttribute("$BEGINSTEXT").trim());
          long sTextEnd = Long.parseLong(getAttribute("$ENDSTEXT").trim());
          if (sTextEnd > sTextStart)
          {
            random.seek(sTextStart);
            text_header = new byte[(int)(sTextEnd - sTextStart + 1)];
            random.readFully(text_header);

            textSegment.readFrom(text_header);
          }
        }
        else if (!Arrays.equals(version, FCS.FCS2))
          throw new FCSException("Unspupported FCS version");

        textSegment.modified = false;
      }
      else
      {
        if (file != null)
          textSegment.setAttribute("$FIL", file.getName());

        textSegment.setAttribute("$DATE", new Date());
        textSegment.setAttribute("$PAR", "0");
        textSegment.setAttribute("$TOT", "000000000");

        if (Arrays.equals(version, FCS.FCS2))
        {
          textSegment.setAttribute("$ENCRYPT", "NONE");
          textSegment.setAttribute("$KWMAXCHARS", FCS.MAX_KEYWORD);
          textSegment.setAttribute("$VALMAXCHARS", FCS.MAX_VALUE);
//          textSegment.setAttribute("$SYS", "PASCAL; VMS_VAX Desk Export");
        }
        else if (Arrays.equals(version, FCS.FCS3))
        {
          textSegment.setAttribute("$BEGINDATA", "00000000000000000000");
          textSegment.setAttribute("$ENDDATA", "00000000000000000000");

          textSegment.setAttribute("$BEGINANALYSIS", 0);
          textSegment.setAttribute("$ENDANALYSIS", 0);

          textSegment.setAttribute("$BEGINSTEXT", 0);
          textSegment.setAttribute("$ENDSTEXT", 0);

          textSegment.setAttribute("$NEXTDATA", 0);
        }
        else
          throw new FCSException("Unspupported FCS version");

        analysisStart = 0;
        analysisEnd = 0;
      }
    }

    return textSegment;
  }

  /**
   * Gets the <code>String</code> value corresponding to the supplied keyword.
   *
   * @param keyword The <code>String</code> for the keyword who's value is
   *   desired.
   * @throws FCSException If the <code>FCSTextSegement</code> cannot be accessed
   * @throws IOException If an input or output exception occurs
   * @return The keyword's value as a <code>String</code> or <code>null</code>
   *   if the keyword is not present
   */
  public String getAttribute(
      String keyword)
      throws FCSException, IOException
  {
    return getTextSegment().getAttribute(keyword);
  }

  /**
   * Gets the <code>int</code> value corresponding to the supplied keyword.
   *
   * @param keyword The <code>String</code> for the keyword who's value is
   *   desired.
   * @return The keyword's value as an <code>int</code> or
   *   <code>Integer.MIN_VALUE</code> if the keyword is not present
   */
  public int getInteger(
      String keyword)
  {
    try
    {
      return Integer.parseInt(getAttribute(keyword).trim());
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
      return Integer.MIN_VALUE;
    }
  }

  /**
   * Gets the total number of events recorded in this dataset. This corresponds
   * to the <code>"$TOT</code> keyword.
   *
   * @return The total number of events
   */
  public int getTotal()
  {
    return getInteger("$TOT");
  }

  /**
   * Gets the <code>int</code> number of parameters measured for each event in
   * this <code>FCSFile</code>
   *
   * @return int
   */
  public int getParameters()
  {
    return getInteger("$PAR");
  }

  /**
   * Gets the <code>List</code> of <code>FCSParameter</code>s, which describes
   * the measurments recorded for each event in this dataset.
   *
   * @throws FCSException If the <code>FCS</code> version is unsupported or the
   *   file is malformed
   * @throws IOException If in input or output exception occurs
   * @return A <code>List</code> of <code>FCSParameter</code>s
   */
  public List getParameterList()
      throws FCSException, IOException
  {
    if (parameters == null)
    {
      int n = getParameters();
      parameters = new ArrayList(n);
      for (int i = 1; i <= n; ++i)
      {
        FCSParameter p = new FCSParameter(getTextSegment());
        p.setIndex(i);
        parameters.add(p);
      }
    }

    return parameters;
  }

  /**
   * Adds a new <code>FCSParameter</code> to this <code>FCSFile</code>. By
   * default the number if bits is set to 16 and the index is one greater than
   * the current maximum.
   *
   * @throws FCSException If the <code>FCS</code> version is not supported or
   *   the file is malformed.
   * @throws IOException If an input or output exception occured
   * @return The newly created <code>FCSParameter<code>
   */
  public FCSParameter addParameter()
      throws FCSException, IOException
  {
    FCSParameter p = new FCSParameter(getTextSegment());
    p.setIndex(getParameters() + 1);
    p.setBits(16);
    p.setLogScale(0, 0);

    getParameterList().add(p);

    getTextSegment().setAttribute("$PAR", String.valueOf(p.getIndex()));

    return p;
  }

  /**
   * Copies numbered attributes from one <code>FCSParameter</code> to another.
   * The keyword is of the form <code>&lt;prefix&gt;n&lt;suffix&gt;</code> where
   * <code>n</code> is the index of the respective parameter.
   *
   * @param newParam The target <code>FCSParameter</code>
   * @param oldParam The source <code>FCSParameter</code>
   * @param prefix The prefix <code>String</code> of the parameter
   * @param suffix The suffix <code>String</code> of the parameter
   * @throws FCSException If the source <code>FCSParameter</code>does
   */
  private void copyAttribute(FCSParameter newParam, FCSParameter oldParam, String prefix, String suffix)
      throws FCSException
  {
    String value = oldParam.getAttribute(prefix, suffix);
    if (value != null)
      newParam.setAttribute(prefix, suffix, value);
  }

  /**
   * Adds a new <code>FCSParameter</code> to the <code>FCSFile</code> with
   * numbered keywords copied from the argument. The keywords <code>$PnB</code>,
   * <code>$PnR</code>, <code>$PnE</code>, <code>$PnG</code>, <code>$PnV</code>,
   * <code>$PnN</code> and <code>$PnS</code> are copied if present.
   *
   * @param oldParam An existing <code>FCSParameter</code> used as a template.
   * @throws FCSException If the FCS version is not supported of the file is malformed
   * @throws IOException If an Input or output exception occurs
   * @return The newly created <code>FCSParameter</code>
   */
  public FCSParameter addParameter(FCSParameter oldParam)
      throws FCSException, IOException
  {
    FCSParameter newParam = addParameter();

    copyAttribute(newParam, oldParam, "$P", "B");
    copyAttribute(newParam, oldParam, "$P", "R");
    copyAttribute(newParam, oldParam, "$P", "E");
    copyAttribute(newParam, oldParam, "$P", "G");
    copyAttribute(newParam, oldParam, "$P", "V");
    copyAttribute(newParam, oldParam, "$P", "N");
    copyAttribute(newParam, oldParam, "$P", "S");

    return newParam;
  }

  /**
   * Gets the <code>FCSParameter</code> corresponding to the supplied index.
   * Note that the FCS standard defineds indecies as 1 origin.
   *
   * @param index The <code>int</code> index of the parameter desired.
   * @throws FCSException If the FCS version is not supported of the file is malformed
   * @throws IndexOutOfBoundsException If the index is less than 1 or greater
   *   than the number of parameters defined in this <code>FCSFile</code>
   * @throws IOException If an Input or output exception occurs
   * @return The <code>FCSParameter</code> at the supplied index.
   */
  public FCSParameter getParameter(
      int index)
      throws FCSException, IndexOutOfBoundsException, IOException
  {
    return (FCSParameter)getParameterList().get(index - 1);
  }

  /**
   * Gets the <code>FCSParameter</code> corresponding to the supplied label. The
   * standard keyword <code>$PnN</code> is used to look up the parameter.
   *
   * @param label The <code>String</code> label of the parameter desired.
   * @throws FCSException If the FCS version is not supported of the file is malformed
   * @throws IOException If an Input or output exception occurs
   * @return The <code>FCSParameter</code> corresponding to the supplied label.
   */
  public FCSParameter getParameter(
      String label)
      throws FCSException, IOException
  {
    Iterator i = getParameterList().iterator();
    while (i.hasNext())
    {
      FCSParameter p = (FCSParameter)i.next();
      if (label.equalsIgnoreCase(p.getAttribute("$P", "N")))
        return p;
    }

    return null;
  }

  /**
   * Gets in <code>FCSHandler</code> suitable for reading data from this
   * <code>FCSFile</code>
   *
   * @throws FCSException If the FCS version is not supported of the file is malformed
   * @throws IOException If an Input or output exception occurs
   * @return An <code>FCSHandler</code> for reading data from this
   *   <code>FCSFile</code>
   */
  public FCSHandler getInputIterator()
      throws FCSException, IOException
  {
    createAccessor();
    String byte_order = getAttribute("$BYTEORD");
    if (byte_order == null || byte_order.length() == 0)
      throw new FCSException("Byte order not specified!");

    random.seek(getDataStart());

    if ("1,2,3,4".equals(byte_order))
      return new LittleEndianHandler(this);
    else if ("4,3,2,1".equals(byte_order))
      return new BigEndianHandler(this);
    else
      throw new FCSException("ancient pdp-11 crud");
  }

  /**
   * Gets an <code>FCSHandler</code> suitable for writing data to this
   * <code>FCSFile</code>
   *
   * @throws FCSException If the FCS version is not supported of the file is malformed
   * @throws IOException If an Input or output exception occurs
   * @return An <code>FCSHandler</code> for writing data to this
   *   <code>FCSFile</code>
   */
  public FCSHandler getOutputIterator()
      throws FCSException, IOException
  {
    createMutator();

    if (Arrays.equals(version, FCS.FCS2))
    {
      getTextSegment().removeAttribute("$BEGINDATA");
      getTextSegment().removeAttribute("$ENDDATA");

      getTextSegment().removeAttribute("$BEGINANALYSIS");
      getTextSegment().removeAttribute("$ENDANALYSIS");

      getTextSegment().removeAttribute("$BEGINSTEXT");
      getTextSegment().removeAttribute("$ENDSTEXT");

      getTextSegment().removeAttribute("$NEXTDATA");
    }
    else if (Arrays.equals(version, FCS.FCS3))
    {
      getTextSegment().setAttribute("$BEGINDATA", "00000000000000000000");
      getTextSegment().setAttribute("$ENDDATA", "00000000000000000000");
      dataStart = 0; // so setters will actually set when called!
      dataEnd = 0;

      getTextSegment().setAttribute("$BEGINANALYSIS", 0);
      getTextSegment().setAttribute("$ENDANALYSIS", 0);

      getTextSegment().setAttribute("$BEGINSTEXT", 0);
      getTextSegment().setAttribute("$ENDSTEXT", 0);

      getTextSegment().setAttribute("$NEXTDATA", 0);
    }
    else
      throw new FCSException("Unspupported FCS version");

    textSegment.setAttribute("$MODE", "L");
    String data_type = textSegment.getAttribute("$DATATYPE");
    if (data_type == null || data_type.length() == 0)
      textSegment.setAttribute("$DATATYPE", "I");
    if ("A".equalsIgnoreCase(data_type))
      throw new FCSException("Ascii data is not supported");
    String byte_order = textSegment.getAttribute("$BYTEORD");
    if (byte_order == null || byte_order.length() == 0)
    {
      byte_order = "1,2,3,4";
      textSegment.setAttribute("$BYTEORD", byte_order);
    }
    textSegment.setAttribute("$FIL", getFile().getName());

    setTextStart(FCS.HEADER_SIZE);
    setTextEnd(textStart + textSegment.size() - 1);

    long align = 1 << getDataAlignment();
    setDataStart((textEnd + align) & ~(align - 1));

    random.seek(dataStart);

    if ("1,2,3,4".equals(byte_order))
      return new LittleEndianHandler(this);
    else if ("4,3,2,1".equals(byte_order))
      return new BigEndianHandler(this);
    else
      throw new FCSException("Ancient pdp-11 crud");
  }

  public void beginEnrichment (FCSFile source, FCSTextSegment enrichment)
      throws FCSException, IOException
  {
    createMutator();

    getTextSegment().readFrom(source.getTextSegment());
    getTextSegment().readFrom(enrichment);

    setTextStart(align(FCS.HEADER_SIZE));
    setDataStart(align(getTextStart() + getTextSegment().size() + 64));
    setDataEnd(getDataStart() + getDataLength() - 1);
  }

  public void copyData (FCSFile source)
      throws FCSException, IOException
  {
    FileChannel inChannel = source.getChannel(false);
    FileChannel outChannel = this.getChannel(true);

    inChannel.position(source.getDataStart());
    outChannel.position(this.getDataStart());

    ByteBuffer buffer = ByteBuffer.allocateDirect(1 << 20);
    long bytesRemaining = this.getDataLength();
    while (bytesRemaining > 0)
    {
      buffer.clear();
      if (buffer.limit() > bytesRemaining)
        buffer.limit((int)bytesRemaining);
      int bytes = inChannel.read(buffer);

      buffer.flip();
      outChannel.write(buffer);
      bytesRemaining -= bytes;
    }
  }

  public void endEnrichment ()
      throws FCSException, IOException
  {
    writeCRC();
  }

  public void enrich (FCSFile source, FCSTextSegment enrichment)
      throws IOException, FCSException
  {
    beginEnrichment(source, enrichment);
    copyData(source);
    endEnrichment();

    source.close();
    this.close();
  }

  /**
   * Gets the <code>long</code> offset to the last <code>byte<code> of the
   * <code>ANALYSIS</code> segment of this <code>FCSFile</code>
   *
   * @throws FCSException If the FCS version is not supported of the file is malformed
   * @throws IOException If an Input or output exception occurs
   * @return The <code>long</code>offset to the last byte of the segment
   */
  public long getAnalysisEnd()
      throws FCSException, IOException
  {
    if (analysisEnd < 0)
      createAccessor();

    if (analysisEnd == 0 && Arrays.equals(version, FCS.FCS3))
    {
      String end = getTextSegment().getAttribute("$ENDANALYSIS");
      if (end != null)
        try
        {
          analysisEnd = Long.parseLong(end.trim());
        }
        catch (NumberFormatException ex)
        {
          throw new FCSException(ex);
        }
    }

    return analysisEnd;
  }

  /**
   * Sets the <code>long</code> offset to the last <code>byte<code> of the
   * <code>ANALYSIS</code> segment of this <code>FCSFile</code>
   *
   * @throws FCSException If the FCS version is not supported of the file is malformed
   * @param analysisEnd long
   */
  public void setAnalysisEnd(long analysisEnd)
      throws FCSException
  {
    if (analysisEnd < 0)
      throw new FCSException("Illegal header offset");

    if (this.analysisEnd == analysisEnd)
      return;

    this.analysisEnd = analysisEnd;
    headerModified = true;
  }

  /**
   * Gets the <code>long</code> offset to the first <code>byte<code> of the
   * <code>ANALYSIS</code> segment of this <code>FCSFile</code>
   *
   * @throws FCSException If the FCS version is not supported of the file is malformed
   * @throws IOException If an Input or output exception occurs
   * @return The <code>long</code>offset to the last byte of the segment
   */
  public long getAnalysisStart()
      throws FCSException, IOException
  {
    if (analysisStart < 0)
      createAccessor();

    if (analysisStart == 0 && Arrays.equals(version, FCS.FCS3))
    {
      String start = getTextSegment().getAttribute("$BEGINANALYSIS");
      if (start != null)
        try
        {
          analysisStart = Long.parseLong(start.trim());
        }
        catch (NumberFormatException ex)
        {
          throw new FCSException(ex);
        }
    }

    return analysisStart;
  }

  /**
   * Sets the <code>long</code> offset to the first <code>byte<code> of the
   * <code>ANALYSIS</code> segment of this <code>FCSFile</code>
   *
   * @throws FCSException If the FCS version is not supported of the file is malformed
   * @param analysisStart long
   */
  public void setAnalysisStart(long analysisStart)
      throws FCSException
  {
    if (analysisStart < 0)
      throw new FCSException("Illegal header offset");

    if (this.analysisStart == analysisStart)
      return;

    this.analysisStart = analysisStart;
    headerModified = true;
  }

  /**
   * Gets the <code>long</code> offset to the last <code>byte<code> of the
   * <code>DATA</code> segment of this <code>FCSFile</code>
   *
   * @throws FCSException If the FCS version is not supported of the file is malformed
   * @throws IOException If an Input or output exception occurs
   * @return The <code>long</code>offset to the last byte of the segment
   */
  public long getDataEnd()
      throws FCSException, IOException
  {
    if (dataEnd < 0)
      createAccessor();

    if (dataEnd == 0 && Arrays.equals(version, FCS.FCS3))
    {
      String end = getTextSegment().getAttribute("$ENDDATA");
      if (end != null)
        try
        {
          dataEnd = Long.parseLong(end.trim());
        }
        catch (NumberFormatException ex)
        {
          throw new FCSException(ex);
        }
    }

    return dataEnd;
  }

  /**
   * Gets the <code>long</code> offset to the first <code>byte<code> of the
   * <code>DATA</code> segment of this <code>FCSFile</code>
   *
   * @throws FCSException If the FCS version is not supported of the file is malformed
   * @throws IOException If an Input or output exception occurs
   * @return The <code>long</code>offset to the last byte of the segment
   */
  public long getDataStart()
      throws FCSException, IOException
  {
    if (dataStart < 0)
      createAccessor();

    if (dataStart == 0 && Arrays.equals(version, FCS.FCS3))
    {
      String start = getTextSegment().getAttribute("$BEGINDATA");
      if (start != null)
        try
        {
          dataStart = Long.parseLong(start.trim());
        }
        catch (NumberFormatException ex)
        {
          throw new FCSException(ex);
        }
    }

    return dataStart;
  }

  public long getDataLength()
      throws IOException, IndexOutOfBoundsException, FCSException
  {
    long bitsPerEvent = 0;
    for (int i = 1, n = getParameters(); i <= n; ++i)
      bitsPerEvent += getParameter(i).getBits();

    return (bitsPerEvent * getTotal() + 7L) / 8L;
  }

  /**
   * Sets the <code>long</code> offset to the last <code>byte<code> of the
   * <code>DATA</code> segment of this <code>FCSFile</code>
   *
   * @throws FCSException If the FCS version is not supported of the file is malformed
   * @param dataEnd long
   */
  public void setDataEnd(long dataEnd)
      throws FCSException
  {
    if (dataEnd < 0)
      throw new FCSException("Illegal header offset");

    if (this.dataEnd == dataEnd)
      return;

    this.dataEnd = dataEnd;
    if (Arrays.equals(version, FCS.FCS3))
      textSegment.setAttribute("$ENDDATA", dataEnd);
    headerModified = true;
    crcModified = true;
  }

  /**
   * Sets the <code>long</code> offset to the first <code>byte<code> of the
   * <code>DATA</code> segment of this <code>FCSFile</code>
   *
   * @throws FCSException If the FCS version is not supported of the file is malformed
   * @param dataStart long
   */
  public void setDataStart(long dataStart)
      throws FCSException
  {
    if (dataStart < 0)
      throw new FCSException("Illegal header offset");

    if (this.dataStart == dataStart)
      return;

    this.dataStart = dataStart;
    if (Arrays.equals(version, FCS.FCS3))
      textSegment.setAttribute("$BEGINDATA", dataStart);
    headerModified = true;
  }

  /**
   * Gets the <code>long</code> offset to the last <code>byte<code> of the
   * <code>TEXT</code> segment of this <code>FCSFile</code>
   *
   * @throws FCSException If the FCS version is not supported of the file is malformed
   * @throws IOException If an Input or output exception occurs
   * @return The <code>long</code>offset to the last byte of the segment
   */
  public long getTextEnd()
      throws FCSException, IOException
  {
    if (textEnd < 0)
      createAccessor();
    return textEnd;
  }

  /**
   * Gets the <code>long</code> offset to the first <code>byte<code> of the
   * <code>TEXT</code> segment of this <code>FCSFile</code>
   *
   * @throws FCSException If the FCS version is not supported of the file is malformed
   * @throws IOException If an Input or output exception occurs
   * @return The <code>long</code>offset to the last byte of the segment
   */
  public long getTextStart()
      throws FCSException, IOException
  {
    if (textStart < 0)
      createAccessor();
    return textStart;
  }

  /**
   * Sets the <code>long</code> offset to the last <code>byte<code> of the
   * <code>TEXT</code> segment of this <code>FCSFile</code>
   *
   * @throws FCSException If the FCS version is not supported of the file is malformed
   * @param textEnd long
   */
  public void setTextEnd(long textEnd)
      throws FCSException
  {
    if (textEnd < 0)
      throw new FCSException("Illegal header offset");

    if (this.textEnd == textEnd)
      return;

    this.textEnd = textEnd;
    headerModified = true;
  }

  /**
   * Sets the <code>long</code> offset to the first <code>byte<code> of the
   * <code>TEXT</code> segment of this <code>FCSFile</code>
   *
   * @throws FCSException If the FCS version is not supported of the file is malformed
   * @param textStart long
   */
  public void setTextStart(long textStart)
      throws FCSException
  {
    if (textStart < 0)
      throw new FCSException("Illegal header offset");

    if (this.textStart == textStart)
      return;

    this.textStart = textStart;
    headerModified = true;
  }

  /**
   * Gets the <code>File</code> underlying this <code>FCSFile</code>
   *
   * @return The <code>File</code> underlying this <code>FCSFile</code>
   */
  public File getFile()
  {
    return file;
  }

  /**
   * Set the <code>File</code> underlying this <code>FCSFile</code>
   *
   * @param file The <code>File</code> to be used when accessing this
   *   <code>FCSFile</code>
   * @throws FCSException If an the FCS version is not supported or the file is
   *   malformed
   * @throws IOException If an Input or output exception occurs
   */
  public void setFile(File file)
      throws FCSException, IOException
  {
    if (!file.equals(this.file) && random != null)
    {
      random.close();
      random = null;
    }

    getTextSegment().setAttribute("$FIL", file.getName());
    this.file = file;
  }

  /**
   * Sets the data alignment for this <code>FCSFile</code>. This represents the
   * power of 2 to which the offset to the data segment will be rounded up when
   * data is written to this <code>FCSFile</code>. For example, the default 8
   * causes the data to start at the next multiple of 2<sup>8</sup> = 256 bytes
   * passed the end of the <code>TEXT</code> segment. Although the FCS standard
   * does not require it many computer archetectures require or perform better
   * if data are aligned on certain <code>byte</code>boundaries.
   * <code>Java</code> in general and this implementation in particular do not
   * care.
   *
   * @param dataAlignment The <code>int</code> power of 2 on which the
   *   <code>DATA</code>segment will be aligned.
   */
  public void setDataAlignment(int dataAlignment)
  {
    this.dataAlignment = dataAlignment;
  }

  /**
   * Gets the <code>int</code> power of 2 to which data are aligned when writting
   * to this <code>FCSFile</code>
   *
   * @return The <code>int</code> power of 2 to which data are aligned for this
   *   <code>FCSFile</code>
   */
  public int getDataAlignment()
  {
    return dataAlignment;
  }

  /**
   * Gets the <code>byte</code> array that encodes the version of this
   * <code>FCSFile</code> in it's header.
   *
   * @throws FCSException If an the FCS version is not supported or the file is
   *   malformed
   * @throws IOException If an Input or output exception occurs
   * @return The <code>byte[]</code> version information from the header of
   *   this <code>FCSFile</code>
   */
  public byte[] getVersion()
      throws FCSException, IOException
  {
    if (version == null)
    {
      if (file.exists())
        createAccessor();
      else
        System.arraycopy(FCS.FCS3, 0, version, 0, FCS.FCS3.length);
    }
    return version;
  }

  /**
   * Sets the version information to be written to the header of this
   * <code>FCSFile</code>. The argument should be one of the constants
   * <code>FCS.FCS2</code> or <code>FCS.FCS3</code>.
   *
   * @param version The <code>byte[]</code> version information for this
   *   <code>FCSFile</code>
   * @throws FCSException If the version supplied is malformed or not supported.
   * @see FCS
   */
  public void setVersion(byte[] version)
      throws FCSException
  {
    if (!Arrays.equals(version, FCS.FCS2) && !Arrays.equals(version, FCS.FCS3))
      throw new FCSException("Unsupported version " + new String(version));

    if (Arrays.equals(this.version, version))
      return;

    this.version = version;
    headerModified = true;
  }
}
