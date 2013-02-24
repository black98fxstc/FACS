package edu.stanford.facs.panel;

public abstract class StainSet
implements Comparable<StainSet>
{
	public final float index;
	
	public abstract int size ();
	public abstract Marker marker (int i);
	public abstract Fluorochrome fluorochrome (int i);
	public abstract boolean isIndirect (int i);
	public abstract Hapten hapten (int i);
	
	@Override
	public int compareTo (StainSet that)
	{
		// stains with higher staining index come first
		// not consistent with equals since different stains may have the same index
		if (this.index > that.index)
			return -1;
		if (this.index < that.index)
			return 1;
		return 0;
	}

	protected StainSet (double index)
	{
		this.index = (float) index;
	}
}
