package edu.stanford.facs.desk.servlet;

import java.io.*;
import java.sql.*;
import javax.naming.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.sql.*;
import java.net.URL;
import edu.stanford.facs.desk.FacsDesk;

public class RedirectServlet
    extends HttpServlet
{
  private static ThreadLocal threadStringBuffer = new ThreadLocal();

  private DataSource dataSource;
  private String redirectHost;
  private String jdbcDriver;
  private String jdbcURL;
  private String jdbcUsername;
  private String jdbcPassword;

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

  protected Connection getConnection()
      throws SQLException, UnavailableException
  {
    Connection connection = null;
    if (dataSource != null)
    {
      try
      {
        connection = dataSource.getConnection();
      }
      catch (Exception ignore)
      {
        dataSource = null;
      }
    }

    if (connection == null && jdbcURL != null)
      connection = DriverManager.getConnection(jdbcURL, jdbcUsername, jdbcPassword);

    if (connection == null)
      throw new UnavailableException("Cannot get database connection");

    connection.setAutoCommit(false);
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

  //Initialize global variables
  public void init ()
      throws ServletException
  {
    try
    {
      Context env = (Context)new InitialContext().lookup("java:comp/env");

      try
      {
        redirectHost = (String)env.lookup("redirect/Host");
      }
      catch (Exception ignore)
      {}
      if (redirectHost == null)
        redirectHost = "FACS.Stanford.EDU";

      try
      {
        dataSource = (DataSource)env.lookup("jdbc/EDeskMetadata");
      }
      catch (Exception ignore)
      {}

      jdbcDriver = getInitParameter("jdbcDriver");
      if (jdbcDriver != null)
        Class.forName(jdbcDriver);
      jdbcURL = getInitParameter("jdbcURL");
      jdbcUsername = getInitParameter("jdbcUsername");
      jdbcPassword = getInitParameter("jdbcPassword");
    }
    catch (Exception ex)
    {
      throw new ServletException(ex);
    }
  }

  //Process the HTTP Get request
  public void doGet (HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException
  {
    String info = request.getPathInfo();

    StringBuffer sb = getStringBuffer();
    sb.append("Darwin");
    sb.append(" CSampl ");
    sb.append(info);
    log(sb.toString());

    int warehouse = FacsDesk.radix36(info.substring(info.lastIndexOf('/') + 1,
        info.lastIndexOf('.')));

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

        StringBuffer url = new StringBuffer(128);
        url.append("/ERS");
        url.append("/o=Stanford University/ou=FACS Desk");

        url.append("/uid=");
        url.append(username);

        url.append("/journal=");
        url.append(instrument);

        url.append("/session=");
        url.append(String.valueOf(protocol));

        url.append("/file=");
        url.append(coord);
        url.append(".fcs");

        response.sendRedirect(url.toString());

        sb = getStringBuffer();
        sb.append("redirecting to ");
        sb.append(url);
        log(sb.toString());

        return;
      }
      else
      {
        URL url = new URL("http", redirectHost, 8080, "/EDesk/Darwin/CSampl" + info);
        System.out.println(url.toExternalForm());

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

  //Clean up resources
  public void destroy ()
  {
  }
}
