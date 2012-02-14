/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.stanford.facs.exp_annotation;

import edu.stanford.facs.compensation.Compensation2;
import edu.stanford.facs.controllers.CGJoController;
import edu.stanford.facs.controllers.CompensationController;
import edu.stanford.facs.gui.DownloadDialogSimple;
import edu.stanford.facs.gui.FCSFileDialog.MappingInterface;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * $Id: JoFile.java,v 1.2 2012/01/31 01:19:01 beauheim Exp $
 * @author cate
 * Parse the Jo file to get the urls for the datafiles.  the url for each data
 * file is the first of two lines (per file).  It starts with http://.  The second
 * line is a description of the file.  The keyword $CELLS may describe the unstained
 * and stained controls.  We hope anyway.  Following the $CELLS keyword maybe
 * FITC Stained Control.  The next key is P4MS.  So we want to pick up everything
 * between the $CELLS and P4MS.  Make a table and return it for display.
 * final Preferences preferences = Preferences.userNodeForPackage(this.getClass());
 * String p = preferences.get("comp_data_folder", null);

 */
public class JoFile  extends SwingWorker<Exception, String[]> 
                     implements MappingInterface {
    
    private File jofile;
    File dataFolder;
    ArrayList <String[]> fileURLMapping = new ArrayList <String[]>();
    ArrayList <String[]> downloadURLS = new ArrayList<String[]>();
   // String tempdir = "myfcsdir";
    static final int BUFFER_SIZE = 8192;
    JDialog dialog = new JDialog();
//    DownLoadListener lis ;
    private String projectName;
    private String divaXMLFilename;
    private File tempFolder;
    private String[][] controlData;
    private HashMap <Integer, ArrayList<String>> parameterNames = new HashMap<Integer,ArrayList<String>>();
    private HashMap <String, TubeInfo> tubeMap = new HashMap<String, TubeInfo>();
 //   private DownloadDialog dd;
    private CompensationController cc;
    private File savedFilesDirectory;
    private boolean saveFiles;
    private HashMap< String, Integer> stainSets = new HashMap<String, Integer>();
    private String detectorName;  //$PnN
    private String reagentName;   //$PnS
    

    
    

     class DataStoreAuthenticator extends Authenticator {

        //override
         protected PasswordAuthentication getPasswordAuthentication() {
            char[] passwd= new String("314159").toCharArray();
             PasswordAuthentication auth = new PasswordAuthentication ("flowjo", passwd);

             return auth;
         }
    }

    //0 is the url, 1 is Stained Control or Unstained Control.
    /**
      * Called by the CGJoController
      * @param filename
      * @param cc 
      */
    public JoFile (String filename, CompensationController cc){
        this.cc = cc;
        jofile = new File (filename);
        init (jofile);
        System.out.println ("  JoFile is returning for parsing");
        String[][] names = teaseOutDetectors();
       for (int i=0; i < names.length; i++){
           for (int j=0; j < names[i].length; j++)
               System.out.print (names[i][j] + " : ");
           System.out.println();
       }
//       lis = new DownLoadListener (this);
//       this.addPropertyChangeListener (lis);
       if (cc instanceof CGJoController)
           System.out.println ("  Yes it is an instance of a CGJoController");
       this.addPropertyChangeListener (cc);  //SwingWorker
    }


    public JoFile (File jofile, File dataFolder){
        this.jofile = jofile;
        this.dataFolder = dataFolder;
        init (jofile);
    }
    /** select jo file from file chooser **/
    public JoFile (CompensationController controller) {
        
        this.cc = controller;
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter ("FlowJo", "jo");
        chooser.setFileFilter (filter);
        this.addPropertyChangeListener (controller);
//        lis = new DownLoadListener(this);
//         this.addPropertyChangeListener (lis);
        int returnVal = chooser.showOpenDialog(null);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
//           System.out.println("You chose to open this file: " +
//                   chooser.getSelectedFile().getName());
           jofile = chooser.getSelectedFile();
           dataFolder = jofile.getParentFile();
//           System.out.println (dataFolder.getAbsoluteFile() + "  "+ jofile.getParentFile());
           init (jofile);
           String[][] names = teaseOutDetectors();
//           for (int i=0; i < names.length; i++){
//               for (int j=0; j < names[i].length; j++)
//                   System.out.print (names[i][j] + " : ");
//               System.out.println();
//           }
//          
           DownloadDialogSimple dialog = new DownloadDialogSimple (this, tubeMap);
          // dd = new DownloadDialog ((MappingInterface) this, names, tubeMap);
        }

    }
    public JFrame getFrame () {
        return cc.getFrame();
    }
    public HashMap<Integer, ArrayList<String>> getParameterNames () {
        return parameterNames;
    }
    
    public void setSaveFiles (boolean saved){
        saveFiles = saved;
    }
    
    private String[] downloadDiva() throws UnsupportedEncodingException {
       String divaname = projectName + ".xml";
       divaXMLFilename = divaname;
       URI uristring = URI.create (divaXMLFilename);
       System.out.println ("URI create ->");
       System.out.println (uristring.toASCIIString() + "  "+ uristring.toString());

       divaname = URLEncoder.encode (divaname, "UTF-8");
       
       System.out.println ("URLEncoder "  + divaname);
       String [] s = downloadURLS.get(0); 
       
       String xmlurl = s[0] +divaname;
       System.out.println ("xml url " + xmlurl);

       String[] divainfo = new String[2];
       divainfo[0] = s[0];
       divainfo[1] = uristring.toString();
       
       System.out.println (divainfo[0] + "--"+ divainfo[1]);
           //downloadURLS.add (divainfo);
           
           return divainfo;
    }
    
    public void returnSelections (HashMap<String, TubeInfo> tubeList, File saveFilesDirectory ){
       
        this.savedFilesDirectory = saveFilesDirectory;  // could be null;
//        System.out.println ("----------  Save selections? ------------- ");
        ArrayList<String> names = new ArrayList<String>();
        Collection <TubeInfo> tubes = tubeList.values();
               Iterator it = tubes.iterator();  
               while (it.hasNext()){
                   TubeInfo one = (TubeInfo) it.next();
                   if (one.isSelected ){
                       names.add (one.getTubeName());
                       String[] s = new String[2];
                       s[0] = one.getURL();
                       s[1] = one.getTubeName();
                       System.out.println (" download urls " + s[0] + s[1] );
                       downloadURLS.add (s);
                   }
               }
               String divafilename="";
               if (projectName == null){
                    String s = jofile.getName();
                    projectName = s.substring(0, s.length()-3);
//                    System.out.println (projectName);
               }
               try {
                   String[] ds = downloadURLS.get(0);
                   int idx  = ds[0].lastIndexOf("/");
                   String urlbasic = ds[0].substring(0, idx+1)+"file%3D" ;
                   
           
                   divafilename = projectName + ".xml";
                   String urldiva = URLEncoder.encode(divafilename, "UTF-8");
                   System.out.println ("this is the url diva " + urldiva + " divafilename "+ divafilename);
//                   try {
                   URI uridiva = URI.create (urldiva);
              System.out.println ("URI to string " + uridiva.toString() +  " ASCII "+ uridiva.toASCIIString() );
              System.out.println ("URL "+ urldiva);
              
              //http://facsdata.stanford.edu:8080/ERS/data/o%3DStanford%20University/ou%3DFACS%20Facility/uid%3Deliverg/journal%3DLSR%20II.2/session%3D4576/file%3DE9%20YS%20%2B-.xml   --this one works

//                   System.out.println ("to string " + uridiva.toString() +  " ASCII "+ uridiva.toASCIIString() + " URL  "+ uridiva.toURL());
//                   } catch (MalformedURLException e){
//                       System.out.println ("malformed url exception ");
//                   }  catch (IllegalArgumentException ee){
//                       System.out.println ("IllegalArgumentException");
//                      
//                   } 
                   
                   String[] newurl = new String[2];
                   newurl[0] = urlbasic+uridiva.toString();
                   newurl[1] = divafilename;
//                   System.out.println (newurl[0] + "  "+ newurl[1]);
                   downloadURLS.add (newurl);
               } catch (UnsupportedEncodingException ee){
                   System.out.println ("  Unable to get the Diva File "+ divafilename);
               }
          String[] controls = new String[names.size()];
          controls = names.toArray(controls);
          

       startDownloads(controls);
       
    }
    
    public File getTempDirectory() {
            
        return savedFilesDirectory;
    }
    
    public HashMap<String, TubeInfo> getTubeMap() {
        return tubeMap;
    }
    
    public String[][]getControlData () {
        return controlData;
    }
    //override  //the property change function can be used instead.  it is called when the thing finishes
    public void putMappingData (String[][] data, File tempFolder) {
        System.out.println ("put MappingData in JoFile");
        this.tempFolder = tempFolder;
        this.controlData = data;
        String unstained=null;
//        System.out.println ("put Mapping Data " + tempFolder.getName() + " "+ tempFolder.getPath());
        for (int i=0; i < data.length; i++){

            if (data[i][2] != null && !data[i][2].equals("")){
//                System.out.println ("  An Unstained control "+  data[i][2]);
                if (unstained == null)
                    unstained = data[i][2];
                else if (!unstained.equals(data[i][2]))
                    unstained = data[i][2];
                
            }
//            System.out.println ();
            if (data[i][4] != null && !data[i][4].equals("")){
//                 System.out.println (tubeMap.get (data[i][4]).getURL());
                 if (unstained != null){
                     String s = tubeMap.get (unstained).getURL();
//                     TubeInfo tone = tubeMap.get (unstained);
//                     if (tone != null)
//                         System.out.println (tone.toString());
                     if (s != null ){
//                         System.out.println ("  url for the unstained "+ s);
                         String[] us = new String[2];
                         us[0] = s;
                         us[1] = unstained;
                         downloadURLS.add (us);
                         unstained = null;
                         
                     }
                 }
                     
                 String[] one = new String[2];
                 one[0] = tubeMap.get(data[i][4]).getURL();
                 one[1] = data[i][4];
                 downloadURLS.add (one);
                 
            }
            
        }
//        System.out.println (jofile.getName());
        if (projectName == null){
            String s = jofile.getName();
            projectName = s.substring(0, s.length()-3);
//            System.out.println (projectName);
        }
        try {
            String[] divaurl = downloadDiva();
            downloadURLS.add (divaurl);   
        } catch (UnsupportedEncodingException ee ){
            System.out.println ("  Unable to encode this Diva filename." + projectName);
        }


//        System.out.println ("---------------------------");
        if (downloadURLS.size() > 0){
                for (String[] one: downloadURLS){
                    System.out.println ("download urls " + one[0] + "  " + one[1]);
                }
            }
//                //startDownloads();
//        System.out.println ("------------------");
        startDownloads();  //called in CompensationControls
        
    }
    
    public String[][] teaseOutDetectors () {
    
        Set <Integer> keys = parameterNames.keySet();
        Integer[] keyArray = new Integer[keys.size()];
        if (keys != null){
        keyArray = keys.toArray(keyArray);
        }
        int min=100;
        int max = 0;
        
        for (Integer ii: keys){
//            System.out.println (ii);
            if ( ii.intValue() < min) min = ii.intValue();
            if (ii.intValue() > max) max = ii.intValue();
        }
        int len = keys.size();
        for (Integer ii: keys){
            int i = ii.intValue() - min;
            if (ii > 0 ){
                keyArray[i] = ii;
//                System.out.println (i +  "  "  + ii.intValue());
            }
        }
       
        
        Iterator it = keys.iterator();
        ArrayList<String[]>detectors = new ArrayList<String[]>();
      
        //while (it.hasNext()){
            
        for (Integer i2:  keyArray){
         //   ArrayList<String> names = parameterNames.get((Integer)it.next());
            ArrayList<String> names = parameterNames.get(i2);
            String[]one = new String[2];
            
            one[0] = names.get(0);
            int ii = 1;
            while (ii < names.size() ){
                one[1] = names.get(ii);
                ii++;
                if (!one[1].endsWith("-A")){
                    detectors.add (one);
                    System.out.println ("  JoFile list of detectors  "+ one[0] + "  "+ one[1]);
                }
                one = new String[2];
                one[0] = names.get(0);
            }
            
        }
        String[][] asStrings = new String[detectors.size()][2];
        asStrings = detectors.toArray (asStrings);
        for (int i=0; i < asStrings.length; i++){
            System.out.println ("JoFile list of detectors as strings " + asStrings[i][0] + "  "+ asStrings[i][1]);
        }
        return asStrings;
    }

    private void init (File file){
        String[] msg = new String[2];
        if (file.getName().endsWith (".jo")){
            if (file.exists() && file.canRead()){
             //  boolean flag = parseJoFileForUrls (file);
                boolean flag = parseJoFile (file);
               if (flag == false){
                   return;

               }
            }
            else {
                msg[0] = "ERROR";
                msg[1] = "The file, "+ file.getName() + ", does not exist or cannot be read.";
                fileURLMapping.add (msg);
            }
        }
        else {

           msg[0] = "ERROR";
           msg[1] = "The file, " + file.getName() + ", is not a FlowJo file.";
           fileURLMapping.add (msg);
        }

//        startDownloads ();
    }
    
    

    public ArrayList getFileURLMapping () {
        return fileURLMapping;
    }

    public ArrayList<String[]> getList() {
        return fileURLMapping;
    }
    
    public HashMap<String,Integer> getStainSets () {
        return stainSets;
    }

    private String[] getListofControls() {
        String[] controls = new String[fileURLMapping.size()];

        for (int i=0; i < downloadURLS.size(); i++){
            String[] one = (String[]) downloadURLS.get(i);
            if (one.length == 2 && one[1] !=null && !one[1].equals(""))
                controls[i]= one[1];
        }
        return controls;

    }
    
    private boolean parseJoFileForUrls (File jofile){
        boolean flag = true;
        try {
            BufferedReader reader = new BufferedReader(new FileReader (jofile));
            String line = reader.readLine();
            String url =null;
            TubeInfo newtube = null;
            String tubeName="";
            String cell="";
            int tubeId=0;
            
            while (line != null){
                
                if (line.startsWith ("http")){
                    if (url != null){
                        newtube = new TubeInfo (url, tubeName, cell);
                        newtube.setTubeId (tubeId);
//                        if (tubeMap.containsKey (tubeName))
//                            System.out.println ("  A Duplicate tube name?  "+ tubeName);
                        tubeMap.put (tubeName, newtube);
                        String[] urlInfo = new String[2];
                        urlInfo[0] = url;
                        urlInfo[1] = tubeName;
                        fileURLMapping.add (urlInfo);
                
                    }
                    
                    url = line;             
                }
                else {
                    String[] parts = line.split("\t");
                    for (int i=0; i < parts.length; i++){
                   
                    if (parts[i].contains("$CELLS") && i+1 < parts.length){                          
                           // fileURLMapping.add (urlInfo);
                            cell = parts[i+1];

                        }
//                        else if (projectName == null){
                        else if (parts[i].contains("$PROJ") && i+1 < parts.length){
                                if (projectName == null){
                                projectName = parts[i+1];
//                                System.out.println ("  Got the projectName "+ projectName);
                            }
                        }
                        //look for the cytogenie keywords also
                        else if (parts[i].contains("TUBE NAME") && i+1 < parts.length){
                            tubeName = parts[i+1];
                        }
                        else if (parts[i].contains ("TUBE-IDENTIFIER") && i+1 < parts.length){
                            tubeId = Integer.parseInt (parts[i+1]);
                        }
                    }
                }
                line = reader.readLine();
            }
            
        } catch (IOException ioe){
            System.out.println ("  Could not open the JO file. ");
            flag = false;
        }
        return flag;
    }

    private boolean parseJoFile (File jofile){
        boolean flag = true;
        try {
            BufferedReader reader = new BufferedReader(new FileReader (jofile));
            String line = reader.readLine();
//            System.out.println ("Size of the jo file "+ jofile.length());

            String[] urlInfo =null;
//            while (line != null && !line.startsWith ("http")){
//                if (line == null){
//                    String msg = " The jo file "+ jofile.getName() + " is not correct for the purpose of downloading data. Can't get data.";
//                  JOptionPane.showMessageDialog (null, msg, " Computation Status ", JOptionPane.ERROR_MESSAGE);
//                  return false;
//
//                }
//                line = reader.readLine();
//            }
            String tubeName=null,tubeType = null;
            String url = null;
            String cell = null;
            TubeInfo newone=null;
            int tubeId=0;
      
            ArrayList<String[]> analytes = new ArrayList<String[]>();
            
            ArrayList<String[]> labels = new ArrayList<String[]>();
            
            ArrayList<String[]>compensations = new ArrayList<String[]>();
            ArrayList<String[]>PnS = new ArrayList<String[]>();
            
            boolean applyCompensation = true;
            
            while (line != null){
                
                if (line.startsWith ("http")){
                    //archive the previous line if this is not first one
                    if (url != null){
                        newone = new TubeInfo (url, tubeName, cell);
                        newone.setTubeType (tubeType);
                        newone.setTubeId (tubeId);
                        newone.setAnalyteLabelledFor(analytes);
                        newone.setLabelledFor (labels);
                        newone.setCompensations (compensations);
                        if (newone.getTubeType().equalsIgnoreCase("analysis")){
                            //see if this set of compensations, that is the stain set is unique.  If unique,
                            //add to stain set somehow to create the unique matrices at the end.
                            
                            addToStainSets (compensations);
                        }
                        else if (newone.getTubeType().equalsIgnoreCase ("compensation")){
//                            System.out.println ("  Compensation tube");
                            String[]s = new String[2];
                            newone.addAntibodyToDetector (PnS);
                             s[0] = newone.getURL();
                             s[1] = newone.getTubeName();
                             System.out.println (" download urls " + s[0] + s[1] );
                             downloadURLS.add (s);
                        }
//                        if (tubeMap.containsKey (tubeName))
//                            System.out.println ("  A Duplicate tube name?  "+ tubeName);
                        tubeMap.put (tubeName, newone);
                        

                        for (int i=0; i < compensations.size(); i++){
                            String[]one = compensations.get(i);
//                            System.out.println ("  compensations?  "+ one[0] + ", "+ one[1]);
                            if (tubeMap.containsKey (one[0])){
                                TubeInfo onetube = tubeMap.get(one[0]);
                                onetube.setCompensationTubeId ( one[1]);
                                
//                                System.out.println ("  Compensation for :  " + onetube.getInfo());
                                tubeMap.put (onetube.getTubeName(), onetube);
                            }
                        }

                        analytes = new ArrayList<String[]>();
                        labels = new ArrayList<String[]>();
                        compensations = new ArrayList<String[]>();
                        PnS = new ArrayList<String[]>();
                        
                    }
                    if ( line.contains ("file%3D1-")){
                    
                        line = reader.readLine();
                        while (line != null && !line.startsWith ("http"))
                            line = reader.readLine();
                        continue;
                    }
                
//                    if (url != null){ 
//                            newone = new TubeInfo (url, tubeName, cell);
////                            System.out.println ("TubeInfo " + newone.toString());
//                            tubeMap.put(tubeName, newone);
//                        
//                    }
                    url = line;
                    System.out.println (line);
                  
                    
//                    System.out.println();
                }
                else {  //this is all one line.  So at the end of this, should have all the info.
                    String[] parts = line.split("\t");
                    for (int i=0; i < parts.length; i++){
//                       System.out.println (i + "  "+ parts[i]);
                        if (parts[i].startsWith ("$P")){
                            pickoutParameter (parts[i], parts[i+1]); 
                            if (parts[i].endsWith("S")){
                                String[] pns=new String[2];
                                pns[0]=parts[i];
                                pns[1]=parts[i+1];
                                PnS.add (pns);
                            }
                                 
                        }   
                        else if (parts[i].contains("$CELLS") && i+1 < parts.length){                          
                            fileURLMapping.add (urlInfo);
                            cell = parts[i+1];

                        }
//                        else if (projectName == null){
                        else if (parts[i].contains("$PROJ") && i+1 < parts.length){
                                if (projectName == null){
                                projectName = parts[i+1];
//                                System.out.println ("  Got the projectName "+ projectName);
                            }
                        }
                        //look for the cytogenie keywords also
                        else if (parts[i].contains("TUBE NAME") && i+1 < parts.length){
                            tubeName = parts[i+1];
                        }
                        else if (parts[i].contains ("TUBE-IDENTIFIER") && i+1 < parts.length){
                            tubeId = Integer.parseInt (parts[i+1]);
                        }
                        else if (parts[i].contains ("TUBE-TYPE") && i+1 < parts.length){
                            tubeType = parts[i+1];
                        }
                        else if (parts[i].contains ("APPLY COMPENSATION") && i+1 < parts.length){
                            applyCompensation = Boolean.parseBoolean (parts[i+1]);
                        }
                        else if (parts[i].contains("ANALYTE-LABELED") && i+1 < parts.length){
                            String[] ana = parts[i].split ("ANALYTE-LABELED-BY-");
                            if (ana.length == 2){
                                String[] analyte_labeled = new String[2];
                               analyte_labeled[0] = ana[1];
                               analyte_labeled[1] = parts[i+1];
                               analytes.add (analyte_labeled);
                            }
                        } 
                        else if (parts[i].contains ("COMPENSATION-FOR") && i+1 < parts.length){
                            String[] comps = parts[i].split ("COMPENSATION-FOR-");
                            if (comps.length == 2){
                                String[] compensationFor = new String[2];
                               compensationFor[0] = comps[1];
                               compensationFor[1] = parts[i+1];
                               compensations.add (compensationFor);
                            }
                        }
                        else if (parts[i].contains ("LABEL-FOR") && i+1 < parts.length){
                            String[] labelsfor = parts[i].split("LABEL-FOR-");
                            if (labelsfor.length == 2){
                                String[] labeledby = new String[2];
                                labeledby[0] = labelsfor[1];
                                labeledby[1] = parts[i+1];
                                labels.add (labeledby);
                            
                            }
                        }
                        else if (parts[i].contains ("LOT-ID") && i+1 < parts.length){
                            String[] lotid = parts[i].split("LOT-ID");
                            if (lotid.length == 2){
                                String[] lotId = new String[2];
                                lotId[0] = lotid[1];
                                lotId[1] = parts[i+1];
                            }
                        }
                        
                    }
                    
                        

                }
                
                line = reader.readLine();
            }
        } catch (IOException ioe){
            System.out.println (ioe.getMessage());
            flag = false;
        }
 
        //----------------------------------------------------------------------
        boolean doit = true;
        if (doit || Compensation2.CATE){
            Set<Integer> keys = parameterNames.keySet();
            System.out.println (" parameterNames");
            for (Integer ii : keys){
                ArrayList<String> ns = parameterNames.get(ii);
                System.out.print (ii + "  ");
                for (String s: ns)
                  System.out.print ( s+ "  ");
                System.out.println();
            }
            Set<String>keyss = tubeMap.keySet();
            System.out.println ("------  Tubes ---------- " + keyss.size());
            for (String ss: keyss){
                TubeInfo tube = tubeMap.get(ss);
                System.out.println (tube.getInfo());
            }
            System.out.println ("End of Tubes");
        }
        //----------------------------------------------------------------------
        return flag;

    }
    
    
    
    private void addToStainSets (ArrayList <String[]> compensations){
        //if this set of compensations is unique, add it to the set of stain sets.
        boolean matches = true;
        String parameter;
        Integer tubeId;
        if (stainSets.isEmpty()){
            addNewStainSet (compensations); //create the first one
             
        }
        else {  //compare for uniqueness
            
            for (String[] ss: compensations){
                    if (ss.length == 2){
                        parameter = ss[0];
                        tubeId = Integer.parseInt (ss[1]);
                        if (stainSets.containsKey(parameter)){
                            Integer oneid = stainSets.get (parameter);
                            if (oneid.intValue() != tubeId.intValue()){
                                matches = false;
                                break;
                            }

                        }
                    }
                    else{
                        System.out.println(" Error in Add to stain sets (1)");
                    }

                
            }
            if (!matches ){
                addNewStainSet (compensations);
                
            }
        }
    }
    
    private void addNewStainSet (ArrayList <String[]> newone){
        String parameter;
        Integer tubeid;
        for (String[] ss: newone){
                if (ss.length == 2){
                    parameter = ss[0];
                    tubeid = Integer.parseInt (ss[1]);
                    stainSets.put (parameter, tubeid);
                }
                else {
                    System.out.println(" Error in Add to stain sets (1)");
                }
            }    
    }
    
    private void pickoutParameter (String key, String value){
        ArrayList<String> NS;
        value = value.toUpperCase();
        if  ( key.endsWith("N") || key.endsWith("S")){
            if (value.endsWith ("-H") || value.startsWith("FSC") || value.startsWith("SSC") || value.startsWith("TIME")){
                return;

              }
        
            else {
//                System.out.println ("pickout Parameter "+ key + ", "+ value);
                String pn = key.substring(2, key.length()-1);
                Integer ikey = Integer.parseInt (pn);

                if (parameterNames.containsKey (ikey)){
                    NS = (ArrayList<String>) parameterNames.get(ikey);
                    if (NS == null )
                        NS = new ArrayList<String>();

                    if (key.endsWith("N") && !NS.contains (value)){
                        NS.add (0, value);
                    }
                    else if (key.endsWith ("S")){
                        if (!NS.contains (value))
                            NS.add (value);

                    }
//                    System.out.println (ikey );
                    parameterNames.put (ikey, NS);

                }
                else {  //doesn't contain this key.  It is a new one
                    NS = new ArrayList<String>();
                    NS.add (value);
                    parameterNames.put (ikey,NS);
                }
            }
        }
          
    }
    public void startDownloads() {
        //this is the old way.
    }

    public void startDownloads ( String[]tubenames){
        
     
        if (cc.isVisual()){
            dialog = new JDialog();

            JPanel panel = new JPanel();
            panel.setLayout (new BorderLayout());
            panel.setBackground (Color.white.darker().darker());
            dialog.setContentPane (panel);
            JLabel label = new JLabel (" One moment please while we get the FCS data ready. ");
            panel.add (label, BorderLayout.NORTH);

            JProgressBar progressBar = new JProgressBar();
            ProgressListener progressListener = new ProgressListener (progressBar);
            progressBar.setValue (0);
            addPropertyChangeListener (progressListener);

            JList mylist = new JList (tubenames);
            panel.add (mylist, BorderLayout.CENTER);

            JPanel panel2 = new JPanel();
            panel2.add (progressBar);
            JButton cancelButton = new JButton ("Cancel Download");
            cancelButton.addActionListener (new ActionListener() {
                public void actionPerformed (ActionEvent e){
                    dialog.setVisible (false);
                }
            });
            panel2.add (cancelButton);


            panel.add (panel2, BorderLayout.SOUTH);

            dialog.setVisible (true);
            dialog.setSize (400, 400);
            dialog.setLocation (500, 500);
        }

        execute();
    }



    

    //override
    protected Exception doInBackground () throws Exception {
        //some kind of message that says -- getting the data together or something.
       System.out.println (" ---doInBackground--- " );//DataStoreAuthenticator
        DataStoreAuthenticator dataStore = new DataStoreAuthenticator();
        DataStoreAuthenticator.setDefault (dataStore);
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesCopied=0;
        InputStream is = null;
        FileOutputStream os = null;
//        if (jofile.exists()  ){
//            dataFolder = jofile.getParentFile();
//        }
        if (savedFilesDirectory != null){
//            System.out.println (savedFilesDirectory.getName() + "  "+ savedFilesDirectory.getPath());
            tempFolder = savedFilesDirectory;
        }
        if (tempFolder == null){
            tempFolder = new File (System.getProperty("user.home")+ File.separator + "tempfcs");
//            tempFolder.deleteOnExit();
            boolean b = tempFolder.mkdir();
            savedFilesDirectory = tempFolder; //this is funky here.  Don't need both.
//            System.out.println (" temp folder has been created "+ tempFolder.getName());
        }
//        System.out.println (tempFolder.getName() + "  "+ tempFolder.getPath());
        boolean flag = tempFolder.mkdir();
//        tempFolder.deleteOnExit();
        System.out.println (flag + " tempfolder mkdir returned this flag ");

        System.out.println (" size of downloadURLS?  "+ downloadURLS.size());

       //look for a
        //add the diva xml file to the list
 
        
       int i=1;
       int step;
       if (downloadURLS.isEmpty() )
           step = 10;
       else
          step = 100 / downloadURLS.size()+1;
       try {
           System.out.println (step);
           for (String[] s : downloadURLS){
               URL url = new URL (s[0]);
               HttpURLConnection http = (HttpURLConnection)url.openConnection();
               int response = http.getResponseCode();
      System.out.println ("------------------  "+ response + "  what is the response?  "+ s[0] + "  "+ s[1]);
               if (response == HttpURLConnection.HTTP_NOT_FOUND){
                   System.out.println ("   Connection not made for this url  "+  s[0] + "  "+ s[1]);
               }
               else if (response == HttpURLConnection.HTTP_OK) {
                   is = http.getInputStream();
                   File currentFile = new File (tempFolder, parseOutFilename (s[0]));
                   if (!saveFiles ){
                       currentFile.deleteOnExit();
                   }
                   System.out.println ("DataFolder plus current file " + tempFolder.getPath() + "  "+ currentFile.getName());
                   System.out.println ("Where is the current file?  " + currentFile.getAbsolutePath() + "   "+ currentFile.getCanonicalPath());
                   os = new FileOutputStream (currentFile);
                   int n=0;
                   while (n > -1 ){
                       n = is.read (buffer);
                       if (n < 0)
                           break;
                       os.write (buffer, 0, n);
                       bytesCopied += n;
                   }
               }
               else {
                   String message = http.getResponseMessage();
                   System.out.println (message );
               }
             //  Thread.sleep (2000);
               i+=step;
    //           if (i >100)i = 100;
    //           setProgress (i);
           }

           setProgress (100);

       } catch (Exception e){
           System.out.println (e.getMessage());
           throw e;
       }
       finally {
           if (is != null) {
               try {
                   is.close();

               } catch (IOException eio){
                  throw eio;
               }
           }
           if (os != null){
               try {
               os.close();
               } catch (IOException eos){
                  throw eos;
               }
           }
       }
       return null;

    }
    
    private void readJoFile () {
        //http url, TUBE NAME, get the $PnN keywords
        //open the Jo file.
        //read searching for the http and then the TUBE NAME.
    }

    private String parseOutFilename (String urlString){
        String filename= null;

        if (urlString.endsWith (".fcs") || urlString.endsWith (".xml")){
            int index = urlString.indexOf ("file");
            if (index < 0)
                return null;
            String substring = urlString.substring (index+7);
//            System.out.println (substring);
            if (substring.contains("%20")){
                char[] asarray = substring.toCharArray();
                StringBuilder buf = new StringBuilder();
                int i=0;
                while(  i < asarray.length){
                    if (asarray[i] != '%'){
                        buf.append(asarray[i++]);
                    }
                    else {
                        buf.append(" ");
                        i+=3;
                    }
                }
                filename = buf.toString();
            }
            else {
                filename = substring;
            }
//            System.out.println ("This is the filename for downloaded file " + filename);
        }
        

        return filename;
    }
    
    
    
    protected void done() {
//        System.out.println (" done ");
        
        try {
            Exception e = get();
            
            if (e != null){
                System.out.println ("(1) " + e.getMessage());
                
            }
             
        }catch (Exception ee){
            System.out.println ("(2)" + ee.getMessage());
            return;
        }
        if (cc.isVisual()){
           JOptionPane.showMessageDialog (null,
                              "FCS Download completes.", "FCS file download c", JOptionPane.INFORMATION_MESSAGE);
           dialog.setVisible (false);
        }
       if (savedFilesDirectory != null )
           System.out.println (savedFilesDirectory.getName() + "  "+ savedFilesDirectory.getParent());
       System.out.println ("...........  Jofile is done. .....");
//       if (cc instanceof CGJoController)
//           ((CGJoController)cc).returnFromDownload(tubeMap, savedFilesDirectory);
//       else
//           cc.returnFromDownload (tubeMap, savedFilesDirectory);
        
    //   cc.putMappingData(controlData, tempFolder);
      // return;
    }
    /**
     * ProgressListener listens to "progress" property
     * changes in the SwingWorkers that search and load
     * images.
     */
    class ProgressListener implements PropertyChangeListener {
        // prevent creation without providing a progress bar
        private ProgressListener() {}

        ProgressListener(JProgressBar progressBar) {
            this.progressBar = progressBar;
            this.progressBar.setValue(0);
        }

        public void propertyChange(PropertyChangeEvent evt) {
            String strPropertyName = evt.getPropertyName();
            if ("progress".equals(strPropertyName)) {
                progressBar.setIndeterminate(false);
                int progress = (Integer)evt.getNewValue();
//                System.out.println ("Progress = " + progress);
                progressBar.setValue(progress);
            }
        }

        private JProgressBar progressBar;
    }

    public static void main (String[] args){
        String fn = "/Users/cate/AFCSData/AutoComp2/Autocomp Development-Cate-cytogenie test/Cate-Cytogenie test.jo";
        File file = new File (fn);
//        JoFile jo = new JoFile ((CompensationController) null);
        JoFile jo = new JoFile (fn, (CompensationController)null);
     //   jo.parseJoFile (file);
//        ArrayList <String[]>parsedInfo = jo.getFileURLMapping();
        System.out.println ("------------------------------------------------------------------------");
        
            
        
    }

}

/*
 * New path:  Open the Jo File.  search for all the TUBE NAME keywords.  Match it up with all the URL's.
 * Within each tube, these are tubes are the $PnN keys words for the parameters.  I really only need to get
 * these once.  Then put up the table -- the FCSFileDialog with the list of Parameters $PnN on the left
 * side.  On the right side in the drag list, list those tube names.  Also need to keep track of the 
 * name of the FCS file-- that's in the UrL.  Then they drag those they want and I then download just 
 * those FCS files.  Then I proably don't even need the DiVa xml file.  I think.  
 * 
 */