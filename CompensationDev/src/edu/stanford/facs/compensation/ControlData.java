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
//    DebuggingInfo debug = DebuggingInfo.getInstance();
    FCSParameter p = fcs.getParameter("FSC-A");
    float[] fsc = data[p.getIndex() - 1];

    p = fcs.getParameter("SSC-A");
    float[] ssc = data[p.getIndex() - 1];
    String[] detectorList = comp.getDetectorList();
//System.out.println ("  is the detectorList null?  "+ detectorList);
    X = new float[detectorList.length][];
    for (int j = 0; j <  detectorList.length; ++j)
    {
      p = fcs.getParameter(detectorList[j]);
      int index = p.getIndex() - 1;
      

      if (p != null)
          X[j] = data[p.getIndex() - 1];
      else
          System.out.println ("  Parameter p getIndex for detector "+ detectorList[j] + " is null.");
//      debug.loadDetectorList (detectorList[j], p);
    }

    Y = new float[2][];
    Y[0] = fsc;
    Y[1] = ssc;
  }
}
