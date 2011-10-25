package edu.stanford.facs.desk;

/**
 * Title:        EDesk Utilities
 * Description:
 * Copyright:    Copyright (c) 2000 by The Board of Trustees of the Leland Stanford Jr. University
 * Company:      Stanford University
 * @author Wayne A. Moore
 * @version 1.0
 */

public class FacsChannel
{
  public final String  sensor;
  public final String  reagent;
  public final int  bits;
  public final float scaleMinimum, scaleMaximum;
  public final boolean isLogScale;

  public FacsChannel(
    String  sensor,
    String  reagent,
    int  bits,
    float scaleMinimum,
    float scaleMaximum,
    boolean isLogScale )
  {
    this.sensor = sensor;
    this.reagent = reagent;
    this.bits = bits;
    this.scaleMinimum = scaleMinimum;
    this.scaleMaximum = scaleMaximum;
    this.isLogScale = isLogScale;
  }
}