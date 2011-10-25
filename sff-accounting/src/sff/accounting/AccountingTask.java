package sff.accounting;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

/**
 * <p>Title: FACS Login Recording</p>
 * <p>Description: Record logins to FACS instrument computers</p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: Stanford University</p>
 * @author Wayne A. Moore
 * @version 1.0
 */

public class AccountingTask
    extends Thread
{
  static final long SECOND = 1000;
  static final long MINUTE = 60 * SECOND;
  static final long HOUR = 60 * MINUTE;
  static final long DAY = 24 * HOUR;
  static final DateFormat fileDate = new SimpleDateFormat("yyyy-MM-dd");

  static File folder;

  private static Properties props;
  private static Set<String> ignored;
  private static File login_record;
  private static long login_pos;
  private static int login_day;
  private static long login_tick;
  private static URL accounting_url;
  private static URL tracking_url;
  private static String host_name;
  private static String user_name;
  private static long login_time;

  static String getProperty(
      String property,
      String otherwise)
  {
    if (props == null)
    {
      props = new Properties();
      try
      {
        InputStream is = new BufferedInputStream(new FileInputStream(new File(
            folder, "accounting.properties")));
        props.load(is);
        is.close();
      }
      catch (IOException ex)
      {
        ex.printStackTrace();
        return null;
      }
    }
    return props.getProperty(property, otherwise);
  }

  static String getProperty(
      String property)
  {
    return getProperty(property, null);
  }

  static boolean isIgnored(
      String someuser)
  {
    if (ignored == null)
    {
      ignored = new HashSet<String>();
      String s = getProperty("sff.accounting.login.ignore");
      if (s != null && s.length() > 0)
      {
        StringTokenizer st = new StringTokenizer(s, ",");
        while (st.hasMoreTokens())
        {
          ignored.add(st.nextToken());
        }
      }
    }
    return ignored.contains(someuser);
  }

  static long getLoginTick()
  {
    if (login_tick == 0)
    {
      String tick = getProperty("sff.accounting.login.tick", "300");
      try
      {
        login_tick = Long.parseLong(tick);
      }
      catch (NumberFormatException ex)
      {
        System.out.println("Illegal login tick: " + tick);
        login_tick = 300;
        System.out.println("Using login tick: " + Long.toString(login_tick));
      }
    }
    return login_tick * SECOND;
  }

  public static String getHostName()
  {
    if (host_name == null)
    {
      try
      {
        StringTokenizer st = new StringTokenizer(InetAddress.getLocalHost().getHostName(), ".");
        host_name = st.nextToken();
      }
      catch (UnknownHostException ex)
      {
        ex.printStackTrace();
        host_name = "localhost";
        System.out.println("Using host name: " + host_name);
      }
    }

    return host_name;
  }

  public static void setUserName(
      String userName)
  {
    user_name = userName;
  }

  public static String getUserName()
  {
    return user_name;
  }

  public static void setLoginTime(
      long loginTime)
  {
    login_time = loginTime;
  }

  public static long getLoginTime()
  {
    return login_time;
  }

  static URL getAccountingURL()
      throws MalformedURLException
  {
    if (accounting_url == null)
    {
      accounting_url = new URL(getProperty("sff.accounting.login.host"));
      String folder_name = getProperty("sff.accounting.login.folder");
      if (!folder_name.endsWith("/"))
        folder_name += "/";
      accounting_url = new URL(accounting_url, folder_name);
    }
    return accounting_url;
  }

  static URL getTrackingURL()
      throws MalformedURLException
  {
    if (tracking_url == null)
    {
      tracking_url = new URL(getProperty("sff.accounting.login.host"));
      String folder_name = getProperty("sff.accounting.tracking.folder");
      if (!folder_name.endsWith("/"))
        folder_name += "/";
      tracking_url = new URL(tracking_url, folder_name);
    }
    return tracking_url;
  }

  static String toTimeInterval(
      long time)
  {
    long hours = time / HOUR;
    long minutes = (time / MINUTE) % 60;
    long seconds = (time / SECOND) % 60;

    StringBuffer radix60 = new StringBuffer("hh:mm:ss".length());
    radix60.append(String.valueOf(hours));
    radix60.append(':');
    if (minutes < 10)
      radix60.append('0');
    radix60.append(String.valueOf(minutes));
    radix60.append(':');
    if (seconds < 10)
      radix60.append('0');
    radix60.append(String.valueOf(seconds));

    return radix60.toString();
  }

  static void writeLoginRecord()
  {
    GregorianCalendar calendar = new GregorianCalendar();
    long current_time = calendar.getTime().getTime();

    if (calendar.get(Calendar.HOUR_OF_DAY) >= 6)
      calendar.add(Calendar.DATE, 1);
    int current_day = calendar.get(Calendar.DAY_OF_YEAR);

    if (login_record == null)
    {
      login_record = new File(folder, fileDate.format(calendar.getTime()) + ".login");
      if (login_record.exists())
        login_pos = login_record.length();
      else
        login_pos = 0;
    }

    if (isIgnored(getUserName()))
      return;

    RandomAccessFile record;
    try
    {
      record = new RandomAccessFile(login_record, "rw");
      record.seek(login_pos);
      record.writeUTF(getHostName());
      record.writeUTF(getUserName());
      record.writeLong(getLoginTime());
      record.writeLong(current_time);
      record.close();
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
      return;
    }

    if (login_day == 0)
      login_day = current_day;
    else if (login_day != current_day)
    {
      login_day = current_day;
      login_time = current_time;
      login_record = null;
    }
  }

  AccountingTask()
  {}

  static void initLog(
      String logName)
  {
    String fn = new File(folder, logName + ".log").getAbsolutePath();
    OutputStream os;
    try
    {
      os = new FileOutputStream(fn, true);
      System.setOut(new PrintStream(os));
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
      System.exit(1);
    }
  }

  static void initAuth()
  {
    final String accounting_user = getProperty("sff.accounting.login.username");
    final String accounting_pass = getProperty("sff.accounting.login.password");
    Authenticator.setDefault(
        new Authenticator()
    {
      protected PasswordAuthentication getPasswordAuthentication()
      {
        return new PasswordAuthentication(accounting_user, accounting_pass.toCharArray());
      }
    });
  }

  static void init(String folder)
  {
    AccountingTask.folder = new File(folder);
  }
}
