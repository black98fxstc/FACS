package edu.stanford.facs.panel;

public class MarkerStaining
{
	public final Marker marker;
	public final int level;
	public final boolean important;
	
	public MarkerStaining(Marker marker, int level, boolean important)
	{
		this.marker = marker;
		this.level = level;
		this.important = important;
	}
}
