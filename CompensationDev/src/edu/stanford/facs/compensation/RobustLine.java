package edu.stanford.facs.compensation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

public class RobustLine
  extends LineEstimator
{
  public RobustLine (Stack<Point> pool, List<Point> data)
  {
    super(pool, data);
  }

  private double absoluteDeviation;

  public double getAbsoluteDeviation ()
  {
    return absoluteDeviation;
  }

  public void data (double x, double y, double w)
  {
    Point p = allocate();
    data.add(p);

    if (Double.isNaN(x))
      throw new IllegalArgumentException();
    if (Double.isNaN(y))
      throw new IllegalArgumentException();
    if (Double.isNaN(w))
      throw new IllegalArgumentException();
    p.x = x;
    p.y = y;
    p.w = w;
  }

  private final double rho (final double slope)
  {
    /*
     * Compute the weighted residuals around the line.
     */
    Point p;
    Iterator<Point> i = data.iterator();
    while (i.hasNext())
    {
      p = i.next();
      p.r = (p.y - slope * p.x) * p.w;
    }
    /*
     * Find the point that is closest to having equal weights on both sides.
     */
    Collections.sort(data);
    int lo = 0, hi = data.size();
    double loSum = 0, hiSum = 0;
    while (lo < hi)
    {
      if (loSum < hiSum)
        loSum += data.get(lo++).w;
      else
        hiSum += data.get(--hi).w;
    }
    if (loSum > hiSum)
      --lo;
    p = data.get(lo);
    intercept = p.r / p.w;
    p = data.get(lo + 1);
    intercept += p.r / p.w;
    intercept /= 2;

    double rho = 0;
    absoluteDeviation = 0;
    i = data.iterator();
    double eps = Math.ulp(1.0f);
    while (i.hasNext())
    {
      p = i.next();
      double d = p.y - (p.x * slope + intercept);
      absoluteDeviation += Math.abs(d);
      if (d != 0)
        d /= Math.abs(d);
      if (Math.abs(d) > eps)
        if (d > 0)
          rho += p.x * p.w;
        else
          rho -= p.x * p.w;
    }
    absoluteDeviation /= data.size();
    return rho;
  }

  public void fit ()
  {
    double S = 0, Sx = 0, Sy = 0, Sxy = 0, Sxx = 0;
    Iterator<Point> i = data.iterator();
    while (i.hasNext())
    {
      Point p = i.next();
      double v = p.w * p.w;
      S += v;
      assert !Double.isNaN(S);
      Sx += v * p.x;
      assert !Double.isNaN(Sx);
      Sy += v * p.y;
      assert !Double.isNaN(Sy);
      Sxx += v * p.x * p.x;
      assert !Double.isNaN(Sxx);
      Sxy += v * p.x * p.y;
      assert !Double.isNaN(Sxy);
    }
    double del = S * Sxx - Sx * Sx;
    double slope = (S * Sxy - Sx * Sy) / del;
    double intercept = (Sxx * Sy - Sx * Sxy) / del;
    if (Double.isNaN(del) || Double.isNaN(slope) || Double.isNaN(intercept))
      System.out.println("oops");
    double chisq = 0;
    i = data.iterator();
    while (i.hasNext())
    {
      Point p = i.next();
      double t = p.y - (slope * p.x + intercept);
      chisq += t * t;
    }
    double slopeSigma = Math.sqrt(chisq / del);

    double S1 = slope;
    double rho1 = rho(S1);
    double S2 = S1;
    if (rho1 >= 0)
      S2 += 3 * slopeSigma;
    else
      S2 -= 3 * slopeSigma;
    double rho2 = rho(S2);
    while (rho1 * rho2 > 0)
    {
      slope = 2 * S2 - S1;
      S1 = S2;
      rho1 = rho2;
      S2 = slope;
      rho2 = rho(S2);
    }

    double tol = slopeSigma / 1000;
    while (Math.abs(S2 - S1) > tol)
    {
      slope = (S1 + S2) / 2;
      if (slope == S1 || slope == S2)
        break;
      double r = rho(slope);
      if (r * rho1 >= 0)
      {
        rho1 = r;
        S1 = slope;
      }
      else
      {
        rho2 = r;
        S2 = slope;
      }
    }

    this.slope = slope;

    try
    {
      PrintWriter pw;
      if (Tools.DEBUG)
      {
        pw = new PrintWriter(new File("robust.txt"));
        pw.println("X,Y,W,R,F");
      }
      i = data.iterator();
      while (i.hasNext())
      {
        Point p = i.next();

        if (Tools.DEBUG)
        {
          pw.print(p.x);
          pw.print(',');
          pw.print(p.y);
          pw.print(',');
          pw.print(p.w);
          pw.print(',');
          double f = p.x * slope + intercept;
          pw.print(f - p.y);
          pw.print(',');
          pw.print(f);
          pw.print(',');
          pw.println();
        }
      }
      if (Tools.DEBUG)
        pw.close();
    }
    catch (FileNotFoundException e)
    {
      e.printStackTrace();
    }
  }
}
