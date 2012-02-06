package edu.stanford.facs.compensation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public abstract class AggregateEstimator
  extends PolynomialEstimator
{
  AggregateEstimator (Stack<Point> pool, List<Point> data, int rank)
  {
    super(pool, data, rank);
  }

  protected final List<Point> raw = new ArrayList<Point>();

  public void reset ()
  {
    pool.addAll(raw);
    raw.clear();
    super.reset();
  }
  
  public void data (double x, double y, double w)
  {
    Point p = allocate();
    
    p.x = x;
    p.y = y;
    p.w = w;
    p.r = x;
    
    raw.add(p);
  }

  protected abstract void aggregate (int i, final int n, final Point Q);

  public void aggregate (final int n)
  {
    try
    {
      PrintWriter pw;
      if (Tools.DEBUG)
      {
        pw = new PrintWriter(new File("variance.txt"));
        pw.println("X,Y,W");
      }

      // compute aggregate statistic for chunks of size N
      // always using the brightest events available
      int offset = raw.size() % n;
      Collections.sort(raw);
      for (int i = 0, m = raw.size() / n; i < m; ++i)
      {
        Point q = allocate();
        aggregate(i * n + offset, n, q);
        data.add(q);

        if (Tools.DEBUG)
        {
          pw.print(q.x);
          pw.print(",");
          pw.print(q.y);
          pw.print(",");
          pw.println(q.w);
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
