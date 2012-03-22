/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.stanford.facs.controllers;

import edu.stanford.facs.exp_annotation.JoFile;
import edu.stanford.facs.exp_annotation.TubeInfo;
import com.apple.eio.FileManager;
import edu.stanford.facs.compensation.Compensation2;
import edu.stanford.facs.exp_annotation.DivaXmlParser;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.isac.fcs.FCSException;
import org.isac.fcs.FCSFile;
import org.isac.fcs.FCSTextSegment;
import edu.stanford.facs.gui.FCSFileDialog.MappingInterface;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Set;
import javax.swing.JFrame;
import org.isac.fcs.FCSParameter;
import edu.stanford.facs.compensation.StainedControl;
import edu.stanford.facs.compensation.UnstainedControl;
//import edu.stanford.facs.diva_xml.FlowJoFiles;
import edu.stanford.facs.gui.CompensationFrame;
import edu.stanford.facs.gui.CompensationResults;
import edu.stanford.facs.gui.FCSFileDialog;
import edu.stanford.facs.gui.MultipleMatrixDialog;
import edu.stanford.facs.compensation.Diagnostic;
import java.awt.BorderLayout;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.SwingWorker;
import javax.swing.border.Border;

/**
 * $Id: CompensationController.java,v 1.2 2012/01/31 01:19:02 beauheim Exp $
 * @author cate
// */
public class CompensationController //extends JFrame 
                       implements  MappingInterface, PropertyChangeListener {
      final Preferences preferences = Preferences.userNodeForPackage(this.getClass());
        // preferences.put("comp_data_dir", dataFolder.getPath());
//      private File datafile;
      private JFileChooser fileChooser = new JFileChooser();
      protected File dataFolder;
      protected File tempJoFolder;
      private File divaFile;
      private CompensationFrame frame; //constructor is title, dataFolder, this.  But it has to have the compensaiton2.
      private CompensationResults results;
      protected FileNameExtensionFilter xmlFilter = new FileNameExtensionFilter ("XML", "xml");
   //   private FileNameExtensionFilter joFilter = new FileNameExtensionFilter ("FlowJo","jo", "wsp");

      protected String experimentName;
      protected Compensation2 compensation2; 
      protected FCSFile[] unstainedFCS;
      protected FCSFile[] stainedFCS;
      protected StainedControl[] stainedControls;
      protected UnstainedControl[] unstainedControls;
      protected String[] detectorList;  //from the Diva File.  This is in the order of the PnN keywords.
      protected String[] controlList; //this is all the values, not just the used ones.
      protected String[] PnSreagents;

      private String[][] controlMappings;
      private JButton runButton, cancelButton;
      protected Float[][]spectrumData;
      private MultipleMatrixDialog mmdialog;
      private JoFile jofile;
      protected HashMap <String, TubeInfo> tubeMap;
      private int answer;
      protected boolean visual= true;
      //these are tube ids.
      protected ArrayList <Integer[]> uniqueStainSets = new ArrayList<Integer[]>();
      private JFrame myframe;
//      private HashMap<String, StainSet> stainSets;

     CompensationController(String fn){
        System.out.println (" CompensationController with one String argument ");   
    }
     CompensationController(){
         
     }

    CompensationController(boolean visual) {
        myframe = new JFrame("---------Compensation-----------");
      this.visual = visual;
      myframe.setResizable (true);
      JPanel panel = new JPanel();
      panel.setBackground (Color.white.darker());
      panel.setLayout(new BoxLayout (panel, BoxLayout.PAGE_AXIS));

      myframe.add (panel);
      JPanel panel2 = new JPanel();
//      panel2.setBorder (BorderFactory.createLineBorder (Color.GREEN));
      panel2.setLayout (new GridLayout (1,1));
      panel2.setBackground (Color.white);
      panel2.setBorder (BorderFactory.createLineBorder(Color.lightGray.darker(), 3));
      StringBuilder buf = new StringBuilder();
      buf.append ("How to Run the Compensation Analysis");
      JLabel label0 = new JLabel (buf.toString());

      panel2.add (label0);
//      JTextArea step1 = new JTextArea (1, 40);
      String sep = System.getProperty ("line.separator");
      JLabel stepOne = new JLabel ("Step 1:  Identify the FCS Files");

      JPanel panel3 = new JPanel();
      panel3.setLayout (new GridLayout (4,1));
      panel3.setBackground (Color.white);
      panel3.setBorder (BorderFactory.createLineBorder (Color.lightGray.darker(), 3));
      ButtonGroup group1 = new ButtonGroup();
      JRadioButton rb1_1 = new JRadioButton ("Use the JO File to download the control files. ");
      final JRadioButton[] buttongroup1 = new JRadioButton[3];
      panel3.add (stepOne);
      buttongroup1[0]=rb1_1;
      rb1_1.addActionListener (new ActionListener() {
          public void actionPerformed (ActionEvent e){
              jofile = new JoFile(CompensationController.this);
              jofile.addPropertyChangeListener (CompensationController.this);
//              dataFolder = jofile.getTempDirectory();
//              System.out.println (dataFolder.getName() );
               turnOnOffButtons (buttongroup1);
          }
      });
      JRadioButton rb1_2 = new JRadioButton ("The FCS files are on my computer now.");
      buttongroup1[1]=rb1_2;
      rb1_2.addActionListener (new ActionListener () {
          public void actionPerformed (ActionEvent e){
              dataFolder = showOpenDialog ("Select FCS file directory", null, JFileChooser.DIRECTORIES_ONLY);
              if (dataFolder != null)
                  preferences.put("comp_data_folder", dataFolder.getParentFile().getPath());
              else if (dataFolder == null){
                  showMessageDialog ("The data folder is null. ") ;
                  return;
              }
              turnOnOffButtons (buttongroup1   );
          }
      });
      JRadioButton rb1_3 = new JRadioButton ("The FCS files are on the server & I know the URL.", false);
      buttongroup1[2] = rb1_3;
      rb1_3.addActionListener (new ActionListener() {
          public void actionPerformed (ActionEvent e){

             JOptionPane.showMessageDialog (CompensationController.this.myframe, " Option not yet implemented.  ",
                     "How would you like this option implemented? ", JOptionPane.INFORMATION_MESSAGE);
             turnOnOffButtons (buttongroup1);
          }
      });
      group1.add (rb1_1);
      group1.add (rb1_2);
      group1.add (rb1_3);


      panel3.add (rb1_1);
      panel3.add (rb1_2);
      panel3.add (rb1_3);


      JPanel panel4 = new JPanel();
      panel4.setLayout(new GridLayout (3,1));
      panel4.setBackground (Color.white);
      panel4.setBorder (BorderFactory.createLineBorder (Color.lightGray.darker(), 3));
      JLabel stepTwo = new JLabel (" Step 2: Identify the Control Files");
      panel4.add (stepTwo);
      ButtonGroup buttonGroup = new ButtonGroup();
      JRadioButton rb1 = new JRadioButton (" Read the DiVA output file to find the controls", false);
      rb1.addActionListener (new ActionListener(){
        
          public void actionPerformed (ActionEvent e){
              runButton.setEnabled (false);
              divaFile = showOpenDialog("Select the DiVa XML file. ", xmlFilter, JFileChooser.FILES_AND_DIRECTORIES) ;
              if (divaFile != null){
                  preferences.put("comp_data_folder", divaFile.getParentFile().getPath());
                  readDivaFile (divaFile);
              }
              //setVisible (false);
              else if (divaFile == null)
                  showMessageDialog (" Could not find the divaFile");
              runButton.setEnabled (true);
          }
      });
      JRadioButton rb2 = new JRadioButton (" I will pick the controls", false);
      rb2.addActionListener (new ActionListener() {
          public void actionPerformed (ActionEvent e){
              String msg = "Not able to read the fcs file in order to find the detector names. ";

              String[] reagentNames = null;
              //I'd like to get the detectorList
              if (dataFolder != null && dataFolder.isDirectory()){
                 
                  File []filelist = dataFolder.listFiles (new FileFilter() {
                      public boolean accept (File f){
                          if (f.getName().endsWith ("fcs"))
                              return true;
                          return false;
                      }
                  });
//                 
                 
                 if (filelist != null && filelist.length > 1){
//                     FCSFile fcsfile = new FCSFile (filelist[filelist.length-1]);
                          scanForAcquisitionChannels (filelist[filelist.length-2].getName(), dataFolder);
                 }
                 
                  else {
                      msg = "No FCS files found in this directory.  Use the browser button to select a new working directory.";
                      JOptionPane.showMessageDialog (frame, msg, "FCS File Error", JOptionPane.INFORMATION_MESSAGE  );
                  }
              }
              if (Compensation2.CATE){
                  System.out.println (" CompensationController calling FCSFileDialog ");
                  if (detectorList != null && detectorList.length > 0){
                      for (String s: detectorList)
                          System.out.println ("detector List " + s);
                  }
                  if (PnSreagents != null && PnSreagents.length > 0){
                      for (String s: PnSreagents)
                          System.out.println (" reagent Names "+ s);
                  }
              }
              runButton.setEnabled (true);
              FCSFileDialog fcsdialog = new FCSFileDialog ((MappingInterface)CompensationController.this, dataFolder, 
                                                            detectorList, reagentNames);
             
              fcsdialog.setModal(true);
              fcsdialog.setVisible(true);
//              runButton.setEnabled (true);
              runAnalysis();
          }
      });
      
      buttonGroup.add (rb1);
      buttonGroup.add (rb2);   
      panel4.add (rb1);
      panel4.add (rb2);

      JPanel bottom = new JPanel();
      bottom.setLayout (new BoxLayout(bottom, BoxLayout.LINE_AXIS));

      cancelButton = new JButton ("Cancel");
      cancelButton.addActionListener (new ActionListener() {
          public void actionPerformed (ActionEvent e){
              showMessageDialog ("Computation was cancelled.  ");
              System.exit(1);

          }
      });
      runButton = new JButton ("Run");
      runButton.setEnabled(false);
      runButton.addActionListener (new ActionListener() {
          public void actionPerformed (ActionEvent e){
              //pass information back to the comprensation frame for further processing?
              //setVisible (false);
              runAnalysis();
            
              }
          });

      bottom.add (cancelButton);
      bottom.add (runButton);
      panel.add (panel2);
      panel.add (panel3);
      panel.add (panel4);
      panel.add (bottom);
      myframe.setSize (525, 450);
      myframe.setLocation (800, 200);
      myframe.setVisible(true);

    }
    
    
    

    protected void runAnalysis() {
      //  System.out.println ("  run analysis ");
        if (visual && frame == null)
            frame = new CompensationFrame (experimentName, dataFolder, this);
        if (compensation2 == null)
            compensation2 = new Compensation2((CompensationResults)frame, dataFolder);
        frame.initUI (compensation2);
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

        
        if (visual){
            if (PnSreagents.length < detectorList.length)            
                frame.initialize (detectorList, detectorList);
            else
                frame.initialize (PnSreagents, detectorList);
    //                frame.initialize (detectorList, detectorList);
        }

 
//       System.out.println (" does setup finish?  ");
       compensation2.execute();

    }

/**
 * Read the diva file and extract the stained and unstained controls.
 * @param divaFile
 */
    protected void readDivaFile(File divaFile) {
      String errmsg = new String();
    
      if (divaFile != null && divaFile.exists() && divaFile.canRead()){
//          String ipath = datafile.getPath();
          if (dataFolder == null)
             dataFolder = new File(divaFile.getParentFile().getPath());

        if (tubeMap != null && tubeMap.size() > 0){
            Collection <TubeInfo>tubes = tubeMap.values();
            Iterator <TubeInfo>it = tubes.iterator();
            if (Compensation2.CATE){
                System.out.println ("-----------Tube List ------------------");
                while (it.hasNext()){
                    TubeInfo newone = (TubeInfo) it.next();
                    System.out.println (newone.getInfo());
                }
                System.out.println ("-------End Tube List ------------------");
            }

        }
        else 
            tubeMap = new HashMap<String, TubeInfo>();
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
        this.myframe.setTitle (experimentName);

        // the allControls list includes the unstained control

        detectorList = parser.getDetectorList();
        String[] fcsfiles = parser.getFCSFilenames();
       

        // check out what we got. Did they use controls?
        // if all Controls are null, then they didn't use DiVa compensation
        // controls. So manually get them.
        // String[]allControls, String[]detector, String[]fcsfiles.
        if (allControls != null && allControls.length > 0){
          //show me what DiVa found.
          answer = showDiVaControls (allControls, fcsfiles, detectorList);
//          System.out.println ("  Return from Show DiVaControls "+ answer);
          if (answer == JOptionPane.YES_OPTION){
            controlList = setUpFcsFiles (allControls, fcsfiles, fl_labels);

          }
          else {
             dataFolder = new File (divaFile.getParentFile().getPath());
             FCSFileDialog fcsdialog = new FCSFileDialog (this, detectorList, divaFile.getParent());
              fcsdialog.setModal (true);
              fcsdialog.setVisible(true);
          }
          
        }
        else {
            //No controls were found in the xml file, but I did find a detectorList and fcsfiles
            dataFolder = new File (divaFile.getParentFile().getPath());
              FCSFileDialog fcsdialog = new FCSFileDialog (this, detectorList, divaFile.getParent());
              fcsdialog.setModal (true);
              fcsdialog.setVisible(true);
        }

      }

    else {
       showMessageDialog("We were unable to read and parse one of the datafiles. "
        + divaFile);

       System.exit(1);
      }


  }
    
   
    
    private int showDiVaControls (String[] controls, String[] fcsfiles, String[] detectors){
             
          
        final JDialog dialog = new JDialog (getFrame(), "Controls found in DiVa file");
        JPanel panel = new JPanel();
        panel.setLayout  (new BorderLayout());
        JPanel panel2 = new JPanel();
        panel2.setLayout (new GridLayout (controls.length,2));
        Border littleBorder = BorderFactory.createEmptyBorder (2,2,2,2);
        if (tubeMap != null){
            Collection <TubeInfo>col = tubeMap.values();
            Iterator <TubeInfo>it = col.iterator();
            while (it.hasNext()){
                TubeInfo one =  it.next();
                if (one.isSelected ){
                    JLabel label = new JLabel (one.getTubeName());
                    label.setBorder (littleBorder);
                    panel2.add (label);
                    
                } 
            }
        }
        else {
            
            for (int i=0; i < controls.length; i++){
                JLabel label = new JLabel (controls[i]);
                panel2.add (label);
                label.setBorder (littleBorder);
                String[] parts = fcsfiles[i].split("/");
                JLabel label2 = new JLabel (parts[parts.length-1]);
                label2.setBorder (littleBorder);
                panel2.add (label2);
            }
        }
//        panel2.setBackground (Color.green);
        panel2.setBorder (BorderFactory.createEmptyBorder (4,4,4,4));
        panel.add (panel2, BorderLayout.CENTER);
        JPanel panel3 = new JPanel();
        panel3.add (new JLabel ("  Do you want to use the controls that DiVa found? "));
        panel.add (panel3, BorderLayout.NORTH);
        JPanel panel4 = new JPanel();
        JButton yesb = new JButton ("Yes, use DiVa Controls.");
        yesb.addActionListener (new ActionListener() {
            public void actionPerformed (ActionEvent e){
                answer = JOptionPane.YES_OPTION;
                dialog.setVisible (false);
            }
        });
        JButton nob = new JButton ("No, I will pick my controls. ");
        nob.addActionListener (new ActionListener() {
            public void actionPerformed (ActionEvent e){
                 answer = JOptionPane.NO_OPTION; 
                 dialog.setVisible (false);
                 
            }
        });
        panel4.add (yesb);
        panel4.add (nob);
        panel.add (panel4, BorderLayout.SOUTH);
       
        dialog.add (panel);
        dialog.setSize (450, 400);
        dialog.setLocation (600, 600);
        dialog.setVisible (true);
        
        
        return answer;
                
        
        
    }
    public void returnFromDownload (HashMap<String, TubeInfo> tubeMap, File savedFilesDirectory){
    //    System.out.println ("CompensationController.returnFromDownload");
       this.tubeMap = tubeMap;
       tempJoFolder = savedFilesDirectory;
       dataFolder = savedFilesDirectory;
       findStainSets();
//        continueAnalysis();
    }
    
    
  
    
    private void findStainSets () {
       //     System.out.println ("Find Stain Sets ");
//         int n=1;            
//        Collection tubes = tubeMap.values();
//        Iterator it = tubes.iterator();
//        while (it.hasNext () ){
//            System.out.println (" tube number "+ n++);
//            TubeInfo onetube = (TubeInfo) it.next();
//            if (onetube.getTubeType().equals ("analysis")){
//                ArrayList<String[]> onecompset = onetube.getCompensations(); 
//                Integer[] onestainset = new Integer[onecompset.size()];
//                for (int i=0; i < onecompset.size(); i++){
//                    String[] one = onecompset.get(i);
//                    onestainset[i] = new Integer (one[1]);
//                }
//                Arrays.sort(onestainset);
//                System.out.println (" sort the stainsets ");
//                if (isUnique(onestainset)){
//                    System.out.println (" is Unique returns");
//                    uniqueStainSets.add (onestainset);
//
//                }
//            }
//        }
        System.out.println (" End find stain sets");
    }
    

 

    private boolean testDetectorForData (UnstainedControl unstained, String detector) {
        boolean flag = true;
        try {
            FCSParameter p = unstained.getFCSFile().getParameter (detector);
            if (p == null)
                flag = false;
        } catch ( FCSException fcs) {
            System.out.println ("testDetectorForData exception "+ detector);
        } catch (IOException io){
            System.out.println (" testDetector for data io exception ");
        }
        return flag;
    }

 
    /**
     * Called when we accept the controls in the Diva file.
     * Gotta be careful if there are tandems in the file.
     * @param controls
     * @param filenames
     * @param fl_labels  if there are multiples, they should be here, but there
     *                  is not guarantee that the presence of fl_labels mean
     *                  that multiples actually exist.
     * @return list of detectors used as controls
     */

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
      showMessageDialog("Problem with the number of unstained and stained controls.  Things don't add up");
      return null;
    }
    unstainedFCS = new FCSFile[unstained];
    stainedFCS = new FCSFile[stained];

    int u = 0, s = 0;
    for (int i = 0; i < allControls.length; i++)
    {
      if (allControls[i].equalsIgnoreCase("Unstained Control")){
          if (tubeMap != null && tubeMap.containsKey ("Unstained Control")){
              TubeInfo onetube = tubeMap.get ("Unstained Control");
              if (!onetube.getTubeType().equalsIgnoreCase( "beads unstained"))
                 unstainedFCS[u++] = new FCSFile(filenames[i]);

          }
//          else {
              unstainedFCS[u++] = new FCSFile (filenames[i]);
//          }
        
      }
      else {
         controlList[s] = allControls[i];
        stainedFCS[s++] = new FCSFile(filenames[i]);

      }
    }
    //the tandems are still on this list.
//    for (String ss: controlList)
//        System.out.println ("stained list "+ ss);
    
    results = (CompensationResults) frame;
    createUnstainedStainedControls (unstainedFCS, stainedFCS, fl_labels, true, results);
    //it is the controlList that works.
    return controlList;
  }

  /**
   * Scan one fcsfile in the control set to find a common list of the detector channels.
   * When the set is first created, this becomes the default list.  Each set of detectors
   * in an FCS file is checked against this original list.  This will be the set that
   * all the files share. But when there are tandems -- more than one stain per parameter.
   * In the analysis, there has to be a stained control for each one.  The n x n matrix has
   * to increase.

   * @param fcsfile
   * @return
   */
  private void scanForAcquisitionChannels (String fcsfilename, File workingDir){

      String[] channelList=new String[0];
      String[]reagentList = new String[0];
//      String pns = null;
      int np;
      String split;
      try {
          FCSFile fcsfile = new FCSFile ( workingDir.getCanonicalPath() + File.separator+fcsfilename);
//          System.out.println ("  fcsfile ?  " + fcsfile.getFile().getName() + "  Canonical path " + 
//                  fcsfile.getFile().getCanonicalPath() + "  absolute path" + fcsfile.getFile().getAbsolutePath());
          FCSTextSegment segment = fcsfile.getTextSegment();
          experimentName = segment.getAttribute ("EXPERIMENT NAME");
          channelList = new String[segment.size()];
          reagentList = new String[segment.size()];
         
            Set<String> attrNames = segment.getAttributeNames();
            for (String s: attrNames){
                 if (s.startsWith ("$P") && s.endsWith ("N")){
                    String att = segment.getAttribute(s);

                    if (!att.startsWith ("Time")&& !att.startsWith ("FSC")&& !att.startsWith("SSC") ){
                        if (!att.endsWith ("-H")){
                            FCSParameter p = fcsfile.getParameter (att);
                            np = p.getIndex() - 1;
                            channelList[np] = att;
                        }
                    }
                }
                 else if (s.startsWith ("$P") && s.endsWith ("S")){
                    
                     split = s.substring (2, s.length()-1);
                     
                     np = Integer.parseInt (split)-1;
//                     System.out.println (split + ",  "+ np + "  "+ s);
                     reagentList[np]= segment.getAttribute(s);
                    // pns = segment.getAttribute(s);
                 }
            }
      } catch (FCSException fcse){
          if (Compensation2.DEBUG)
              fcse.printStackTrace();
          String msg = " Unable to read a FCS File";
        JOptionPane.showMessageDialog (null, msg, "FCS File Error", JOptionPane.INFORMATION_MESSAGE);


      } catch (IOException ioe){
          if (Compensation2.DEBUG)
              ioe.printStackTrace();
          String msg = " Unable to open a fcs file.  Perhaps the directory is not correct?";
          JOptionPane.showMessageDialog (null, msg, "FCS File Error", JOptionPane.INFORMATION_MESSAGE);
      }
      ArrayList<String>dlist = new ArrayList<String>();
      ArrayList<String>rlist = new ArrayList<String>();
      for (int i = 0; i < channelList.length; i++){
          if (channelList[i] != null){
              dlist.add (channelList[i]);
              if (reagentList[i] != null)
                  rlist.add (reagentList[i]);
              else
                  rlist.add (channelList[i]);
          }
      }


      detectorList = new String[dlist.size()];
      detectorList = dlist.toArray(detectorList);
      PnSreagents = new String[rlist.size()];
      PnSreagents = rlist.toArray(PnSreagents);
      if (Compensation2.CATE) {
         System.out.println ("================New  DetectorList ================");
         for (String s: detectorList)
             System.out.println ("\t"+s);
       System.out.println ("================end of DetectorList ================");
       System.out.println ("================reagents ================");
      
       for (int i=0; i < PnSreagents.length; i++)
           System.out.println (i + ". " + PnSreagents[i]);
      }
 

   

  }
  
  
  /**
   * This is called with the DivA controls that only allow for one unstained FCSFile.  That
   * is the assumption here.  
   * @param unstainedFCSFiles
   * @param stainedFCSFiles
   */

  protected void createUnstainedStainedControls (FCSFile[] unstainedFCSFiles, FCSFile[] stainedFCSFiles, 
                                                 String[][]fl_labels, boolean mode, CompensationResults results){
//      System.out.println (" createUnstainedStainedControls -- DiVA entry point");
      visual = mode;
      boolean areCells= false;
     if (visual){ //interact or bath
      if (frame == null)
          frame = new CompensationFrame (experimentName, dataFolder, this);
          this.results = (CompensationResults) frame;
      } 
     else {
    	
         this.results = results;
     }
      if (compensation2 == null) {
    	  /**if (results == null){
    	  if (mode ) {
      }
    	  String title, File mydataFolder, CompensationController controller)
    		results = new CompensationFrame();
    	  }**/
          compensation2 = new Compensation2(this.results, dataFolder);
      }
      ArrayList<StainedControl> tempstained = new ArrayList<StainedControl>();

      unstainedControls = new UnstainedControl[unstainedFCSFiles.length];
   //   stainedControls = new StainedControl[stainedFCSFiles.length];
//      if (compensation2 == null){
//          System.out.println ("  compensation2 is null in createUnstainedStained Control from DiVA");
//      }
      if (unstainedFCSFiles.length > 1){
          System.out.println ("  Warning about the Unstained FCS Files -- more than one unstained control");
      }
      else if (unstainedFCSFiles.length == 0){
          System.out.println ("  Warning about No Unstained FCSFiles");

      }
      if (unstainedFCSFiles.length > 0){
          for (int i=0; i < unstainedFCSFiles.length; i++){
             unstainedControls[i] = new UnstainedControl (compensation2, unstainedFCSFiles[i]);
          }
      }
      else {
          unstainedControls= new UnstainedControl[1];
          unstainedControls[0] = null;
      }


      PnSreagents = new String[detectorList.length];
      int currow=0;
      if (fl_labels != null && fl_labels.length > 0){
          for (int i=0; i < fl_labels.length; i++){
//              System.out.println (fl_labels[i][0] + ", " + fl_labels[i][1]);
              int detectorindex = getDetectorIndex (fl_labels[i][0], detectorList);

              if (detectorindex < 0){
                  continue;
              }
              else {
                  while (detectorindex > currow ){
                      tempstained.add  ((StainedControl)null);
                      currow++;
                  }
                  unstainedControls[0].setAreCells(areCells);    
                  tempstained.add ( new StainedControl (compensation2, stainedFCSFiles[i],
                           detectorindex, detectorindex, detectorList[detectorindex], 
                           unstainedControls[0], areCells));
                  currow++;
                  if (fl_labels[i][1] == null)
                      PnSreagents[detectorindex]= detectorList[detectorindex];
                  PnSreagents[detectorindex] = fl_labels[i][1];
              }
          }
      }
      else {
         for (int i=0; i < controlList.length; i++){
//             System.out.println ( controlList[i]);
             int detectorindex = getDetectorIndex (controlList[i], detectorList);
             if (detectorindex < 0)
                 continue;
//             System.out.println (detectorindex + "  "+ i);
             PnSreagents[detectorindex] = detectorList[detectorindex];
             while ( detectorindex > currow ){
                 tempstained.add((StainedControl) null);
                 currow++;
             }
             unstainedControls[0].setAreCells(areCells);
             tempstained.add( new StainedControl (compensation2, stainedFCSFiles[i],
                      detectorindex, detectorindex, detectorList[detectorindex], unstainedControls[0], areCells));
             currow++;
         }
      }

      for (int i=0; i < detectorList.length; i++){
          if (PnSreagents[i] == null)
              PnSreagents[i] = detectorList[i];
//          System.out.println (PnSreagents[i] + " <--> "+ detectorList[i]);
      }
      stainedControls = new StainedControl[tempstained.size()];
      stainedControls = tempstained.toArray (stainedControls);
      if (Compensation2.CATE){
          for (int i = 0; i < stainedControls.length; i++){
              if (stainedControls[i] != null)
                  System.out.println (i + ". "+ stainedControls[i].toString());
              else
                  System.out.println (i + ". Not being used ");
          }
      }
     
      
  }
  /**
   *
   * @param name
   * @return
   */

  protected int getDetectorIndex (String name, String[] list){
      int index=-1;
      boolean found = false;
      int i=0;
//      System.out.println (" getDetectorIndex for ("+ name + ") "+ name.trim());
      while (!found && i < list.length){
          if (list[i].startsWith (name)){
              found = true;
              index = i;
          }
          else i++;
      }
      if (!found) index = -1;

      return index;

  }

  // Not being called
  private String[] uniqueDetectorList (String[] detect){
      String[] uniquelist;
      ArrayList<String> list = new ArrayList<String>();

      for ( String s: detect){
          if (!list.contains (s))
              list.add (s);
      }
      uniquelist = new String[list.size()];
      uniquelist = list.toArray (uniquelist);
      return uniquelist;

  }
  //Not being called
  /**private int getReagentIndex (String name){
      int index=-1;
      boolean found = false;
      int i=0;

      if (name == null || name.length() == 0)
          return index;
      while (!found && i < PnSreagents.length){
          if (PnSreagents[i].equals (name)){
              found = true;
              index = i;
          }
          else i++;
      }
      if (!found) index = -1;

      return index;

  }**/

  /**
   * this one is called when we are downloading the jo files.  
   */
  protected void createUnstainedStainedControls (String[][]data, File workingDir,
                                                 boolean mode, CompensationResults results,
                                                 HashMap <String, TubeInfo> tubeMap){
    
      if (results == null){
          System.out.println ("CompensationController.createUnstained controls CompensationResults are null");
      }
      this.results = results;
      ArrayList<StainedControl> stainedControlList = new ArrayList<StainedControl>();
      HashMap<String, UnstainedControl> unstainedControlList = new HashMap<String, UnstainedControl>();
      ArrayList<String> reagentList = new ArrayList<String>();
      visual = mode;
      if (visual){ //that is visual = true or false = batch
      if (frame == null)
          frame = new CompensationFrame (experimentName, dataFolder, this);
          this.results = (CompensationResults) frame;
      }
      if (compensation2 == null)
          compensation2 = new Compensation2(results, dataFolder);
      // 0 is detector, 1 is reagent, 2 is unstained control fcs, 3 is stained
    // control fcs file

    StainedControl newstained;
    TubeInfo tone;
    boolean areCells = false;
    controlList = new String[data.length];
    if (detectorList == null){
        System.out.println ("  The detectorList is empty ......");
        detectorList = new String[data.length];
        for (int i=0; i < data.length; i++){
            detectorList[i] = data[i][0];
            System.out.println (detectorList[i] + "  detectors in createUnstained  "+ data[i][0]);
        }
    }

    for (int i = 0; i < data.length; i++)  {
        areCells = false;
        int detectorIndex = getDetectorIndex (data[i][0], detectorList);
        System.out.println (" createUnstained controls. " + data[i][0] + "  "+ detectorIndex);
        if (detectorIndex < 0){
//            System.out.println ("  Skip this one.no detector index for "+ data[i][0]);
            continue;
        }

      UnstainedControl newunstained = null;
      String unfn = null;
      String thisreagent=null;
      //I need to know what the index in the detectorList of this data[i][0].  That is
      //what is the primaryDetector, given this String, what is the index in the detectorList
      //that matches.


      if (data[i][1] != null && !data[i][1].equals("")){
        
         thisreagent = data[i][1];
      }
      else {
//             PnSreagents[detectorIndex] = new String (detectorList[detectorIndex]);
//             thisreagent = PnSreagents[detectorIndex];
          thisreagent = detectorList[detectorIndex];
      }

      if (data[i][3] != null && !data[i][3].equals("")) { // stained control file
        // if there is an unstained one, but doesn't have to have an unstained control
        if (data[i][2] != null && !data[i][2].equals("")) {
            //is the information stored here an fcs filename or a tubename?
            if (data[i][2].endsWith(".fcs"))
          // this is the unstained control
                unfn = workingDir + File.separator + data[i][2];
            else{
                
                tone = tubeMap.get(data[i][2]);
                areCells = tone.getAreCells();
                unfn = workingDir + File.separator + tone.getFcsFilename();
            }

          if (!unstainedControlList.containsKey(data[i][2]))
          {
              
              newunstained = new UnstainedControl(compensation2, new FCSFile(unfn));
              unstainedControlList.put(data[i][2], newunstained);
          }
          else
          {
            newunstained = unstainedControlList.get(data[i][2]);
          }
          //test this detector for data collection again the unstained control.
      //    boolean hasData = testDetectorForData (newunstained, alldetectors[i]);
  //        System.out.println ("  result for testDetector For Data "+ hasData + "  "+ alldetectors[i]);        
        }
        String stainfn;
        if (data[i][3].endsWith (".fcs")){
        	if (data[i].length ==6 && data[i][5].equalsIgnoreCase("T")){
        		areCells = true;	
        	}
        	System.out.println("new stained control with are cells =  "+ areCells);
        	newunstained.setAreCells (areCells);
            newstained = new StainedControl(compensation2, new FCSFile(workingDir + File.separator
                + data[i][3]), detectorIndex, stainedControlList.size(), thisreagent, newunstained, areCells);
        }
        else {
            tone = tubeMap.get(data[i][3]);
            System.out.println ("CompensationController line 998" + tone.getInfo());
            stainfn = tone.getFcsFilename();
            System.out.println ("Name of stain control filename "+ stainfn);
            newunstained.setAreCells (areCells);
            newstained = new StainedControl (compensation2, new FCSFile (workingDir + File.separator
                    + stainfn), detectorIndex, stainedControlList.size(), thisreagent, newunstained, tone.getAreCells());
            
        }

       stainedControlList.add (newstained);
//       System.out.println (i + ". " +newstained.toString());
       reagentList.add (thisreagent);

      }
      else {
        stainedControlList.add ((StainedControl)null);
//        System.out.println (i + " this one is blank");
        reagentList.add (thisreagent);

      }

    }

//    System.out.println ("stainedControlList   " +stainedControlList.size());
    //they seem to need to be in detector order
    stainedControls = new StainedControl[stainedControlList.size()];
    stainedControls = stainedControlList.toArray (stainedControls);

    PnSreagents = new String[reagentList.size()];
    PnSreagents = reagentList.toArray (PnSreagents);
    
    unstainedControls = new UnstainedControl[unstainedControlList.size()];
    Set<String> keys = unstainedControlList.keySet();
    Iterator<String> it = keys.iterator();
    int j = 0;
    while (it.hasNext())   {
      unstainedControls[j++] = unstainedControlList.get((String)it.next());
    }
  //  runAnalysis();

    if (Compensation2.CATE ){
    System.out.println (" ------------------detector list------------------------");
      j=0;
      for (String s: detectorList){
           System.out.println (j++ + ". detector list "+ s);
      }
    System.out.println (" ------------------stained control list------------------------");
      j = 0;
      for (StainedControl sc: stainedControls){
          if (sc != null){
          System.out.println (j++ + ". StainedControls "+ sc.getPrimaryDetector() +detectorList[sc.getPrimaryDetector()] +
                               sc.toString());
          }
          else
              System.out.println ("  This stained control is null ");

      }
      System.out.println ("----------------------------PnSReagent list ------------");
      for (String s: PnSreagents){
          System.out.println ("\t" + s);
      }
System.out.println (" ------------------end of list------------------------");
    }

      
  }

  /*  Created in three ways:  one from the controls found in Diva and the
   *   other by the controls created by the user in the dialog.
   * Not currently handling multiples -- don't list the detector more than
   * once in the case of a multiple.
   */
  protected void createUnstainedStainedControls (String[][]data, File workingDir, 
                                                 boolean mode, CompensationResults results){
  
     
    ArrayList<StainedControl> stainedControlList = new ArrayList<StainedControl>();
   // StainedControl [] templist;
    HashMap<String, UnstainedControl> unstainedControlList = new HashMap<String, UnstainedControl>();
    boolean found = false;
    ArrayList<String> reagentList = new ArrayList<String>();
    boolean areCells = false;
    boolean skip = false;
    if (detectorList != null && detectorList.length > 0 && data !=null && data.length >0){
    	skip = true;
    }
  
    String fcsfilename= null;
    int i=0;
    if (data == null){
        System.out.println (" createUnstainedStainedControls.  Data is null.");
        return;
    }
    if (!skip){
    while (!found && i < data.length){
        if (data[i][3] != null && !data[i][3].equals("")){  //this could be the tube name and not the name
            found = true;                                   //of the file.  
            if (data[i].length > 4 && data[i][4] != null  && data[i][3].equals(data[i][4])){
                //this is a tube name
                TubeInfo tube = tubeMap.get(data[i][3]);
                
                if (tube != null){
                    fcsfilename = tube.getFcsFilename();
                   
                }
                
            }
            else
                fcsfilename = data[i][3];
            
        }
        else i++;
    }
    if (found)
        //I have a real fcs file now that is involved in the control.  Use this
        //to find the correct list of acquisition channels in the $PnN keyword.
        //the detectorList is possible changed.
        //also it looks for the $PnS and creates the list PnSreagents.
        scanForAcquisitionChannels ( fcsfilename, workingDir);
    else{
        System.out.println (" Error -- could not find any fcs files!!!" + workingDir.getAbsolutePath());
        System.out.println ("  fcsfilename = "+ fcsfilename);
        String msg = " Error -- could not find any fcs files! ";
//        JOptionPane.showOptionDialog(this, msg, msg, JOptionPane.WARNING_MESSAGE);

    }
    }
    if (visual){
     if (frame == null)
          frame = new CompensationFrame (experimentName, dataFolder, this);
    }
      if (compensation2 == null)
          compensation2 = new Compensation2((CompensationResults)frame, dataFolder);
     

    // 0 is detector, 1 is reagent, 2 is unstained control fcs, 3 is stained
    // control fcs file

    StainedControl newstained;
    controlList = new String[data.length];
  
    
    for ( i = 0; i < data.length; i++)  {
        areCells = false;
        int detectorIndex = getDetectorIndex (data[i][0], detectorList);
        if (detectorIndex < 0){
//            System.out.println ("  Skip this one.no detector index for "+ data[i][0]);
            continue;
        }

      UnstainedControl newunstained = null;
      String unfn = null;
      String thisreagent=null;
      //I need to know what the index in the detectorList of this data[i][0].  That is
      //what is the primaryDetector, given this String, what is the index in the detectorList
      //that matches.


      if (data[i][1] != null && !data[i][1].equals("")){
        
         thisreagent = data[i][1];
      }
      else {
//             PnSreagents[detectorIndex] = new String (detectorList[detectorIndex]);
//             thisreagent = PnSreagents[detectorIndex];
          thisreagent = detectorList[detectorIndex];
      }

      if (data[i][3] != null && !data[i][3].equals("")) { // stained control file
    	  if (data[i].length ==6 && data[i][5].equalsIgnoreCase("T")|| data[i][5].equalsIgnoreCase("true"))
    		  areCells = true;
        // if there is an unstained one, but doesn't have to have an unstained control
        if (data[i][2] != null && !data[i][2].equals("")) {
          // this is the unstained control
          unfn = workingDir + File.separator + data[i][2];

          if (!unstainedControlList.containsKey(data[i][2]))
          {
              
              newunstained = new UnstainedControl(compensation2, new FCSFile(unfn));
              unstainedControlList.put(data[i][2], newunstained);
          }
          else
          {
            newunstained = unstainedControlList.get(data[i][2]);
          }
          //test this detector for data collection again the unstained control.
      //    boolean hasData = testDetectorForData (newunstained, alldetectors[i]);
  //        System.out.println ("  result for testDetector For Data "+ hasData + "  "+ alldetectors[i]);        
        }
        if (newunstained != null) {
            newunstained.setAreCells(areCells);
        }
        else if(areCells) { // and newunstained == null, issue a warning
        	JOptionPane.showMessageDialog (this.myframe, "When using cells for stained controls,  unstained cells are required. ", 
        			"Matrix File IO Error",
                    JOptionPane.ERROR_MESSAGE);
        	return;
        	
        }
        System.out.println("Create Stained Control 1 "+ areCells);
        newstained = new StainedControl(compensation2, new FCSFile(workingDir + File.separator
            + data[i][3]), detectorIndex, stainedControlList.size(), thisreagent, newunstained, areCells);

       stainedControlList.add (newstained);
//       System.out.println (i + ". " +newstained.toString());
       reagentList.add (thisreagent);

      }
      else {
        stainedControlList.add ((StainedControl)null);
//        System.out.println (i + " this one is blank");
        reagentList.add (thisreagent);

      }

    }

//    System.out.println ("stainedControlList   " +stainedControlList.size());
    //they seem to need to be in detector order
    stainedControls = new StainedControl[stainedControlList.size()];
    stainedControls = stainedControlList.toArray (stainedControls);

    PnSreagents = new String[reagentList.size()];
    PnSreagents = reagentList.toArray (PnSreagents);
    
    unstainedControls = new UnstainedControl[unstainedControlList.size()];
    Set<String> keys = unstainedControlList.keySet();
    Iterator<String> it = keys.iterator();
    int j = 0;
    while (it.hasNext())
    {
       
      unstainedControls[j++] = unstainedControlList.get((String)it.next());
       System.out.println ("what about the unstained ?  " + unstainedControls[j-1]);
    }

    if (Compensation2.CATE ){
    System.out.println (" ------------------detector list------------------------");
      j=0;
      for (String s: detectorList){
           System.out.println (j++ + ". detector list "+ s);
      }
    System.out.println (" ------------------stained control list------------------------");
      j = 0;
      for (StainedControl sc: stainedControls){
          if (sc != null){
          System.out.println (j++ + ". StainedControls "+ sc.getPrimaryDetector() +detectorList[sc.getPrimaryDetector()] +
                               sc.toString());
          }
          else
              System.out.println ("  This stained control is null ");

      }
      System.out.println ("----------------------------PnSReagent list ------------");
      for (String s: PnSreagents){
          System.out.println ("\t" + s);
      }
System.out.println (" ------------------end of list------------------------");
    }

  }


/**
 * Turn the on buttons off and the off button on.
 * @param rbs
 */
    private void turnOnOffButtons (AbstractButton[] rbs){
        for (AbstractButton rb: rbs){
            rb.setEnabled (!rb.isEnabled());
        }

    }
    /*
     * return the dataFolder
     */
     public File getDataFolder(){
         return dataFolder;
     }
     /*
      * return the DivaFile that the user selected.
      */
     public File getDivaFile(){
         return divaFile;
     }

    public File showOpenDialog(String title, FileNameExtensionFilter filter, int type){
      String tt = null;
      if (dataFolder != null)
        System.out.println ("What about the data folder?  " + dataFolder.getName());

    if (filter != null)
        fileChooser.addChoosableFileFilter (filter);
    fileChooser.setFileSelectionMode (type);
    fileChooser.setDialogTitle(title + "Compensation:  "+ title );
    if (fileChooser.showOpenDialog(this.myframe)!=JFileChooser.APPROVE_OPTION){
        showMessageDialog (" No file was selected.");
      return null;
    }
    else
      return fileChooser.getSelectedFile();
  }

  public void showMessageDialog (String msg){
      System.out.println ("  Is this being called?");
      JOptionPane.showMessageDialog (this.myframe, msg, " Computation Status ", JOptionPane.INFORMATION_MESSAGE);
//      scatterDialog.setVisible(true);

  }


  

   protected void printFlowJoMatrixForPC (File fn, Float[][]data, String[] detectorNames, String expName){

    //  XmlParser parser = new XmlParser (fn, data, detectorNames, expName);
      DivaXmlParser parser = new DivaXmlParser();
      parser.printFlowJoMatrixforPC(fn, data, detectorNames, expName);

      //the parser takes care of creating the matrix.  The constructor signature
      //determines the action.

  }
   public void reportMessage(String msg){
       FileWriter fw = null;
       if (dataFolder.exists() && dataFolder.canWrite()){
           try{
               File logfile = new File (dataFolder.getCanonicalPath() + File.separator + "log.txt");
               fw = new FileWriter (logfile);
               fw.append (msg);
               fw.close();
               
           } catch (IOException e){
               System.err.println ("Unable to open a log file or find the folder in order to create a log file.  ");
               if (fw != null)
                   try{
                   fw.close();
                   }catch (IOException ee){
                       
                   }
           } 
       }
       
   }

   public void setUpForMatrixPrinting ( Float[][] data, String[] detectorNames){

       int[] counts = new int[detectorList.length];
       spectrumData = data;
       boolean hasmultiples = checkForMultiples(counts);
       int sum = 0;
       Diagnostic.List msgs;
       for (int i: counts){
           sum += i;
       }
       if (!hasmultiples){
           //printFlowJoMatrix ( fn, data, detectorNames, expName);
           printFlowJoMatrix ( data, detectorNames, experimentName);
   //        writeMatrixToFlowJo (data, detectorNames);
       }
       else {
          if (visual) {
              Multiples[] multiples = createMultiples (detectorNames);
              mmdialog = new MultipleMatrixDialog(this, multiples, sum);
          }
       }
       StringBuilder  allmsgs = new StringBuilder();
       for (int i=0; i < data.length; i++){
           for (int j=0; j < data[i].length; j++) {
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
   
   

   /**
    * Pairs are created from the list of StainedControls.  the row is the place
    * in the array.  the column is the primary.
    * @return
    */
   private Multiples[] createMultiples(String[] detectorNames ) {

       
       Multiples[] multiples = new Multiples[detectorNames.length];

     
       for (int i = 0; i < stainedControls.length; i++){
           if (stainedControls[i] != null){
               int primary = stainedControls[i].getPrimaryDetector();
               if ( multiples[primary] == null){
                   multiples[primary] = new Multiples (detectorNames[primary]);
               }
               multiples[primary].addReagentEntry (stainedControls[i].getParameterName(), i, primary);

           }
//           else {
//               System.out.println ("  This is not a correct assumption for a null stained control");
//               System.out.println (i + ", "+ detectorNames[i]);
//               //multiples[i] = new Pair (detectorNames[i], detectorNames[i],i, i );
//           }

       }
       for (int i=0; i < multiples.length; i++){
           if (multiples[i] == null){
               multiples[i]= new Multiples(detectorNames[i]);
           }
       }


       return multiples;
   }

   public void printFlowJoMatrix (int[] indices){

   }

   /**
    * I want this to be called with multiples already checked and just ready to
    * print.  Get the filename.
    * @param data  This is also known as spectrumData.
    * @param detectorNames
    * @param expName
    */
   public void printFlowJoMatrix ( Float[][] spectrumData, String[] detectorNames, String expName){
       File fn = null;

//System.out.println (" print FlowJo Matrix "+ experimentName + " vs "+ expName);
       if (visual){
          JFileChooser chooser = new JFileChooser(dataFolder);

           chooser.setFileFilter (makeFilterForOS());
           int ans = chooser.showSaveDialog (frame);
           if (ans == JFileChooser.APPROVE_OPTION){
               fn = chooser.getSelectedFile();
           }
       }
       else{
           fn = new File (dataFolder + File.separator + expName+"_matrix");
       }
           String os = System.getProperty ("os.name");
 // printFlowJoMatrixForPC (fn, spectrumData, detectorNames, experimentName);
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
                MathContext mc = new MathContext (4);
                for (int i=0; i < dim; i++){
                    for (int j=0; j < dim-1; j++){
//                        fw.print (spectrumData[i][j].floatValue());
//                        System.out.println ("-----floatValue "+ spectrumData[i][j].floatValue() + " ---  "+ Float.toString (spectrumData[i][j]));
                      //  fw.write (Float.toString(spectrumData[i][j]));
                        BigDecimal bd = new BigDecimal (spectrumData[i][j].floatValue(), mc);
                        fw.print (bd.toPlainString());
//                        System.out.println ("-----BigDecimal-------->>>" + bd.toPlainString() + " |||  " + bd.toString());
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
                JOptionPane.showMessageDialog (this.myframe, "Error trying to write the matrix file ", "Matrix File IO Error",
                        JOptionPane.WARNING_MESSAGE);
            }
        }


  }
   

  public void getSelections (Multiples[] multiples){
      //printFlowJoMatrix ( Float[][] data, String[] detectorNames, String expName)
     
      Float[][] matrixData = new Float[multiples.length][multiples.length];
      for (int i=0; i < multiples.length; i++){
          ArrayList<Multiples.ReagentEntry> rlist = multiples[i].getReagentEntryList();

          int j=0;
          while  ( j < rlist.size()){
              System.out.println ("getSelections "+ rlist.get(j).toString() + "  "+ rlist.get(j).isSelected() );
              if (rlist.get(j).isSelected()){
                  int row = rlist.get(j).getRow();

                //  matrixData[i] = spectrumData[row];

                 // matrixData[i] = new Float[n];
                  //there is a blank column in the data from the table
                  //at the very end.
                  for (int k=0; k < multiples.length; k++)
                      matrixData[i][k] = new Float (spectrumData[row][k]);

                  break;
              }
              j++;
          }
          if (rlist.isEmpty() ){
              //matrixData[i] = new Float[multiples.length];

              for (int k=0; k < multiples.length; k++){
                  if (i == k) matrixData[i][k] = new Float(1.0);
                  else matrixData[i][k] = new Float(0.0);

              }
          }
      }
      printFlowJoMatrix (matrixData, detectorList, experimentName);


  }

  protected boolean checkForMultiples (int[] counts){
      boolean multiples = false;
      for (int i=0; i < counts.length; i++)
          counts[i] = 0;

      for (int i=0; i < stainedControls.length; i++){
          if (stainedControls[i] != null){
              counts[stainedControls[i].getPrimaryDetector()] +=1;
              if (counts[stainedControls[i].getPrimaryDetector()]> 1)
                  multiples = true;
          }
      }


      return multiples;
  }
  /**
   *
   * @param type
   * @return
   */
  protected static int makeOSType (String type) {
        int osType = 0;

        if (type == null || type.length() != 4)
          throw new IllegalArgumentException();
        for (int i = 0; i < 4; ++i)
        {
          osType <<= 8;
          osType |= type.charAt(i);
        }

        return osType;
  }

  private FileNameExtensionFilter makeFilterForOS() {
      String os = System.getProperty ("os.name");
      FileNameExtensionFilter filter;
      if (os.equalsIgnoreCase ("Mac OS X")){
          filter = new FileNameExtensionFilter ("FlowJo Matrix", "txt");
      }
      else
          filter = new FileNameExtensionFilter ("FlowJo Matrix", "mtx");
      return filter;
  }

  public void deleteTempFiles() {
      String location = System.getProperty("user.home")+ File.separator + "tempfcs";
      File tempdir = new File (location);
      if (tempdir.isDirectory()){
          File [] list = tempdir.listFiles();
          for (File f: list){
              System.out.print (f.getName());
              boolean b = f.delete();
              System.out.println (" "+ b);
          }
          tempdir.deleteOnExit();
      }
      
      
  }
  public boolean isVisual() {
        return visual;
    }


   

    //override
    /*
     * the fcsfiledialog class sends this data when its continue button is clicked
     * this is a pass through.
     */
    public void putMappingData (String[][] info, File dataFolder) {
        System.out.println (" accept Mapping Information !! in the CompensationController." );
        this.dataFolder = dataFolder;
        for (int i=0; i < info.length; i++){
        	for (int j=0; j < info[i].length; j++)
               System.out.println(i + " "+j+". "+info[i][j]);
        }

if (info != null){
        controlMappings = new String[info.length][];
        for (int i=0; i < info.length; i++){
            controlMappings[i] = new String[info[i].length];
            for (int j=0; j < info[i].length; j++){
                if (info[i][j] == null) info[i][j]="";
                controlMappings[i][j] = info[i][j];
            }
        }
}
        if (Compensation2.CATE)
            System.out.println (" how many stained controls? "+ info.length);
        createUnstainedStainedControls (info, dataFolder, true, (CompensationResults)frame);

    }
    public JFrame getFrame() {
        return (JFrame) this.myframe;
    }
    
    
    public static void main (String[] args){
       SwingUtilities.invokeLater(new Runnable()
    {
      public void run ()
      {
        try
        {
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
          CompensationController c = new CompensationController (true);
        }
        catch (Exception e)
        {
          e.printStackTrace();
          System.exit(0);
        }
      }
    });
  }

    //override
    public void propertyChange (PropertyChangeEvent pce) {
        System.out.println (" CompensationController propertyChanged");
        System.out.println (pce.getSource().getClass().getName());
        System.out.println (pce.getNewValue().toString() + "  "+ pce.getOldValue().toString());
//        if (pce.getNewValue() == SwingWorker.StateValue.DONE){
        if ("state".equals(pce.getPropertyName())
                 && SwingWorker.StateValue.DONE == pce.getNewValue()) {
            System.out.println ("CompensationController  Done with download.....");
            if (jofile != null){
////                controlMappings = jofile.getControlData();
                tempJoFolder = jofile.getTempDirectory(); 
                dataFolder = jofile.getTempDirectory();
                if (dataFolder != null)
                    System.out.println (dataFolder.getName());
                //this is null
                tubeMap = jofile.getTubeMap();
              //  ArrayList<String[]> mappings = new ArrayList<String[]>();
                Collection <TubeInfo>col = tubeMap.values();
                Iterator <TubeInfo>it = col.iterator();
                while (it.hasNext()){
                    TubeInfo one =  it.next();
                    if (one.isSelected ){
                        String[] row = new String[4];
                        
                    }
                }
//                ArrayList<String[]> controlInformation = jofile.getList();
              /**  for (int i=0; i < controlInformation.size(); i++){
                  for (String s: controlInformation.get(i)){
                      System.out.print (s + " ");
                  }
                  System.out.println();
              }**/
                
            }
//            createUnstainedStainedControls (controlMappings, tempJoFolder, true, (CompensationResults)frame, tubeMap);

            
        }
//         protected void createUnstainedStainedControls (FCSFile[] unstainedFCSFiles, FCSFile[] stainedFCSFiles, 
//                                                 String[][]fl_labels, boolean mode, CompensationResults results){
    }


    public class Multiples  {
        private String detector;
 //       private int col;
        ArrayList<ReagentEntry> reagentlist = new ArrayList<ReagentEntry>();


        public class ReagentEntry implements ActionListener, ItemListener {
            private String reagent;
            private int row, col;
            private boolean isSelected;
            ReagentEntry (String reagent, int row, int col){
                this.reagent = reagent;
                this.row = row;  // connects to the index of the stained controls
                this.col = col;  // connects to the index of the acquistion channel or primary
            }
            public String toString(){
                return reagent;
            }
            public int getRow() {
                return row;
            }
            public int getCol() {
                return col;
            }

            String getReagent() {
                return reagent;
            }

            boolean isSelected() {
                return isSelected;
            }

            public void setSelected (boolean b){
                isSelected = b;
            }

            //override
            public void actionPerformed (ActionEvent ae) {
               
                if (ae.getSource() instanceof JCheckBox){
                    JCheckBox cb = (JCheckBox)ae.getSource();
                    this.isSelected = cb.isSelected();

                }

            }

            //override
            public void itemStateChanged (ItemEvent ie) {
                if (ie.getStateChange() == ItemEvent.DESELECTED ) isSelected = false;
                else isSelected = true;

            }

        }

        Multiples (String detector){
           this.detector = detector;
        }

        public ActionListener getActionListener (int i){
            if (i < reagentlist.size())
                return reagentlist.get(i);

            return null;
        }
        public ItemListener getItemListener (int i){
            if (i < reagentlist.size())
                return reagentlist.get(i);
            return null;
        }
        public ArrayList<ReagentEntry> getReagentEntryList() {
            return reagentlist;
        }

        public String[] getReagentList() {
            String[] rlist = new String[reagentlist.size()];
            if (rlist != null){
                for (int i=0; i < reagentlist.size(); i++)
                    rlist[i] = reagentlist.get(i).getReagent();
            }
            return rlist;
        }

        public void addReagentEntry (String reagent, int row, int col){
            reagentlist.add (new ReagentEntry (reagent, row, col));
        }
        public String toString() {
            return detector;
        }

         public String getDetector() {
                return detector;
            }

    }




}
