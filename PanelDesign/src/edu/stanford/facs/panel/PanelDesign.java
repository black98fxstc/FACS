package edu.stanford.facs.panel;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

import edu.stanford.facs.panel.Instrument.Detector;

public class PanelDesign
{
	private final static boolean DEBUG = false;
	private final static boolean SINGLE_THREAD = true;
	
	private int nWorkers;
	private int nSolutions;

	private Instrument instrument;
	private Marker[] markers;
	private Fluorochrome[] fluorochromes;
	private Hapten[] haptens;
	private Stain[] catalogStains;
	private MarkerStaining[][] targets;
	private int nImportant;
	private float[][] distance;
	private double[][] spectra;
	private ArrayBlockingQueue<FluorochromeSet> fluorochromeSetQueue = new ArrayBlockingQueue<FluorochromeSet>(5);
	private StainSet[] results;
	private int resultCount = 0;
	private int uselessResults = 0;
	private int solutions = 0;
	
	private  FluorochromeSetWorker[] workers;
	

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

		int nextClear (int start)
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
	}

	private static class FluorochromeSet
			extends Combination
			implements Comparable<FluorochromeSet>
	{
		float minDistance;
		short[] chosen;

		FluorochromeSet (int size)
		{
			super(size);
		}
		
		FluorochromeSet (FluorochromeSet that)
		{
			super(that);
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

	private class FluorochromeSetWorker
			extends Thread
	{
		private final short[] markers = new short[PanelDesign.this.markers.length];
		private final Fluorochrome[] fluorochromes = new Fluorochrome[markers.length];
		private final Detector[] detectors = new Detector[markers.length];
		private final double spectrum[][] = new double[markers.length][markers.length];
		private final double compensationSquared[][] = new double[markers.length][markers.length];
		private final double[] trueSignal = new double[markers.length];
		private final double[] observedSignal = new double[markers.length];
		private final double[] variance = new double[markers.length];
		private final Stack<Solution> toDo = new Stack<Solution>();
		private final List<StainSet> results = new ArrayList<StainSet>(100);
		
		/*
		 * Score the staining on one population
		 */
		
		private double scorePopulation (MarkerStaining[] targets)
		{
			double score = 0;

			// start with the background on each detector
			for (int j = 0; j < detectors.length; ++j)
				observedSignal[j] = detectors[j].background;
			// for each marker/fluorochrome
			for (int i = 0; i < markers.length; ++i)
			{
				// compute the direct signal from the marker and fluorochrome alone
				int markerLevel = targets[markers[i]].level;
				trueSignal[i] = spectrum[i][i] * markerLevel;
				// and the total signal expected from the combined stain
				for (int j = 0; j < detectors.length; ++j)
					observedSignal[j] += spectrum[i][j] * markerLevel;
			}
			
			// compute compensation variance
			Arrays.fill(variance, 0);
			for (int j = 0; j < markers.length; ++j)
				for (int k = 0; k < markers.length; ++k)
					variance[j] += compensationSquared[j][k] * observedSignal[j];
			for (int j = 0; j < markers.length; ++j)
				if (targets[j].important)
					score += Math.log(trueSignal[j] / Math.sqrt(variance[j]));
			
			return score;
		}
		
		/*
		 * Score this staining panel
		 */
		private double scorePanel (short[] panel)
		{
			// find the markers assigned to each fluorochrome in this panel
			for (int j = 0; j < markers.length; ++j)
				markers[j] = catalogStains[panel[j]].marker;
			
			// sum scores from each target population
			double score = 0;
			for (int i = 0; i < targets.length; ++i)
				score += scorePopulation(targets[i]);
			
			return Math.exp(score / nImportant);
		}
		
		/*
		 * Find and score all feasible combinations of reagents utilizing
		 * a specified set of fluorochromes
		 * 
		 * (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */

		public void run ()
		{
			try
			{
				// get a complete set of fluorochromes from the queue
				FluorochromeSet set = fluorochromeSetQueue.take();
				if (DEBUG)
				{
					Arrays.sort(set.chosen);
					synchronized (PanelDesign.this)
					{
						System.out.print(set.minDistance);
						for (int i = 0; i < set.chosen.length; ++i)
						{
							System.out.print(", ");
							System.out.print(PanelDesign.this.fluorochromes[set.chosen[i]].name);
						}
						System.out.println();
					}
				}

				// check for detector collision and initialize some arrays
				// needed for scoring all panels using this set of fluorochromes
				for (int i = 0; i < markers.length; ++i)
				{
					Fluorochrome fluorochrome = fluorochromes[i] = PanelDesign.this.fluorochromes[set.chosen[i]];
					Detector detector = detectors[i] = instrument.detector(fluorochrome);
					// make sure this set of dyes uses distinct detectors on the instrument
					for (int j = 0; j < i; ++j)
						if (detectors[i] == detectors[j])
						{
							if (DEBUG)
								synchronized (PanelDesign.this)
								{
									System.out.println("detector clash " + fluorochromes[i].name + " and " + fluorochromes[j].name);
								}
							return;
						}
				}
				for (int i = 0; i < markers.length; ++i)
				{
					for (int j = 0; j < markers.length; ++j)
						compensationSquared[i][j] = spectrum[i][j] = spectra[set.chosen[i]][detectors[j].position];
					for (int j = 0; j < markers.length; ++j)
						compensationSquared[i][j] /= compensationSquared[i][i];
				}
				invert(compensationSquared);
				for (int i = 0; i < markers.length; ++i)
					for (int j = 0; j < markers.length; ++j)
						compensationSquared[i][j] *= compensationSquared[i][j];

				// construct an initial empty solution
				// excluding any stains that don't use these fluorochromes
				Solution initial = new Solution(catalogStains.length);
				for (int i = 0; i < catalogStains.length; ++i)
					if (!set.get(catalogStains[i].fluorochrome))
						initial.set(i);

				// keep trying to refine the partial solutions 
				// till we find all the complete ones
				toDo.push(initial);
				while (!toDo.isEmpty())
				{
					// get a partial solution and find the next available reagent
					// if there aren't any this solution in infeasible and we drop it
					Solution work = toDo.pop();
					int i = work.nextClear(0);
					if (i < catalogStains.length)
					{
						// found a reagent so we will dispose of it one way or the other now
						// by dividing the solutions into two cases
						work.set(i);

						// either use it now and therefore fix a reagent in the panel
						Solution used = new Solution(work, i);
						if (used.panel.length < markers.length)
						{
							// if that didn't complete the panel, remove any other reagents that
							// would clash and keep trying
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
							Arrays.sort(used.panel);
							double stainingIndex = scorePanel(used.panel);
							StainSetImpl stainSet = new StainSetImpl(stainingIndex, used.panel);
							
							if (DEBUG)
								synchronized (PanelDesign.this)
								{
									stainSet.print();
								}

							results.add(stainSet);
							if (results.size() > nSolutions)
								returnResults(results);
						}

						// otherwise never use this reagent in this family of solutions
						// since the stains are sorted by marker, if the next available 
						// stain isn't for this marker then the solution is no longer feasible
						int j = work.nextClear(i + 1);
						if (j < catalogStains.length && catalogStains[i].marker == catalogStains[j].marker)
							toDo.push(work);
					}
				}
				
				returnResults(results);
			}
			catch (InterruptedException e)
			{
				return;
			}
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

	private static void invert (double[][] matrix)
  {
    int row = 0, col = 0, n = matrix.length;
    int pivot[] = new int[n];
    int row_index[] = new int[n];
    int col_index[] = new int[n];

    for (int i = 0; i < n; ++i)
    {
      double big = 0;
      for (int j = 0; j < n; ++j)
      {
        if (pivot[j] != 1)
          for (int k = 0; k < n; ++k)
          {
            if (pivot[k] == 0)
            {
              double abs = Math.abs(matrix[j][k]);
              if (abs >= big)
              {
                big = abs;
                row = j;
                col = k;
              }
            }
            else if (pivot[k] > 1)
              throw new IllegalArgumentException("Matrix is singular");
          }
      }
      ++pivot[col];
      row_index[i] = row;
      col_index[i] = col;

      if (row != col)
        for (int k = 0; k < n; ++k)
        {
          double t = matrix[row][k];
          matrix[row][k] = matrix[col][k];
          matrix[col][k] = t;
        }

      if (matrix[col][col] == 0)
        throw new IllegalArgumentException("Matrix is singular");
      double inverse = 1 / matrix[col][col];
      matrix[col][col] = 1;
      for (int j = 0; j < n; ++j)
        matrix[col][j] *= inverse;
      for (int j = 0; j < n; ++j)
        if (j != col)
        {
          double t = matrix[j][col];
          matrix[j][col] = 0;
          for (int k = 0; k < n; ++k)
            matrix[j][k] -= matrix[col][k] * t;
        }
    }
  }
	
	private synchronized void returnResults (Collection<StainSet> results)
	{
		solutions += results.size();
		
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
		
		results.clear();
	}
	
	private void startWorkers ()
	{
		if (SINGLE_THREAD)
		{
			workers = new FluorochromeSetWorker[1];
			workers[0] = new FluorochromeSetWorker();
		}
		else
		{
			workers = new FluorochromeSetWorker[nWorkers];
			for (int i = 0; i < workers.length; i++)
			{
				FluorochromeSetWorker worker = workers[i] = new FluorochromeSetWorker();
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
					workers[i].interrupt();
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
		{
			for (MarkerStaining staining : populations.get(i))
			{
				targets[i][Arrays.binarySearch(markers, staining.marker)] = staining;
				if (staining.important)
					++nImportant;
			}
			for (int j = 0; j < markers.length; ++j)
				if (targets[i][j] == null)
					targets[i][j] = new MarkerStaining(markers[j], 0, false);
		}

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
		
		// compute the complete spectra of the fluorochromes
		
		spectra = new double[fluorochromes.length][instrument.detectors.size()];
		for (int i = 0; i < fluorochromes.length; ++i)
		{
			Fluorochrome fluorochrome = fluorochromes[i];
			for (int j = 0; j < instrument.detectors.size(); ++j)
			{
				Detector detector = instrument.detectors.get(j);
				double brightness = fluorochrome.brightness;
				double emissionEffiency = fluorochrome.emissionEffiency(detector);
				double excitationEffiency = fluorochrome.excitationEffiency(detector);
				int laserPower = detector.laserPower;
				spectra[i][j] = brightness * emissionEffiency * excitationEffiency * laserPower;
			}
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
		for (int i = 0; i < fluorochromes.length - 1; i++)
			for (int j = i + 1; j < fluorochromes.length; ++j)
			{
				FluorochromeSet set = new FluorochromeSet(fluorochromes.length);
				set.set(i);
				set.set(j);
				short[] chosen = { (short) i, (short) j };
				set.chosen = chosen;
				set.minDistance = distance[i][j];
				priorityQueue.add(set);
			}

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
					workers[0].run();
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
					// if this new fluorochrome is further away that the current minimum then there
					// was a set of the same size with a better score that has already tried this
					if (minDistance < set.minDistance)
					{
						FluorochromeSet bigger = new FluorochromeSet(set);
						bigger.set(i);
						bigger.chosen = Arrays.copyOf(set.chosen, set.chosen.length + 1);
						bigger.chosen[set.chosen.length] = (short) i;
						bigger.minDistance = minDistance;
						priorityQueue.add(bigger);
					}
				}
		}
		stopWorkers();
		
		results.clear();
		for (int i = 0; i < resultCount; ++i)
			results.add(this.results[i]);

		return results;
	}
}
