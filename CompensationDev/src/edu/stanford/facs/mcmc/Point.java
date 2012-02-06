package edu.stanford.facs.mcmc;

public class Point
{
  public Point (int i, int j, int p, double x, double y, double residual)
  {
    super();
    this.i = i;
    this.j = j;
    this.p = p;
    this.x = x;
    this.y = y;
    this.residual = residual;
  }

  public final int i;
  public final int j;
  public final int p;
  public final double x;
  public final double y;
  public final double residual;

  public double lnP (State s)
  {
    double slope = s.spillover[i][j];
    double variance = (s.backgroundSignal[j] + s.signalPerPhoton[j] * slope * x)
      + slope * slope
      * (s.backgroundSignal[p] + s.signalPerPhoton[p] * x);
    return -.5 * residual / variance - Math.log(variance);
  }
}
