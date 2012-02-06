package edu.stanford.facs.compensation;

import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import edu.stanford.facs.compensation.Estimator.Point;

public class LeastSquaresLine
		extends LineEstimator
{
	public LeastSquaresLine(Stack<Point> pool, List<Point> data)
	{
		super(pool, data);
	}

	private double slopeSigma;
	private double interceptSigma;
	private double covariance;

	public void fit ()
	{
		Point p;

		double S = 0;
		double Sx = 0;
		double Sy = 0;
		Iterator<Point> i = data.iterator();
		while (i.hasNext())
		{
			p = i.next();
			double v = p.w * p.w;
			S += v;
			Sx += v * p.x;
			Sy += v * p.y;
		}
		i = data.iterator();
		double Stt = 0;
		double Sty = 0;
		while (i.hasNext())
		{
			p = i.next();
			double t = p.w * (p.x - Sx / S);
			Stt += t * t;
			Sty += p.w * t * p.y;
		}

		slope = Sty / Stt;
		intercept = (Sy - Sx * slope) / S;
		slopeSigma = Math.sqrt(1 / Stt);
		interceptSigma = Math.sqrt((1 + Sx * Sx / S / Stt) / S);
		covariance = -Sx / S / Stt;
	}

	public double getSlopeSigma ()
	{
		return slopeSigma;
	}

	public double getInterceptSigma ()
	{
		return interceptSigma;
	}

	public double getCovariance ()
	{
		return covariance;
	}

	public double chiSquared ()
	{
		double chi2 = 0;
		for (Point p : data)
		{
			double yhat = slope * p.x + intercept;
			double d = p.w * (yhat - p.y);
			chi2 += d * d;
		}

		return chi2;
	}

	public int degreesOfFreedom ()
	{
		return data.size() - 2;
	}

	public double goodnessOfFit ()
	{
		return Tools.Q(degreesOfFreedom() / 2, chiSquared() / 2);
	}
}
