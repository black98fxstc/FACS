package edu.stanford.facs.transform;

/**
 * Thrown by constructors to indicate that the parameters of the scale requested
 * are not valid.
 * 
 * @author Wayne A. Moore
 * @version 1.0
 */
public class TransformParameterException
		extends IllegalArgumentException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -1130649252534189960L;

	public TransformParameterException(String string)
	{
		super(string);
	}
}
