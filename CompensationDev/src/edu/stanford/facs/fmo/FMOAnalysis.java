/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.stanford.facs.fmo;


import edu.stanford.facs.data.FluorescenceCompensation;
import edu.stanford.facs.exp_annotation.DivaXmlParser;
import java.io.File;
import java.io.FilenameFilter;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import org.isac.fcs.FCSFile;
import org.isac.fcs.FCSTextSegment;


/**
 * given the data directory, the xml file, the instrument file and a matrix file.
 * Read the matrix file.  Match the array of detectors (in the matrix file)
 * with the detectors in the FCS file.  We don't care about the detectors that
 * are not used.
 * Read the instrument conf file for the reference voltage, area scaling values
 * From the xml file, get the observed voltage and area_scaling factors
 * EAch FCS file = one tube.  It contains the data matrix of [nevents, ndetectors].
 * that means that each column is the data for that detector.
 * multiple each row by the compensation matrix.
 * Write out using the writeAugmentedData()
 *
 * Calculate the Adjusted PE-Scaling vector =
 *    (ref area scaling / observed area scaling) * (ref PMTV/ obs PMT voltage
 *
 * 0^PMT exp)
 * (observed area scaling/ref area scaling) * (obs pmtV/ref PMTV)^pmt exp * inverse of each value
 * in the column called pe-/unit est.
 * Data[i] X adjusted scaling[i] * matrix not yet known.
 * This is then the raw data x this new matrix.
 *
 * Technically it would be a square diagonal matrix but yes it only has N non-zero values.
 * The data row vector would be first multiplied by the diagonal matrix to convert
 * signal levels to variances and then by the compensation matrix but with the
 * squared components to sum the variances. In practice we want to multiply the
 * diagonal matrix on the left of the squares matrix to compose them so the
 * computation is most efficient, i.e.,

V[i][j] = A[i] * C[i][j] * C[i][j]

for all i, j, where C is the compensation matrix and A is the adjustment.
 */

public class FMOAnalysis
{
  final static boolean DEBUG = true;

  final FMOFrame frame;
  final SwingWorker<Exception, Void> worker;

  String[] detectorNames;
  FluorescenceCompensation fmoCompMatrix; // the operations on the spectral
                                          // overlap matrix.

  private String[][] scalingValues; // from the xml file
  private String[][] voltValues; // from the xml file

  private File[] FCSfilenames;

  /**
   * Opens the FMOFrame to prompt the user to select the working direcotory
   * where the FCS files are located, the matrix file ( a csv file ), and the
   * instrument description file. Calls FMOAnanlysis constructor with these
   * parameters.
   */
  public FMOAnalysis (FMOFrame frame)
  {
    this.frame = frame;
    this.worker = new FMOWorker();
  }

  public void setFCSFiles (File[] files){
      FCSfilenames = files;
  }
   
    /** make a list of filenames in the working directory that end in
     * .fcs
     * @return File[]  the list of fcs files in the working directory.
     * Not being used.  Not doing it interactively.
     **/
    private  File[] getFCSFileList(){

        MyFilenameFilter filter = new MyFilenameFilter(".fcs", "fmo");

        File[] fcslist = frame.workingDirectory.listFiles (filter);

        return fcslist;
    }

    /**
     *
     * @param String  Matches a name with the list of detectorNames
     * @return int    Returns the index of the matching name in the detector List.
     */
    private int matchDetectorName (String name){
        boolean flag = false;
        int index=-1;
        for (int i=0; i < detectorNames.length; i++){
            if (detectorNames[i].equals (name))
                index=i;
                
        }

        return index;
    }


    /**
     *
     * @param matrixfile  File contains a matrix that was possibly exported
     *                    from from flowjo.  At least in a flowjo exported
     *                    format.
     */
    private void readMatrixFile(File matrixfile){
        fmoCompMatrix = new FluorescenceCompensation (matrixfile);
        detectorNames = fmoCompMatrix.getDetectorNames();
       // spectralOverlapMatrix = fmoCompMatrix.getSpillOverMatrix();

    }

    /**
     *
     * @param instrparam
     * @return
     */
    private Instrument readInstrumentParameters(String instrument_name) {

        InstrumentParameters instrParameters = new InstrumentParameters (instrument_name+".csv", detectorNames,
                                                    scalingValues, voltValues);
        
        Instrument instrument = instrParameters.getInstrument (instrument_name);
        return instrument;

    }

  private void readXMLFile ()
  {
    DivaXmlParser parser = new DivaXmlParser(frame.xmlFile, detectorNames);
    scalingValues = parser.parseAreaScalingValues(); // laser/scaling value
    voltValues = parser.parseVoltValues();// detector/voltage
  }
  
   


   class MyFilenameFilter implements FilenameFilter {
        String ext, second;
        MyFilenameFilter (String ext, String second){
           this.ext = ext;
           this.second = second;
        }
        public boolean accept (File file, String string) {
            boolean flag = false;
            if (file.isDirectory()  ){
                if (second != null && string.contains (second))
                    flag= false;
                else if (string.endsWith (ext))
                    flag = true;

            }

            return flag;
        }
    }

  public class FMOWorker
    extends SwingWorker<Exception, Void>
  {

    @Override
    protected Exception doInBackground ()
      throws Exception
    {
      try
      {
        readMatrixFile(frame.spilloverFile);
        readXMLFile();

        if (FCSfilenames == null || FCSfilenames.length == 0)
          throw new IllegalArgumentException("No FCS files found");

        String instrument_name;
        FCSFile fcsfile = new FCSFile(FCSfilenames[0]);
        FCSTextSegment segment = fcsfile.getTextSegment();
        instrument_name = segment.getAttribute("$CYT");
        if (DEBUG)
          System.out.println("Instrument name " + instrument_name);
        if (instrument_name == null)
          throw new IllegalArgumentException(
            " Unable to get the instrument name from the FCS file");

        Instrument instrument = readInstrumentParameters(instrument_name);
        Detector[] detector = instrument.getDetectors(detectorNames);

        int step = 1;
        int stepsize = (int)Math.round(100.0 / FCSfilenames.length);
        System.out.println(stepsize + "  " + FCSfilenames.length);
        for (File f : FCSfilenames)
        {
          fcsfile = new FCSFile(f);

          FMOData fmodata = new FMOData(fcsfile, "-fmo");
          fmodata.setFluoresenceCompensation(fmoCompMatrix);
          fmodata.setDetectorNames(detectorNames);

          fmodata.read(fmoCompMatrix);
          fmodata.analyze(detector);
          fmodata.writeAugmented();
          System.out.println("write augmented is done");

          step += stepsize;
          System.out.println("the step is " + step);
          if (step > 100)
            step = 100;
          setProgress(step);
        }
      }
      catch (Exception e)
      {
        e.printStackTrace();
        return e;
      }

      setProgress(100);
      // not thread safe!
     // frame.setButtonEnabled(true);
      return null;
    }

        @Override
    protected void done() {
          try
          {
            // need this to find out if the background thread died!
            // otherwise it hides from Eclipse debugger.
            Exception e = get();
          }
          catch (InterruptedException e)
          {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
          catch (ExecutionException e)
          {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
            System.out.println (" Swing Worker is done !");
        frame.matrixReady (fmoCompMatrix.getSpillOverMatrix(), detectorNames, "FMO ");
        frame.setButtonEnabled(true);
        // public void matrixReady (double[][] adjMatrix, String[] detectorNames, String exp_name) {
    }
  }
}
