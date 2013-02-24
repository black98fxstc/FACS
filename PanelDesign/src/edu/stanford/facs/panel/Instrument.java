package edu.stanford.facs.panel;

import java.util.*;

public abstract class Instrument
		extends PanelComponent
{
	public final List<Detector> detectors;
	
	public final static class BandPass
	{
		public final int nmMin;
		public final int nmMax;
		
		public BandPass (int nmMin, int nmMax)
		{
			this.nmMin = nmMin;
			this.nmMax = nmMax;
		}
	}
	
	public abstract class Detector
	{
		public final int position;
		public final String laser;
		public final int wavelength;
		public final int laserPower;
		public final BandPass[] bandPass;
		public final Set<String> names;

		protected Detector(int position, Set<String> names, String laser, int wavelength, int power,
				BandPass[] bandPass)
		{
			this.position = position;
			this.names = Collections.unmodifiableSet(names);
			this.laser = laser;
			this.wavelength = wavelength;
			this.laserPower = power;
			this.bandPass = bandPass;
		}
	}
	
	Instrument(String name, List<? extends Detector> detectors)
	{
		super(name);
		this.detectors = Collections.unmodifiableList(detectors);
	}
	
	public abstract Detector detector (String name);
	public abstract Detector detector (Fluorochrome fluorochrome);
}
