package edu.stanford.facs.desk;

/**
 * Title:        EDesk Utilities
 * Description:
 * Copyright:    Copyright (c) 2000 by The Board of Trustees of the Leland Stanford Jr. University
 * Company:      Stanford University
 * @author Wayne A. Moore
 * @version 1.0
 */

public class FacsSample
{
  public final String  coord;
  public final String  label;
  public final int  events;
  public final int  warehouse;
  public final FacsChannel[]  channel;

  public FacsSample (
    String  coord,
    String  label,
    int  events,
    int  warehouse,
    FacsChannel[]  channel )
  {
    this.coord = coord;
    this.label = label;
    this.events = events;
    this.warehouse = warehouse;
    this.channel = channel;
  }
}