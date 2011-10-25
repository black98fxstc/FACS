package sff.accounting;

import java.io.*;
import java.util.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: Stanford University</p>
 * @author not attributable
 * @version 1.0
 */

public class ScheduleSummary
    extends ScheduleReport
{
  public ScheduleSummary(File f)
  {
    super(f);
  }

  protected void printHTML (PrintWriter pw, String text)
  {
    for (int i = 0, n = text.length(); i < n; ++i)
    {
      char c = text.charAt(i);
      if (c == '&')
      {
        pw.print("&amp;");
      }
      else if (c == '<')
      {
        pw.print("&lt;");
      }
      else if (c == '>')
      {
        pw.print("&gt;");
      }
      else
      {
        pw.print(c);
      }
    }
  }

  protected void printReservation(PrintWriter pw, Reservation r, String sterility, String lasers, String nozzle)
  {
    pw.print("<tr class=\"");
    pw.print(sterility);
    pw.print("\">");
    pw.print("<td>");
    pw.print(reportDate.format(r.startTime));
    pw.print("</td>");
    pw.print("<td>");
    pw.print(reportDate.format(r.endTime));
    pw.print("</td>");
    pw.print("<td>");
    printHTML(pw,r.name);
    pw.print("</td>");
    pw.print("<td>");
    printHTML(pw,r.onBehalfOf);
    pw.print("</td>");
    pw.print("<td>");
    printHTML(pw,r.operator);
    pw.print("</td>");
    pw.print("<td class=\"");
    pw.print(lasers);
    pw.print("\">");
    printHTML(pw,r.lasers);
    pw.print("</td>");
    pw.print("<td>");
    printHTML(pw,r.sterility);
    pw.print("</td>");
    pw.print("<td class=\"");
    pw.print(nozzle);
    pw.print("\">");
    printHTML(pw,r.use);
    pw.print("</td>");
    pw.println("</tr>");
  }

  protected void printMachineFooter(PrintWriter pw)
  {
    pw.print("<tr>");
    pw.print("<td>");
    pw.print("</td>");
    pw.println("</tr>");
  }

  protected void printMachineHeader(PrintWriter pw, String machine)
  {
    pw.print("<tr>");
    pw.print("<th align=\"left\" colspan=\"8\">");
    pw.print("<font size=\"+2\">");
    pw.print(machine);
    pw.print("</font>");
    pw.print("</th>");
    pw.println("</tr>");

    pw.print("<tr>");
    pw.print("<th align=\"left\">");
    pw.print("Start");
    pw.print("</th>");
    pw.print("<th align=\"left\">");
    pw.print("End");
    pw.print("</th>");
    pw.print("<th align=\"left\">");
    pw.print("User");
    pw.print("</th>");
    pw.print("<th align=\"left\">");
    pw.print("On Behalf Of");
    pw.print("</th>");
    pw.print("<th align=\"left\">");
    pw.print("Operator");
    pw.print("</th>");
    pw.print("<th align=\"left\">");
    pw.print("Lasers");
    pw.print("</th>");
    pw.print("<th align=\"left\">");
    pw.print("Sterility");
    pw.print("</th>");
    pw.print("<th align=\"left\">");
    pw.print("Use");
    pw.print("</th>");
    pw.println("</tr>");
  }

  protected void printDateHeader(PrintWriter pw, Date thisDate)
  {
    pw.print("<tr>");
    pw.print("<th align=\"center\" colspan=\"8\">");
    pw.print("<font size=\"+3\">");
    pw.print(titleDate.format(thisDate));
    pw.print("</font>");
    pw.print("</th>");
    pw.println("</tr>");

    pw.print("<tr>");
    pw.print("<td>");
    pw.print("</td>");
    pw.println("</tr>");
  }

  /**
   * printDateFooter
   *
   * @param pw PrintWriter
   */
  protected void printDateFooter(PrintWriter pw)
  {
  }

  protected void printFileHeader(PrintWriter pw)
  {
    pw.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">");
    pw.println("<html>");
    pw.println("<head>");
    pw.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">");
    pw.print("<title>");
    pw.print("FACS Facility - Today's Schedule");
    pw.println("</title>");
    pw.println("<style type=\"text/css\">");
    pw.println(".normal {");
    pw.println("  background:  #FFFFFF; }");
    pw.println(".sterile {");
    pw.println("  background:  #FFFF7F; }");
    pw.println(".bl1 {");
    pw.println("  background:  #FF8888; }");
    pw.println(".bl2 {");
    pw.println("  background:  #FF0000; }");
    pw.println(".lasers {");
    pw.println("  background:  #FFAAFF; }");
    pw.println(".nozzle {");
    pw.println("  background:  #7FFF7F; }");
    pw.println("</style>");
    pw.println("</head>");
    pw.println("<body>");
    pw.println("<center>");
    pw.println("<table width=\"95%\">");
  }

  protected void printFileTrailer(PrintWriter pw)
  {
    pw.println("</table>");
    pw.println("</center>");
    pw.println("</body>");
    pw.println("</html>");
    pw.flush();
  }

  public static void main(String[] args)
  {
    File f = new File(args[0]);
    PrintWriter pw = new PrintWriter(System.out);

    new ScheduleSummary(f).print(pw);
  }
}
