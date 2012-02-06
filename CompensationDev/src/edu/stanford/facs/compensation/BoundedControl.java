package edu.stanford.facs.compensation;

import org.isac.fcs.FCSFile;

public class BoundedControl
  extends GatedControl
{
  public int NoutOfRange;
  
  public BoundedControl (Compensation2 comp, FCSFile fcsfile)
  {
    super(fcsfile, comp);
  }

  @Override
  protected void analyze ()
  {
    int Noriginal = exclude.cardinality();
    int ndetectors = comp.getDetectorLength();
    for (int j = 0; j < ndetectors; ++j)
    {
      for (int k = 0; k < Nevents; ++k)
      {
        if (X[j][k] > .9 * comp.fluorescenceRange[j])
          censor(k, Gate.RANGE);
      }
    }
    for (int s = 0; s < comp.scatterRange.length; ++s)
    {
      for (int k = 0; k < Nevents; ++k)
      {
        float scatter = Y[s][k];
        if (scatter > .95 * comp.scatterRange[s])
          censor(k, Gate.RANGE);
        if (scatter <= 0)
        {
          censor(k, Gate.RANGE);
          Y[s][k] = .0001F;
        }
        Y[s][k] = (float)Math.log(scatter);
      }
    }
    NoutOfRange = exclude.cardinality() - Noriginal;
    if (Compensation2.DEBUG){
      System.out.println((100.0 * NoutOfRange / (Nevents))
          + "% excluded for out of range");
    }
  }
}
