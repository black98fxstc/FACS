package edu.stanford.facs.transform;

/**
 * Logarithmic display transform
 * 
 * Maps a data value onto the interval [0,1] such that:
 * <ul>
 *   <li>data value T is mapped to 1</li>
 *   <li>M decades of data are mapped into the interval
 * </ul>
 * 
 * @author wmoore
 *
 */
public class Logarithmic
		extends Transform
{
  /**
   * Formal parameter of the logarithmic scale as defined in the Gating-ML standard.
   */
  public final double T, M;

  /**
   * Actual parameter of the logarithmic scale as implemented
   */
  public final double a, b;

  protected Logarithmic (double T, double M, int bins)
  {
    if (T <= 0)
      throw new TransformParameterException("T is not positive");
    if (M <= 0)
      throw new TransformParameterException("M is not positive");

    // standard parameters
    this.T = T;
    this.M = M;

    // actual parameters
    b = M * LN_10;
    a = T / Math.exp(b);
  }

	@Override
	public double scale (double value)
	{
		return Math.log(value / a) / b;
	}

	@Override
	public double inverse (double scale)
	{
		return a * Math.exp(b * scale);
	}

	@Override
	double[] axisLabels ()
	{
		// TODO Auto-generated method stub
		return null;
	}
}
