package edu.stanford.facs.flowjo;

import java.io.*;

import javax.activation.*;

import java.awt.datatransfer.*;

class FlowJoSummaryHandler
    extends DataHandler
{
  public FlowJoSummaryHandler(
      DataSource source)
  {
    super(source);
  }

  public Object getContent(
      DataSource source)
  {
    try
    {
      Reader r = new InputStreamReader(source.getInputStream());
      char[] cb = new char[1024];
      StringBuffer sb = new StringBuffer();
      for (; ; )
      {
        int n = r.read(cb);
        if (n < 0)
          break;
        sb.append(cb, 0, n);
      }
      return sb.toString();
    }
    catch (Exception ex)
    {}

    return null;
  }

  public Object getTransferData(
      DataFlavor flavor,
      DataSource source)
  {
    try
    {
      if (flavor.getRepresentationClass().equals(Class.forName(
          "java.lang.String")))
        return getContent(source);
      else if (flavor.getRepresentationClass().equals(Class.forName(
          "java.io.InputStream")))
        return source.getInputStream();
    }
    catch (IOException ioe)
    {}
    catch (ClassNotFoundException cnf)
    {}

    return null;
  }

  public void writeTo(
      Object obj,
      String mimeType,
      OutputStream os)
      throws IOException
  {
    if (obj instanceof String)
    {
      Writer w = new OutputStreamWriter(os);
      w.write((String)obj);
      w.flush();
    }
    else if (obj instanceof InputStream)
    {
      InputStream is = (InputStream)obj;
      byte[] buf = new byte[4096];
      for (; ; )
      {
        int n = is.read(buf);
        if (n < 0)
          break;
        os.write(buf, 0, n);
      }
      os.flush();
    }
    else if (obj instanceof Reader)
    {
      Reader r = (Reader)obj;
      Writer w = new OutputStreamWriter(os);
      char[] buf = new char[4096];
      for (; ; )
      {
        int n = r.read(buf);
        if (n < 0)
          break;
        w.write(buf, 0, n);
      }
      w.flush();
    }
    else if (obj instanceof byte[])
    {
      os.write((byte[])obj);
    }
    else
      throw new java.lang.IllegalArgumentException("writeTo( "
          + obj.getClass().getName() + " )");
  }
}
