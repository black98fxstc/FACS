package edu.stanford.facs.logicle;

public class SimpleLogicle
  extends Logicle
{
  private final double bod;
  private final double bodp1;

  public SimpleLogicle (double T, double W, double M, double A)
  {
    super(T, W, M, A);

    // save constants for later
    bod = b / d;
    bodp1 = bod + 1;
  }

  public SimpleLogicle (double T, double W, double M)
  {
    this(T, W, M, 0);
  }

  public SimpleLogicle (double T, double W)
  {
    this(T, W, DEFAULT_DECADES, 0);
  }

  @Override
  public double scale (double value)
  {
    // reflect negative values
    boolean negative = value < 0;
    if (negative)
      value = -value;

    // initial guess at solution
    double x;
    if (value > f)
      x = Math.log(value / a) / b;
    else
      x = x1 + value / taylor[0];
    // change to new variables
    double z = Math.exp(d * x);

    double fmy = f - value;
    for (int i = 0; i < 10; ++i)
    {
      // compute the function and two derivatives
      double z2bod = Math.pow(z, bod);
      double g = a * z2bod * z + fmy * z - c;
      double dg = a * bodp1 * z2bod + fmy;
      double ddg = a * bodp1 * bod * z2bod / z;

      // Halley's method
      double delta = g / (dg * (1 - g * ddg / (2 * dg * dg)));
      z -= delta;
      
      if (Math.abs(delta) < 2 * Math.ulp(z))
      {
        // change variables back
        x = Math.log(z) / d;
        // handle negative arguments
        if (negative)
          return 2 * x1 - x;
        else
          return x;
      }
    }

    throw new IllegalStateException("too many iterations");
  }
}
