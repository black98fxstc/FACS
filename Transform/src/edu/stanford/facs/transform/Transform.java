/**
 * 
 */
package edu.stanford.facs.transform;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * @author wmoore
 *
 */
public abstract class Transform
{
  /**
   * Number of decades in default scale
   */
  public static final double DEFAULT_DECADES = 4.5;
  
  protected static final double LN_10 = Math.log(10);

	abstract public double scale (final double value);
	
	abstract public double inverse (final double scale);
	
	double[] axisLabels ()
	{
		throw new NotImplementedException();
	}
}
