package sff.accounting;

import java.io.*;
import java.util.*;
import java.net.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: Stanford University</p>
 * @author not attributable
 * @version 1.0
 */

public class MoveInvestigators
    extends AccountingTask
{
  static final String INVESTIGATORS = "investigators.properties";
  static final String EMAIL = "email.properties";

//  static File folder;
  static URL base_url;
//  static Properties prop = new Properties();

  public static void main(String[] args)
  {
    init(args);
    initAuth();

    try
    {
      base_url = new URL(getProperty("sff.accounting.login.host"));
      String folder_name = getProperty("sff.accounting.login.folder");
      if (!folder_name.endsWith("/"))
        folder_name += "/";
      base_url = new URL(base_url, folder_name);
    }
    catch (MalformedURLException ex)
    {
      ex.printStackTrace();
      return;
    }

    if (args.length > 0)
    {
      if (args[args.length-1].equalsIgnoreCase("PUT"))
        putInvestigators();
      if (args[args.length-1].equalsIgnoreCase("GET"))
        getInvestigators();
    }
    System.out.println("Usage  [<accountin folder>] (PUT|GET)");
  }

  private static void putInvestigators()
  {
    try
    {
      File f = new File(login_folder, getProperty( "sff.accounting.checkin.user_list", "XUSER_LIST.txt" ));
      System.out.println(f.getAbsolutePath());
      BufferedReader br = new BufferedReader(new FileReader(f));
      StringBuffer sb = new StringBuffer();
      List<String> users = new ArrayList<String>();
      List<String> emails = new ArrayList<String>();

      for (int row = 0; ; ++row)
      {
        String userName = null;
        String eMail = null;
        String sunetId = null;
        String active = null;
        int chr = -1;

        for (int col = 'A'; ; ++col)
        {
          sb.setLength(0);
          for (; ; )
          {
            try
            {
              chr = br.read();
            }
            catch (Exception ex)
            {
              ex.printStackTrace();
              System.exit(1);
            }
            if (chr == '\t')
              break;
            else if (chr == '\n')
              break;
            else if (chr == '\r')
              break;
            else if (chr == -1)
              break;
            sb.append( (char) chr);
          }

          switch (col)
          {
          case 'C':
            userName = sb.toString();
            if (userName.startsWith("\""))
              userName = userName.substring(1, userName.length());
            if (userName.endsWith("\""))
              userName = userName.substring(0, userName.length() - 1);
            break;
          case 'J':
            sunetId = sb.toString();
            break;
          case 'T':
            active = sb.toString();
            break;
          case 'V':
            eMail = sb.toString();
            break;
          default:
            break;
          }
          if (chr == '\n')
            break;
          else if (chr == '\r')
            break;
          else if (chr == -1)
            break;
        }
        if (chr == -1)
          break;
        if (row == 0)
          continue;
        if (!"active".equalsIgnoreCase(active))
          continue;
        if (sunetId == null || sunetId.length() == 0)
          continue;

        sb.setLength(0);
        sb.append(sunetId);
        while (sb.length() < 15)
          sb.append(' ');
        sb.append(' ');
        sb.append(userName);
        users.add(sb.toString());

        if (eMail == null || eMail.length() == 0)
          continue;
        sb.setLength(0);
        sb.append(sunetId);
        sb.append('=');
        sb.append(eMail);
        emails.add(sb.toString());
      }
      br.close();

      if (users.isEmpty())
      {
        System.out.println("User list is empty");
        return;
      }

      Collections.<String>sort(users);
      Collections.<String>sort(emails);

      URL url = new URL(base_url, INVESTIGATORS);
      HttpURLConnection http = (HttpURLConnection) url.openConnection();
      http.setRequestMethod("PUT");
      http.setDoOutput(true);

      PrintWriter pw = new PrintWriter(http.getOutputStream());

      Iterator<String> i = users.iterator();
      while (i.hasNext())
      {
        pw.println(i.next());
      }
      pw.close();

      int sts = http.getResponseCode();

      url = new URL(base_url, EMAIL);
      http = (HttpURLConnection) url.openConnection();
      http.setRequestMethod("PUT");
      http.setDoOutput(true);

      pw = new PrintWriter(http.getOutputStream());

      i = emails.iterator();
      while (i.hasNext())
      {
        pw.println(i.next());
      }
      pw.close();

      sts = http.getResponseCode();
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
    }
    System.exit(0);
  }

  private static void getInvestigators()
  {
    try
    {
      URL url = new URL(base_url, INVESTIGATORS);
      HttpURLConnection http = (HttpURLConnection) url.openConnection();
      http.setRequestMethod("GET");

      File  investigators = new File( getProperty("sff.accounting.checkin.folder"), INVESTIGATORS );
      FileOutputStream  fos = new FileOutputStream(investigators);
      InputStream  is = http.getInputStream();

      for (byte[]  buffer = new byte[2048];;)
      {
        int  n = is.read(buffer);
        if (n < 0)
          break;
        fos.write(buffer, 0, n);
      }
      fos.close();

      int sts = http.getResponseCode();
      if (sts != HttpURLConnection.HTTP_OK)
        System.out.println("Could not GET: " + url.toExternalForm());

      url = new URL(base_url, EMAIL);
      http = (HttpURLConnection) url.openConnection();
      http.setRequestMethod("GET");

      File  emails = new File( getProperty("sff.accounting.checkin.folder"), EMAIL );
      fos = new FileOutputStream(emails);
      is = http.getInputStream();

      for (byte[]  buffer = new byte[2048];;)
      {
        int  n = is.read(buffer);
        if (n < 0)
          break;
        fos.write(buffer, 0, n);
      }
      fos.close();

      sts = http.getResponseCode();
      if (sts != HttpURLConnection.HTTP_OK)
        System.out.println("Could not GET: " + url.toExternalForm());
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
    }
    System.exit(0);
  }
}
