<%@ page contentType="text/html; charset=UTF-8" import="sun.net.ftp.*,edu.stanford.facs.desk.*,java.io.*,java.util.*,javax.mail.*,javax.mail.internet.*" %>
<%!
  FacsDesk  eDesk;
  static Session  mail;
%>
<%
  String  username = "";
  String  password = "";
  String  exported = "";
  DataDrawer  drawer = null;
  int  protocol = 0;
  String  error = null;
  String  version = null;
  String  launch = null;
  String  tapes = null;

  if (eDesk == null)
  {
    eDesk = new FacsDesk( getServletContext().getInitParameter( "edesk.root" ), getInitParameter( "edesk.site" ) );
//    eDesk = new FacsDesk( "/var/EDesk", "Darwin" );
  }
  username = request.getParameter( "Username" );
  if (username == null)
    username = "";
  password = request.getParameter( "Password" );
  if (password == null)
    password = "";
  if ("Import".equalsIgnoreCase( request.getParameter( "Submit" ) ))
  {
    exported = request.getParameter( "Exported" );
    if (exported == null || exported.length() == 0)
      error = "You must enter a protocol number or export name.";
    else if (username == null || username.length() == 0)
      error = "You must enter your VAX username.";
    else if (password == null || password.length() == 0)
      error = "You must enter your VAX password.";

    File  ex = eDesk.getSiteFile( "EXPORT.TMP" );

    if (error == null)
    {
      FileOutputStream  fos = new FileOutputStream( ex );
      FtpClient  ftp = new FtpClient();
      try
      {
        ftp.openServer( eDesk.getProperty( "ftp.host" ) );
	ftp.login( username, password );
	ftp.binary();

	byte[]  buffer = new byte[8192];
	InputStream  is = null;
        int  dot = exported.lastIndexOf( '.' );
        try
        {
          if (dot < 0)
            is = ftp.get( exported + ".DAT" );
          else
            is = ftp.get( exported );
        }
        catch (FileNotFoundException fnfe)
        {
          ftp.closeServer();
          ftp.openServer( eDesk.getProperty( "ftp.host" ) );
          ftp.login( username, password );
          ftp.binary();
          if (dot < 0)
            is = ftp.get( exported + ".ED_EXPORT" );
          else
            is = ftp.get( exported.substring( 0, dot ) + ".ED_EXPORT" );
          version = "V3";
        }
	int  total = 0;
        try
        {
          for (;;)
          {
            int  n = is.read( buffer );
            if (n < 0)
              break;
            fos.write( buffer, 0, n );
            total += n;
          }
        }
        catch (IOException ioe)
        {
          error = "FTP data exception";
          log("ftp data error", ioe);
        }
        finally
        {
          is.close();
        }
      }
      catch (IOException  ioe)
      {
        if (ioe instanceof FtpLoginException)
        {
          error = "VAX login failed";
          password = null;
        }
        else if (ioe instanceof FileNotFoundException)
          error = "FACS/DESK export file "+exported+" not found";
        else
        {
          error = "FTP server exception";
          log("ftp server error", ioe);
        }
      }
      finally
      {
	ftp.closeServer();
      }
      fos.close();
    }

    if (error == null)
    {
      drawer = DataDrawer.parse( ex );
      protocol = drawer.protocol;
      File  cache = eDesk.getCacheProtocol( protocol );
      if (cache.exists())
	ex.delete();
      else
      {
        if (!cache.getParentFile().exists())
          cache.getParentFile().mkdirs();
        ex.renameTo( cache );
      }

      int[]  tapeIndex = (int[]) eDesk.getSiteObject( eDesk.TAPE_INDEX );
      List  requests = new ArrayList();
      for (int  i = 0;  i < drawer.sample.length;  ++i)
        if (!eDesk.getCacheData( drawer.sample[i].warehouse ).exists())
        {
          eDesk.warehouseFault( drawer.sample[i].warehouse, username );
          for (int  tape = 0;  tape < tapeIndex.length;  ++tape)
          {
            if (tapeIndex[tape] == 0)
              continue;
            else if (drawer.sample[i].warehouse <= tapeIndex[tape])
            {
              Integer  volume = new Integer( tape );
              if (!requests.contains( volume ))
                requests.add( volume );
              break;
            }
          }
        }

      if (!requests.isEmpty())
      {
        Iterator  i = requests.iterator();
        tapes = String.valueOf((Integer) i.next());
        while (i.hasNext())
          tapes += ", " + String.valueOf((Integer) i.next());
      }

      launch = "/EDesk/FlowJo/"+eDesk.getProperty("name")+"/"+protocol+"/"+drawer.title+"  "+drawer.cytometer+" "+drawer.protocol+".jo";
      if (version != null)
        launch += "?FlowJo="+version;
    }
  }

  if ("Clear".equalsIgnoreCase( request.getParameter( "Submit" ) ))
  {
    username = "";
    password = "";
    exported = "";
  }

  if (tapes != null)
  {
    synchronized (this.getClass())
    {
      if (mail == null)
      {
        Properties  props = new Properties();
        props.put( "mail.smtp.host", "smtp.stanford.edu" );
        props.put( "mail.smtp.sendpartial", "true" );
        mail = Session.getInstance( props, null );
//        mail.setDebug( true );
      }
    }

    synchronized (mail)
    {
      MimeMessage  msg = new MimeMessage( mail );
      msg.setFrom( new InternetAddress( "warehouse@facsdata.stanford.edu", "FACS Data Warehouse" ) );
      msg.setSubject( "User "+username.toUpperCase()+" requests tape "+tapes );
      msg.setContent( "\nUser "+username.toUpperCase()+" requests tape "+tapes + "\n", "text/plain" );

      msg.addRecipient( Message.RecipientType.TO , new InternetAddress( "tolmasof" ) );
      msg.addRecipient( Message.RecipientType.TO , new InternetAddress( "prop" ) );
      msg.addRecipient( Message.RecipientType.TO , new InternetAddress( "wmoore" ) );

      Transport  smtp = mail.getTransport( "smtp" );
      smtp.connect();
      smtp.sendMessage( msg, msg.getRecipients( Message.RecipientType.TO ) );
      smtp.close();
    }
  }
%>
<html>
<head>
<title>
Export
</title>
</head>
<body>
<center>
<h2>
Export FACS data from the VAXen.
</h2>
</center>
<h3>First - Export your data from FACS/DESK</h3>
<p>
Connect to Beadle.Stanford.EDU.
Start FACS/DESK and position the cursor on the data drawer you wish to export.
Press
<blockquote>[Enter][PF4][Space][Return]</blockquote>
You will be prompted for an export file name.
The default is the unique protocol number.
You may safely repeat this step as long as you use unique export names.
</p>
<p>
Exit FACS/DESK.
</p>

<h3>Second - Import your data to the FACSDATA server</h3>
<p>
Enter the export file name used in the first step and your VAX username and password then select "Import".
</p>
<center>
<form method="post">
<table>
<tr><th>Desk Export&nbsp;</th><td><input type="text" name="Exported" value="<%= exported %>"></td></tr>
<tr><th>VAX Username</th><td><input type="text" name="Username" value="<%= username %>"></td></tr>
<tr><th>VAX Password</th><td><input type="password" name="Password" value="<%= password %>"></td></tr>
</table>
<table>
<tr><td><input type="submit" name="Submit" value="Import"></td><td><input type="submit" name="Submit" value="Clear"></td></tr>
</table>
</form>
</center>
<%
  if (error != null)
  {
%>
<center>
<h2 style="color:red"><%= error %></h2>
</center>
<%
  }
%>
<%
  if (launch != null)
  {
%>
<center>
<h2 style="color:green">Data imported successfully.</h2>
</center>
<%
  }
%>
<%
  if (tapes != null)
  {
%>
<center>
<h3 style="color:blue">Some or all of the data is currently offline<br>on warehouse tape <%= tapes %>.</h3>
</center>
<%
  }
%>
<h3>Third - Create a <b>FlowJo</b> workspace</h3>
<p>
You will get a Summary File that can be dropped on an empty <b>FlowJo</b> workspace to initialize it.
You can save this file on the local disk or in your AFS directory or you can have it e-mailed to yourself and/or a collegue.
</p>
<!--
<p>
If you are using a <b>Macintosh</b> computer running <b>OS/X</b> and <b>FlowJo</b> and your browser is <a href="#browser">configured properly</a>
you can launch <b>FlowJo</b> directly from this page.
</p><p>
If you use a <b>Macintosh</b> e-mail program you can send yourself a <b>FlowJo</b> summary file
that also can be used to create a <b>FlowJo</b> workspace.
</p><p>
If you using <b>Windows</b> or <b>Unix</b> you must retrieve the <b>FlowJo</b> summary file using one of the
methods above and save the result as a file.
You must then transfer it to a suitable <b>Macintosh</b> and use it to launch <b>FlowJo</b>.
One simple way to do this is to save it to your personal <b>AFS</b> directory, which can be mounted
on <b>Windows</b>, <b>Unix</b> and <b>Macintosh</b> systems.
See below on how to <a href="#afs">configure <b>AFS</b></a>.
</p>
-->
<%
  if (launch != null)
  {
%>
<center>
<p>
<font size="+2"><b>FlowJo</b> Summary File for <a href="<%= launch %>"><code><b><%= drawer.title %>  <%= drawer.cytometer %> <%= drawer.protocol %></b></code></a></font>
</p>
<p>
<form action="ExportNotify.jsp" method="post">
<input type="hidden" name="Username" value="<%= username %>">
<input type="hidden" name="Password" value="<%= password %>">
<input type="hidden" name="Protocol" value="<%= protocol %>">
<%
    if (version != null)
    {
%>
<input type="hidden" name="FlowJo" value="<%= version %>">
<%
    }
%>
<table>
<tr>
<td rowspan="2"><input type="submit" name="Submit" value="Send e-Mail To"></td>
<td><input type="text" size="30" name="Recipient0" value="<%= username %>"></td>
<td><input type="text" size="30" name="Recipient1" value=""></td>
</tr>
<tr>
<td><input type="text" size="30" name="Recipient2" value=""></td>
<td><input type="text" size="30" name="Recipient3" value=""></td>
</tr>
</table>
</form>
</p>
</center>
<%
  }
%>
<%
  if (tapes != null)
  {
%>
<p>
The facility staff have been notified to load the required tapes.
You can go ahead and create a <b>FlowJo</b> workspace at this time if you wish.
<b>FlowJo</b> will report "Error 404" (which means "file not found") until the data is restored from tape.
</p>
<%
  }
%>
</body>
</html>
