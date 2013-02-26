/**
 * 
 */
package edu.stanford.facs.panel;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import edu.stanford.facs.panel.Instrument.BandPass;
import edu.stanford.facs.panel.Instrument.Detector;

/**
 * @author wmoore
 * 
 */
public class PanelTest
		implements PanelFactory
{
	private final static int NM_MAX = 1000;
	private final static int NM_MIN = 200;
	private final static int NM_BAND = NM_MAX - NM_MIN;

	private final Map<String, Marker> markers = new HashMap<String, Marker>();
	private final Map<String, Hapten> haptens = new HashMap<String, Hapten>();
	private final Map<String, Fluorochrome> fluorochromes = new HashMap<String, Fluorochrome>();
	private final Map<String, Instrument> instruments = new HashMap<String, Instrument>();

	private ZipFile spectraZip;
	
	private static class TestDetector
			extends Instrument.Detector
	{
		protected TestDetector(Instrument instrument, int position, Set<String> names, String laser,
				int wavelength, int power, BandPass[] bandPass)
		{
			instrument.super(position, names, laser, wavelength, power, bandPass, 0);
		}
	}
	
	private static class TestInstrument
			extends Instrument
	{
		private Map<String, Detector> nameMap;
		private Map<Fluorochrome, Detector> fluorochromeMap = new HashMap<Fluorochrome, Detector>();
		
		public TestInstrument(String name, List<TestDetector> detectors)
		{
			super(name, detectors);
		}

		@Override
		public Detector detector (String name)
		{
			if (nameMap == null)
			{
				nameMap = new HashMap<String, Detector>();
				for (Detector detector : detectors)
					for (String detector_name : detector.names)
						nameMap.put(detector_name, detector);
			}
			
			return nameMap.get(name);
		}

		@Override
		public Detector detector (Fluorochrome fluorochrome)
		{
			Detector detector = fluorochromeMap.get(fluorochrome);
			if (detector == null)
			{
				double peakExcitation = 0;
				double peakEmission = 0;
				int peakWavelength = 0;
				int lastWavelength = 0;
				for (int i = 0; i < detectors.size(); ++i)
				{
					Detector d = detectors.get(i);
					if (lastWavelength != d.wavelength)
					{
						double excitation = fluorochrome.excitationEffiency(d);
						if (excitation > peakExcitation)
						{
							peakExcitation = excitation;
							peakWavelength = d.wavelength;
							peakEmission = 0;
							detector = null;
						}
					}
					if (peakWavelength == d.wavelength)
					{
						double emission = fluorochrome.emissionEffiency(d);
						if (emission > peakEmission)
						{
							peakEmission = emission;
							detector = d;
						}
					}
					lastWavelength = d.wavelength;
				}
				assert detector != null : "No detector for " + fluorochrome.name;
				fluorochromeMap.put(fluorochrome, detector);
			}
			
			return detector;
		}
	}
	
	private static class TestMarker
			extends Marker
	{
		TestMarker(String name)
		{
			super(name);
		}
	}

	private static class TestHapten
			extends Hapten
	{
		public TestHapten(String name)
		{
			super(name);
		}
	}

	private static class FluorochromeBrightness
	{
		final String name;
		final String instrument;
		final String detector;
		final double spe;

		public FluorochromeBrightness(String name, String instrument,
				String detector, double spe)
		{
			this.name = name;
			this.instrument = instrument;
			this.detector = detector;
			this.spe = spe;
		}
	}

	private static class TestFluorochrome
			extends Fluorochrome
	{
		final double[] excitation;
		final double[] emission;
		final int excitationMaximum;

		TestFluorochrome(String name, double[] excitation, int excitationMaximum,
				double[] emission, double brightness)
		{
			super(name, brightness);
			this.excitation = excitation;
			this.emission = emission;
			this.excitationMaximum = excitationMaximum;
		}

		public double distance (Fluorochrome that)
		{
			return distance((TestFluorochrome) that);
		}

		private double distance (TestFluorochrome that)
		{
			double thisEmission = 0;
			double thatEmission = 0;
			double emissionDistance = 0;
			double thisExcitation = 0;
			double thatExcitation = 0;
			double excitationDistance = 0;
			for (int i = 0; i < NM_BAND; ++i)
			{
				thisEmission += this.emission[i];
				thatEmission += that.emission[i];
				emissionDistance =
						Math.max(emissionDistance, Math.abs(thisEmission - thatEmission));

				thisExcitation += this.excitation[i];
				thatExcitation += that.excitation[i];
				excitationDistance =
						Math.max(excitationDistance,
								Math.abs(thisExcitation - thatExcitation));
			}
			return emissionDistance + excitationDistance;
		}

		@Override
		public double emissionEffiency (Detector detector)
		{
			return PanelTest.emissionEffiency(emission, detector);
		}

		@Override
		public double excitationEffiency (Detector detector)
		{
			return PanelTest.excitationEffiency(excitation, excitationMaximum, detector);
		}
	}
	
	public PanelTest () throws IOException
	{
		spectraZip = new ZipFile("spectra.zip");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.facs.panel.PanelFactory#getMarker(java.lang.String)
	 */
	@Override
	public Marker getMarker (String name)
	{
		Marker m = markers.get(name);
		if (m == null)
		{
			m = new TestMarker(name);
			markers.put(name, m);
		}
		return m;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.facs.panel.PanelFactory#getHapten(java.lang.String)
	 */
	@Override
	public Hapten getHapten (String name)
	{
		Hapten h = haptens.get(name);
		if (h == null)
		{
			h = new TestHapten(name);
			haptens.put(name, h);
		}
		return h;
	}
	
	public Instrument getInstrument (String name) 
			throws IOException
	{
		Instrument instrument = instruments.get(name);
		if (instrument == null)
		{
			List<TestDetector> detectors = new ArrayList<TestDetector>();
			instrument = new TestInstrument(name, detectors);
			instruments.put(name, instrument);
			
			// parse a Diva style configuration file
			// eventually should replace with instrument.xml

			BufferedReader instrument_reader = new BufferedReader(new FileReader(new File(name + ".csv")));
			String line;
			for (int i = 1; i < 12; ++i)
				line = instrument_reader.readLine();
			int position = 0;
			String detector_name = null;
			String laser_name = null;
			int laser_power = 0;
			int laser_wavelength = 0;
			String band_pass = null;
			int nm_min = 0;
			int nm_max = 0;
			int last_long_pass = 0;
			int this_long_pass = 0;
			TestDetector detector = null;
			Set<String> names = null;
			while ((line = instrument_reader.readLine()) != null)
			{
				String[] field = line.split(",");
				if (field.length < 10 || field[9] == null || field[9].length() == 0)
					continue;
				detector_name = field[9].substring(1, field[9].length() - 1);
				if (field[0] != null && field[0].length() > 0)
				{
					laser_name = field[0];
					laser_wavelength = Integer.parseInt(field[2]);
					laser_power = Integer.parseInt(field[3]);
					assert laser_power > 0;
					last_long_pass = NM_MAX;
				}
				if (field[8] != null && field[8].length() > 0)
				{
					band_pass = field[8];
					if (field[7] != null && field[7].length() > 0)
						this_long_pass =
								Integer.parseInt(field[7].substring(0, field[7].indexOf(' ')));
					else
						this_long_pass = 0;
					int band_mid =
							Integer.parseInt(band_pass.substring(0, band_pass.indexOf('/')));
					int band_width =
							Integer.parseInt(band_pass.substring(band_pass.indexOf('/') + 1,
									band_pass.indexOf(' ')));
					nm_min = band_mid - ((band_width + 1) / 2);
					nm_max = band_mid + ((band_width + 1) / 2);
					if (nm_min < this_long_pass)
						nm_min = this_long_pass;
					if (nm_max > last_long_pass)
						nm_max = last_long_pass;
					last_long_pass = this_long_pass;
					
					BandPass[] bandPass = { new BandPass(nm_min, nm_max) };
					names = new HashSet<String>();
					detector = new TestDetector(instrument, position++, names, laser_name, laser_wavelength, laser_power, bandPass);
					detectors.add(detector);
				}
				names.add(detector_name);
			}
			instrument_reader.close();
		}
		
		return instrument;
	}

	private Map<String, FluorochromeBrightness> fluorochromeBrightness;

	private FluorochromeBrightness getBrightness (String fluorochrome)
			throws IOException
	{
		if (fluorochromeBrightness == null)
		{
			fluorochromeBrightness = new HashMap<String, FluorochromeBrightness>();
			BufferedReader brightness_reader =
					new BufferedReader(new FileReader(new File("brightness.csv")));
			String line = brightness_reader.readLine();
			while ((line = brightness_reader.readLine()) != null)
			{
				String[] field = line.split(",");
				String instrument = field[0];
				String detector = field[1].substring(0, field[1].length() - 2);
				String name = field[3];
				double spe = Double.parseDouble(field[4]);
				FluorochromeBrightness fb =
						new FluorochromeBrightness(name, instrument, detector, spe);
				fluorochromeBrightness.put(name, fb);
			}
			brightness_reader.close();
		}

		return fluorochromeBrightness.get(fluorochrome);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.facs.panel.PanelFactory#getFluorochrome(java.lang.String)
	 */
	@Override
	public Fluorochrome getFluorochrome (String name)
			throws IOException
	{
		Fluorochrome fluorochrome = fluorochromes.get(name);
		if (fluorochrome != null)
			return fluorochrome;

		ZipEntry ze = spectraZip.getEntry(name + ".csv");
		if (ze == null)
			return null;

		// read the spectrum information

		double[] excitation = new double[PanelTest.NM_BAND];
		double[] emission = new double[PanelTest.NM_BAND];
		BufferedReader spectrum_reader =
				new BufferedReader(new InputStreamReader(spectraZip.getInputStream(ze)));
		String line = spectrum_reader.readLine();
		while ((line = spectrum_reader.readLine()) != null)
		{
			String[] values = line.split(",");
			if (values.length < 2)
				continue;
			if (values[0].length() > 0)
			{
				int nm = (int) Math.floor(Double.parseDouble(values[0]));
				double ex = Double.parseDouble(values[1]);
				if (ex < 0)
					ex = 0;
				excitation[nm - PanelTest.NM_MIN] = ex;
			}
			if (values.length < 4)
				continue;
			if (values[2].length() > 0)
			{
				int nm = (int) Math.floor(Double.parseDouble(values[2]));
				double em = Double.parseDouble(values[3]);
				if (em < 0)
					em = 0;
				emission[nm - PanelTest.NM_MIN] = em;
			}
		}
		spectrum_reader.close();

		// normalize the spectra and find the excitation maximum

		double excitation_sum = 0;
		double emission_sum = 0;
		int excitation_max = 0;
		for (int j = 0; j < PanelTest.NM_BAND - 1; j++)
		{
			excitation_sum += excitation[j];
			emission_sum += emission[j];
			if (excitation[j] > excitation[excitation_max])
				excitation_max = j;
		}
		excitation_max += PanelTest.NM_MIN;
		for (int j = 0; j < PanelTest.NM_BAND; j++)
		{
			excitation[j] /= excitation_sum;
			emission[j] /= emission_sum;
		}

		// calculate the detector efficiency

		FluorochromeBrightness brightness = getBrightness(name);
		if (brightness == null)
			return null;
		Detector detector = getInstrument(brightness.instrument).detector(brightness.detector);

		double excitation_efficiency = excitationEffiency(excitation, excitation_max, detector);
		double emmission_efficiency = emissionEffiency(emission, detector);

		fluorochrome =
				new TestFluorochrome(name, excitation, excitation_max, emission,
						brightness.spe
								/ excitation_efficiency
								/ emmission_efficiency
								/ detector.laserPower);
		fluorochromes.put(name, fluorochrome);

		return fluorochrome;
	}

	private static double excitationEffiency (double[] excitation, int excitationMaximum,
			Detector detector)
	{
		double excitation_efficiency =
				excitation[detector.wavelength - PanelTest.NM_MIN]
						/ excitation[excitationMaximum - PanelTest.NM_MIN];
		assert excitation_efficiency >= 0;
		return excitation_efficiency;
	}

	private static double emissionEffiency (double[] emission, Detector detector)
	{
		double emmission_efficiency = 0;
		for (int i = 0; i < detector.bandPass.length; i++)
			for (int wavelength = detector.bandPass[i].nmMin; wavelength <= detector.bandPass[i].nmMax; ++wavelength)
				emmission_efficiency += emission[wavelength - PanelTest.NM_MIN];
		assert emmission_efficiency >= 0;
		return emmission_efficiency;
	}

	/**
	 * @param args
	 */
	public static void main (String[] args)
	{
		int Nmarkers;

		Nmarkers = args.length;
		Nmarkers = 10;

		try
		{
			PanelFactory factory = new PanelTest();

			// read a panel
			
			List<PopulationStaining> populations = new ArrayList<PopulationStaining>();
			BufferedReader panel_reader = new BufferedReader(new FileReader(new File("panel1.csv")));
			String line = panel_reader.readLine();
			String[] markers = line.split("\t");
			String species = markers[0];
			while ((line = panel_reader.readLine()) != null)
			{
				String[] population = line.split("\t");
				List<MarkerStaining> staining = new ArrayList<MarkerStaining>();
				for (int i = 1; i < population.length; ++i)
				{
					Marker marker = factory.getMarker(markers[i]);
					int level = Integer.parseInt(population[i]);
					boolean important = level > 0;
					staining.add(new MarkerStaining(marker, level, important));
				}
				populations.add(new PopulationStaining(population[0], staining));
			}
			panel_reader.close();

			// get a set of markers for the populations

			Set<Marker> markerSet = new HashSet<Marker>();
			for (PopulationStaining population : populations)
				for (MarkerStaining staining : population)
					markerSet.add(staining.marker);

			// initialize direct stains

			Map<Marker, Set<Fluorochrome>> directStains =
					new HashMap<Marker, Set<Fluorochrome>>();
			for (Marker marker : markerSet)
				directStains.put(marker, new HashSet<Fluorochrome>());

			// initialize indirect stains

			Map<Hapten, Set<Marker>> haptenReagents =
					new HashMap<Hapten, Set<Marker>>();
			Map<Hapten, Set<Fluorochrome>> indirectStains =
					new HashMap<Hapten, Set<Fluorochrome>>();

			// make up some biotin/avidin reagents

			Hapten biotin = factory.getHapten("Biotin");
			haptenReagents.put(biotin, new HashSet<Marker>());

			Set<Fluorochrome> avidin = new HashSet<Fluorochrome>();
			avidin.add(factory.getFluorochrome("Fluorescein (FITC)"));
			avidin.add(factory.getFluorochrome("PerCP-Cy5.5"));
			avidin.add(factory.getFluorochrome("Pacific Blue"));
			avidin.add(factory.getFluorochrome("BV421"));
			avidin.add(factory.getFluorochrome("BV510"));
			avidin.add(factory.getFluorochrome("Pacific Orange"));
			avidin.add(factory.getFluorochrome("BV570"));
			avidin.add(factory.getFluorochrome("Qdot 605"));
			avidin.add(factory.getFluorochrome("Qdot 655"));
			avidin.add(factory.getFluorochrome("BV650"));
			avidin.add(factory.getFluorochrome("Qdot 705"));
			avidin.add(factory.getFluorochrome("BV711"));
			avidin.add(factory.getFluorochrome("BV785"));
			avidin.add(factory.getFluorochrome("APC"));
			avidin.add(factory.getFluorochrome("Alexa Fluor 647"));
			avidin.add(factory.getFluorochrome("APC-Cy5.5"));
			avidin.add(factory.getFluorochrome("APC-Cy7"));
			avidin.add(factory.getFluorochrome("PE"));
			avidin.add(factory.getFluorochrome("PE-Texas Red"));
			avidin.add(factory.getFluorochrome("PE-Alexa Fluor 610"));
			avidin.add(factory.getFluorochrome("PE-Cy7"));

			indirectStains.put(biotin, avidin);
			
			// look through the catalog for usable reagents

			int catalogTotal = 0;
			int markerTotal = 0;
			int usefulTotal = 0;
			ZipFile zip = new ZipFile(species + ".zip");
			Enumeration<? extends ZipEntry> e = zip.entries();
			while (e.hasMoreElements())
			{
				ZipEntry ze = e.nextElement();
				BufferedReader spectrum_reader =
						new BufferedReader(new InputStreamReader(zip.getInputStream(ze)));
				line = spectrum_reader.readLine();
				String[] field = line.split("\t");
				int c = -1;
				int a = -1;
				for (int i = 0; i < field.length; i++)
				{
					if (field[i].equals("Conjugate (color or hapten)"))
						c = i;
					if (field[i].equals("Antigen molecule"))
						a = i;
				}
				if (c == -1 || a == -1)
					throw new IllegalStateException("Can't read antibody catalog "
							+ ze.getName());
				while ((line = spectrum_reader.readLine()) != null)
				{
					String[] data = line.split("\t");
					String conjugated = data[c];

					Marker antigen = factory.getMarker(data[a]);
					if (conjugated.equalsIgnoreCase("Unconjugated"))
						continue;
					if (conjugated.equalsIgnoreCase("Alkaline Phosphatase"))
						continue;
					if (conjugated.equalsIgnoreCase("HRP"))
						continue;
					if (conjugated.equalsIgnoreCase("ECD"))
						continue;

					++catalogTotal;
					if (!markerSet.contains(antigen))
						continue;
					++markerTotal;
					if (conjugated.equalsIgnoreCase("Biotin"))
					{
						Set<Marker> biotinMarkers = haptenReagents.get(biotin);
						biotinMarkers.add(antigen);
						++usefulTotal;
					}
					else
					{
						Fluorochrome fluorochrome = factory.getFluorochrome(conjugated);
						if (fluorochrome != null)
						{
							directStains.get(antigen).add(fluorochrome);
							++usefulTotal;
						}
					}
				}
			}
			zip.close();

			System.out.println("full catalog contains " + catalogTotal);
			System.out.println("catalog reagents for markers " + markerTotal);
			System.out.println("useful catalog reagents for markers " + usefulTotal);
			
			// target instrument is always LSR-II for now
			
			Instrument instrument = factory.getInstrument("LSR-II");

			long begin = System.currentTimeMillis();
			PanelDesign designer = new PanelDesign(1, 100);
			
			List<StainSet> results = designer.design(instrument, populations, 
							directStains, haptenReagents, indirectStains);
			
			double seconds = (double) (System.currentTimeMillis() - begin) / 1000D;

			System.out.println("run time " + seconds);
			System.out.println();
			
			for (StainSet stainSet : results)
				stainSet.print();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
