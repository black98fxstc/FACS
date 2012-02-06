/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.stanford.facs.controllers;

import edu.stanford.facs.compensation.Compensation2;
import edu.stanford.facs.compensation.Diagnostic;
import edu.stanford.facs.compensation.UnstainedControl;
import edu.stanford.facs.exp_annotation.JoFile;
import edu.stanford.facs.exp_annotation.TubeInfo;
import edu.stanford.facs.gui.CompensationResults;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import javax.swing.SwingWorker;

/**
 * $Id: CGJoController.java,v 1.2 2012/01/31 01:19:02 beauheim Exp $
 * @author cate
 */
public class CGJoController extends CompensationController implements CompensationResults,
                                                                      PropertyChangeListener {
    protected boolean[][] failsLinearityTest;
    protected boolean[][] failsMedianTest;
    protected boolean[][] failsSignificanceTest;
    protected boolean[][] failsInterceptTest;
    protected Float[][] sensitivityData;
    protected Float[][] spectrumData;
    protected Float[][] errorData; 
    private JoFile joFile;
    private String outdir;
    
    
    CGJoController (){
        System.out.println ("CGJoController constructor with no parameters");
    }
    
    public CGJoController (String jofilename, String outdir){
        File jofile = new File (jofilename);
        dataFolder = new File (outdir);
        visual = false;
        if (jofile.exists() && jofile.canRead()){
            //creating the JoFile also parses the JoFile and sets up everything.  
            joFile = new JoFile (jofilename, this);
            System.out.println("---- jo file is read ");
          
            setUpAnalysis (joFile);
            
        }
        else {
            System.out.println ("  Cannot read the jo file "+ jofilename);
        }
        
    }
    
    private void setUpAnalysis (JoFile jo){
        ArrayList <String> urls = new ArrayList<String>();
        tubeMap = jo.getTubeMap();  //this is inherited.
        //these are actually the list of compensation controls.  Not the complete list of detectors.
        String[][] parameters = jo.teaseOutDetectors();
        HashMap<Integer, ArrayList <String>> allParameters = jo.getParameterNames();
        Set keys = allParameters.keySet();
        Integer[] sortedKeys = new Integer[keys.size()];
        Iterator itt = keys.iterator();
        int k=0;
        
        while (itt.hasNext()){
            sortedKeys[k++] = (Integer) itt.next();
            
        }
        Arrays.sort(sortedKeys);
        ArrayList <String> controls = new ArrayList<String>();
        ArrayList <String> channels = new ArrayList<String>();
        int total = 0;
        for (int i=0; i < sortedKeys.length; i++){
            ArrayList<String> p = allParameters.get(sortedKeys[i]);
            if (p.size() > 0){
                channels.add (p.get(0));
            }
            if (p.size() == 1){
                controls.add (p.get(0));
            }
            else if (p.size() > 1){
                for (int j=1; j < p.size(); j++){
                    controls.add (p.get(0) + " " + p.get(j));
                }
            }
            
            
        }
        for (int i=0; i < channels.size(); i++){
            System.out.println (" \t channels   "+ channels.get(i));
        }
        for (int j = 0; j < controls.size(); j++){
            System.out.println (" \t  controls " + controls.get(j));
        }
        detectorList = new String[channels.size()];
        detectorList = channels.toArray(detectorList);
        controlList = new String[controls.size()];
        controlList = controls.toArray(controlList);
            
        //the key to the array is the N of the fcs header value.  
        //There may be only one string in the list and that is the PnN value.  Subsequent
        //strings are the PnS values
//         PnSreagents = new String[parameters.length];
//         detectorList = new String[parameters.length];
//         controlList = new String[parameters.length];
        

//        for (int i=0; i < parameters.length; i++){
//            System.out.println ("\t set up Analysis  " +  parameters[i][0] + "  "+ parameters[i][1]);
//            detectorList[i] = parameters[i][0];
//            controlList[i] = parameters[i][0];
//            PnSreagents[i] = parameters[i][1];
//        }
        Collection tubes =  tubeMap.values();
        Iterator it = tubes.iterator();
        while (it.hasNext()){
            TubeInfo tube = (TubeInfo) it.next();
            if (tube.getTubeType().equalsIgnoreCase ("compensation")){
                urls.add (tube.getURL());
            }
        }
        String[] c = new String[urls.size()];
        c = urls.toArray (c);
//        System.out.println ("----------- URLS -------");
//        for (String s: c){
//            System.out.println (s);
//        }
        jo.startDownloads (c);
//        System.out.println ("---------CGJoController has returned to from startDownloads----------");
        //set up the controls and line up with the parameterNames
        //download the controls in JoFile
        //then I can call createUnstained...
        //createUnstainedStainedControls (String[][]data, File workingdir, boolean mode, CompensationResults, HashMap)
        //initialize () creates memory for fail arrays
        //runAnalysis();
        //get the results (spectrumData()
        //apply the stain information and write out the matrix as FlowJo matrix.
        
        
        
    }
    
     public void returnFromDownload (HashMap<String, TubeInfo> tubeMap, File savedFilesDirectory){
        System.out.println ("CGJoController.returnFromDownload");
       this.tubeMap = tubeMap;
       tempJoFolder = savedFilesDirectory;
       dataFolder = savedFilesDirectory;
       findStainSets();
        continueAnalysis();
    }
    
    
    protected void  continueAnalysis() {
        System.out.println (" continue Analysis");
        Collection values = tubeMap.values();
        Iterator it = values.iterator();
        ArrayList<String[]> dataraw = new ArrayList<String[]>();
        while (it.hasNext()){
            
            TubeInfo tube = (TubeInfo) it.next();
            System.out.println (tube.getInfo());
            if (tube.getTubeType().equalsIgnoreCase ("compensation")){
                String[] onerow = new String[4];
                onerow[0] = tube.getAnalyteFor(0);
                onerow[1] = tube.getLabelFor(0);
                onerow[3] = tube.getFcsFilename();
//                System.out.println (onerow[0] + ", "+ onerow[1] + ", "+ onerow[3]);
                dataraw.add (onerow);
            }
            
            
        }
        String[][] data = new String[dataraw.size()][4];
        data = dataraw.toArray (data);
        for (int i=0; i < dataraw.size(); i++){
            
            data[i] = dataraw.get(i);
            
        }
        System.out.println (" end of Tubes");
        File dir;
        if (outdir != null){
            dir = new File (outdir);
        }
        else {
            String home = System.getProperty("user.home");
            dir = new File (home + File.separator + "Compensation.out");
        }
         initialize();
        //this is the directory where the data is, the Fcs files.  
        createUnstainedStainedControls (data, joFile.getTempDirectory(), false, (CompensationResults)this, tubeMap);
       
        runAnalysis();
        
        //what is the name of the detector, the reagent, the unstained fcs file and the stained fcs file.
//        String[][]data, File workingDir,
//            boolean mode, CompensationResults results,
//            HashMap <String, TubeInfo> tubeMap
        //createUnstainedStainedControls (String[][]data, File workingdir, boolean mode, CompensationResults, HashMap)
        //initialize () creates memory for fail arrays
        //runAnalysis();
        //get the results (spectrumData()
        //apply the stain information and write out the matrix as FlowJo matrix.
    }
    
    protected void runAnalysis() {
        System.out.println ("  run analysis ");
        
        if (compensation2 == null)
            compensation2 = new Compensation2((CompensationResults)this, dataFolder);
       System.out.println ("CGJoController. runAnalysis " + detectorList.length + "  "+ PnSreagents.length);
       for (int i=0; i < detectorList.length; i++){
            System.out.print ("Run Analysis "+ detectorList[i] + "  ");
            if (i < PnSreagents.length)
                System.out.println (PnSreagents[i]);
       }
        if (PnSreagents.length < detectorList.length)
            compensation2.setDetectorsAndControls (detectorList, detectorList);
        else
            compensation2.setDetectorsAndControls (detectorList, PnSreagents);
       
        if (stainedControls == null || stainedControls.length == 0){
            System.out.println ("  no stained controls");
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
            System.out.println (" dtector list is empty");

        
System.out.println (" CGJoController.runanalysis end....... ");
       compensation2.execute();
       int i=0;
       while (compensation2.getState() != SwingWorker.StateValue.DONE || i > 50){
          try {
             Thread.sleep (1000);
             i++;
          } catch (Exception e){
              
          }
          System.out.println ("Been asleep for " + i );


       }
       

    }
    
    private void finish() {
        System.out.println ("  CGJocontroller.finish");
        String filename;
        if (experimentName != null && !experimentName.equals("")){
            filename = experimentName+"_Matrix";
        }
        else
            filename = "Matrix";
         
        System.out.println ("  filename of matrix printing out.....");
        printFlowJoMatrix (spectrumData, detectorList, filename);
        printDiagnostics();
        //spectrumData
        //output directory
        //what about the diagnostics.  then it is done.
    }
    
    protected void printDiagnostics() {
       StringBuilder  allmsgs = new StringBuilder();
       Diagnostic.List msgs;
       
       System.out.println ("............. ... printDiagnostics ............");
       if (spectrumData == null ){
           System.out.println ("  printdiagnostics -- no spectrum Data ");
       }
       
      
       for (int i=0; i < spectrumData.length; i++){
           for (int j=0; j < spectrumData[i].length; j++) {
                msgs = compensation2.getDiagnosticsCell (i, j);
                if (msgs != null && msgs.size()>0) {
                    for (int h=0; h < msgs.size(); h++){
                        allmsgs.append (msgs.get(h).toString()).append("\n");
                      System.out.println  (msgs.get(h).toString());
                    }
               }
           }
       }
       
       reportMessage (allmsgs.toString());
   }

    
    private void findStainSets () {
            System.out.println ("Find Stain Sets ");
         int n=1;            
        Collection tubes = tubeMap.values();
        Iterator it = tubes.iterator();
        while (it.hasNext () ){
            System.out.println (" tube number "+ n++);
            TubeInfo onetube = (TubeInfo) it.next();
            if (onetube.getTubeType().equals ("analysis")){
                ArrayList<String[]> onecompset = onetube.getCompensations(); 
                Integer[] onestainset = new Integer[onecompset.size()];
                for (int i=0; i < onecompset.size(); i++){
                    String[] one = onecompset.get(i);
                    onestainset[i] = new Integer (one[1]);
                }
                Arrays.sort(onestainset);
                System.out.println (" sort the stainsets ");
                if (isUnique(onestainset)){
                    System.out.println (" is Unique returns");
                    uniqueStainSets.add (onestainset);

                }
            }
        }
        System.out.println (" End find stain sets");
    }
    
    protected boolean isUnique(Integer[] onestainset){
        boolean unique= false;
        System.out.println ("isUnique ");
//        if (uniqueStainSets.isEmpty()){
//           unique = true;
//            
//        }
//        else {
//            while (!unique ){
//                for (Integer[] nextone: uniqueStainSets){
//                    for (int i=0; i < nextone.length; i++){
//                        if (nextone[i].intValue() != onestainset[i].intValue()){
//                            unique = true;
//                            break;
//                        }
//                    }
//                }
//                
//            }
//        }
        
//        return unique;
        return true;
    }
 
private void initialize () {
      //the controlList is really the number of compensation tubes and what will become
    //the number of rows in the matrix.  
    //The detectorList is the number of channels on this instrument and will become
    //the number of columns in the matrix.  
        System.out.println ("Initialize ?  "  + controlList.length + ", "+ detectorList.length);
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

    @Override
    public void setSpectrum (int i, int j, double spillover, double uncertainty) {
        
         if (i < spectrumData.length && j < spectrumData[i].length){
            spectrumData[i][j] = new Float(spillover);
//            System.out.println ("--------------setSpectrum " + i + ", "+ j + ", "+ spillover);
            errorData[i][j] = new Float(uncertainty);
          }
    }

    @Override
    
    public void reportMessage (String msg, boolean flag) {
        System.out.println (" CGJoController.reportMessage "+ msg + "  "+ flag);
    }

    @Override
    public void spilloverNotSignificantTest (int reagent, int j, boolean b) {
        if (reagent < failsSignificanceTest.length){
           if (j < failsSignificanceTest[reagent].length);
              failsSignificanceTest[reagent][j] = b;
        }
    }

    @Override
    public void setFailsSignificanceTest (int i, int j, boolean fails) {
         if (i < failsSignificanceTest.length && j < failsSignificanceTest[i].length)
            failsSignificanceTest[i][j] = fails;

    }
    
    public void propertyChange (PropertyChangeEvent pce) {
        System.out.println ("CGJoController  propertyChanged " + pce.getSource().getClass() );
        if (pce.getSource() instanceof Compensation2){
            if ("state".equals (pce.getPropertyName()) && SwingWorker.StateValue.DONE == pce.getNewValue()){
                 System.out.println ("  Compensation2 has finished  !!");
                 
                 finish();
                
            }
            else 
                System.out.println ("  Compensation2 has not yet finished "+ pce.getNewValue().toString());
            
        }
//        if ("state".equals (pce.getPropertyName()))
//            System.out.println (pce.getOldValue() + "   " + pce.getNewValue());
////        System.out.println (pce.getSource().getClass().getName());
////        System.out.println (pce.getNewValue().toString() + "  "+ pce.getOldValue().toString());
        
        else if (pce.getSource() instanceof JoFile) {
            if ("state".equals(pce.getPropertyName())
                 && SwingWorker.StateValue.DONE == pce.getNewValue()) {
             System.out.println ("  CGJo Controller property change stateValue is done ");
             continueAnalysis();
         }
        }
        
//        if (pce.getNewValue().toString().equals("100")){
////        if (pce.getNewValue().toString().equals ("100")){
//            System.out.println ("  Done with download..... in CGJoController  ");
//            
//            
//        }
    }

    @Override
    public void enableButton (boolean b) {
    }
    
    public static void main (String[] args){
        if (args.length == 2){
            CGJoController cjc = new CGJoController (args[0], args[1]);
            
        }
        else {
            System.out.println ("  Need the path to the Jo file. And the directory for the output.");
            String fn = "/Users/cate/AFCSData/AutoComp2/Autocomp Development-Cate-cytogenie test/Cate-Cytogenie test.jo";
            File file = new File (fn);
            String dirname = file.getParent();
            System.out.println (dirname);
            CGJoController cjc = new CGJoController (fn, dirname);
    //        JoFile jo = new JoFile ((CompensationController) null);
          //  JoFile jo = new JoFile (fn);
         //   jo.parseJoFile (file);
    //        ArrayList <String[]>parsedInfo = jo.getFileURLMapping();
            System.out.println ("------------------------------------------------------------------------");       
        
    }
        
    }
    
}
