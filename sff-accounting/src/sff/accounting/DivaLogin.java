package sff.accounting;

import java.io.*;
import java.text.*;
import java.util.*;

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
public class DivaLogin
    extends AccountingTask
{
  protected final static boolean DEBUG = false;

  private static GregorianCalendar calendar = new GregorianCalendar();
  private static RandomAccessFile records;
  private static DateFormat divaDate = new SimpleDateFormat("MMMM dd yyyy", Locale.US);
  private static DateFormat divaTime = new SimpleDateFormat("hh:mm:ss aa", Locale.US);
  private static Date firstTime;
  private static Date lastTime;

  private static Date getDivaTimestamp (String timeString, String dateString)
      throws ParseException
  {
    Date time = divaTime.parse(timeString);
    Date date = divaDate.parse(dateString);

    calendar.setTime(time);
    int hour = calendar.get(Calendar.HOUR_OF_DAY);
    int minute = calendar.get(Calendar.MINUTE);
    int second = calendar.get(Calendar.SECOND);
    calendar.setTime(date);
    calendar.set(Calendar.HOUR_OF_DAY, hour);
    calendar.set(Calendar.MINUTE, minute);
    calendar.set(Calendar.SECOND, second);

    return calendar.getTime();
  }

  private static void readDivaLoginRecords (File diva_log)
      throws ParseException, FileNotFoundException, IOException
  {
    BufferedReader br = new BufferedReader(new FileReader(diva_log));
    String line = br.readLine();
    for (line = br.readLine(); line != null; line = br.readLine())
    {
      if (line.trim().length() == 0)
        continue;
      StringTokenizer st = new StringTokenizer(line, ",");

      String username = st.nextToken();
      st.nextToken();
      st.nextToken();
      st.nextToken();
      st.nextToken();
      st.nextToken();
      String loginTime = st.nextToken();
      String loginDate = st.nextToken();
      String logoutTime;
      String logoutDate;
      try
      {
        logoutTime = st.nextToken();
        logoutDate = st.nextToken();
      }
      catch (NoSuchElementException ex)
      {
        logoutTime = " ";
        logoutDate = " ";
      }

      Date login = getDivaTimestamp(loginTime, loginDate);
      Date logout;
      if (logoutTime.equals(" ") && logoutDate.equals(" "))
        logout = login;
      else
        logout = getDivaTimestamp(logoutTime, logoutDate);

      if (logout.compareTo(firstTime) < 0 || login.compareTo(lastTime) > 0)
        continue;

      if (login.compareTo(firstTime) < 0)
      {
        if (login.equals(logout))
          logout = firstTime;
        login = firstTime;
      }
      if (logout.compareTo(lastTime) > 0)
        logout = lastTime;

      if (!isIgnored(username))
      {
        records.writeUTF(getHostName());
        records.writeUTF(username);
        records.writeLong(login.getTime());
        records.writeLong(logout.getTime());
      }
    }
    br.close();;
  }

  public static void main(String[] args)
  {
    init(args[0]);
    if (!DEBUG)
      initLog("sff-logins");

    File divaFolder = new File("C:\\Program Files\\Common Files\\BD");
    if (DEBUG)
      divaFolder = folder;
    DateFormat divaFile = new SimpleDateFormat("yyyy MMMM'.csv'", Locale.US);

    try
    {
      if (args.length > 1)
        calendar.setTime(fileDate.parse(args[1]));
      calendar.set(Calendar.HOUR_OF_DAY, 6);
      calendar.set(Calendar.MINUTE, 0);
      calendar.set(Calendar.SECOND, 0);


      File login_record = new File(folder, fileDate.format(calendar.getTime()) + ".login");
      records = new RandomAccessFile(login_record, "rw");

      lastTime = calendar.getTime();
      calendar.add(GregorianCalendar.DATE, -1);
      firstTime = calendar.getTime();

      File firstFile = new File(divaFolder, divaFile.format(firstTime));
      File lastFile = new File(divaFolder, divaFile.format(lastTime));

      readDivaLoginRecords(firstFile);
      if (!firstFile.equals(lastFile))
        readDivaLoginRecords(lastFile);

      records.close();
      records = null;
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
      System.exit(1);
    }
    finally
    {
      if (records != null)
        try
        {
          records.close();
        }
        catch (IOException ignore)
        {  }
    }

    System.exit(0);
  }
}
