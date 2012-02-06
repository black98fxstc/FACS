package edu.stanford.facs.compensation;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

public abstract class Estimator
{
  protected static class Point
    implements Comparable<Point>
  {
    double x;
    double y;
    double w;
    double r;

    public int compareTo (Point that)
    {
      return Double.compare(this.r, that.r);
    }
  }

  protected final Stack<Point> pool;
  protected final List<Point> data;

  Estimator (Stack<Point> pool, List<Point> data)
  {
    this.pool = pool;
    this.data = data;
  }

  protected Point allocate ()
  {
    if (pool.empty())
      return new Point();
    else
      return pool.pop();
  }

  protected void free (Collection<Point> points)
  {
    pool.addAll(points);
  }

  protected Iterator<Point> points ()
  {
    return data.iterator();
  }

  public void reset ()
  {
    free(data);
    data.clear();
  }

  void data (double x, double y, double w, double r)
  {
    Point p = allocate();

    p.x = x;
    p.y = y;
    p.w = w;
    p.r = r;

    data.add(p);
  }

  public void data (double x, double y, double w)
  {
    data(x, y, w, x);
  }

  public void data (double x, double y)
  {
    data(x, y, 1);
  }

  public abstract void fit ();

  public abstract double coefficient (int j);

  public double standardError (int i)
  {
    throw new UnsupportedOperationException();
  }

  public double covariance (int i, int j)
  {
    throw new UnsupportedOperationException();
  }
}