package edu.stanford.facs.compensation;

import java.awt.Color;
import java.util.ArrayList;

public class Diagnostic implements Comparable<Diagnostic>
{
	public final double importance;
	public final int reagent;
	public final int detector;
    public final String reagentName;
	public final String message;
    public final Color myorange;
    public final Color mypink;
	
	public static class List extends ArrayList<Diagnostic>
	{	}
	
	Diagnostic (double importance, int reagent, int detector, String reagentName, String message)
	{
		this.importance = importance;
		this.reagent = reagent;
		this.detector = detector;
        this.reagentName = reagentName;
		this.message = message;
        myorange = new Color (204, 204, 255);
        mypink = new Color (238,64, 64);
	}
	
	public Color color ()
	{
		if (importance >= .75) //this used to be pink
			return mypink;
		if (importance >= .5) //this used to be orange
			return myorange;
		if (importance >= .25)
			return Color.YELLOW;
		return Color.WHITE;
	}
	
	public String toString ()
	{
        
		return reagentName + ": " +message;
	}

	//override
	public int compareTo (Diagnostic that)
	{
		if (this.importance < that.importance)
			return 1;
		if (this.importance > that.importance)
			return -1;
		return 0;
	}


}
