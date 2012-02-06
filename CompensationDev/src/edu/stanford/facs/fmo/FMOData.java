package edu.stanford.facs.fmo;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.isac.fcs.FCSException;
import org.isac.fcs.FCSFile;
import org.isac.fcs.FCSParameter;
import org.isac.fcs.FCSTextSegment;

import edu.stanford.facs.data.CompensatedData;
import edu.stanford.facs.data.FluorescenceCompensation;

/**
 * $Id: FMOData.java,v 1.10 2010/10/08 23:32:32 wmoore Exp $
 * 
 * @author cate
 */

public class FMOData
  extends CompensatedData
{
  private String[] detectorNames;
  private FluorescenceCompensation compensation;
  private Instrument instrument;
  private float[][] V;
  private float[][] S;

  public FMOData (FCSFile fcsfile, String tag)
  {
    super(fcsfile, "-fmo");
  }

  public FMOData (FCSFile fcsfile, Instrument instrument,
    FluorescenceCompensation fmoCompMatrix)
  {
    super(fcsfile, "-fmo");
    this.compensation = fmoCompMatrix;
    this.instrument = instrument;
  }

  /**
   * @param fmoCompMatrix
   *          Pass in the compensation matrix.
   */
  public void setFluoresenceCompensation (FluorescenceCompensation fmoCompMatrix)
  {
    this.fc = fmoCompMatrix;

  }

  /**
   * @param detectors
   */
  public void setDetectorNames (String[] detectors)
  {
    this.detectorNames = detectors;
  }

  public void analyze (Detector[] detector)
  {
    int rank = detector.length;
    /*
     * First do the ordinary compensation
     */
    super.analyze();
    /*
     * Now compute the variances of the compensated values
     */
    V = new float[rank][Nevents];
    for (int i = 0; i < rank; i++)
      V[i] = addFloatAnalysis(mangle(detectorNames[i], "V"));
    /*
     * Get the compensation matrix and square its elements
     */
    double[][] C = fc.getCompensationMatrix();
    double[][] D = new double[rank][rank];
    for (int i = 0; i < rank; ++i)
      for (int j = 0; j < rank; ++j)
        D[i][j] = C[i][j] * C[i][j];
    /*
     * Compute the constant variance due to electronic noise propagated through
     * compensation.
     */
    float[] bg = new float[rank];
    for (int i = 0; i < rank; ++i)
      for (int j = 0; j < rank; ++j)
        bg[j] += detector[i].electronicVariance * D[i][j];
    /*
     * Adjust the matrix so it gives variance per signal
     */
    for (int i = 0; i < rank; ++i)
      for (int j = 0; j < rank; ++j)
        D[i][j] *= detector[i].nowSensitivity;
    /*
     * Add in the constant variance due to background light propagated through
     * compensation
     */
    for (int i = 0; i < rank; ++i)
      for (int j = 0; j < rank; ++j)
        bg[j] += detector[i].nowBackgroundLight * D[i][j];
    /*
     * Initialize all events to their constant background
     */
    for (int i = 0; i < rank; ++i)
      Arrays.fill(V[i], bg[i]);
    /*
     * Add in the variance due to photon conuting statistics for each event
     */
    for (int i = 0; i < rank; ++i)
      for (int j = 0; j < rank; ++j)
        for (int k = 0; k < Nevents; ++k)
          V[j][k] += X[i][k] * D[i][j];
    /*
     * Finally compute the compensated value divided by its estimated standard
     * deviation
     */
    S = new float[rank][Nevents];
    for (int i = 0; i < rank; i++)
      S[i] = addFloatAnalysis(mangle(detectorNames[i], "S"));
    for (int i = 0; i < rank; i++)
      for (int j = 0; j < Nevents; j++)
        S[i][j] = (float)(100 * Y[i][j] / Math.sqrt(V[i][j]));
  }

  protected void read (FluorescenceCompensation fc)
    throws FCSException, IOException
  {
    this.fc = fc;

    detectorNames = fc.getDetectorNames();
    float[][] float_data = super.read();
    FCSTextSegment segment = fcs.getTextSegment();
    java.util.Set<String> attrNames = segment.getAttributeNames();

    String cy = segment.getAttribute("$CYT");
    String spill = segment.getAttribute("$SPILL");
    System.out.println("$CYT attribute = " + cy);
    System.out.println(" $SPILL = " + spill);
    String cm = segment.getAttribute("$COMP");
    System.out.println("  Comp?  " + cm);

    // inherited from Compesated Data
    X = new float[detectorNames.length][];

    // for (String s : detectors){
    for (int i = 0; i < detectorNames.length; i++)
    {
      FCSParameter p = fcs.getParameter(detectorNames[i]);
      X[i] = float_data[p.getIndex() - 1];
    }

  }

  /* get the data for these detectors */

  class MyFilenameFilter
    implements FilenameFilter
  {
    String ext, second;

    MyFilenameFilter (String ext, String second)
    {
      this.ext = ext;
      this.second = second;
    }

    public boolean accept (File file, String string)
    {
      boolean flag = false;
      if (file.isDirectory())
      {
        if (second != null && string.contains(second))
          flag = false;
        else if (string.endsWith(ext))
          flag = true;

      }

      return flag;
    }
  }
}
