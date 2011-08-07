/**
 * 
 */
package edu.stanford.facs.logicle;

/**
 * Thrown to indicate that the argument of the FastLogicle function is out of range.
 * 
 * @author Wayne A. Moore
 * @version 1.0
 */
public class LogicleArgumentException
  extends IllegalArgumentException
{
  /**
   * The serial version identifier
   */
  private static final long serialVersionUID = -6650230539452096072L;

  public LogicleArgumentException (double value)
  {
    super("Illegal argument to Logicle scale: " + Double.toString(value));
  }

  public LogicleArgumentException (int index)
  {
    super("Illegal argument to Logicle inverse: " + Integer.toString(index));
  }
}
