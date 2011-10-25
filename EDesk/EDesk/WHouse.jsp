<%@  page import="edu.stanford.facs.desk.*,java.io.*,java.util.*" %>
<%! FacsDesk  eDesk; %>
<%
  String  error = null;
  List  requests = new ArrayList();

  if (eDesk == null)
    eDesk = new FacsDesk( getServletContext().getInitParameter( "edesk.root" ), getInitParameter( "edesk.site" ) );

  int[]  tapeIndex = (int[]) eDesk.getSiteObject( eDesk.TAPE_INDEX );

  List  faults = (List) eDesk.getSiteObject( eDesk.PENDING_FAULTS );
  if (faults == null)
    faults = new ArrayList();
  faults.addAll( eDesk.getWarehouseFaults() );

  ListIterator  li = faults.listIterator();
  while (li.hasNext())
  {
    FacsDesk.WarehouseFault  wf = (FacsDesk.WarehouseFault) li.next();
    if (eDesk.getCacheData( wf.warehouse ).exists())
      li.remove();
  }
  while (li.hasPrevious())
  {
    FacsDesk.WarehouseFault  wf = (FacsDesk.WarehouseFault) li.previous();
    for (int  tape = 0;  tape < tapeIndex.length;  ++tape)
    {
      if (tapeIndex[tape] == 0)
        continue;
      else if (wf.warehouse <= tapeIndex[tape])
      {
        Integer  volume = new Integer( tape );
        if (!requests.contains( volume ))
          requests.add( volume );
        break;
      }
    }
  }
//  leave in request order for now
//  Collections.sort(requests);
%>
<html>
<head>
<title>
WHouse Tape Requests
</title>
</head>
<body>
<h1>
Shared FACS Facility
</h1>
<h1>
Tape Mount Requests
</h1>
<table cellpadding="2" cellspacing="2" border="1">
<font size="+1">
<%
  %><tr><%
  ListIterator  req = requests.listIterator();
  for (int  i = 0;  req.hasNext();  ++i)
  {
    if (i % 8 == 0 && i > 0)
      %></tr><tr><%;
    String  volume = String.valueOf( ((Integer) req.next()).intValue() );
    volume = "WH000".substring( 0, 5 - volume.length() ) + volume;
    %><td><%= volume %></td><%
  }
  %></tr><%
%>
</font>
</table>
</body>
</html>
