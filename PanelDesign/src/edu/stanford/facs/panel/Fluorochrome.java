package edu.stanford.facs.panel;

import edu.stanford.facs.panel.Instrument.Detector;

public abstract class Fluorochrome
		extends PanelComponent
{
	public final double brightness;
	
	public abstract double distance (Fluorochrome that);
	
	public abstract double emissionEffiency (Detector detector);

	public abstract double excitationEffiency (Detector detector);

	public Fluorochrome(String name, double brightness)
	{
		super(name);
		this.brightness = brightness;
	}
}
