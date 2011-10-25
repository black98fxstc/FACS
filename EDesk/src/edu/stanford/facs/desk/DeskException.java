package edu.stanford.facs.desk;

import java.io.IOException;

/** Description of class
 *
 * @author Wayne A. Moore
 * @version 1.0
 */

public class DeskException
extends java.io.IOException
{
  public DeskException (
	  String  detail )
  {
    super(detail);
  }
}