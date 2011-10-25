package edu.stanford.facs.desk.servlet;

import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.isac.fcs.*;
import edu.stanford.facs.desk.*;
import edu.stanford.facs.flowjo.*;

//import org.isac.*;

/**
 * Title:        EDesk Utilities
 * Description:
 * Copyright:    Copyright (c) 2000 by The Board of Trustees of the Leland Stanford Jr. University
 * Company:      Stanford University
 * @author Wayne A. Moore
 * @version 1.0
 */

public class CSdataServlet
extends DeskServlet
{

  /**Process the HTTP Get request*/
  public void doGet (
    HttpServletRequest request,
    HttpServletResponse response )
    throws ServletException, IOException
  {
    String  info = request.getPathInfo();

    StringBuffer sb = getStringBuffer();
    sb.append(eDesk.getSiteName());
    sb.append(" CSdata ");
    sb.append(info);
    log(sb.toString());

    String  extension;
    int  protocol;
    try
    {
      int  pos = info.lastIndexOf( '.' );
      if (pos < 0)
        extension = "";
      else
        extension = info.substring( pos ).toLowerCase();
      protocol = Integer.parseInt( info.substring( info.lastIndexOf( '/' ) + 1, pos ) );
    }
    catch (Exception ex)
    {
      response.sendError( response.SC_BAD_REQUEST );
      return;
    }
    File  cache = eDesk.getCacheProtocol( protocol );
    if (!cache.exists())
    {
      response.sendError( response.SC_NOT_FOUND );
      return;
    }
    else if (extension.equals( ".jo" ) || extension.equals( ".flowjo" ))
    {
      try
      {
        String version = request.getParameter("FlowJo");
        FlowJoSummaryFile summary;
        if (version != null && "V3".equals(version))
          summary = FlowJo.getExportFile(
              DataDrawer.parse(cache), eDesk.getProperty("ftp.host"));
        else
          summary = FlowJo.getSummaryFile(
              DataDrawer.parse(cache), eDesk.getProperty("http.host"));
        response.setContentType(summary.getContentType());
        summary.getDataHandler().writeTo(response.getOutputStream());
      }
      catch (FCSException ex)
      {
        throw new ServletException(ex);
      }
    }
    else if (extension.equals( ".html" ))
    {
      response.setContentType( HTML_TYPE );
      DataDrawer.parse( cache ).index( response.getWriter() );
    }
    else
    {
      response.setContentType( CSDATA_TYPE );
      sendFile( cache, response );
    }
  }
}
