package sff.accounting;

/**
 * <p>Title: FACS Login Recording</p>
 * <p>Description: Record logins to FACS instrument computers</p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: Stanford University</p>
 * @author Wayne A. Moore
 * @version 1.0
 */

public class WindowsLogin
    extends AccountingTask
{
  private static Runnable recorder = new Runnable()
  {
    public void run()
    {
      setUserName(System.getProperty("user.name"));
      setLoginTime(System.currentTimeMillis());

      for (boolean interrupted = false; !interrupted; )
      {
        writeLoginRecord();
        try
        {
          synchronized (this)
          {
            wait(getLoginTick());
          }
        }
        catch (InterruptedException ex)
        {
          interrupted = true;
        }
      }
    }
  };

  public static void main(String[] args)
  {
    init(args);
    initLog("sff-login");

    new Thread(recorder).start();
  }
}
