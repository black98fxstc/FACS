package edu.stanford.facs.transform;

/**
 * Arcsinh display transform.
 * 
 * Maps a data value onto the interval [0,1] such that:
 * <ul>
 * <li>data value T is mapped to 1</li>
 * <li>large data values are mapped to locations similar to an M + A decade
 * logarithmic scale</li>
 * <li>A decades of negative data are brought on scale.</li>
 * </ul>
 * Equivalent to Logicle(T,0,M,A)
 * 
 * @author Wayne A. Moore
 * @version 1.0
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

	protected Arcsinh(double T, double M, double A, int bins)
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

	public Arcsinh(double T, double M, double A)
	{
		this(T, M, A, 0);
	}

	@Override
	public double scale (double value)
	{
		double x = value / a;
		// this formula for the arcsinh loses significance when x is negative
		// therefore we take advantage of the fact that sinh is an odd function
		boolean negative = x < 0;
		if (negative)
			x = -x;
		double asinhx = Math.log(x + Math.sqrt(x * x + 1));
		if (negative)
			return (c - asinhx) / b;
		else
			return (asinhx + c) / b;
	}

	@Override
	public double inverse (double scale)
	{
		return a * Math.sinh(b * scale - c);
	}

	@Override
	protected double slope (double scale)
	{
		return a * b * Math.cosh(b * scale - c);
	}

	@Override
	public double[] axisLabels ()
	{
		// smallest power of 10 in the positive logarithmic region
		double log10x = Math.ceil(Math.log(T) / LN_10 - M);
		// data value at that point
		double x = Math.exp(LN_10 * log10x);
		// number of positive labels
		int np;
		if (x > T)
		{
			x = T;
			np = 1;
		}
		else
			np = (int) (Math.floor(Math.log(T) / LN_10 - log10x)) + 1;
		// bottom of scale
		double B = this.inverse(0);
		// number of negative labels
		int nn;
		if (x > -B)
			nn = 0;
		else if (x == T)
			nn = 1;
		else
			nn = (int) Math.floor(Math.log(-B) / LN_10 - log10x) + 1;

		// fill in the axis labels
		double[] label = new double[nn + np + 1];
		label[nn] = 0;
		for (int i = 1; i <= nn; ++i)
		{
			label[nn - i] = -x;
			label[nn + i] = x;
			x *= 10;
		}
		for (int i = nn + 1; i <= np; ++i)
		{
			label[nn + i] = x;
			x *= 10;
		}

		return label;
	}
}
