package sff.accounting;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * <p>
 * Title: FACS Login Recording
 * </p>
 * <p>
 * Description: Record logins to FACS instrument computers
 * </p>
 * <p>
 * Copyright: Copyright (c) 2004
 * </p>
 * <p>
 * Company: Stanford University
 * </p>
 * 
 * @author Wayne A. Moore
 * @version 1.0
 */

public class MoveLoginRecords
  extends AccountingTask
{
  public static void main (String[] args)
  {
    long now = System.currentTimeMillis();

    init(args[0]);
    initAuth();

    long copy_days, purge_days;
    try
    {
      copy_days = Long.parseLong(getProperty("sff.accounting.login.copy_days",
        "14"));
      purge_days = Long.parseLong(getProperty(
        "sff.accounting.login.purge_days", "90"));
    }
    catch (NumberFormatException ex)
    {
      ex.printStackTrace();
      return;
    }

    DateFormat report_format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    DateFormat file_format = new SimpleDateFormat("yyyy-MM-dd");

    try
    {
      File todaysLogins = new File(folder, file_format.format(new Date(now))
        + ".login");
      if (!todaysLogins.exists())
        todaysLogins.createNewFile();
    }
    catch (IOException ex1)
    {
    }

    String[] files = folder.list();
    for (int i = 0; i < files.length; ++i)
    {
      if (!files[i].endsWith(".login"))
        continue;
      String file_date = files[i].substring(0, "yyyy-MM-dd".length());
      long date;
      try
      {
        date = file_format.parse(file_date).getTime();
      }
      catch (java.text.ParseException ex)
      {
        continue;
      }
      long age = now - date;

      File file = new File(folder, files[i]);
      if (age < 0)
        continue;
      else if (age <= copy_days * DAY)
      {
        try
        {
          URL url = new URL(getAccountingURL(), file_date + "-" + getHostName()
            + ".txt");
          HttpURLConnection http = (HttpURLConnection)url.openConnection();
          http.setRequestMethod("HEAD");
          int sts = http.getResponseCode();
          if (sts == HttpURLConnection.HTTP_OK)
            continue;

          http = (HttpURLConnection)url.openConnection();
          http.setRequestMethod("PUT");
          http.setDoOutput(true);
          PrintWriter pw = new PrintWriter(http.getOutputStream());

          RandomAccessFile login_record = new RandomAccessFile(file, "r");
          for (;;)
          {
            String host_name;
            String user_name;
            long start_time;
            long end_time;
            try
            {
              host_name = login_record.readUTF();
              user_name = login_record.readUTF();
              start_time = login_record.readLong();
              end_time = login_record.readLong();
            }
            catch (EOFException ex)
            {
              break;
            }
            pw.print(host_name);
            pw.print('\t');
            pw.print(user_name.toLowerCase());
            pw.print('\t');
            pw.print(report_format.format(new Date(start_time)));
            pw.print('\t');
            pw.print(report_format.format(new Date(end_time)));
            pw.print('\t');
            pw.print(toTimeInterval(end_time - start_time));
            pw.print("\n");
          }
          login_record.close();

          pw.close();
          sts = http.getResponseCode();
          if (sts != HttpURLConnection.HTTP_CREATED)
            System.out.println("Could not PUT: " + url.toExternalForm());
        }
        catch (IOException ex)
        {
          ex.printStackTrace();
          return;
        }
      }
      else if (age > purge_days * DAY)
      {
        file.delete();
      }
    }
  }
}
