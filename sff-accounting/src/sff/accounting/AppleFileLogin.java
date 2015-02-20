package sff.accounting;

import java.io.*;
import java.text.*;
import java.util.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: Stanford University</p>
 * @author not attributable
 * @version 1.0
 */

public class AppleFileLogin
    extends AccountingTask
{
  static class LoginInfo
  {
    String host_hame;
    String user_name;
    Date login_time;
  }

  static Map<String,LoginInfo> host_map = new HashMap<String,LoginInfo>();

  public static void main(String[] args)
  {
    init(args);

    DateFormat appleDate = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss"); // no timzone, timezone in log is wrong

    GregorianCalendar calendar = new GregorianCalendar();

    try
    {
      File log_file = new File("/Library/Logs/AppleFileService/AppleFileServiceAccess.log");
//      File log_file = new File(folder, "AppleFileServiceAccess.log");
      File login_record = new File(login_folder, fileDate.format(calendar.getTime()) + ".login");
      RandomAccessFile records = new RandomAccessFile(login_record, "rw");

      Date last_time = calendar.getTime();
      calendar.add(GregorianCalendar.DATE, -1);
      Date first_time = calendar.getTime();

      BufferedReader logFile = new BufferedReader(new FileReader(log_file));

      StringBuffer sb = new StringBuffer(128);

      outer:for (; ; )
      {
        String record = null;

        sb.setLength(0);
        for (; ; )
        {
          int c = logFile.read();
          if (c < 0)
            break outer;
          if (c == '\n')
            break;
          if (c == 0)
            continue;
          sb.append((char)c);
        }
        record = sb.toString();

        if (!record.startsWith("IP"))
          continue;

        String time = record.substring(record.indexOf('[') + 1,
            record.indexOf(']'));
        Date date = appleDate.parse(time);
        if (first_time.compareTo(date) > 0)
          continue;
        if (last_time.compareTo(date) < 0)
          continue;

        String ip_addr = record.substring(3, record.substring(3).indexOf(' ') + 3);
        String type = record.substring(record.indexOf('"') + 1,
            record.lastIndexOf('"'));
        String success = record.substring(record.lastIndexOf('"') + 2);
        if (type.startsWith("Login"))
        {
          if (!success.equals("0 0 0"))
            continue;

          LoginInfo info = new LoginInfo();
          info.user_name = type.substring("Login ".length());
          info.host_hame = getProperty("sff.accounting.host." + ip_addr, getHostName());
          info.login_time = date;

          host_map.put(ip_addr, info);
        }
        else if (type.startsWith("Logout"))
        {
          if (!success.equals("0 0 0"))
            continue;

          LoginInfo info = (LoginInfo)host_map.get(ip_addr);
          if (info == null)
          {
            info = new LoginInfo();
            info.user_name = type.substring("Logout ".length());
            info.host_hame = getProperty("sff.accounting.host." + ip_addr, getHostName());
            info.login_time = first_time;
          }
          else if (!info.user_name.equals(type.substring("Logout ".length())))
            throw new IllegalStateException("usernames do not match");

          if (!isIgnored(info.user_name))
          {
            records.writeUTF(info.host_hame);
            records.writeUTF(info.user_name);
            records.writeLong(info.login_time.getTime());
            records.writeLong(date.getTime());
          }

          host_map.remove(ip_addr);
        }
      }
      logFile.close();

      Iterator<LoginInfo> i = host_map.values().iterator();
      while (i.hasNext())
      {
        LoginInfo info = i.next();

        if (!isIgnored(info.user_name))
        {
          records.writeUTF(info.host_hame);
          records.writeUTF(info.user_name);
          records.writeLong(info.login_time.getTime());
          records.writeLong(last_time.getTime());
        }
      }

      records.close();
      logFile.close();
    }
    catch (ParseException ex)
    {
      ex.printStackTrace();
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
    }

    System.exit(0);
  }
}
