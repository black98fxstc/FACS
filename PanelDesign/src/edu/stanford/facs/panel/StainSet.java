package edu.stanford.facs.panel;

public abstract class StainSet
implements Comparable<StainSet>
{
	public final double index;
	
	public abstract int size ();
	public abstract Marker marker (int i);
	public abstract Fluorochrome fluorochrome (int i);
	public abstract boolean isIndirect (int i);
	public abstract Hapten hapten (int i);
	
	@Override
	public int compareTo (StainSet that)
	{
		if (this.index > that.index)
			return -1;
		if (this.index < that.index)
			return 1;
		return 0;
	}

	protected StainSet (double index)
	{
		this.index = index;
	}
}
