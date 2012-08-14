package edu.stanford.facs.compensation;

import java.io.IOException;

import org.isac.fcs.FCSException;
import org.isac.fcs.FCSFile;
import org.isac.fcs.FCSParameter;

import edu.stanford.facs.data.AnalyzedData;

public abstract class ControlData
  extends AnalyzedData
{
  public final Compensation2 comp;
  public float X[][]; // fluorescence data
  public float Y[][]; // scatter data

  protected ControlData (Compensation2 comp, FCSFile fcsfile)
  {
    super(fcsfile, "-chi");
    this.comp = comp;
  }

  protected void load ()
    throws FCSException, IOException
  {
    float[][] data = super.read();

    String[] detectorList = comp.getDetectorList();
    X = new float[detectorList.length][];
    for (int j = 0; j <  detectorList.length; ++j)
    {
      FCSParameter p = fcs.getParameter(detectorList[j]);
      assert p != null : "No FCS parameter " + detectorList[j];
      X[j] = data[p.getIndex() - 1];
    }
    
    Y = new float[comp.scatter.length][];
    for (int j = 0; j < comp.scatter.length; ++j)
    {
    	FCSParameter p = fcs.getParameter(comp.scatter[j]);
      assert p != null : "No FCS parameter " + comp.scatter[j];
      Y[j] = data[p.getIndex() - 1];
    }
  }
}
