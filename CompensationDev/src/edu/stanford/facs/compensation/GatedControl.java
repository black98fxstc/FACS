package edu.stanford.facs.compensation;

import java.io.IOException;
import java.util.BitSet;

import org.isac.fcs.FCSException;
import org.isac.fcs.FCSFile;

public abstract class GatedControl
  extends ControlData
{
  public int[] gate;
  public BitSet exclude;

  public GatedControl (FCSFile fcsfile, Compensation2 comp)
  {
    super(comp, fcsfile);
  }

  //override
  protected void load ()
    throws FCSException, IOException
  {
    super.load();

    gate = addIntegerAnalysis("GATE");
    exclude = new BitSet(Nevents);
  }

  public void censor (int k, Gate g)
  {
    gate[k] = g.code;
    exclude.set(k);
  }

  public boolean censored (int k)
  {
    return exclude.get(k);
  }

  protected void excludeHighDataValues (final double[] range)
  {
    int Noriginal = exclude.cardinality();
    int ndetectors = comp.getDetectorLength();
    for (int j = 0; j < ndetectors; ++j)
    {
      for (int k = 0; k < Nevents; ++k)
      {
        if (X[j][k] > .9 * range[j])
          censor(k, Gate.RANGE);
      }
    }
    if (Compensation2.DEBUG)
      System.out
        .println((100.0 * (exclude.cardinality() - Noriginal) / (Nevents))
          + "% excluded for out of range");

  }
}
