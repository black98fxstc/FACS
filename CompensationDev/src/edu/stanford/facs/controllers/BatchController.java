/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.stanford.facs.controllers;

import edu.stanford.facs.exp_annotation.TubeInfo;
import com.apple.eio.FileManager;
import edu.stanford.facs.compensation.Compensation2;
import edu.stanford.facs.compensation.UnstainedControl;
import edu.stanford.facs.exp_annotation.DivaXmlParser;
import edu.stanford.facs.gui.CompensationResults;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.SwingWorker;
import org.isac.fcs.FCSFile;

/**
 * $Id: BatchController.java,v 1.1 2012/01/25 01:41:07 beauheim Exp $
 * @author cate
 */
public class BatchController extends CompensationController implements CompensationResults{
   // private String workingDir;
    
    /*
     * to hold the compensation results.  Probably these should be part of the
     * CompensationResults interface.  
     */
    private boolean[][] failsLinearityTest;
    private boolean[][] failsMedianTest;
    private boolean[][] failsSignificanceTest;
    private boolean[][] failsInterceptTest;
    private Float[][] sensitivityData;
    private Float[][] spectrumData;
    private Float[][] errorData;
    private StringBuilder logMessages = new StringBuilder();
    private HashMap <String, TubeInfo> tubeMap = new HashMap<String,TubeInfo>();
    
    
    BatchController(String prop) {
        super (prop);
        String[]datadirs = readDirectoryList (prop); 
        processList (datadirs);
    }
    
    private String[] readDirectoryList (String propfile){
        BufferedReader reader = null;
        ArrayList<String> directories = new ArrayList<String>();
        String[] dirs = new String[directories.size()];
        File pfile = new File (propfile);
        if (pfile.exists() && pfile.canRead()){
            try {
                reader = new BufferedReader (new FileReader (pfile)); 
                String line = reader.readLine();
                while (line != null){
                    directories.add (line);
                    line = reader.readLine();
                }
                reader.close();
            } catch (IOException e1){
                StringBuilder msg = new StringBuilder().append ("File IOException ").append (pfile.getName());
                reportMessage (msg.toString());
            }
        }
        else {
            StringBuilder msg = new StringBuilder().append ("The file of working directories could not be found or read ").append (propfile);
            reportMessage(msg.toString());
        }
        dirs = directories.toArray(dirs);
        for (int i=0; i < dirs.length; i++)
            System.out.println (dirs[i]);
        return dirs;
        
    }
    
    private void processList(String[] datadirs) {
        XmlFilenameFilter xmlfilter = new XmlFilenameFilter();
        String mappingfilename;
        for (int i=0; i < datadirs.length; i++){
            mappingfilename = null;
          
            String[] mappingdir = datadirs[i].split(",");
            File onedir = new File (mappingdir[0]);
            if (mappingdir.length == 2)
                mappingfilename = mappingdir[1];
            else
                mappingfilename = null;
//            File onedir = new File (datadirs[i]);
     System.out.println (mappingdir[0] + "  "+ mappingfilename);
            if (onedir.isDirectory() && onedir.canRead()){
                dataFolder = onedir;
                if (mappingfilename != null){
                    File mappingFile = new File (mappingdir[0]+File.separator+mappingfilename);
                    String[][] mappingInfo = readMappingFile (mappingFile);
                    System.out.println ("  Mapping Info ");
                    StringBuilder buf = new StringBuilder ("\n----------");
                    buf.append (onedir.getName() ).append ("   ").append (mappingfilename).append("--------\n");
                    reportMessage (buf.toString());
                    
                    for (int j=0; j < mappingInfo.length; j++){
                        System.out.println (mappingInfo[j][0] + ", "+ mappingInfo[j][1] + ", "+ mappingInfo[j][2]+ ", "+mappingInfo[j][3]);
                    }
                    
                    createUnstainedStainedControls (mappingInfo, onedir, false, (CompensationResults)this);
                    initialize();
                    runAnalysis (onedir, mappingfilename);//runAnalysis is called from createUnstainedStainedControls
                    
                    
                }
                else {
                    String[] xmls = onedir.list (xmlfilter);
                    if (xmls.length > 1){
                        StringBuilder msg= new StringBuilder().append ("hmmm.  more than one xml file in this directory:  ");
                        for (String s: xmls)
                            msg.append (s).append(", ");
                        reportMessage(msg.toString());
                    }
                    File divafile = new File (dataFolder + File.separator + xmls[0]);
                    readDivaFile (divafile);
                    runAnalysis(onedir, mappingfilename);
                }
            }
            else {
                reportMessage (" Unable to open the data directory "+ onedir.getName() + ": "+ onedir.getPath());
            }
            writeLogFile(onedir);
            cleanUp();
            
        }
        
    }
    
    private void cleanUp() {
        System.out.println(" Clean up the memory, return and reset but what?  ");
        compensation2 = null;
        logMessages = new StringBuilder();
    }
    
    private String[][] readMappingFile (File file){
        ArrayList<String[]> dataone = new ArrayList<String[]>();
        String[][] data = null;
        
        try {
            if (file.exists() && file.canRead()){
                BufferedReader reader = new BufferedReader (new FileReader (file));
                String line = reader.readLine();
                while (line != null){
                    String[] oneline = line.split(",");
                    
                    dataone.add(oneline);
                    line = reader.readLine();
                }
                reader.close();
                data = new String[dataone.size()][4];
                data = dataone.toArray (data);
            }
            
        }catch (IOException ioe){
            System.out.println (ioe.getMessage());
        }
        
       
        
        return data;
        
        
    }
    
    private void writeLogFile (File onedir){
        
       //logMessages  
        if (logMessages == null){
            System.out.println ("  No messages for this directory "+ onedir.getName());
            return;
        }
        if (onedir.isDirectory() && onedir.canWrite()){
            File logfile = new File (onedir.getAbsolutePath()+ File.separator+"log.txt");
//            if (logfile.canWrite()){
                try {
                    FileWriter writer = new FileWriter (logfile, true);
                    System.out.println (logfile.getName() + ": " + logMessages.toString());
                    writer.write (logMessages.toString());
                    writer.close();
                } catch (IOException ioe){
                    System.out.println ("  Unable to file the log file!");
                }
//            }
        }
    }
    
    protected void readDivaFile(File divaFile) {
      String errmsg = new String();
    
      if (divaFile != null && divaFile.exists() && divaFile.canRead()){
//          String ipath = datafile.getPath();
          if (dataFolder == null)
             dataFolder = new File(divaFile.getParentFile().getPath());

        
        DivaXmlParser parser = new DivaXmlParser(divaFile, tubeMap);
//        stainSets = parser.getStainSets();
        String[][] fl_labels = null;
        if (parser.hasMultipleFluorescents()){
            fl_labels = parser.getFl_LabelList();

        }
        
        //this is actually the names of the Stained Controls.  In the case
        //of a stanford diva file, it is a combination of the reagent
        //and the detector.  For the tandem experiment, there are
        //16 stained controls + 1 unstained control, but 10 acquisition channels

        String[] allControls = parser.getControlList();
       
        errmsg = "There were no controls found in the diva file ";
        //allcontrols here means the Unstained Control + all the others

        experimentName = parser.getExperimentName();
       

        // the allControls list includes the unstained control

        detectorList = parser.getDetectorList();
        System.out.println (detectorList.length + "  the detector list.");
        String[] fcsfiles = parser.getFCSFilenames();
        if (Compensation2.CATE){
              
              if (detectorList != null && detectorList.length > 0){
                  for (String s: detectorList)
                      System.out.println ("detector List " + s);
              }
              if (PnSreagents != null && PnSreagents.length > 0){
                  for (String s: PnSreagents)
                      System.out.println (" reagent Names "+ s);
              }
          }
       

        // check out what we got. Did they use controls?
        // if all Controls are null, then they didn't use DiVa compensation
        // controls. So manually get them.
        // String[]allControls, String[]detector, String[]fcsfiles.
        if (allControls != null && allControls.length > 0){
 
              controlList = setUpFcsFiles (allControls, fcsfiles, fl_labels);
              System.out.println ("Did we come up with any controls?  " + controlList.length);
              initialize();
              
          
        }
        else {
            //No controls were found in the xml file, but I did find a detectorList and fcsfiles
            reportMessage (" No controls were found in the DiVa xml file.  ");
        }

      }

    else {
       reportMessage("We were unable to read and parse one of the datafiles. "
        + divaFile);

       System.exit(1);
      }


  }
    protected String[] setUpFcsFiles (String[] allControls, String[] filenames,String[][]fl_labels) {
    int unstained = 0;
    int stained = 0;

    for (String s : allControls)
    {
      if (s != null)
      {
        if (s.equalsIgnoreCase("Unstained Control"))
          unstained++;
        else
          stained++;
      }
    }
    controlList = new String[stained];

    if (unstained + stained != allControls.length)
    {
//      System.out.println("Problem with the number of unstained and stained controls.  Things don't add up");
      reportMessage("Problem with the number of unstained and stained controls.  Things don't add up");
      return null;
    }
    unstainedFCS = new FCSFile[unstained];
    stainedFCS = new FCSFile[stained];

    int u = 0, s = 0;

    for (int i = 0; i < allControls.length; i++)
    {
// System.out.println ("Set up fcsfiles::  controls "+ allControls[i]);
      if (allControls[i].equalsIgnoreCase("Unstained Control"))
      {
        unstainedFCS[u++] = new FCSFile(filenames[i]);

      }
      else
      {
//         System.out.println ("Stained Controls " + allControls[i] + "  "+ filenames[i] );
         controlList[s] = allControls[i];
      //  PnSreagents[s] = allControls[i];
        stainedFCS[s++] = new FCSFile(filenames[i]);

      }
    }
 

    createUnstainedStainedControls (unstainedFCS, stainedFCS, fl_labels, false, (CompensationResults)this);
    //it is the controlList that works.
    return controlList;
  }


       
    protected void runAnalysis(File workingdir, String matrixfilename) {
      
        
        if (PnSreagents.length < detectorList.length)
            compensation2.setDetectorsAndControls (detectorList, detectorList);
        else
            compensation2.setDetectorsAndControls (detectorList, PnSreagents);
       
        if (stainedControls == null || stainedControls.length == 0){
            reportMessage ("  no stained controls");
        }
        else {
            compensation2.stainedControl = stainedControls;
        }

//if there are no unstained controls, it should be represented as an array
        //of UnstainedControl[] with size 0;
        if (unstainedControls == null ){
            unstainedControls = new UnstainedControl[0];
            compensation2.unstainedControl = unstainedControls;
        }
        else {
            compensation2.unstainedControl = unstainedControls;
        }

        //do we have a detectorList?
        if (detectorList == null || detectorList.length == 0)
            reportMessage (" detector list is empty");


       compensation2.execute();
       int i = 0;
       while (compensation2.getState() != SwingWorker.StateValue.DONE || i > 50){
          try {
             Thread.sleep (1000);
             i++;
          } catch (Exception e){
              
          }
          System.out.println ("Been asleep for " + i );


           }
       int[] counts= new int[detectorList.length];
       
       boolean multiples = checkForMultiples(counts);
       if (multiples){
           //got to do something here
           System.out.println ("multiples are true  ");
           for (int j=0; j < detectorList.length; j++)
              System.out.println (detectorList[j] + "  "+ counts[j]);
       }    
       printFlowJoMatrix (  spectrumData,  detectorList,  experimentName);
       if (matrixfilename == null)
             printMatrixForR (spectrumData, detectorList, experimentName);
       else
           printMatrixForR (spectrumData, detectorList, matrixfilename);
       reportMessage("Execution is complete " + workingdir.getName());
       

    }

    @Override
    public void setFailsLinearityTest (int i, int j, boolean fails) {
       System.out.println ("Fails LinearityTest " + i + " "+ j + "  "+ failsLinearityTest.length+ "  "+ failsLinearityTest[i].length);
       if (i < failsLinearityTest.length && j < failsLinearityTest[i].length)
            failsLinearityTest[i][j] = fails;
        
    }

    @Override
    public void setFailsInterceptTest (int i, int j, boolean fails) {
    
      if (i < failsInterceptTest.length && j < failsInterceptTest[i].length)

        failsInterceptTest[i][j] = fails;
       
    }
    public void setFailsSignificanceTest (int i, int j, boolean fails) {
    if (i < failsSignificanceTest.length && j < failsSignificanceTest[i].length)
        failsSignificanceTest[i][j] = fails;
        
//    spectrumModel.setSpectrum();
    }

    @Override
    public void setSpectrum (int i, int j, double spillover, double uncertainty) {
          if (i < spectrumData.length && j < spectrumData[i].length){

            spectrumData[i][j] = new Float(spillover);
            System.out.println ("setSpectrum " + i + ", "+ j + ", "+ spillover);
            errorData[i][j] = new Float(uncertainty);
          }
    
    }
    
    public void reportMessage(String msg){
        logMessages.append ("\n").append(msg);
        System.out.println ("  Report Message "+ msg);
       
      
    }
    
    public void spilloverNotSignificantTest (int reagent, int j, boolean b){
        if (reagent < failsSignificanceTest.length){
           if (j < failsSignificanceTest[reagent].length);
              failsSignificanceTest[reagent][j] = b;
        }
    }
    
    public void enableButton (boolean b){
       //not relevant in batch mode. 
    }
    
    private void initialize () {
      
        System.out.println (controlList.length + ", "+ detectorList.length);
/*changing the size of these arrays */
      failsLinearityTest = new boolean[controlList.length][detectorList.length+1];
      failsMedianTest = new boolean[controlList.length][detectorList.length+1];
      failsSignificanceTest = new boolean[controlList.length][detectorList.length+1];
      failsInterceptTest = new boolean[controlList.length][detectorList.length+1];

      sensitivityData = new Float[6][controlList.length+1];
      spectrumData = new Float[controlList.length][detectorList.length+1];
      for (int i=0; i < controlList.length; i++) {
          for (int j=0; j < detectorList.length; j++){
              if (i == j) spectrumData[i][j]= (float)1.0;
              else spectrumData[i][j]=(float)0.0;
          }
      }
      errorData = new Float[controlList.length][detectorList.length+1];
    }
    
    private void printMatrixForR ( Float[][] spectrumData, String[] detectorNames, String filename){
        
        StringBuilder buf = new StringBuilder();
        int idx = filename.indexOf(".");
        if (idx > -1)
            filename = filename.substring (0, filename.indexOf("."));
        System.out.println ("  filename "+ filename);
        buf.append (dataFolder.getAbsolutePath()).append (File.separator).append (filename).append("_matrix.txt");
        File fn = new File (buf.toString());
        String split = "\t";
        String sep = System.getProperty("line.separator");
        try {
           PrintWriter pw = new PrintWriter (new BufferedWriter (new FileWriter (fn)));
           int dim = spectrumData.length;
           for (int i=0; i < detectorNames.length-1; i++){
               pw.print (detectorNames[i]);
               pw.print (split);
           }
           pw.print (detectorNames[detectorNames.length-1]);
           pw.print (sep);
        System.out.println (detectorNames.length + "  "+ spectrumData.length) ;  
           for (int i=0; i < spectrumData.length; i++){
               for (int j=0; j < spectrumData.length-2; j++){
                   pw.print (spectrumData[i][j] );
                   pw.print (split);
               }
               pw.print (spectrumData[i][spectrumData[i].length-2]+sep);
           }
           pw.close();
       } catch (IOException ioe){
           reportMessage ("IO Exception.  Unable to write the matrix file "+ fn.getName());
       }
        
    }
    
    public void reportMessage (String msg, boolean flag){
        
    }
    
    public void printFlowJoMatrix ( Float[][] spectrumData, String[] detectorNames, String expName){
        StringBuilder buf = new StringBuilder();
        buf.append (dataFolder.getAbsolutePath()).append( File.separator).append (expName).append ("_matrix");
       File fn = new File (buf.toString());
       
       

           String os = System.getProperty ("os.name");
//  printFlowJoMatrixForPC (fn, spectrumData, detectorNames, experimentName);
      if (!os.equalsIgnoreCase ("Mac OS X")){
          if (!fn.getName().endsWith (".mtx"))
              fn = new File (fn.getPath() +".mtx");
          printFlowJoMatrixForPC (fn, spectrumData, detectorNames, experimentName);
          return;
      }

//        String nl = System.getProperty ("line.separator");
        String nl = "\r";

        String split = "\t";
        if (fn != null){
            if (!fn.getName().endsWith (".txt")){
                fn = new File (fn.getPath() +".txt");

            }

            try {
                PrintWriter fw = new PrintWriter (new BufferedWriter(new FileWriter(fn)));
                if (expName == null)expName="";
                fw.print(expName);

                fw.print(nl);
                fw.print("<"+split+">");

                fw.print (nl);
                fw.print (detectorNames[0]);
                for (int i=1; i < detectorNames.length; i++){
                    fw.print(split);
                    fw.print (detectorNames[i]);
                }
                fw.print(nl);
                int dim = spectrumData.length;
                System.out.println ("  spectrumData dimension " + dim + " == "+ spectrumData[0].length+ "  "+ detectorNames.length);
//                if ( dim != spectrumData[0].length  ){
//                    System.out.println ("  Not a square matrix" + dim + spectrumData[0].length);
//                    JOptionPane.showMessageDialog(this, "Not a Square Matrix", "Not a Square Matrix", JOptionPane.WARNING_MESSAGE);
//                    fw.close();
//                    return;
//                }
                for (int i=0; i < dim; i++){
                    for (int j=0; j < dim-1; j++){
                        fw.print (spectrumData[i][j].floatValue());
                        System.out.println (spectrumData[i][j].floatValue() + "  "+ Float.toString (spectrumData[i][j]));
                      //  fw.write (Float.toString(spectrumData[i][j]));
                        fw.print (split);
                    }
                    fw.print (spectrumData[i][dim-1].floatValue());
                    fw.print (nl);
                }
                fw.close();
                os = System.getProperty ("os.name");
                if (os.equalsIgnoreCase ("Mac OS X")){
                    int t = makeOSType ("TEXT");
                    int c = makeOSType ("ttxt");  
                    FileManager.setFileTypeAndCreator (fn.getPath(), t, c);
                }

            } catch (IOException e){
                System.out.println (" IO Exception " + e.getMessage());
                
                reportMessage("Error trying to write the matrix file. "+ fn.getName());
            }
        }


  }
    
    class XmlFilenameFilter implements FilenameFilter {
        
        public boolean accept (File dir, String name){
            if (name.endsWith (".xml"))
                return true;
            return false;
        }
    }
    
    public static void main (String[] args){
        
        if (args == null || args.length < 1){
            System.out.println ("  No command line arguments");
            System.exit(1);
        }
        BatchController controller = new BatchController(args[0]);
    }
    
   
    
}
