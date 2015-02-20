package sff.accounting;

public class Dither
  extends AccountingTask
{
  public static void main (String[] args)
  {
    init(args);

    try
    {
      long dither = Long.parseLong(
        getProperty("sff.accounting.login.dither", "600"));
      Thread.sleep(getLoginTick() + Math.round(dither * SECOND * Math.random()));
    }
    catch (NumberFormatException ex)
    {
      ex.printStackTrace();
      return;
    }
    catch (InterruptedException ex)
    {
    }
  }
}
