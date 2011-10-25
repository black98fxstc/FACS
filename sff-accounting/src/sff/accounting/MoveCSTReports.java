package sff.accounting;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

public class MoveCSTReports
  extends AccountingTask
{
  private final static boolean DEBUG = true;

  public static void main (String[] args)
  {
    init(args[0]);
    initAuth();

    System.out.println(new Date());
    try
    {
      Stack<File> directories = new Stack<File>();
      List<File> files = new ArrayList<File>();

      directories.push(new File("C:\\BDCytometerSetupAndTracking"));
      while (!directories.empty())
      {
        File[] list = directories.pop().listFiles();
        for (int i = 0; i < list.length; i++)
        {
          if (list[i].isDirectory())
          {
            directories.push(list[i]);
            continue;
          }
          String name = list[i].getName();
          if (!name.toLowerCase().endsWith(".pdf"))
            continue;
          if (name.startsWith("CytometerBaseline")
            || name.startsWith("CytometerSetup"))
            files.add(list[i]);
        }
      }

      URL instrument_url = new URL(getTrackingURL(), getHostName() + "/");
      Iterator<File> i = files.iterator();
      while (i.hasNext())
      {
        File f = i.next();
        URL url = new URL(instrument_url, f.getName());
        HttpURLConnection http = (HttpURLConnection)url.openConnection();
        http.setRequestMethod("HEAD");
        int sts = http.getResponseCode();
        if (sts == HttpURLConnection.HTTP_OK)
          continue;

        System.out.println(url);
        http = (HttpURLConnection)url.openConnection();
        http.setRequestMethod("PUT");
        http.setDoOutput(true);
        OutputStream os = http.getOutputStream();
        BufferedInputStream bis = new BufferedInputStream(
          new FileInputStream(f));
        byte[] buffer = new byte[8192];
        for (;;)
        {
          int n = bis.read(buffer);
          if (n < 0)
            break;
          os.write(buffer, 0, n);
        }
        bis.close();
        os.close();
        sts = http.getResponseCode();
        if (sts != HttpURLConnection.HTTP_CREATED)
          System.out.println("Could not PUT: " + url.toExternalForm());
      }
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
      return;
    }
  }

}
