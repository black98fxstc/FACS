package edu.stanford.facs.compensation;

import java.util.List;
import java.util.Stack;

import edu.stanford.facs.compensation.Estimator.Point;

public class RobustHeteroskedastic
  extends AggregateEstimator
{
  public RobustHeteroskedastic (Stack<Point> pool, List<Point> data)
  {
    super(pool, data, 3);
  }

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
      selector.add(p.y * p.y);
    }
    q.x = sumx / n;
    q.y = selector.median() / Tools.MEDIAN_CHI_SQUARED_1;
    q.w = 1 / (sig2 / n * Math.sqrt(2));
  }
  
  public void aggregate ()
  {
  	aggregate(65);
  }

  public void fit ()
  {
    aggregate(65);
    super.fit();
  }
}
