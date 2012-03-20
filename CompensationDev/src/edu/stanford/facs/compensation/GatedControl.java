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
  
  protected void analyze ()
  {
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
}
