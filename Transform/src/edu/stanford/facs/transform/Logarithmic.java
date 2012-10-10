package edu.stanford.facs.transform;

/**
 * Logarithmic display transform.
 * 
 * Maps a data value onto the interval [0,1] such that:
 * <ul>
 * <li>data value T is mapped to 1</li>
 * <li>M decades of data are mapped into the interval
 * </ul>
 * 
 * @author Wayne A. Moore
 * @version 1.0
 */
public class Logarithmic
		extends Transform
{
	/**
	 * Formal parameter of the logarithmic scale as defined in the Gating-ML
	 * standard.
	 */
	public final double T, M;

	/**
	 * Actual parameter of the logarithmic scale as implemented
	 */
	public final double a, b;

	protected Logarithmic(double T, double M, int bins)
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

	public Logarithmic(double T, double M)
	{
		this(T, M, 0);
	}

	public Logarithmic(double T)
	{
		this(T, DEFAULT_DECADES, 0);
	}

	public double scale (double value)
	{
		return Math.log(value / a) / b;
	}

	public double inverse (double scale)
	{
		return a * Math.exp(b * scale);
	}

	protected double slope (double scale)
	{
		return a * b * Math.exp(b * scale);
	}

	public double[] axisLabels ()
	{
		// smallest power of 10 in the display region
		double log10x = Math.ceil(Math.log(T) / LN_10 - M);
		// data value at that point
		double x = Math.exp(LN_10 * log10x);
		// number of labels
		int n;
		if (x > T)
		{
			x = T;
			n = 0;
		}
		else
			n = (int) (Math.floor(Math.log(T) / LN_10 - log10x));

		// fill in the axis labels
		double[] label = new double[n + 1];
		for (int i = 0; i <= n; ++i)
		{
			label[i] = x;
			x *= 10;
		}

		return label;
	}
}
