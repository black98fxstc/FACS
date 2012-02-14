package edu.stanford.facs.compensation;

import java.util.List;
import java.util.Stack;

public class RobustLine2
  extends AggregateEstimator
{

  RobustLine2 (Stack<Point> pool, List<Point> data)
  {
    super(pool, data, 2);
  }

  //override
  protected void aggregate (final int i, final int n, final Point q)
  {
    Selector selector = Tools.getSelector();
    
    double sumx = 0;
    double sig2 = 0;
    selector.reset();
    for (int j = 0; j < n; ++j)
    {
      Point p = raw.get(i + j);
      sumx += p.x;
      sig2 += 1 / p.w / p.w;
      selector.add(p.y);
    }
    q.x = sumx / n;
    q.y = selector.median();
    q.w = 1 / Math.sqrt(sig2/n/n * Math.PI/2);
  }

  public void fit ()
  {
    aggregate(33);
    super.fit();
  }

  public double getSlope ()
  {
    return coefficient[1];
  }

  public double getIntercept ()
  {
    return coefficient[0];
  }
}