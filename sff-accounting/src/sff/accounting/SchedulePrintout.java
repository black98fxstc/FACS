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

public class SchedulePrintout
    extends ScheduleReport
{
  private int pageCount = 0;

  public SchedulePrintout(File p0)
  {
    super(p0);
  }

  private void printPSStringChar(PrintWriter pw, char c)
  {
    if (c > 0xFF)
      throw new IllegalArgumentException(
          "Character is not in PostScript encoding");
    if (c == '(' || c == ')' || c == '\\')
      pw.write('\\');
    pw.write(c);
  }

  private void printPSString(PrintWriter pw, String s)
  {
    pw.write('(');
    for (int i = 0; i < s.length(); ++i)
      printPSStringChar(pw, s.charAt(i));
    pw.write(')');
  }

  protected void printFileHeader(PrintWriter pw)
  {
    pw.println("%!PS-Adobe-3.0");
    pw.print("%%Pages: ");
    pw.println(perDay.size());
    try
    {
      InputStream is = getClass().getResourceAsStream("prolog.ps");
      byte[] buffer = new byte[8192];
      for (; ; )
      {
        int n = is.read(buffer);
        if (n < 0)
          break;
        for (int i = 0; i < n; ++i)
          pw.write(buffer[i]);
      }
      is.close();
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
      System.exit(1);
    }
  }

  protected void printFileTrailer(PrintWriter pw)
  {
    pw.println("%%EOF");
  }

  protected void printDateHeader(PrintWriter pw, Date thisDate)
  {
    pw.print("%%Page: ");
    printPSString(pw, "Schedule Printout");
    pw.println(" " + ++pageCount);
    pw.println("%%BeginPageSetup");
    pw.println("/pagesave save def");
    pw.println("%%EndPageSetup");
    pw.println("docolor");
    printPSString(pw, titleDate.format(thisDate));
    pw.println(" dayTitle");
  }

  protected void printDateFooter(PrintWriter pw)
  {
    pw.println("pagesave restore");
    pw.println("showpage");
  }

  protected void printReservation(PrintWriter pw, Reservation r, String sterility, String lasers, String nozzle)
  {
    pw.print("[ ");
    printPSString(pw, reportDate.format(r.startTime));
    printPSString(pw, reportDate.format(r.endTime));
    printPSString(pw, r.name);
    printPSString(pw, r.onBehalfOf);
    printPSString(pw, r.operator);
    printPSString(pw, r.lasers);
    printPSString(pw, r.sterility);
    printPSString(pw, r.use);
    pw.print("] ");
    pw.print(sterility);
    pw.print(" ");
    pw.print(lasers);
    pw.print(" ");
    pw.print(nozzle);
    pw.println(" reservation");
  }

  protected void printMachineHeader(PrintWriter pw, String machine)
  {
    printPSString(pw, machine);
    pw.println(" machineTitle");
  }

  protected void printMachineFooter(PrintWriter pw)
  {
    ;
  }

  public static void main(String[] args)
  {
    File f = new File(args[0]);
    PrintWriter pw = new PrintWriter(System.out);

    new SchedulePrintout(f).print(pw);
  }

  /**
   * print
   *
   * @param pw PrintWriter
   */
  public void print(PrintWriter pw)
  {
    super.print(pw);
    pw.close();
  }
}
