package edu.stanford.facs.logicle;

public class OldLogicle
extends Logicle
{
  private final double maximumValue;

  public OldLogicle (double T, double W, double M, double A)
  {
    super(T, W, M, A);
    maximumValue = inverse(1);
  }

  public double scale (double value)
  {
    // handle true zero separately
    if (value == 0)
      return x1;

    // reflect negative values
    boolean negative = value < 0;
    if (negative)
      value = -value;

    // establish a bracket around the solution
    // so that x_lo < scale(value) < x_hi
    double x_lo = x1; // because we've dealt with 0
    double x_hi = 1; // start with the normal range
    double e2bx;
    double e2dx;
    if (value >= maximumValue)
    {
      e2bx = Math.exp(b); // uh oh, out of range
      e2dx = Math.exp(d);
      double y_max;
      do
      {
        x_hi *= 2; // double the upper bound
        e2bx *= e2bx;
        e2dx *= e2dx;
        y_max = a * e2bx - (c / e2dx - f);
      } while (value >= y_max); // until we've bracketed the root
    }

    // first step is a bisection
    double x = (x_lo + x_hi) / 2;
    double last_delta = x_hi - x_lo;
    double delta;

    // evaluate the biexponential and its derivative
    e2bx = Math.exp(b * x);
    e2dx = Math.exp(d * x);
    double y = (a * e2bx - value) - (c / e2dx - f);

    for (int i = 0;; ++i)
    {
      if (i == 100)
        throw new IllegalStateException("too many iterations in scale()");

      double dy = a * b * e2bx + c * d / e2dx;
      // if Newton's method would step out of the bracket
      // or if it isn't converging quickly enough
      if (((x - x_hi) * dy - y) * ((x - x_lo) * dy - y) >= 0
        || Math.abs(2 * y) > Math.abs(last_delta * dy))
      {
        // take a bisection step
        delta = (x_hi - x_lo) / 2;
        x = x_lo + delta;
        if (x == x_lo)
          break;
      }
      else
      {
        // otherwise take a Newton's method step
        delta = y / dy;
        double t = x;
        x -= delta;
        if (x == t)
          break;
      }
      // if we've converged to the desired precision then we're done
      if (Math.abs(delta) < Math.ulp(1D))
        break;
      last_delta = delta;

      // evaluate the biexponential and its derivative again
      e2bx = Math.exp(b * x);
      e2dx = Math.exp(d * x);
      if (x < xTaylor)
        // near zero use the Taylor series
        y = seriesBiexponential(x) - value;
      else
        // this formulation has better roundoff behavior
        y = (a * e2bx - value) - (c / e2dx - f);
      if (y == 0) // can't get closer than this to the root!
        break;

      // update the bracketing interval
      if (y < 0)
        x_lo = x;
      else
        x_hi = x;
    }
    
    // if the root is very close to zero use a simple linear approximation
    if (x - x1 < Math.ulp(1D))
      x = x1 + value/taylor[0];

    // handle negative arguments
    if (negative)
      return 2 * x1 - x;
    else
      return x;
  }

}
