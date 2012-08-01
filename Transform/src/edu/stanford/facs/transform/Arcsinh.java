package edu.stanford.facs.transform;

/**
 * Arcsinh display transform
 * 
 * Maps a data value onto the interval [0,1] such that:
 * <ul>
 *   <li>data value T is mapped to 1</li>
 *   <li>large data values are mapped to locations similar to an M + A decade logarithmic scale</li>
 *   <li>A decades of negative data are brought on scale.</li>
 * </ul>
 * Equivalent to Logicle(T,0,M,A)
 * 
 * @author wmoore
 *
 */

public class Arcsinh
		extends Transform
{
  /**
   * Formal parameter of the arcsinh scale as defined in the Gating-ML standard.
   */
  public final double T, M, A;

  /**
   * Actual parameter of the arcsinh scale as implemented
   */
  public final double a, b, c;

  protected Arcsinh (double T, double M, double A, int bins)
  {
    if (T <= 0)
      throw new TransformParameterException("T is not positive");
    if (M <= 0)
      throw new TransformParameterException("M is not positive");
    if (A < 0)
      throw new TransformParameterException("A is negative");

    // standard parameters
    this.T = T;
    this.M = M;
    this.A = A;

    // actual parameters
    b = (M + A) * LN_10;
    c = A * LN_10;
    a = T / Math.sinh(b - c);
  }

	@Override
	public double scale (double value)
	{
		double x = value / a;
		double asinhx = Math.log(x + Math.sqrt(x*x + 1));
		return (asinhx + c) / b;
	}

	@Override
	public double inverse (double scale)
	{
		return a * Math.sinh(b * scale - c);
	}

	@Override
	double[] axisLabels ()
	{
		// TODO Auto-generated method stub
		return null;
	}
}
