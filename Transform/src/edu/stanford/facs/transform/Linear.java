package edu.stanford.facs.transform;
/**
 * Linear display transform.
 * 
 * Maps a data value onto the interval [0,1] linearly such that:
 * <ul>
 *   <li>data value T is mapped to 1</li>
 *   <li>data value -A is mapped to 0</li>
 * </ul>
 * 
 * @author Wayne A. Moore
 * @version 1.0
 */

public class Linear
		extends Transform
{
  /**
   * Formal parameter of the linear scale as defined in the Gating-ML standard.
   */
  public final double T, A;

  /**
   * Actual parameter of the linear scale as implemented
   */
  public final double a, b;

  protected Linear (double T, double A, int bins)
  {
    if (T <= 0)
      throw new TransformParameterException("T is not positive");
    if (T <= A)
      throw new TransformParameterException("T is not greater than A");

    // standard parameters
    this.T = T;
    this.A = A;

    // actual parameters
    a = (T - A);
    b = -A / a;
  }

	@Override
	public double scale (double value)
	{
		return (value - b) / a;
	}

	@Override
	public double inverse (double scale)
	{
		return a * scale + b;
	}
}
