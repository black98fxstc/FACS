package edu.stanford.facs.desk.servlet;

import java.io.*;
import java.net.*;

import javax.servlet.*;
import javax.servlet.http.*;

import edu.stanford.facs.desk.*;

/**
 * Title:        EDesk Utilities
 * Description:
 * Copyright:    Copyright (c) 2000 by The Board of Trustees of the Leland Stanford Jr. University
 * Company:      Stanford University
 * @author Wayne A. Moore
 * @version 1.0
 */

public class DeskServlet
extends HttpServlet
{
  public static final int  DEFAULT_BUFFER_SIZE = 2048;
  public static final String CSDATA_TYPE = "text/plain";
  public static final String HTML_TYPE = "text/html";
  public static final String CSAMPL_TYPE = "application/octetstream";
  public static final String FLOWJO_TYPE = "application/x-flowjo";

  protected static FacsDesk  eDesk;

  private static ThreadLocal threadStringBuffer = new ThreadLocal();

  protected StringBuffer getStringBuffer()
  {
    StringBuffer stringBuffer = (StringBuffer)threadStringBuffer.get();

    if (stringBuffer == null)
    {
      stringBuffer = new StringBuffer(128);
      threadStringBuffer.set(stringBuffer);
    }
    stringBuffer.setLength(0);

    return stringBuffer;
  }

  protected void  sendFile (
    File  file,
    HttpServletResponse  response )
  throws IOException
  {
    byte[]  buffer;
    if (response.getBufferSize() == 0)
      buffer = new byte[ DEFAULT_BUFFER_SIZE ];
    else
      buffer = new byte[ response.getBufferSize() ];

    response.setContentLength( (int) file.length() );
    OutputStream  out = response.getOutputStream();
    InputStream  in = new FileInputStream( file );
    try
    {
      for (;;)
      {
        int  n = in.read( buffer );
        if (n < 0)
          break;
        out.write( buffer, 0, n );
      }
    }
    catch (IOException ioe)
    {
      if (!(ioe instanceof SocketException))
        throw  ioe;
    }
    finally
    {
      in.close();
    }
  }

  public void init ()
  throws ServletException
  {
    try
    {
      eDesk = new FacsDesk( (String) getServletContext().getInitParameter( "edesk.root" ),
                            (String) getInitParameter( "edesk.site" ) );
    }
    catch (DeskException  de)
    {
      throw  new ServletException( de );
    }
  }
}
