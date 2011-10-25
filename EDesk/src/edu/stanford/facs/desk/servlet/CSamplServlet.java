package edu.stanford.facs.desk.servlet;

import java.io.*;
import java.net.*;
import java.sql.*;

import javax.naming.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.sql.*;

import edu.stanford.facs.desk.*;

/**
 * Title:        EDesk Utilities
 * Description:
 * Copyright:    Copyright (c) 2000 by The Board of Trustees of the Leland Stanford Jr. University
 * Company:      Stanford University
 * @author Wayne A. Moore
 * @version 1.0
 */

public class CSamplServlet
    extends DeskServlet
{
  private DataSource dataSource;
  private String ersHost;
  private int ersPort = 8080;
  private String deskHost;
  private int deskPort = 8080;

  protected Connection getConnection()
      throws SQLException, UnavailableException
  {
    Connection connection = null;

    try
    {
      connection = dataSource.getConnection();
    }
    catch (Exception ignore)
    {
      throw new UnavailableException("Cannot get database connection");
    }

    return connection;
  }

  protected void cleanupDatabase(ResultSet results, Statement statement, Connection connection)
  {
    try
    {
      if (results != null)
        results.close();
    }
    catch (SQLException ignore)
    {}
    try
    {
      if (statement != null)
        statement.close();
    }
    catch (SQLException ignore)
    {}
    try
    {
      if (connection != null)
        connection.close();
    }
    catch (SQLException ignore)
    {}
  }

  /**Process the HTTP Get request*/
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException
  {
    String info = request.getPathInfo();

    StringBuffer sb = getStringBuffer();
    sb.append(eDesk.getSiteName());
    sb.append(" CSampl ");
    sb.append(info);
    log(sb.toString());

    int warehouse = FacsDesk.radix36(info.substring(info.lastIndexOf('/') + 1, info.lastIndexOf('.')));

    if (ersHost != null)
    {
      Connection connection = null;
      PreparedStatement statement = null;
      ResultSet results = null;
      try
      {
        connection = getConnection();

        statement = connection.prepareStatement(
            "SELECT P.USERNAME, P.INSTRUMENT, S.PROTOCOL, S.COORDINATE " +
            "FROM EDESK_PROTOCOLS AS P, EDESK_SAMPLES AS S " +
            "WHERE S.WAREHOUSE = ? AND P.PROTOCOL = S.PROTOCOL");

        statement.setInt(1, warehouse);
        results = statement.executeQuery();
        if (results.next())
        {
          int ac = 1;
          String username = results.getString(ac++);
          String instrument = results.getString(ac++);
          int protocol = results.getInt(ac++);
          String coord = results.getString(ac++);

          StringBuffer path = new StringBuffer(128);
          path.append("/ERS/data");
          path.append("/o=Stanford University/ou=FACS Desk");

          path.append("/uid=");
          path.append(username);

          path.append("/journal=");
          path.append(instrument);

          path.append("/session=");
          path.append(String.valueOf(protocol));

          path.append("/file=");
          path.append(coord);
          path.append(".fcs");

          URL url = new URL("http", ersHost, ersPort, path.toString());

          response.sendRedirect(url.toExternalForm());

          sb = getStringBuffer();
          sb.append("redirecting to ");
          sb.append(url);
          log(sb.toString());

          return;
        }
      }
      catch (Exception ex)
      {
        throw new ServletException(ex);
      }
      finally
      {
        cleanupDatabase(results, statement, connection);
      }
    }

    if (deskHost != null)
    {
      URL url = new URL("http", deskHost, deskPort, request.getRequestURI());

      response.sendRedirect(url.toExternalForm());

      sb = getStringBuffer();
      sb.append("redirecting to ");
      sb.append(url);
      log(sb.toString());

      return;
    }
    else
    {
      File cache = eDesk.getCacheData(warehouse);
      if (cache.exists())
      {
        response.setContentType(CSAMPL_TYPE);
        sendFile(cache, response);
      }
      else
      {
        response.sendError(response.SC_NOT_FOUND);
        String user = request.getRemoteUser();
        eDesk.warehouseFault(warehouse, user);
        return;
      }
    }
//    {
//      String host = eDesk.getProperty("ftp.host");
//      String username = eDesk.getProperty("ftp.user");
//      String password = eDesk.getProperty("ftp.password");
//      File tmp = eDesk.getSiteFile("CSampl.TMP");
//      FtpClient ftp = new FtpClient();
//      InputStream in;
//      try
//      {
//        ftp.openServer(host);
//        ftp.login(username, password);
//        ftp.binary();
//        try
//        {
//          in = ftp.get("ED_FTP:[" + ((warehouse / 36) % 245) + "]"
//              + FacsDesk.radix36(warehouse) + ".ED_WH");
//        }
//        catch (FileNotFoundException fnfe)
//        {
//          ftp.closeServer();
//          ftp.openServer(host);
//          ftp.login(username, password);
//          ftp.binary();
//          in = ftp.get(FacsDesk.warehouse(warehouse));
//        }
//        int total = 0;
//        byte[] buffer;
//        if (response.getBufferSize() == 0)
//          buffer = new byte[DEFAULT_BUFFER_SIZE];
//        else
//          buffer = new byte[response.getBufferSize()];
//
//        OutputStream fos = new FileOutputStream(tmp);
//        OutputStream out = response.getOutputStream();
//        response.setContentType(CSAMPL_TYPE);
//        try
//        {
//          for (; ; )
//          {
//            int n = in.read(buffer);
//            if (n < 0)
//              break;
//            out.write(buffer, 0, n);
//            fos.write(buffer, 0, n);
//            total += n;
//          }
//        }
//        catch (IOException ioe)
//        {
//          ioe.printStackTrace();
//        }
//        finally
//        {
//          in.close();
//          fos.close();
//        }
//        if (!cache.getParentFile().exists())
//          cache.getParentFile().mkdirs();
//        tmp.renameTo(cache);
//      }
//      catch (FileNotFoundException fnfe)
//      {
//        response.sendError(response.SC_NOT_FOUND);
//        String user = request.getRemoteUser();
//        eDesk.warehouseFault(warehouse, user);
//        return;
//      }
//      finally
//      {
//        try
//        {
//          ftp.closeServer();
//        }
//        catch (IOException ex)
//        {
//          ex.printStackTrace();
//        }
//      }
//    }
  }

  public void init()
      throws ServletException
  {
    super.init();

    try
    {
      Context env = (Context)new InitialContext().lookup("java:comp/env");

      try
      {
        dataSource = (DataSource)env.lookup("jdbc/EDeskMetadata");
      }
      catch (NamingException ignore)
      {
        dataSource = null;
      }

      try
      {
        ersHost = (String)env.lookup("ers/Host");
      }
      catch (NamingException ignore)
      {
        ersHost = null;
      }

      try
      {
        ersPort = ((Integer)env.lookup("ers/Port")).intValue();
      }
      catch (NamingException ne)
      {
        ersPort = 8080;
      }

      try
      {
        deskHost = (String)env.lookup("desk/Host");
      }
      catch (NamingException ignore)
      {
        deskHost = null;
      }

      try
      {
        deskPort = ((Integer)env.lookup("desk/Port")).intValue();
      }
      catch (NamingException ne)
      {
        deskPort = 8080;
      }
    }
    catch (Exception ex)
    {
      throw new ServletException(ex);
    }
  }
}
