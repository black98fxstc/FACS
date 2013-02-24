package edu.stanford.facs.panel;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

import edu.stanford.facs.panel.Instrument.Detector;

public class PanelDesign
{
	private final static boolean DEBUG = true;
	private final static boolean SINGLE_THREAD = true;
	
	private int nWorkers;
	private int nSolutions;

	private Instrument instrument;
	private Marker[] markers;
	private Fluorochrome[] fluorochromes;
	private Hapten[] haptens;
	private Stain[] catalogStains;
	private MarkerStaining[][] targets;
	private float[][] distance;
	private double[][] spectra;
	private ArrayBlockingQueue<FluorochromeSet> fluorochromeSetQueue = new ArrayBlockingQueue<FluorochromeSet>(5);
	private StainSet[] results;
	private int resultCount = 0;
	private int uselessResults = 0;
	
	private  Worker[] workers;
	
	private final Stack<Solution> toDo = new Stack<Solution>();

	long started = 0;
	long finished = 0;
	int solutions = 0;
	private volatile boolean workersExit = false;

	private class StainSetImpl
			extends StainSet
	{
		short[] stains;

		@Override
		public int size ()
		{
			return stains.length;
		}

		@Override
		public Marker marker (int i)
		{
			return markers[catalogStains[stains[i]].marker];
		}

		@Override
		public Fluorochrome fluorochrome (int i)
		{
			return fluorochromes[catalogStains[stains[i]].fluorochrome];
		}

		@Override
		public boolean isIndirect (int i)
		{
			return catalogStains[stains[i]].hapten >= 0;
		}

		@Override
		public Hapten hapten (int i)
		{
			if (catalogStains[stains[i]].hapten < 0)
				return null;
			else
				return haptens[catalogStains[stains[i]].hapten];
		}

		public StainSetImpl(double index, short[] stains)
		{
			super(index);
			this.stains = stains;
		}
	}

	private static class Combination
	{
		protected final int BIT_SHIFT = 6;
		protected final int BIT_MULT = 1 << BIT_SHIFT;
		protected final int BIT_MASK = BIT_MULT - 1;
		protected final long[] bits;

		Combination(int size)
		{
			// this is actually more than we need but allows us to
			// skip checking a boundry condition when the size is
			// a multiple of 64
			this.bits = new long[size / BIT_MULT + 1];
		}

		Combination(Combination that)
		{
			this.bits = Arrays.copyOf(that.bits, that.bits.length);
		}

		void set (int choice)
		{
			bits[choice >> BIT_SHIFT] |= 1L << (choice & BIT_MASK);
		}

		void clear (int choice)
		{
			bits[choice >> BIT_SHIFT] &= ~(1L << (choice & BIT_MASK));
		}

		boolean get (int choice)
		{
			return (bits[choice >> BIT_SHIFT] & 1L << (choice & BIT_MASK)) != 0;
		}

		int nextSet (int start)
		{
			int i = start >> BIT_SHIFT;
			long mask = (1L << (start & BIT_MASK)) - 1L;
			int j = Long.numberOfTrailingZeros(bits[i] & ~mask);
			if (j < BIT_MULT)
				return i * BIT_MULT + j;
			for (++i; i < bits.length; ++i)
			{
				j = Long.numberOfTrailingZeros(bits[i]);
				if (j < BIT_MULT)
					return i * BIT_MULT + j;
			}
			return i * BIT_MULT;
		}

		int nextClearX (int start)
		{
			int i = start >> BIT_SHIFT;
			long mask = (1L << (start & BIT_MASK)) - 1L;
			int j = Long.numberOfTrailingZeros(~bits[i] & ~mask);
			if (j < BIT_MULT)
				return i * BIT_MULT + j;
			for (++i; i < bits.length; ++i)
			{
				j = Long.numberOfTrailingZeros(~bits[i]);
				if (j < BIT_MULT)
					return i * BIT_MULT + j;
			}
			return i * BIT_MULT;
		}
		
		int nextClear (int start)
		{
			int i;
			for (i = start; i < bits.length << BIT_SHIFT; ++i)
				if (get(i) == false)
					break;
			int j = nextClearX(start);
			assert i == j : "nextClear failed";
			return j;
		}
	}

	private static class FluorochromeSet
			extends Combination
			implements Comparable<FluorochromeSet>
	{
		float minDistance;
		short[] chosen;

		FluorochromeSet(int size, int choice)
		{
			super(size);
			this.chosen = new short[1];
			this.chosen[0] = (short) choice;
			this.set(choice);
			this.minDistance = Float.POSITIVE_INFINITY;
		}

		FluorochromeSet(FluorochromeSet that, int choice, float distance)
		{
			super(that);
			this.chosen = Arrays.copyOf(that.chosen, that.chosen.length + 1);
			this.chosen[that.chosen.length] = (short) choice;
			this.set(choice);
			this.minDistance = distance;
		}

		@Override
		public boolean equals (Object obj)
		{
			FluorochromeSet that = (FluorochromeSet) obj;
			if (this.minDistance != that.minDistance)
				return false;
			if (this.chosen.length != that.chosen.length)
				return false;
			for (int i = 0; i < this.bits.length; i++)
				if (this.bits[i] != that.bits[i])
					return false;
			return true;
		}

		@Override
		public int compareTo (FluorochromeSet that)
		{
			if (this.minDistance > that.minDistance)
				return -1;
			if (this.minDistance < that.minDistance)
				return 1;
			if (this.chosen.length > that.chosen.length)
				return -1;
			if (this.chosen.length < that.chosen.length)
				return 1;
			for (int i = 0; i < bits.length; i++)
				if (this.bits[i] != that.bits[i])
				{
					if ((this.bits[i] & Long.lowestOneBit(this.bits[i] ^ that.bits[i])) != 0)
						return -1;
					else
						return 1;
				}
			return 0;
		}
	}

	private static class Stain
			implements Comparable<Stain>
	{
		short marker;
		short fluorochrome;
		short hapten;

		Stain(int marker, int fluorochrome, int hapten)
		{
			this.marker = (short) marker;
			this.fluorochrome = (short) fluorochrome;
			this.hapten = (short) hapten;
		}

		Stain(int marker, int fluorochrome)
		{
			this(marker, fluorochrome, -1);
		}

		@Override
		public boolean equals (Object that)
		{
			return this.compareTo((Stain) that) == 0;
		}

		@Override
		public int compareTo (Stain that)
		{
			if (this.marker < that.marker)
				return -1;
			if (this.marker > that.marker)
				return 1;
			if (this.fluorochrome < that.fluorochrome)
				return -1;
			if (this.fluorochrome > that.fluorochrome)
				return 1;
			if (this.hapten < that.hapten)
				return -1;
			if (this.hapten > that.hapten)
				return 1;
			return 0;
		}
	}

	private static class Solution
			extends Combination
			implements Comparable<Solution>
	{
		short[] panel;
		float stainingIndex;

		public Solution(int size)
		{
			super(size);
			this.panel = new short[0];
		}

		public Solution(Solution that, int choose)
		{
			super(that);
			this.panel = Arrays.copyOf(that.panel, that.panel.length + 1);
			this.panel[that.panel.length] = (short) choose;
			set(choose);
		}

		@Override
		public int compareTo (Solution o)
		{
			if (this.stainingIndex > o.stainingIndex)
				return -1;
			if (this.stainingIndex < o.stainingIndex)
				return 1;
			return 0;
		}
	}

	public PanelDesign(int nWorkers, int nSolutions)
	{
		this.nWorkers = nWorkers;
		this.nSolutions = nSolutions;
	}

	public int getNWorkers ()
	{
		return nWorkers;
	}

	public void setNWorkers (int nWorkers)
	{
		this.nWorkers = nWorkers;
	}

	public int getNSolutions ()
	{
		return nSolutions;
	}

	public void setNSolutions (int nSolutions)
	{
		this.nSolutions = nSolutions;
	}

	synchronized void doTask (Solution work)
	{
		++started;
		toDo.push(work);
		notify();
	}

	private void evaluateFluorochromeSet (FluorochromeSet set,
			List<StainSet> results)
	{
		if (DEBUG)
		{
			Arrays.sort(set.chosen);
			System.out.print(set.minDistance);
			for (int i = 0; i < set.chosen.length; ++i)
			{
				System.out.print(", ");
				System.out.print(fluorochromes[set.chosen[i]].name);
			}
			System.out.println();
		}
		
		Fluorochrome[] fluorochromes = new Fluorochrome[markers.length];
		Detector[] detectors = new Detector[markers.length];
		double spectrum[][] = new double[markers.length][markers.length];
		for (int i = 0; i < markers.length; ++i)
		{
			Fluorochrome fluorochrome = fluorochromes[i] = this.fluorochromes[set.chosen[i]];
			Detector detector = detectors[i] = instrument.detector(fluorochrome);
			// make sure this set of dyes use distinct detectors on the instrument
			for (int j = 0; j < i; ++j)
				if (detectors[i] == detectors[j])
				{
					if (DEBUG)
						System.out.println("detector clash " + fluorochromes[i].name + " and " + fluorochromes[j].name);
					return;
				}
			for (int j = 0; j < markers.length; ++j)
				spectrum[i][j] = spectra[set.chosen[i]][detector.position];
		}
		
		// complete set of fluorochromes so construct a partial solution
		Solution partial = new Solution(catalogStains.length);
		// excluding any stains that don't use these fluorochromes
		for (int i = 0; i < catalogStains.length; ++i)
			if (!set.get(catalogStains[i].fluorochrome))
				partial.set(i);

		// compute the spectrum matrix in Spe for this set of fluorochromes
		
//		for (int j = 0; j < markers.length; j++)
//		{
//			Detector detector = instrument.detector(fluorochromes[set.chosen[j]]);
//			for (int i = 0; i < markers.length; i++)
//				spectrum[i][j] = spectra[set.chosen[i]][detector.position];
//		}

		toDo.push(partial);
		while (!toDo.isEmpty())
			findFeasiblePanels(set.minDistance, toDo.pop(), results);
		
		returnResults(results);
	}
	
	private synchronized void returnResults (Collection<StainSet> results)
	{
		double minimum = 0;
		if (resultCount >= nSolutions)
			minimum = this.results[nSolutions - 1].index;
		for (StainSet stainSet : results)
			if (stainSet.index > minimum)
			{
				uselessResults = 0;
				if (resultCount >= this.results.length)
				{
					Arrays.sort(this.results, 0, resultCount);
					minimum = this.results[nSolutions - 1].index;
					resultCount = nSolutions;
				}
				this.results[resultCount++] = stainSet;
			}
			else
				++uselessResults;
		
		Arrays.sort(this.results, 0, resultCount);
		if (resultCount > nSolutions)
			resultCount = nSolutions;
	}

	private void findFeasiblePanels (double score, Solution work, Collection<StainSet> results)
	{
		// find the next possible reagent
		// if there is none this partial solutions fails
		int i = work.nextClear(0);
		if (i < catalogStains.length)
		{
			// we'll dispose of this reagent here
			work.set(i);

			// by dividing the solutions into two cases
			// either use it now and therefore fix a reagent in the panel
			Solution used = new Solution(work, i);

			// or never use this reagent
			toDo.push(work);

			// if that didn't complete the panel, remove any other reagents that
			// would clash and keep trying
			if (used.panel.length < markers.length)
			{
				Stain stain = catalogStains[i];
				while ((i = used.nextClear(++i)) < catalogStains.length)
				{
					if (catalogStains[i].marker == stain.marker
							|| catalogStains[i].fluorochrome == stain.fluorochrome
							|| (stain.hapten >= 0 && catalogStains[i].hapten == stain.hapten))
						used.set(i);
				}
				toDo.push(used);
			}
			else
			{
				// finished the panel so score it and add it to the list of
				// candidates
				++solutions;
				double stainingIndex = scorePanel(score, used.panel);
				Arrays.sort(used.panel);
				StainSetImpl stainSet = new StainSetImpl(stainingIndex, used.panel);
				results.add(stainSet);
				
				if (DEBUG)
					stainSet.print();
			}
		}
	}
	
	private double scorePanel (double score, short[] panel)
	{
		double[][] dyeExpected = new double[targets.length][markers.length];
		for (int i = 0; i < targets.length; ++i)
			for (int j = 0; j < markers.length; ++j)
				if (targets[i][j] == null)
					dyeExpected[i][j] = 0;
				else
				{
					Fluorochrome fluorochrome = fluorochromes[catalogStains[panel[j]].fluorochrome];
					Detector detector = instrument.detector(fluorochrome);
					double brightness = fluorochrome.brightness;
					double emissionEffiency = fluorochrome.emissionEffiency(detector);
					double excitationEffiency = fluorochrome.excitationEffiency(detector);
					int laserPower = detector.laserPower;
					dyeExpected[i][j] = targets[i][j].level * brightness * excitationEffiency * laserPower;
				}
		double[][] speExpected = new double[targets.length][markers.length];
		for (int i = 0; i < targets.length; ++i)
			for (int j = 0; j < markers.length; ++j)
			{
				Fluorochrome fluorochrome = fluorochromes[catalogStains[panel[j]].fluorochrome];
				for (int k = 0; k < markers.length; ++k)
				{
					Detector detector = instrument.detector(fluorochromes[catalogStains[panel[k]].fluorochrome]);
					speExpected[i][j] += fluorochrome.emissionEffiency(detector);
				}
			}
		
		return score * Math.exp(-.3 * solutions * Math.random());
	}

	private static class Worker
			extends Thread
	{
		private List<StainSet> results = new ArrayList<StainSet>(100);

		public void run ()
		{
		}
	}
	
	private void startWorkers ()
	{
		if (!SINGLE_THREAD)
		{
			this.workers = new Worker[nWorkers];
			for (int i = 0; i < workers.length; i++)
			{
				Worker worker = workers[i] = new Worker();
				worker.start();
			}
		}
	}
	
	private void stopWorkers ()
	{
		if (!SINGLE_THREAD)
		{
			for (int i = 0; i < workers.length; i++)
				try
				{
					workers[i].join();
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
					return;
				}
		}
	}

	public final List<StainSet> design (Instrument instrument,
			List<PopulationStaining> populations,
			Map<Marker, Set<Fluorochrome>> directStains,
			Map<Hapten, Set<Marker>> haptenReagents,
			Map<Hapten, Set<Fluorochrome>> indirectStains)
			throws InterruptedException
	{
		resultCount = 0;
		uselessResults = 0;
		if (results == null || results.length < nSolutions * 2)
			results = new StainSet[nSolutions * 2];
		
		this.instrument = instrument;
		
		Set<Marker> markerSet1 = new HashSet<Marker>();
		for (PopulationStaining population : populations)
			for (MarkerStaining staining : population)
				markerSet1.add(staining.marker);
		
		Set<Marker> markerSet2 = new HashSet<Marker>();
		for (Marker marker : directStains.keySet())
			markerSet2.add(marker);
		for (Set<Marker> markers : haptenReagents.values())
			markerSet2.addAll(markers);
		
		assert markerSet1.equals(markerSet2);
		
		markers = markerSet2.toArray(new Marker[markerSet2.size()]);
		Arrays.sort(markers);
		
		targets = new MarkerStaining[populations.size()][markers.length];
		for (int i = 0; i < targets.length; ++i)
			for (MarkerStaining staining : populations.get(i))
				targets[i][Arrays.binarySearch(markers, staining.marker)] = staining;

		Set<Fluorochrome> fluorochromeSet = new HashSet<Fluorochrome>();
		for (Set<Fluorochrome> fluorochromes : directStains.values())
			fluorochromeSet.addAll(fluorochromes);
		for (Set<Fluorochrome> fluorochromes : indirectStains.values())
			fluorochromeSet.addAll(fluorochromes);
		fluorochromes = fluorochromeSet.toArray(new Fluorochrome[fluorochromeSet.size()]);
		Arrays.sort(fluorochromes);

		haptens = haptenReagents.keySet().toArray(new Hapten[haptenReagents.size()]);
		Arrays.sort(haptens);

		Set<Stain> catalogSet = new TreeSet<Stain>();
		int directTotal = 0;
		for (Map.Entry<Marker, Set<Fluorochrome>> entry : directStains.entrySet())
		{
			int markerCode = Arrays.binarySearch(markers, entry.getKey());
			for (Fluorochrome fluorochrome : entry.getValue())
			{
				int fluorochromeCode =
						Arrays.binarySearch(fluorochromes, fluorochrome);
				catalogSet.add(new Stain(markerCode, fluorochromeCode));
				++directTotal;
			}
		}
		if (DEBUG) System.out.println("direct stains for markers " + directTotal);

		int indirectTotal = 0;
		for (Map.Entry<Hapten, Set<Marker>> entry : haptenReagents.entrySet())
		{
			Hapten hapten = entry.getKey();
			int haptenCode = Arrays.binarySearch(haptens, hapten);
			for (Marker marker : entry.getValue())
			{
				int markerCode = Arrays.binarySearch(markers, marker);
				for (Fluorochrome fluorochrome : indirectStains.get(hapten))
				{
					int fluorochromeCode =
							Arrays.binarySearch(fluorochromes, fluorochrome);
					if (!catalogSet.contains(new Stain(markerCode, fluorochromeCode)))
					{
						catalogSet.add(new Stain(markerCode, fluorochromeCode, haptenCode));
						++indirectTotal;
					}
				}
			}
		}
		if (DEBUG) System.out.println("indirect stains for markers " + indirectTotal);
		if (DEBUG) System.out.println("possible stains " + catalogSet.size());

		catalogStains = catalogSet.toArray(new Stain[catalogSet.size()]);
		Arrays.sort(catalogStains);
		
		if (DEBUG)
			for (int i = 0; i < catalogStains.length; i++)
			{
				System.out.print(markers[catalogStains[i].marker]);
				System.out.print(':');
				if (catalogStains[i].hapten >= 0)
				{
					System.out.print(haptens[catalogStains[i].hapten]);
					System.out.print(':');
				}
				System.out.println(fluorochromes[catalogStains[i].fluorochrome].name);
			}
		
		// compute the spectra of the fluorochromes
		
		spectra = new double[fluorochromes.length][instrument.detectors.size()];
		for (int i = 0; i < fluorochromes.length; ++i)
		{
			double peak = 0;
			for (int j = 0; j < instrument.detectors.size(); ++j)
			{
				Detector detector = instrument.detectors.get(j);
				double signal = fluorochromes[i].emissionEffiency(detector);
				spectra[i][j] = signal;
				if (signal > peak)
					peak = signal;
			}
			for (int j = 0; j < instrument.detectors.size(); ++j)
				spectra[i][j] /= peak;
		}

		// compute the spectrum metric

		distance = new float[fluorochromes.length][fluorochromes.length];
		for (int i = 0; i < fluorochromes.length; i++)
		{
			for (int j = 0; j < i; ++j)
				distance[i][j] = distance[j][i] = (float) fluorochromes[i].distance(fluorochromes[j]);
			distance[i][i] = 0;
		}
		for (int i = 0; i < fluorochromes.length; ++i)
			for (int j = 0; j < i; ++j)
				for (int m = 0; m < fluorochromes.length; ++m)
					for (int n = 0; n < m; ++n)
						if (i != m || j != n)
							assert distance[i][j] != distance[m][n] : "metric degeneracy";

		// initialize the priority queue

		SortedSet<FluorochromeSet> priorityQueue = new TreeSet<FluorochromeSet>();
		for (int i = 0; i < fluorochromes.length; i++)
			priorityQueue.add(new FluorochromeSet(fluorochromes.length, i));

		List<StainSet> results = new ArrayList<StainSet>();
		startWorkers();
		Set<FluorochromeSet> alreadySeen = new TreeSet<FluorochromeSet>();
		while (!priorityQueue.isEmpty() && uselessResults < nSolutions * 10)
		{
			// get the best current solution

			FluorochromeSet set = priorityQueue.first();
			priorityQueue.remove(set);
			assert !alreadySeen.contains(set) : "duplicate fluorochrome set";
			if (set.chosen.length == markers.length)
			{
				alreadySeen.add(set);
				// complete set found so evaluate it
				fluorochromeSetQueue.put(set);
				if (SINGLE_THREAD)
				{
					// eventually will be in separate thread
					set = fluorochromeSetQueue.take();
					results.clear();
					evaluateFluorochromeSet(set, results);
				}
			}
			else
				// create new sets by adding each currently unused fluorochrome to the set
				// note that this will cause collisions, which is why the Set interface is required
				for (int i = 0; (i = set.nextClear(i)) < fluorochromes.length; ++i)
				{
					Float minDistance = set.minDistance;
					for (int j = 0; j < set.chosen.length; j++)
					{
						float d = distance[i][set.chosen[j]];
						if (d < minDistance)
							minDistance = d;
					}
					if (minDistance < set.minDistance)
						priorityQueue.add(new FluorochromeSet(set, i, minDistance));
				}
		}
		stopWorkers();
		
		results.clear();
		for (int i = 0; i < resultCount; ++i)
			results.add(this.results[i]);

		return results;
	}
}
