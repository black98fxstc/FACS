package edu.stanford.facs.data;

import java.io.IOException;

import org.isac.fcs.FCSException;
import org.isac.fcs.FCSFile;
import org.isac.fcs.FCSHandler;

public class FlowData
{
  public int Nevents;
  protected final FCSFile fcs;

  protected FlowData (FCSFile fcsfile)
  {
    super();
    this.fcs = fcsfile;
  }
  
  public static String mangle (String detector, String variable)
  {
    int pos = detector.lastIndexOf('-');
    if (pos >= 0)
      return detector.substring(0,pos) + "-" + variable;
    else
      return detector.substring(0,pos) + "-" + variable;
  }

  public FCSFile getFCSFile() {
      return fcs;
  }
  
  protected float[][] read ()
    throws FCSException, IOException
  {

    Nevents = fcs.getTotal();
    int Nparameters = fcs.getParameters();
    float[][] data = new float[Nparameters][Nevents];

    FCSHandler ii = fcs.getInputIterator();
    for (int k = 0; k < Nevents; ++k)
      for (int j = 0; j < Nparameters; ++j)
        data[j][k] = ii.readFloat();

    return data;
  }

  public int getNevents ()
  {
    return Nevents;
  }
}
