package edu.stanford.facs.desk.servlet;

import java.io.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

/**
 * <p>Title: EDesk Utilities</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2000 by The Board of Trustees of the Leland Stanford Jr. University</p>
 * <p>Company: Stanford University</p>
 * @author Wayne A. Moore
 * @version 1.0
 */

public class FlowJoLaunchServlet
extends HttpServlet
{
  //Initialize global variables
  public void init() throws ServletException
  {
  }
  //Process the HTTP Get request
  public void doGet (
      HttpServletRequest request,
      HttpServletResponse response )
  throws ServletException, IOException
  {
    StringTokenizer  st = new StringTokenizer( request.getPathInfo(), "/" );
    String  site = st.nextToken();
    String  protocol = st.nextToken();

    String  uri = "/"+site+"/CSdata/"+protocol+".jo";
    String  version = request.getParameter( "FlowJo" );
    if (version != null)
      uri += "?FlowJo=" + version;

    getServletContext().getRequestDispatcher( uri ).forward( request, response );
  }
  //Clean up resources
  public void destroy()
  {
  }
}
