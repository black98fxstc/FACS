package edu.stanford.facs.compensation;

import java.util.Arrays;

public class NormalityTest
{
  private double[] data = new double[1000];
  private int count;

  public void reset ()
  {
    count = 0;
  }

  public void add (double x)
  {
    if (Double.isNaN(x))
      throw new IllegalArgumentException();
    if (count == data.length)
    {
      double[] temp = new double[data.length * 2];
      System.arraycopy(data, 0, temp, 0, count);
      data = temp;
    }
    data[count++] = x;
  }
  
	public double test ()
	{
		int j;
		double d, dt, en, ff, fn, fo = 0.0;
		Arrays.sort(data, 0, count);
		en = count;
		d = 0.0;
		for (j = 0; j < count; j++)
		{
			fn = (j + 1) / en;
			ff = 0.5 * ERF.erfc(-0.707106781186547524 * data[j]); // CDF of standard normal
			dt = Math.max(Math.abs(fo - ff), Math.abs(fn - ff));
			if (dt > d)
				d = dt;
			fo = fn;
		}
		en = Math.sqrt(en);
		return KSDist.qks((en + 0.12 + 0.11 / en) * d);
	}
}
