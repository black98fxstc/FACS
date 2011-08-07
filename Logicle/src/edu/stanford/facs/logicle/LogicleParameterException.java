/**
 * 
 */
package edu.stanford.facs.logicle;

/**
 * Thrown by constructors to indicate that the parameters of the Logicle 
 * scale requested are not valid.
 * 
 * @author Wayne A. Moore
 * @version 1.0
 */
public class LogicleParameterException
  extends IllegalArgumentException
{
  /**
   * Serial Version Identifier
   */
  private static final long serialVersionUID = 6759739726347190123L;

  public LogicleParameterException (String string)
  {
    super(string);
  }
}
