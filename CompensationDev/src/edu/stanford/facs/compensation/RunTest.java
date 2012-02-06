package edu.stanford.facs.compensation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public final class RunTest
{
  private final Stack<RunItem> runItems = new Stack<RunItem>();
  private final List<RunItem> runList = new ArrayList<RunItem>();
  
  private double standardError;
  private int observedRuns;
  private double expectedRuns;
  private double runsVariance;

  private int count = 0;

  private static final class RunItem
    implements Comparable<RunItem>
  {
    private float value;
    private double error;
    private int sequence;

    /**
     * Compares this object with the specified object for order.
     * 
     * @param item
     *          the Object to be compared.
     * @return a negative integer, zero, or a positive integer as this object is
     *         less than, equal to, or greater than the specified object.
     * @todo Implement this java.lang.Comparable method
     */
    public final int compareTo (RunItem item)
    {
      if (item.value<value)
        return -1;
      else if (value<item.value)
        return 1;
      else if (item.sequence<sequence)
        return -1;
      else if (sequence<item.sequence)
        return 1;
      else
        return 0;
    }
  }

  public final void add (float value, double error)
  {
    RunItem item;
    if (runItems.isEmpty())
      item = new RunItem();
    else
      item = runItems.pop();

    item.value = value;
    item.error = error;
    item.sequence = count++;

    runList.add(item);
  }

  public final void reset ()
  {
    runItems.addAll(runList);
    runList.clear();
    count = 0;
    standardError = Double.NaN;
  }

  public final double standardError ()
  {
  	if (!Double.isNaN(standardError))
  		return standardError;
  	
  	Collections.sort(runList);

    observedRuns = 1;
    double Nneg = 0;
    double Npos = 0;

    RunItem last = runList.get(0);
    if (last.error<0)
      ++Nneg;
    else
      ++Npos;

    for (int i = 1, m = runList.size(); i<m; ++i)
    {
      RunItem item = runList.get(i);
      if (item.error<0)
        ++Nneg;
      else
        ++Npos;
      if ((item.error<0)!=(last.error<0))
        ++observedRuns;
      last = item;
    }

    expectedRuns = 2*Npos*Nneg/(Npos+Nneg)+1;
    runsVariance = 2*Npos*Nneg*(2*Npos*Nneg-Npos-Nneg)/(Npos+Nneg)
        /(Npos+Nneg)/(Npos+Nneg-1);
    double SDruns = Math.sqrt(2*Npos*Nneg*(2*Npos*Nneg-Npos-Nneg)/(Npos+Nneg)
        /(Npos+Nneg)/(Npos+Nneg-1));
    if (Tools.DEBUG)
      System.out.println("Runs = "+observedRuns+" E = "+expectedRuns+" SD = "+Math.sqrt(runsVariance));

    standardError = (observedRuns - expectedRuns) / Math.sqrt(runsVariance);
    
    return standardError;
  }

  public final boolean passes (double tolerance)
  {
    Collections.sort(runList);

    int Nruns = 1;
    double Nneg = 0;
    double Npos = 0;

    RunItem last = runList.get(0);
    if (last.error<0)
      ++Nneg;
    else
      ++Npos;

    for (int i = 1, m = runList.size(); i<m; ++i)
    {
      RunItem item = runList.get(i);
      if (item.error<0)
        ++Nneg;
      else
        ++Npos;
      if ((item.error<0)!=(last.error<0))
        ++Nruns;
      last = item;
    }

    expectedRuns = 2*Npos*Nneg/(Npos+Nneg)+1;
    runsVariance = 2*Npos*Nneg*(2*Npos*Nneg-Npos-Nneg)/(Npos+Nneg)
        /(Npos+Nneg)/(Npos+Nneg-1);
    double SDruns = Math.sqrt(2*Npos*Nneg*(2*Npos*Nneg-Npos-Nneg)/(Npos+Nneg)
        /(Npos+Nneg)/(Npos+Nneg-1));
    if (Tools.DEBUG)
      System.out.println("Runs = "+Nruns+" E = "+expectedRuns+" SD = "+Math.sqrt(runsVariance));

    return Nruns>expectedRuns-tolerance*SDruns;
  }

  public final boolean passes ()
  {
    return passes(2);
  }
  
  public final boolean fails (double tolerance)
  {
    return !passes(tolerance);
  }
  
  public final boolean fails ()
  {
    return !passes();
  }
}
