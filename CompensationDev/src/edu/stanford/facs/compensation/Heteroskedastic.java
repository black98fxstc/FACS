package edu.stanford.facs.compensation;

import java.util.List;
import java.util.Stack;

public class Heteroskedastic
  extends AggregateEstimator
{
  public Heteroskedastic (Stack<Point> pool, List<Point> data)
  {
    super(pool, data, 3);
  }

  protected void aggregate (final int i, final int N, Point q)
  {
    double sumx = 0;
    double sig2 = 0;
    double chi2 = 0;
    for (int j = 0; j < N; ++j)
    {
      Point p = raw.get(i + j);
      sumx += p.x;
      chi2 += p.y * p.y;
      sig2 += 1 / p.w / p.w;
    }
		q.x = sumx / N;
		q.y = chi2 / N;
		q.w = 1 / (sig2 / N * Math.sqrt(2.0 / N));
  }
  
  public void aggregate()
  {
  	aggregate(16);
  }
  
  public void fit ()
  {
    aggregate(16);
    super.fit();
  }
}
