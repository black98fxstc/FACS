<%@ page import="sun.net.ftp.*,edu.stanford.facs.desk.*,edu.stanford.facs.flowjo.*,java.io.*,java.util.*,javax.mail.*,javax.mail.internet.*" %>
<%!
  FacsDesk  eDesk;
  static Session  mail;
%>
<%
  String  username = "";
  String  password = "";
  String  version = null;
  int  protocol = -1;

  if (eDesk == null)
  {
    eDesk = new FacsDesk( getServletContext().getInitParameter( "edesk.root" ), getInitParameter( "edesk.site" ) );
//    eDesk = new FacsDesk( "/var/EDesk", "Darwin" );
  }
  synchronized (this.getClass())
  {
    if (mail == null)
    {
      Properties  props = new Properties();
      props.put( "mail.smtp.host", "smtp.stanford.edu" );
      props.put( "mail.smtp.sendpartial", "false" );
      mail = Session.getInstance( props, null );
//      mail.setDebug( true );
    }
  }
%>
<html>
<head>
<title>
ExportNotify
</title>
</head>
<body>
<center>
<h2>Sending e-Mail Notification</h2>
<table width="80%" border="1">
<%
  String[]  recipient = new String[4];
  List  addresses = new ArrayList();
  DataDrawer  drawer = null;
  String  error = null;

  boolean  nothingHappened = true;
  boolean  errorsOccured = false;

  if ("Send e-Mail To".equalsIgnoreCase( request.getParameter( "Submit" ) ))
  {
    username = request.getParameter( "Username" );
    password = request.getParameter( "Password" );
    version = request.getParameter( "FlowJo" );
    protocol = Integer.parseInt( request.getParameter( "Protocol" ) );
    File  cache = eDesk.getCacheProtocol( protocol );
    drawer = DataDrawer.parse( cache );

    FlowJoSummaryFile  summary;
    if (version != null && "V3".equals( version ))
      summary = FlowJo.getExportFile (
          DataDrawer.parse( cache ), eDesk.getProperty( "ftp.host" ) );
    else
      summary = FlowJo.getSummaryFile (
          DataDrawer.parse( cache ), eDesk.getProperty( "http.host" ) );

    String  index = drawer.index();

    String  subject = drawer.title + "  " + drawer.cytometer + " " + drawer.protocol;

    for (int  i = 0;  i < recipient.length;  ++i)
    {
      String  mailbox = request.getParameter( "Recipient" + i );
      if (mailbox != null && mailbox.length() > 0)
        addresses.add( new InternetAddress( mailbox ) );
    }

    synchronized (mail)
    {
      MimeBodyPart  html = new MimeBodyPart();
      html.setContent( index, "text/html" );

      MimeBodyPart  flowjo = new MimeBodyPart();
      flowjo.setDataHandler( summary.getDataHandler() );
      flowjo.setHeader("Content-Transfer-Encoding", "base64");
      flowjo.setDisposition( "attachment; filename=\"" + subject + ".jo\"" );

      MimeMultipart  multi = new MimeMultipart();
      multi.addBodyPart( html );
      multi.addBodyPart( flowjo );

      MimeMessage  msg = new MimeMessage( mail );
      msg.setFrom( new InternetAddress( drawer.cytometer + "@FACS.Stanford.EDU", drawer.cytometer ) );
      msg.setSubject( subject );
      msg.setContent( multi );

      Iterator  ia = addresses.iterator();
      while (ia.hasNext())
        msg.addRecipient( Message.RecipientType.TO , (InternetAddress) ia.next() );

      ia = addresses.iterator();
      InternetAddress[]  addr = new InternetAddress[1];
      Transport  smtp = mail.getTransport( "smtp" );
      smtp.connect();
      while (ia.hasNext())
      {
        error = null;
        addr[0] = (InternetAddress) ia.next();
        try
        {
          smtp.sendMessage( msg, addr );
          nothingHappened = false;
        }
        catch (MessagingException me)
        {
          error = me.getMessage();
          errorsOccured = true;
          log("e-mail error", me);
        }
%>
<%
        if (error == null)
        {
%>
<tr><th width="25%"><%= addr[0].toString() %></th><td style="color:green">Success</td></tr>
<%
        }
        else
        {
%>
<tr><th width="25%"><%= addr[0].toString() %></th><td style="color:red"><%= error %></td></tr>
<%
        }
      }
      smtp.close();
    }
%>
</table>
<%
    if (nothingHappened)
    {
%>
<h2 style="color:red">But nothing happened!</h2>
<%
    }
    else if (errorsOccured)
    {
%>
<h2 style="color:red">Some messages could not be sent.</h2>
<%
    }
    if (nothingHappened || error != null)
    {
%>
<h2>Would you like to try again?</h2>
<form method="post">
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
<td><input type="text" size="30" name="Recipient0" value=""></td>
<td><input type="text" size="30" name="Recipient1" value=""></td>
</tr>
<tr>
<td><input type="text" size="30" name="Recipient2" value=""></td>
<td><input type="text" size="30" name="Recipient3" value=""></td>
</tr>
<%
    }
  }
%>
</table>
</form>

<p>
<form action="Export.jsp" method="post">
<input type="hidden" name="Username" value="<%= username %>">
<input type="hidden" name="Password" value="<%= password %>">
<input type="hidden" name="Protocol" value="<%= protocol %>">
<input type="submit" name="Submit" value="Export Another Experiment">
</form>
</p>

</center>
</body>
</html>
