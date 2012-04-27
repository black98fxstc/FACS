package edu.stanford.facs.compensation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.swing.SwingWorker;

import org.isac.fcs.FCSParameter;
import edu.stanford.facs.gui.CompensationResults;

import edu.stanford.facs.gui.CompensationFrame;
import java.beans.PropertyChangeListener;

public class Compensation2
  extends SwingWorker<Exception, StainedControl> 
{
  public static final double PMT_EXPONENT = 7.4D;
  public static final double PMT_STANDARD_VOLTS = 500D;

  public static final boolean WAYNE = false;
  public static final boolean DEBUG = false;
  public static final boolean CATE = false;

  public static final boolean RECORD_RESIDUALS = false;
  public static final boolean RECORD_FIT = false;
  public static final boolean RECORD_GATES = true;
  public static final boolean RECORD_CSV = false;
  public static final boolean RECORD_FCS = false;
  public static final boolean RECORD_ANY = 
    (RECORD_RESIDUALS || RECORD_FIT || RECORD_GATES)
    && (RECORD_FCS || RECORD_CSV);

  private String[] detector;
  public String[] reagent;
  public UnstainedControl[] unstainedControl;
  public StainedControl[] stainedControl;

  public File dataFolder;
  public CompensationFrame ui;
  
  private CompensationResults compResults;


  protected double[] volts;
  protected double[] scatterRange;
  protected double[] fluorescenceRange;
  /*stainedControl[i].Nevents;
          stats[1] = stainedControl[i].NscatterGated;
          stats[2] = stainedControl[i].Nautofluorescence;
          stats[3] = stainedControl[i].NspectrumGated;
          //stats[4] = stainedControl[i].Range;
          stats[4] = 0;
          stats[5] = stainedControl[i].Nfinal;*/
  private String[] statLabels = {"Total Events", "Out of Range", "Scatter Gated", "Autofluorescence",
                                 "Spectrum Gated", "Final"};


  class Point
  {
    public Point (int i, int j, int p, double x, double y, double yHat,
      double residual)
    {
      super();
      this.i = i;
      this.j = j;
      this.p = p;
      this.x = x;
      this.y = y;
      this.yHat = yHat;
      this.residual = residual;
    }

    public final int i;
    public final int j;
    public final int p;
    public final double x;
    public final double y;
    public final double yHat;
    public final double residual;

    public double lnP (State s)
    {
      double slope = ((StainedControl)stainedControl[i]).slope[j];
      double variance = s.signalPerPhoton[j] * (s.backgroundSignal[j] + yHat)
        + slope * slope * s.signalPerPhoton[p] * (s.backgroundSignal[p] + x);

      return -.5 * (residual / variance + Math.log(residual * variance));
    }
  }

  public class State
  {
    final List<Point> points;
    final Random random = new Random();
    final boolean RECORD = true;

    double lnP;
    double logStep = .015;
    double transitionRatio = Double.NaN;
    State proposed;
    PrintWriter pw;

    public double[] signalPerPhoton;
    public double[] backgroundSignal;

    private State (List<Point> points)
    {
      signalPerPhoton = new double[detector.length];
      backgroundSignal = new double[detector.length];
      this.points = points;
    }

    public State (List<Point> points, double[] estimatedSignal,
      double[] estimatedBackground, File log)
    {
      this(points);

      for (int j = 0; j < detector.length; ++j)
        signalPerPhoton[j] = estimatedSignal[j];
      for (int j = 0; j < backgroundSignal.length; ++j)
        backgroundSignal[j] = estimatedBackground[j];

      this.lnP = 0;
      Iterator<Point> it = points.iterator();
      while (it.hasNext())
        this.lnP += it.next().lnP(this);

      proposed = new State(points);

      try
      {
        if (log != null)
        {
          pw = new PrintWriter(log);
          pw.print("Acceptance,q ratio,log P");
          for (int j1 = 0; j1 < detector.length; ++j1)
          {
            pw.print(',');
            pw.print("s/pe ");
            pw.print(detector[j1]);
            pw.print(',');
            pw.print("bg ");
            pw.print(detector[j1]);
          }
          pw.println();
        }
      }
      catch (FileNotFoundException e)
      {
        e.printStackTrace();
      }
    }

    private void record (String value)
    {
      if (RECORD && pw != null)
      {
        pw.print(value);
        pw.print(',');
        pw.print(proposed.transitionRatio);
        pw.print(',');
        pw.print(proposed.lnP);
        for (int j = 0; j < detector.length; j++)
        {
          pw.print(',');
          pw.print(proposed.signalPerPhoton[j]);
          pw.print(',');
          pw.print(proposed.backgroundSignal[j]);
        }
        pw.println();
      }
    }

    public State propose (double logStep)
    {
      proposed.transitionRatio = 1;
      for (int j = 0; j < detector.length; j++)
      {
        proposed.signalPerPhoton[j] = this.signalPerPhoton[j]
          * Math.exp(logStep * random.nextGaussian());
        proposed.transitionRatio *= proposed.signalPerPhoton[j]
          / this.signalPerPhoton[j];

        proposed.backgroundSignal[j] = this.backgroundSignal[j]
          * Math.exp(logStep * random.nextGaussian());
        proposed.transitionRatio *= proposed.backgroundSignal[j]
          / this.backgroundSignal[j];
      }

      proposed.lnP = 0;
      Iterator<Point> i = points.iterator();
      while (i.hasNext())
        proposed.lnP += i.next().lnP(proposed);

      return proposed;
    }

    public void accept ()
    {
      record("ACCEPT");
      for (int i = 0; i < detector.length; i++)
      {
        this.signalPerPhoton[i] = proposed.signalPerPhoton[i];
        this.backgroundSignal[i] = proposed.backgroundSignal[i];
      }
      this.lnP = proposed.lnP;
    }

    public void reject ()
    {
      if (false)
        record("REJECT");
    }

    public void finalize ()
    {
      if (RECORD && pw != null)
      {
        pw.close();
      }
    }
  }

  protected void analyze ()
  {
    if (true)
    {
      double[] signalEstimate = new double[detector.length];
      double[] backgroundEstimate = new double[detector.length];
      double[] highestSlope = new double[detector.length];
      
      List<Point> points = new ArrayList<Point>();
      for (int i = 0; i < stainedControl.length; ++i)
      {
        StainedControl control = (StainedControl)stainedControl[i];
        control.markovData(i, points);
        for (int j = 0; j < detector.length; ++j)
        {
          if (j == control.primary){
            continue;
          }
          if (control.slope[j] < control.slopeSigma[j])
            continue;
          double variancePerSignal = control.varianceSlope[j]
            / control.slope[j];
          if (highestSlope[j] < control.slope[j]
            && variancePerSignal > signalEstimate[j])
          {
            signalEstimate[j] = variancePerSignal;
            highestSlope[j] = control.slope[j];
          }
          if (control.varianceIntercept[j] > backgroundEstimate[j])
            backgroundEstimate[j] = control.varianceIntercept[j];
        }
      }
      for (int j = 0; j < detector.length; ++j)
        backgroundEstimate[j] /= signalEstimate[j];

      State state = new State(points, signalEstimate, backgroundEstimate,
        new File(dataFolder, "Markov.csv"));
      double logStep = .01;
      int count = 0;
      int steps = 100;
      for (int i = 0; i < 400; ++i)
      {
        count = 0;
        for (int j = 0; j < steps; ++j)
        {
          State proposed = state.propose(logStep);

          double acceptance = Math.min(1, Math.exp(proposed.lnP - state.lnP)
            * proposed.transitionRatio);
          if (Math.random() < acceptance)
          {
            state.accept();
            ++count;
          }
          else
            state.reject();
        }

        if (count < steps * .1)
          logStep /= 1.2;
        else if (count > steps * .4)
          logStep *= 1.2;
      }
      state.finalize();
    }
  }

  public void setDetectorList (String[] detectorList){
      detector = detectorList;
  }
  public void setControlList (String[] controlList){
      this.reagent = controlList;
  }
  public void setDetectorsAndControls (String[] detectors, String[] controls){
      this.detector = detectors;
      this.reagent = controls;
    
  }

  public String[] getDetectorList () {
      return detector;
  }
  
  public int getDetectorLength () {
      if (detector == null)
          return -1;
      return detector.length;
  }
  
  public CompensationFrame getUIFrame ()
  {
    return ui;
  }

  public float[] getDataX (int row, int col){
      float[] data = null;

      if (row < stainedControl.length && stainedControl[row] != null){
          //System.out.println (" getDataX "+ row + ", "+ col + " "+ stainedControl[row].getPrimaryDetector());

          if (col < stainedControl[row].X.length && stainedControl[row].X[col] != null)
          data = stainedControl[row].X[col];
      }
      return data;
  }

  public float[] getDataY (int row, int col){
      float[] data= null;

      if (row < stainedControl.length && stainedControl[row] != null){
          if (col < stainedControl[row].Y.length && stainedControl[row].Y[col] != null)
          data = stainedControl[row].Y[col];
      }
      return data;
  }

  public int[] getGates (int row, int col){
      int[] gates= null;

      if (row < stainedControl.length && stainedControl[row].reagent == row)
          gates = stainedControl[row].gate;

      return gates;

  }

  public int[] getStatistics (int row, int col){
      int[] stats = new int[6];

//      int i=0;
//      boolean flag = false;
//      while (i < stainedControl.length && !flag){
//          if (stainedControl[i].reagent == row)
//              flag = true;
//          else i++;
//      }
    //  if (flag){
//      if (row < stainedControl.length && stainedControl[row].reagent == row)
//      stats[0] = stainedControl[row].Nevents;
//      stats[1] = stainedControl[row].NscatterGated;
//      stats[2] = stainedControl[row].Nautofluorescence;
//      stats[3] = stainedControl[row].NspectrumGated;
//      stats[5] = stainedControl[row].Nfinal;
      int ac = 0;
      stats[ac++] = stainedControl[row].Nevents;
      stats[ac++] = stainedControl[row].NoutOfRange;
      stats[ac++] = stainedControl[row].NscatterGated;
      stats[ac++] = stainedControl[row].Nautofluorescence;
      stats[ac++] = stainedControl[row].NspectrumGated;
      stats[ac++] = stainedControl[row].Nfinal;
      

   //   }

      return stats;
  }
  public String[] getStatLabels() {
      return statLabels;
  }

  

  public Compensation2 (CompensationResults ui, File dataFolder){
//            public Compensation2 (CompensationFrame ui, File dataFolder)
	    
    compResults = ui;
    this.dataFolder = dataFolder;
    this.addPropertyChangeListener ((PropertyChangeListener)compResults);
    
//    if (ui == null)
//        V_UI = false;
//    else V_UI = true;
  }

  protected Exception doInBackground ()
  {
    /*
     * The contract with the UI is that detector, unstainedControls and
     * stainedControls shall exist and be unchanging and that
     * stainedControls.length > 1 when execute() is called. In return when
     * computations on each reagent are complete the worker will publish the row
     * after which the reagent data are constant and thread safe to process()
     * the results on the dispatch thread. When finished or an exception is
     * thrown done() will be called on the dispatch thread to shut down.
     */
      if (Compensation2.CATE){
          System.out.println (detector.length + "  "+ reagent.length);
          for (int i=0; i < detector.length; i++){
              System.out.print  (detector[i] + "   ");
              if (i < reagent.length){
                  System.out.println (reagent[i]);
              }
          }
      }
              
      int s0=0;
      if (unstainedControl == null ){
           System.out.println (" oh dear the unstained control is null");
      }
      else {
//          System.out.println (unstainedControl.length);
          s0=unstainedControl.length;
      }
//    assert unstainedControl != null && unstainedControl.length > 0;
    assert unstainedControl != null ;

    assert stainedControl != null && stainedControl.length > 1;

    int steps = s0 + stainedControl.length + 1;
    int thistep = 1;
//    System.out.println("Steps = " + steps + ", this step " + thistep);
    try
    {
      setProgress((int)(100.0 * thistep++ / steps));
      volts = new double[detector.length];
      fluorescenceRange = new double[detector.length];
      FCSParameter p;
      // The volt values are the same for each of the files. Just get one set
      // of values.
      int index=0;
      //Get any FCS file, but can't use a null stained control.
      while (index < stainedControl.length&& stainedControl[index] == null  ) index++;
      if (index >= stainedControl.length){
          System.out.println ("  Error Could not find a stained Control that wasn't null");
          return null;
      }
      for (int j = 0; j < detector.length; j++)
      {
          System.out.println (" detector "+ detector[j]);
          p = stainedControl[index].getFCSFile().getParameter(detector[j]);
          if (p != null){
              volts[j] = p.getDouble("$P", "V");
              fluorescenceRange[j] = p.getMaximum();
          }
      }
      scatterRange = new double[2];
//      System.out.println (stainedControl[index].getFCSFile().getFile().getName());
      if (stainedControl[index].getFCSFile().getFile().exists()){
          //System.out.println ("  Yes the file exists " + stainedControl[index].getFCSFile().getFile().getName());
          if (stainedControl[index].getFCSFile().getFile().canRead()){
              System.out.println (" yes it can also be read");
          }
          else
              System.out.println ("  No it cannot be read");
      }
      else {
          System.out.println ("  No the file does not exist ");
      }

      p = stainedControl[index].getFCSFile().getParameter("FSC-A");
      if (p == null){
          System.out.println (" the parameter is null for the FSC ");
          System.out.println (index + "  index of the stainedControl that doesn't have the FSC-A parameter.  ");
System.out.println (stainedControl[index].getFCSFile().getFile().getName() + "  " + stainedControl[index].getFCSFile().getFile().getPath());

      }
      scatterRange[0] = p.getMaximum();
      p = stainedControl[index].getFCSFile().getParameter("SSC-A");
      scatterRange[1] = p.getMaximum();
     

      for (int i = 0; i < unstainedControl.length; i++)
      {
        unstainedControl[i].load();
        unstainedControl[i].analyze();
        if (RECORD_ANY)
        {
          if (RECORD_FCS)
            unstainedControl[i].writeAugmented();
          if (RECORD_CSV)
            unstainedControl[i].writeAugmentedCSV();
        }
        setProgress((int)(100.0 * thistep++ / steps));
        // System.out.println (" this steps after the unstained control " +
        // thistep);
        if (isCancelled())
          return null;
      }
//       System.out.println (" Let's do the stainedControls now. "+ stainedControl.length);
      for (int i = 0; i < stainedControl.length; i++)
      {
          if (stainedControl[i] != null){
                stainedControl[i].load();
                stainedControl[i].analyze();
                
                publish(stainedControl[i]);
                if (RECORD_ANY)
                {
                  if (RECORD_FCS)
                    stainedControl[i].writeAugmented();
                  if (RECORD_CSV)
                    stainedControl[i].writeAugmentedCSV();
                }

                setProgress((int)(100.0 * thistep++ / steps));
                // System.out.println (" thistep after the ith stained control " +
                // thistep + ", "+ i);
                if (isCancelled())
                  return null;
          }
          else
              System.out.println (" no stained control for "+ i);
      }
      if (false)
        analyze();
    }
    catch (Exception e)
    {
      if (DEBUG)
        e.printStackTrace();
      return e;
    }
    setProgress(100);
   
    return null;
  }

  protected void process (List<StainedControl> controls)
  {
    // can't assume list has only one element
    Iterator<StainedControl> i = controls.iterator();
    if (i.hasNext())
    {
      while (i.hasNext())
      {
        StainedControl control = i.next();
        for (int j = 0; j < detector.length; ++j)
        {
          if (j == control.primary)
            continue;
      //    ui.setFailsSignificanceTest(control.reagent, j, control.slope[j] < control.slopeSigma[j]);
 //System.out.println ("Compensation2 process "+ control.reagent + ", "+ j + ", "+ control.slope[j]);
 //System.out.println("StainedControl in process "+ control.toString());
 if (compResults == null){
     System.out.println (" compResults are null ");
 }
              compResults.spilloverNotSignificantTest(control.reagent, j, control.slope[j] < control.slopeSigma[j]);

              compResults.setFailsLinearityTest(control.reagent, j, !control.linearity[j]);
              compResults.setFailsInterceptTest(control.reagent, j, control.intercept[j] < 0
                && -control.intercept[j] > 3 * control.interceptSigma[j]);
              compResults.setSpectrum(control.reagent, j, control.slope[j], 
                      Math.abs(control.slopeSigma[j] / control.slope[j]));
          
          
        }
      }
    }
    
  }

  protected void done ()
  {
    try
    {
      // need this to find out if the background thread died!
      // otherwise it hides from Eclipse debugger.
      Exception e = get();
      if (e != null){
        if (DEBUG )
          e.printStackTrace();

        StringBuilder buf = new StringBuilder(
          "An error condition has been detected.  ");
        buf.append(System.getProperty("line.separator")).append( e.getMessage());
        buf.append(System.getProperty("line.separator")).append ("The Application will exit.");
        compResults.reportMessage(buf.toString(), true);
           //ui.showMessageDialog(buf.toString());
        //System.exit(1);
      }

      else if (isCancelled()) {
          compResults.reportMessage ("Computation has been cancelled.", false);
           // ui.showMessageDialog("Computation has been cancelled. ");
        setProgress(100);
        System.exit(1);
      }
      else {
        setProgress(100);
        compResults.reportMessage(" Computation has completed successfully.", false);
        compResults.enableButton(true);
       
      }
    }
    catch (InterruptedException ie)
    {
      ; // for now just give way
    }
    catch (Exception e)
    {
      if (DEBUG )
        e.printStackTrace();
      StringBuilder buf = new StringBuilder(
        "An error condition has been detected.  ");
      buf.append(System.getProperty("line.separator")).append( e.getMessage());
      buf.append(System.getProperty("line.separator")).append ("The application will exit.");
      System.out.println (buf.toString());
//      if (ui != null)  
//          ui.showMessageDialog(buf.toString());
//      System.exit(1);
    }
  }

  public Diagnostic.List getDiagnosticsCell (int row, int col){
      

      if (row >= stainedControl.length || stainedControl[row] == null)
          return null;
     
      if (stainedControl[row].getPrimaryDetector() == row){
          
          Diagnostic.List[] one = stainedControl[row].diagnostics;
          if (one != null){
              if (col < one.length )
                  return one[col];
          }
      }
     
    
      return null;
  }


  public Diagnostic.List[]  getDiagnosticsForColumn (int col){
      Diagnostic.List[] newlist = new Diagnostic.List[stainedControl.length];
      for (int i=0; i < stainedControl.length; i++){
          if (stainedControl[i] != null){
//              System.out.println (stainedControl[i].toString());
              if (stainedControl[i].diagnostics != null)
                  if (stainedControl[i].diagnostics[col] != null )
                      newlist[i] = stainedControl[i].diagnostics[col];
          }
          
      }
      return newlist;
  }

  public Diagnostic.List[] getDiagnosticsForRow (int row){
 
      
      if (row < stainedControl.length && stainedControl[row] != null)
          return stainedControl[row].diagnostics;
     

      return null;

  }

  public int getPrimaryDetector (int row){

      int primary = -1;
//      for (int i=0; i < stainedControl.length; i++){
//          if (stainedControl[i].primary == row){
//              primary = stainedControl[i].primary;
//              break;
//          }
//      }
      if (row < stainedControl.length && stainedControl[row] != null)
          primary = stainedControl[row].primary;
      return primary;
  }

  public boolean isSpillOverNotSignificant (int row, int col){
      boolean flag = false;

      if (row < stainedControl.length && stainedControl[row] != null)
          flag = stainedControl[row].isSpillOverNotSignificant(col);

      return flag;
  }
  
  



  
}
