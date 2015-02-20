package sff.accounting;

import java.io.*;

/**
 * <p>Title: FACS Login Recording</p>
 * <p>Description: Record logins to FACS instrument computers</p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: Stanford University</p>
 * @author Wayne A. Moore
 * @version 1.0
 */

public class MacLogin
    extends AccountingTask
{
  public static void main(String[] args)
  {
    init(args);
    initLog("sff-login");

    if (args.length < 2)
    {
      System.out.println("No command");
      System.exit(1);
    }

    if (args[1].equalsIgnoreCase("Login"))
    {
      if (args.length < 3)
      {
        System.out.println("Missing username");
        System.exit(1);
      }
      try
      {
        File login = new File(login_folder, "mac-login.dat");
        DataOutputStream dos = new DataOutputStream(new FileOutputStream(login));
        String username = args[2];
        long current_time = System.currentTimeMillis();
        dos.writeUTF(username);
        dos.writeLong(current_time);
        dos.close();
      }
      catch (IOException ex)
      {
        ex.printStackTrace();
        System.exit(1);
      }
    }
    else if (args[1].equalsIgnoreCase("Logout"))
    {
      try
      {
        File login = new File(login_folder, "mac-login.dat");
        DataInputStream dis = new DataInputStream(new FileInputStream(login));
        setUserName(dis.readUTF());
        setLoginTime(dis.readLong());
        dis.close();
        login.delete();

        writeLoginRecord();
      }
      catch (IOException ex)
      {
        ex.printStackTrace();
        System.exit(1);
      }
    }
    else
    {
      System.out.println("Illegal command: " + args[1]);
      System.exit(1);
    }
  }
}
