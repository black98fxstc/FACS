<%@ page contentType="text/html; charset=UTF-8" import="edu.stanford.facs.desk.*,java.io.*,java.util.*" %>
<%!
  FacsDesk  eDesk;
%>
<%
  String  exported = "";
  File  cache = null;
  DataDrawer  drawer = null;
  int  protocol = 0;
  String  error = null;
  int  killed = 0;

  if (eDesk == null)
  {
    eDesk = new FacsDesk( getServletContext().getInitParameter( "edesk.root" ), getInitParameter( "edesk.site" ) );
//    eDesk = new FacsDesk( "/var/EDesk", "Darwin" );
  }

  if ("Submit".equalsIgnoreCase( request.getParameter( "Submit" ) ))
  {
    exported = request.getParameter( "Exported" );
    if (exported == null || exported.length() == 0)
      error = "You must enter a protocol number.";

    if (error == null)
    {
      try
      {
        protocol = Integer.parseInt( exported );
      }
      catch (NumberFormatException ex)
      {
        error = "Invalid protocol number.";
      }
    }

    if (error == null)
    {
      cache = eDesk.getCacheProtocol( protocol );
      if (!cache.exists())
        error = "Protocol has not been exported.";
    }

    if (error == null)
    {
      drawer = DataDrawer.parse( cache );
      for (int  i = 0;  i < drawer.sample.length;  ++i)
      {
        FacsSample  sample = drawer.sample[i];
        File  data = eDesk.getCacheData( sample.warehouse );
        if (!data.exists())
          continue;
        long  file_len = data.length() * 8;
        long  event_len = 0;
        for (int  j = 0;  j < sample.channel.length;  ++j)
          event_len += drawer.sample[i].channel[j].bits;
        long  bits_len = 32 + sample.channel.length * 16 + sample.events * event_len;
        if (file_len >= bits_len)
          continue;
        String  fn = data.getName();
        fn = fn.substring( 0, fn.lastIndexOf( '.' ) );
        data.renameTo( new File( data.getParent(), fn + ".ED_SHORT" ) );
        ++killed;
      }

      if (killed == 0)
        error = "No truncated files found.";
    }
  }
%>
<html>
<head>
<title>
StarKiller
</title>
</head>
<body>
<center>
<h2>
Fix the "stars in your data".
</h2>
</center>
<center>
<form method="post">
<table>
<tr><th>Desk Protocol&nbsp;</th><td><input type="text" name="Exported" value="<%= exported %>"></td></tr>
</table>
<table>
<tr><td><input type="submit" name="Submit" value="Submit"></td><td><input type="submit" name="Submit" value="Clear"></td></tr>
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
  if (killed > 0 )
  {
%>
<center>
<h2 style="color:green"><%= killed %> truncated files removed.</h2>
</center>
<%
  }
%>
</body>
</html>
