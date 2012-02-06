package edu.stanford.facs.compensation;

import java.util.List;
import java.util.Stack;

import edu.stanford.facs.compensation.Estimator.Point;


public abstract class LineEstimator
extends Estimator
{
  LineEstimator (Stack<Point> pool, List<Point> data)
  {
    super(pool, data);
  }

  protected double slope;
  protected double intercept;

  public double getSlope ()
  {
    return slope;
  }

  public double getIntercept ()
  {
    return intercept;
  }

  @Override
  public double coefficient (int j)
  {
    throw new UnsupportedOperationException();
  }
}
