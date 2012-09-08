package edu.stanford.facs.transform;

/**
 * Linear display transform.
 * 
 * Maps a data value onto the interval [0,1] linearly such that:
 * <ul>
 * <li>data value T is mapped to 1</li>
 * <li>data value -A is mapped to 0</li>
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

	protected Linear(double T, double A, int bins)
	{
		if (T <= 0)
			throw new TransformParameterException("T is not positive");
		if (T <= A)
			throw new TransformParameterException("T is not greater than A");
		if (A < 0)
			throw new TransformParameterException("A is negative");

		// standard parameters
		this.T = T;
		this.A = A;

		// actual parameters
		a = (T + A);
		b = -A;
	}

	public Linear(double T, double A)
	{
		this(T, A, 0);
	}

	public Linear(double T)
	{
		this(T, 0, 0);
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

	@Override
	protected double slope (double scale)
	{
		return a;
	}

	@Override
	public double[] axisLabels ()
	{
		// total range of the data
		double r = T - A;
		// largest power of 10 in the range
		double log10x = Math.floor(Math.log(r) / LN_10);
		double x = Math.exp(log10x * LN_10);

		// chose an interval x that gives at least three but not more than six
		// labels
		// suitable for use by monkeys, i.e., pretty combinations of 2, 5 and 10
		double n = r / x;
		if (n <= 1.2)
			x /= 5;
		else if (n <= 1.5)
			x /= 4;
		else if (n <= 3)
			x /= 2;
		else if (n >= 7)
			x *= 2;

		int np = (int) Math.floor(T / x);
		int nn = (int) Math.floor(A / x);

		// fill in the axis labels
		double[] label = new double[nn + np + 1];
		label[nn] = 0;
		for (int i = 1; i <= nn; ++i)
		{
			label[nn - i] = -x * i;
			label[nn + i] = x * i;
		}
		for (int i = nn + 1; i <= np; ++i)
		{
			label[nn + i] = x * i;
		}

		return label;
	}
}
