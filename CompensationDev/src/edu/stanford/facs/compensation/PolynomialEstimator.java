package edu.stanford.facs.compensation;

import java.util.List;
import java.util.Stack;

public class PolynomialEstimator
  extends Estimator
{
  protected final int MAX_RANK;
  protected final double[] coefficient;
  protected final double[][] covariance;
  protected int rank;
  
  private final double [] x2n;

  PolynomialEstimator (Stack<Point> pool, List<Point> data, int max)
  {
    super(pool, data);
    MAX_RANK = max;
    coefficient = new double[MAX_RANK];
    covariance = new double[MAX_RANK][MAX_RANK];
    x2n = new double[MAX_RANK];
  }

  
  public double coefficient (int j)
  {
    return coefficient[j];
  }

 
  public double covariance (int i, int j)
  {
    return covariance[i][j];
  }

  public double[] coefficients (double[] coefficients)
  {
    if (coefficients == null || coefficients.length != this.coefficient.length)
      coefficients = this.coefficient.clone();
    else
      System.arraycopy(this.coefficient, 0, coefficients, 0,
        coefficients.length);
    return coefficients;
  }

 
  public double standardError (int i)
  {
    return Math.sqrt(covariance(i, i));
  }

  public void fit (int n)
  {
  	rank = n;
  	
  	double[][] alpha = covariance;
    for (int i = 0; i < MAX_RANK; ++i)
      for (int j = 0; j < MAX_RANK; ++j)
        alpha[i][j] = 0;
    double[] beta = coefficient;
    for (int i = 0; i < MAX_RANK; ++i)
      beta[i] = 0;

    for (Point p : data)
    {
      x2n[0] = 1;
      for (int i = 1; i < n; ++i)
        x2n[i] = p.x * x2n[i-1];
      double w2 = p.w * p.w;
      for (int i = 0; i < n; ++i)
      {
        for (int j = 0; j <= i; ++j)
          alpha[i][j] += w2 * x2n[i] * x2n[j];
        beta[i] += w2 * p.y * x2n[i];
      }
      for (int i = 1; i < n; ++i)
        for (int j = 0; j < i; ++j)
          alpha[j][i] = alpha[i][j];
    }

    Tools.gaussJordan(alpha, beta, n);
  }
  
  public double chiSquared ()
  {
    double chi2 = 0;
    for (Point p : data)
    {
      double yhat = coefficient[rank - 1];
      for (int i = rank - 2; i >= 0; --i)
        yhat = yhat * p.x + coefficient[i];
      double d = p.w * (yhat - p.y);
      chi2 += d * d;
    }
    
    return chi2;
  }
  
  public int degreesOfFreedom ()
  {
  	return data.size() - rank;
  }
  
  public double goodnessOfFit ()
  {
  	return Tools.Q(degreesOfFreedom()/2, chiSquared()/2);
  }

  
  public void fit ()
  {
    fit(MAX_RANK);
  }

  public static double evaluate (double[] coefficient, double argument)
  {
    int n = coefficient.length;
    double value = coefficient[n - 1];
    for (int i = n - 2; i >= 0; --i)
      value = value * argument + coefficient[i];
    return value;
  }
}