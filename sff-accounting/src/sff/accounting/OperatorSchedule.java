package sff.accounting;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.Map.Entry;

/**
 * <p>Title: SFF Accounting</p>
 *
 * <p>Description: Shared FACS Facility Accounting Utilities</p>
 *
 * <p>Copyright: Copyright (c) 2004</p>
 *
 * <p>Company: Stanford University</p>
 *
 * @author not attributable
 * @version 1.0
 */
public class OperatorSchedule
{
  SimpleDateFormat reportDate = new SimpleDateFormat("HH:mm");
  SimpleDateFormat titleDate = new SimpleDateFormat("EEEE, MMMM d, yyyy");
  SortedMap<Date, List<Reservation>> perDay = new TreeMap<Date, List<Reservation>>();
  String operator;

  class Reservation
      implements Comparable<Reservation>
  {
    String subject;
    String uid;
    String date;
    String time;
    String duration;
    String availability;
    String repeatType;
    String submittedBy;
    String sterility;
    String accessLevel;
    String name;
    String machine;
    String canceled;
    Date startTime;
    Date endTime;
    /**
     * Compares this object with the specified object for order.
     *
     * @param o the Object to be compared.
     * @return a negative integer, zero, or a positive integer as this object is
     *   less than, equal to, or greater than the specified object.
     * @todo Implement this java.lang.Comparable method
     */
    public int compareTo(Reservation o)
    {
      return this.startTime.compareTo(o.startTime);
    }

    public boolean equals(Reservation o)
    {
      return compareTo(o) == 0;
    }
  }

  public OperatorSchedule(File file, String operator)
  {
    this.operator = operator;
    try
    {
      FileReader fr = new FileReader(file);
      BufferedReader br = new BufferedReader(fr);
      GregorianCalendar gc = new GregorianCalendar();
      SimpleDateFormat calDate = new SimpleDateFormat("M/d/yyyy");
      SimpleDateFormat calTime = new SimpleDateFormat("M/d/yyyy hh:mmaa");
      br.readLine();
      outer:for (; ; )
      {
        Reservation r = new Reservation();
        StringBuffer sb = new StringBuffer();
        inner:for (int i = 1; ; ++i)
        {
          sb.setLength(0);
          for (; ; )
          {
            int c = br.read();
            if (c < 0)
              break outer;
            if (c == '\r' || (c == '\n'))
              break inner;
            if (c == '\t')
              break;
            sb.append((char)c);
          }

          String token = sb.toString();
          switch (i)
          {
          case 1:
            r.subject = token;
            break;
          case 2:
            r.date = token;
            break;
          case 3:
            r.time = token;
            break;
          case 4:
            r.duration = token;
            break;
          case 5:
            r.submittedBy = token;
            break;
          case 6:
            r.availability = token;
            break;
          case 7:
            r.accessLevel = token;
            break;
          case 8:
            r.sterility = token;
            break;
          case 10:
            r.repeatType = token;
            break;
          case 15:
            r.machine = token;
            break;
          case 16:
            r.uid = token;
            break;
          case 18:
            r.uid = token;
            break;
          case 19:
            r.name = token;
            break;
          case 21:
            r.canceled = token;
            break;
          default:
            break;
          }
        }
        if (!r.canceled.equals("No"))
          continue;
        if (r.repeatType.startsWith("Repeats"))
          continue;
        if (!r.availability.equals("Not Available"))
          continue;
        if (!r.subject.startsWith("S:"))
          continue;

        r.machine = r.subject.substring("S:".length());
        r.machine = r.machine.substring(0, r.machine.indexOf(':'));
        if (r.machine.equals("default"))
          r.machine = "Aida";

        r.startTime = calTime.parse(r.date + " " + r.time);
        gc.setTime(r.startTime);
        gc.add(GregorianCalendar.MINUTE, Integer.parseInt(r.duration));
        r.endTime = gc.getTime();

        Date d = calDate.parse(r.date);
        List<Reservation> report = perDay.get(d);
        if (report == null)
        {
          report = new ArrayList<Reservation>();
          perDay.put(d, report);
        }
        report.add(r);
      }
      br.close();
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }

    Iterator<List<Reservation>> i = perDay.values().iterator();
    while (i.hasNext())
    {
      List<Reservation> report = i.next();
      Collections.<Reservation>sort(report);
      for (int k = report.size() - 2; k >= 0; --k)
        if (report.get(k).equals(report.get(k + 1)))
          report.remove(k);
    }
  }

  public void print(PrintWriter pw)
  {
    printFileHeader(pw);

    Iterator<Entry<Date,List<Reservation>>> i = perDay.entrySet().iterator();
    while (i.hasNext())
    {
      Entry<Date,List<Reservation>> day_entry = i.next();
      Date thisDate = day_entry.getKey();
      List<Reservation> report = day_entry.getValue();

      printDateHeader(pw, thisDate);

      Iterator<Reservation> j = report.iterator();
      while (j.hasNext())
      {
        Reservation r = j.next();
        printReservation(pw, r);
      }

      printDateFooter(pw);
    }

    printFileTrailer(pw);
  }

  protected void printReservation(PrintWriter pw, Reservation r)
  {
    pw.print("<tr>");
    pw.print("<td>");
    pw.print(reportDate.format(r.startTime));
    pw.print("</td>");
    pw.print("<td>");
    pw.print(reportDate.format(r.endTime));
    pw.print("</td>");
    pw.print("<td>");
    pw.print(r.machine);
    pw.print("</td>");
    pw.print("<td>");
    pw.print(r.uid);
    pw.print("</td>");
    pw.print("<td>");
    pw.print(r.name);
    pw.print("</td>");
    pw.println("</tr>");
  }

  protected void printDateHeader(PrintWriter pw, Date thisDate)
  {
    pw.print("<th align=\"left\" colspan=\"5\">");
    pw.print("<font size=+2>");
    pw.print(titleDate.format(thisDate));
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
    pw.print("Machine");
    pw.print("</th>");
    pw.print("<th align=\"left\">");
    pw.print("UID");
    pw.print("</th>");
    pw.print("<th align=\"left\">");
    pw.print("User");
    pw.print("</th>");
    pw.println("</tr>");
  }

  /**
   * printDateFooter
   *
   * @param pw PrintWriter
   */
  protected void printDateFooter(PrintWriter pw)
  {
    pw.print("<tr>");
    pw.print("<td>");
    pw.print("&nbsp;");
    pw.print("</td>");
    pw.println("</tr>");
  }

  protected void printFileHeader(PrintWriter pw)
  {
    pw.println(
        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">");
    pw.println("<html>");
    pw.println("<head>");
    pw.println(
        "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">");
    pw.print("<title>");
    pw.print("FACS Facility - Operator Schedule - ");
    pw.print(operator);
    pw.println("</title>");
    pw.println("<style type=\"text/css\">");
    pw.println(".sample {");
    pw.println("  background:  #CFCFFF; }");
    pw.println(".parameter {");
    pw.println("  background:  #FFFFAF; }");
    pw.println("</style>");
    pw.println("</head>");
    pw.println("<body>");
    pw.println("<center>");
    pw.println("<table width=\"95%\">");

    pw.print("<tr>");
    pw.print("<th align=\"center\" colspan=\"5\">");
    pw.print("<font size=+3>");
    pw.print("Operator Schedule - ");
    pw.print(operator);
    pw.print("</font>");
    pw.print("</th>");
    pw.println("</tr>");

    pw.print("<tr>");
    pw.print("<td>");
    pw.print("&nbsp;");
    pw.print("</td>");
    pw.println("</tr>");
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

    new OperatorSchedule(f, args[1]).print(pw);
  }
}
