package edu.stanford.facs.flowjo;

import java.io.*;

import javax.activation.*;

import org.isac.fcs.*;

public class FlowJoSummaryFile
    implements DataSource
{
  public static final String MIME_TYPE = "application/x-flowjo";
  public static final String DEFAULT_NAME = "FlowJo Summary File";

  public final static class Version
  {
    byte[] HEADER;
    byte[] TERMINATOR;

    public Version(
        String header,
        String terminator)
    {
      this.HEADER = header.getBytes();
      this.TERMINATOR = terminator.getBytes();
    }
  }

  public final static Version V3 = new Version("EXPORT_II V1.0", "\r\n");
  public final static Version V4 = new Version("SUMMARY_FILE 1", "\r");

  byte[] summary;
  FCSTextSegment[] headers;
  String[] urls;
  File file;
  String name;

  public FlowJoSummaryFile(
      String[] urls,
      FCSTextSegment[] headers)
  {
    this(urls, headers, DEFAULT_NAME, V4);
  }

  public FlowJoSummaryFile(
      String[] urls,
      FCSTextSegment[] headers,
      String name)
  {
    this(urls, headers, name, V4);
  }

  public FlowJoSummaryFile(
      String[] urls,
      FCSTextSegment[] headers,
      String name,
      Version version)
  {
    this.urls = urls;
    this.headers = headers;
    this.name = name;
    summary = flowjoSummaryFile(version, urls, headers);
  }

  public DataHandler getDataHandler()
  {
    return new FlowJoSummaryHandler(this);
  }

  byte[] flowjoSummaryFile(
      Version version,
      String[] urls,
      FCSTextSegment[] headers)
  {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try
    {
      flowjoSummaryFile(version, urls, headers, baos);
      baos.close();
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
    }

    return baos.toByteArray();
  }

  void flowjoSummaryFile(
      Version version,
      String[] urls,
      FCSTextSegment[] headers,
      OutputStream out)
  {
    if (headers.length != urls.length)
      throw new IllegalArgumentException(
          "FlowJoSummaryFile.flowJoSummaryFile(): array lengths differ");
    try
    {
      out.write(version.HEADER);
      out.write(version.TERMINATOR);
      for (int i = 0; i < headers.length; ++i)
      {
        out.write(urls[i].getBytes());
        out.write(version.TERMINATOR);

        headers[i].writeTo(out);
        out.write(version.TERMINATOR);
      }
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
    }
  }

  public InputStream getInputStream()
      throws java.io.IOException
  {
    return new ByteArrayInputStream(summary);
  }

  public OutputStream getOutputStream()
      throws java.io.IOException
  {
    throw new java.lang.UnsupportedOperationException(
        "Method getOutputStream() not implemented.");
  }

  public String getContentType()
  {
    return MIME_TYPE;
  }

  public String getName()
  {
    return name;
  }
}
