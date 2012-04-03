/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.stanford.facs.gui;

import edu.stanford.facs.exp_annotation.TubeInfo;
//import edu.stanford.facs.compensation.Compensation2;
import java.awt.BorderLayout;
import java.io.IOException;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;


import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.FileWriter;


import java.io.FilenameFilter;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import javax.swing.ImageIcon;
//import javax.swing.event.ChangeEvent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.Document;
import org.isac.fcs.FCSException;
import org.isac.fcs.FCSFile;
import org.isac.fcs.FCSTextSegment;




/**
 * $Id: FCSFileDialog.java,v 1.27 2012/01/25 01:41:07 beauheim Exp $
 * @author cate
 * FCSTextSegment segment = file.getTextSegment();
 * Set<String> attrNames = segment.getAttributeNames();
 * segment.getAttribute("SPECIMEN NAME"));
 */
public class FCSFileDialog extends JDialog {
    private File workingDir;
    static File propertyFile;
    private MappingInterface parent;
    private JList list;
    private String[] detectorNames;
    private String[] fluorochromeList;
    private JPanel leftPanel;
    private DefaultListModel listModel = new DefaultListModel();
    private ArrayList<ControlInformation> allInfo = new ArrayList<ControlInformation>();
    private String[] colNames={"Add Row", "Detector", "Reagent",
                               "Unstained FCSFile ",
                               "Stained FCSFile", "Cells?"};
   
	private static final long serialVersionUID = 1L;

    private final URL addURL = FCSFileDialog.class.getResource ("/edu/stanford/facs/gui/add.png");
    private final ImageIcon addIcon = new ImageIcon (addURL);
    private JLabel message = new JLabel ("Drag and drop the unstained and stained control files to the detector rows.");
    private HashMap<String, TubeInfo> tubeMap = new HashMap <String, TubeInfo>();
   // private HashMap<Integer, ArrayList<String>> detectorMap;
    private JCheckBox useUnstained;
    private String singleUnstained;
    
    
    /**
     * when Diva file has been read, controls found, but not accepted.
     * @param parent 
     * @param detectors
     * @param workingdir
     * @param comp
     */
    public FCSFileDialog (MappingInterface parent, String[] detectors, String workingdir){

        super();
        this.parent = parent;
       
        this.detectorNames = detectors;
        this.workingDir = new File (workingdir);

         createDialog(parent, false);

    }

    /**
     * This is a special case that was used in development when no UI is present.
     * @param detectors
     * @param mappings
     * @param comp
     */
    public FCSFileDialog (String[] detectors, File mappings){
        super();
      
        this.detectorNames = detectors;
        this.workingDir = new File (mappings.getParent());
        propertyFile = mappings;
        initDetectorsNoUI();
        getMyProperties (propertyFile);
    }

    /**
     * Called from CompensationFrame.setUp().  We have accepted the DiVa Controls and just showing it.
     *  not being called.
     * @param parent
     * @param mappingInfo
     * @param comp
     */
    public FCSFileDialog (MappingInterface parent, ArrayList<String[]> mappingInfo){

        super ();
        this.parent = parent;
       
        for ( String[]s : mappingInfo){
            allInfo.add (new ControlInformation (s));
        }
        createDialog (parent, false);
    }
    /**
     * Possibly this is not being called at all!
     * @param parent
     * @param detectorList
     * @param tubeMap 
     */
    public FCSFileDialog (MappingInterface parent, HashMap<Integer, ArrayList<String>> detectorList, 
                                                   HashMap<String, TubeInfo> tubeMap){
        this.parent = parent;
      //  this.detectorMap = detectorList;
        this.tubeMap = tubeMap;
        System.out.println ("  FCSFile Dialog constructor 3 with the tubeMap");
        createDialog (parent, false);
    }
   

    /**
     * Being called from the InformationDialog when picking my own controls.
     * @param parent
     * @param workingdir
     * @param detectorList
     */
    public FCSFileDialog (MappingInterface parent, File workingdir, String[] detectorList, 
                                                                    String[] fluorochromeList){
        super ();
       
        this.parent = parent;
        this.detectorNames = detectorList;
        this.workingDir = workingdir;
        this.fluorochromeList = fluorochromeList;
        createDialog (parent, false);
    }



    private void createDialog(final MappingInterface parent, boolean jofile) {
        
        setDefaultCloseOperation (JDialog.HIDE_ON_CLOSE);
        JComponent panel = new JPanel();
        panel.setOpaque(true); //content panes must be opaque
        
        panel.setLayout (new BorderLayout());
        JPanel northpanel = new JPanel ();
       
        //northpanel.setLayout (new GridLayout(2,2));
        final JLabel label1;
        if (workingDir == null && jofile)
            System.out.println ("  jo file doesn't need a working dir");
        if (workingDir == null)
            label1 = new JLabel ("Select the data directory: ");
        else {
            label1 = new JLabel ("Data Directory: " + workingDir.getName());
            if (!jofile)
                getFCSList (workingDir);
            else {
                System.out.println ("  Jo File -- get the tube list");
            }
        }
        JButton button1 = new JButton ("Browse");
        button1.addActionListener (new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                
                File tempDir = showOpenDialog ("Change the Data Directory",
                        new JFileChooser(), JFileChooser.DIRECTORIES_ONLY);
                if (tempDir != null && tempDir.getName() != null){
                    workingDir = tempDir;
                    label1.setText("DataDirectory: "+ workingDir.getName());
                }
                //find the list of FCS files in the working directory;
                getFCSList (workingDir);
                

            }
        });
        final JLabel label2 = new JLabel ("Optional:  Select a property file");
        JButton button2 = new JButton ("Browse");
        button2.addActionListener (new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              
                propertyFile = showOpenDialog ("Select the property file",
                        new JFileChooser(workingDir), JFileChooser.FILES_AND_DIRECTORIES);
                if (propertyFile == null){
                    JOptionPane.showMessageDialog (FCSFileDialog.this,
                            " No file selected.", "File Selection Status",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                label2.setText ("Property file: " + propertyFile);

                getMyProperties (propertyFile);

            }
        });
        northpanel.add (label1);
        northpanel.add (button1);
        northpanel.add (label2);
        northpanel.add (button2);
        list = new JList(listModel);
        list.setVisibleRowCount(-1);
       // listModel.addListDataListener ((ListDataListener) list);
        list.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setDragEnabled(true);
        list.setTransferHandler(new TransferHandler() {
            static final long serialVersionUID=123456789;
            public boolean canImport(TransferHandler.TransferSupport info) {
                // we only import Strings
                if (!info.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    return false;
                }

                JList.DropLocation dl = (JList.DropLocation)info.getDropLocation();
                if (dl.getIndex() == -1) {
                    return false;
                }
                return true;
            }

            public boolean importData(TransferHandler.TransferSupport info) {
                if (!info.isDrop()) {
                    return false;
                }

                // Check for String flavor
                if (!info.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    displayDropLocation("List doesn't accept a drop of this type.");
                    return false;
                }

                JList.DropLocation dl = (JList.DropLocation)info.getDropLocation();
                DefaultListModel listModel = (DefaultListModel)list.getModel();
                int index = dl.getIndex();
              //  boolean insert = dl.isInsert();
                // Get the current string under the drop.
                String value = (String)listModel.getElementAt(index);
//System.out.println (" value of element at this index "+ value + ", "+ index);
                // Get the string that is being dropped.
                Transferable t = info.getTransferable();
                String data;
                try {
                    data = (String)t.getTransferData(DataFlavor.stringFlavor);
                }
                catch (Exception e) { return false; }

                // Display a dialog with the drop information.
                String dropValue = "\"" + data + "\" dropped ";
                if (dl.isInsert()) {
                    if (dl.getIndex() == 0) {
                        displayDropLocation(dropValue + "at beginning of list");
                    } else if (dl.getIndex() >= list.getModel().getSize()) {
                        displayDropLocation(dropValue + "at end of list");
                    } else {
                        String value1 = (String)list.getModel().getElementAt(dl.getIndex() - 1);
                        String value2 = (String)list.getModel().getElementAt(dl.getIndex());
                        displayDropLocation(dropValue + "between \"" + value1 + "\" and \"" + value2 + "\"");
                    }
                } else {
                    displayDropLocation(dropValue + "on top of " + "\"" + value + "\"");
                }


		        return false;
            }

            //override
            public int getSourceActions(JComponent c) {
                return COPY;
            }

            //override
            protected Transferable createTransferable(JComponent c) {
                JList list = (JList)c;
                Object[] values = list.getSelectedValues();

                StringBuilder buff = new StringBuilder();

                for (int i = 0; i < values.length; i++) {
                    Object val = values[i];
                    buff.append(val == null ? "" : val.toString());
                    if (i != values.length - 1) {
                        buff.append("\n");
                    }
                }
                return new StringSelection(buff.toString());
            }
        });
        list.setDropMode(DropMode.ON_OR_INSERT);


        leftPanel = new JPanel();
        if (detectorNames != null)
            createDetectorPanel (leftPanel, detectorNames);

        JPanel rightPanel = createVerticalBoxPanel();
        rightPanel.add (list);
        Dimension dimleft = new Dimension (650, 500);
        Dimension dimright = new Dimension (60, 500);
        JScrollPane rightscroll = new JScrollPane (rightPanel);
        rightscroll.setMinimumSize(dimright);

        JScrollPane leftscroll = new JScrollPane (leftPanel);
        leftscroll.setMinimumSize (dimleft);


        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                              leftscroll, rightscroll);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation (750);
        panel.add(splitPane, BorderLayout.CENTER);
        if (!jofile)
            panel.add (northpanel, BorderLayout.NORTH);
        add (panel);
        JButton saveButton = new JButton ("Save settings to file");
        saveButton.addActionListener (new ActionListener() {
            public void actionPerformed (ActionEvent e){
                JFileChooser savefile = new JFileChooser (workingDir);//getworking directory
                savefile.setDialogType (JFileChooser.SAVE_DIALOG);
                savefile.setDialogTitle ("Save stained control associations.");
                int retval = savefile.showSaveDialog (FCSFileDialog.this);
                if (retval == JFileChooser.APPROVE_OPTION){
                    File file = savefile.getSelectedFile();
                    saveMyProperties(file);
                }
                

            }
        });
        

        JButton cancelButton = new JButton ("Cancel ");
        cancelButton.addActionListener (new ActionListener() {
             public void actionPerformed (ActionEvent e){
                  JOptionPane.showMessageDialog (FCSFileDialog.this,
                          "The computation will be terminated.", "Info", JOptionPane.INFORMATION_MESSAGE);
                  FCSFileDialog.this.setVisible(false);
                  System.exit(1);
             }
        });
        JButton continueButton = new JButton("Continue ");
        
        continueButton.addActionListener (new ActionListener() {
            public void actionPerformed (ActionEvent e){
                 if (parent != null){
                     parent.putMappingData (getMappingInfo(), workingDir);
                 }
                 //control goes back to the CompensationController

                 FCSFileDialog.this.setVisible(false);

            }
        });
        JPanel panel2 = new JPanel();
        panel2.add (saveButton);
        panel2.add(cancelButton);
        panel2.add (continueButton );
        panel.add (panel2, BorderLayout.SOUTH);
        setSize (new Dimension (950, 500));
        if (parent != null)
             setLocationRelativeTo( parent.getFrame());
        else
            setLocation (700, 400);
    }
/**
    private String getTextAttribute (String fname, String key){
        String value = "";

        try {
            FCSFile fcsfile = new FCSFile (fname);
            FCSTextSegment segment = fcsfile.getTextSegment();
            value = segment.getAttribute (key);
            if (value == null) value="";
//            System.out.println ("Value of smno " + segment.getAttribute("$SMNO") + " and the key "+ key + "  "+ value);

        } catch (FCSException fcse){
            System.out.println (" FCSException " + fcse.getMessage());
            String msg = "No values were found for "+ key + " in the fcs file";
            JOptionPane.showMessageDialog (null, msg, "FCSFile Error", JOptionPane.INFORMATION_MESSAGE);
//            return value;
        } catch (IOException ioe){
            System.out.println (" IOException (2)"+ ioe.getMessage());
            String msg = "Error reading the file " + fname;
            JOptionPane.showMessageDialog (null, msg, "FCSFile Error", JOptionPane.INFORMATION_MESSAGE);
//            return value;
        }


        return value;
    }**/
    
    private TubeInfo getTubeFileAttributes (String fname){
        //TubeInfo newone = null;
        TubeInfo tone = null;
        FCSFile fcsfile = null;
        File f = new File (workingDir + File.separator+fname);
        if (f.exists() && f.canRead()){
               fcsfile = new FCSFile (f);
        try {        
    
            FCSTextSegment segment = fcsfile.getTextSegment();
            String altfilename = segment.getAttribute ("$FIL");
            String tubename = segment.getAttribute ("TUBE NAME");
            System.out.println (altfilename +" $FIL,  tubename  "+ tubename+ " fcsfile"+ fname);
         
            
            if (!tubeMap.containsKey (tubename)){
              //  tone = new TubeInfo (tubename, fcsfile, segment.getAttribute("$CELLS
                tone = new TubeInfo (tubename, f, altfilename);
            }
                 
            else {
                tone = tubeMap.get(tubename);
                
            }
            //if (tone != null){
              //  tone.addFcsFilename (fname);
              //  tone.addAlternativeFilename(altfilename);
                tubeMap.put (tubename, tone);
            //}
            

        } catch (FCSException fcse){
            System.out.println (" FCSException " + fcse.getMessage());
           // String msg = "No values were found for "+ fname + " in the fcs file";
//            JOptionPane.showMessageDialog (null, msg, "FCSFile Error", JOptionPane.INFORMATION_MESSAGE);
//            return value;
        } catch (IOException ioe){
            System.out.println (" IOException (1)"+ ioe.getMessage());
           // String msg = "Error reading the file " + fname;
//            JOptionPane.showMessageDialog (null, msg, "FCSFile Error", JOptionPane.INFORMATION_MESSAGE);
//            return value; 
        }
        finally {
            try {
                fcsfile.close();
            } catch (FCSException fe){
            } catch (IOException ii){}
            }
        }
        return tone;
    }
/**
 * there needs to be a sync betwwen the control information and the tube map
 * more consistently this is saving 4 fields, except if there areCells.  No 
 * tubeName. if I was using a tube name for the stained control, then I should also be
 * having a field for the tube name for the unstained tube.  this is a bit of overkill
 * because I want to make this a container for tubes.  
 */
    private void saveMyProperties(File file) {
        
        if (file != null ){
            try {
                FileWriter writer = new FileWriter (file);
                StringBuilder buf = new StringBuilder();
                for (ControlInformation ci: allInfo){
                    ci.getData();
                    //first check to see if this ci has any information
                    if (ci.hasData()){
                    	System.out.println(ci.toString());
                    	ci.trimSpaces();
                        buf.append (ci.detectorName).append(",").append(ci.reagent).append(",");
                      //  buf.append(ci.unstainedControlFile).append(",").append(ci.stainedControlFile);
                    //    buf.append (System.getProperty ("line.separator"));
                        System.out.println(buf.toString());
                        System.out.println("\nStained control name: " +ci.stainedTubeName);
                      String unstainedfcsfn="", stainedfcsfn="";
                        if (!ci.stainedControlFile.endsWith(".fcs")){
                            if (tubeMap.containsKey(ci.stainedControlFile)){
                            	TubeInfo tone = tubeMap.get(ci.stainedControlFile);
                            	tone.setTubeType("compensation");
                            	ci.addStainedTube(tone);
                            	System.out.println (ci.compensationCells+ ","+tone.getAreCells());
                            	tone.setAreCells(ci.compensationCells);
                            	
                            	stainedfcsfn = tone.getFcsFilename();
                            	System.out.println ("Tube : "+tone.getInfo());
                            	
                            }
                        }
                        else 
                        	stainedfcsfn = ci.stainedControlFile;
                        if (ci.unstainedControlFile !=null&& !ci.unstainedControlFile.equals("")){
                        	if (!ci.unstainedControlFile.endsWith(".fcs")){
                        		if (tubeMap.containsKey(ci.unstainedControlFile)){
                        			TubeInfo tone = tubeMap.get(ci.unstainedControlFile);
                                	ci.addStainedTube(tone);
                                	unstainedfcsfn = tone.getFcsFilename();
                        		}
                        	}
                        	
                        }
                        else unstainedfcsfn = ci.unstainedControlFile;
                        buf.append(unstainedfcsfn).append(",").append(stainedfcsfn);
                        
                        if (ci.compensationCells ){
                        	buf.append(",").append(ci.compensationCells);
                        }
                        buf.append (System.getProperty ("line.separator"));

                        
                    }
//                    writer.write (ci.detectorName+","+ci.reagent+","+ ci.unstainedControlFile+","+ci.stainedControlFile);
//                    writer.write (System.getProperty ("line.separator"));
                }
                writer.write (buf.toString());
                //writer.flush();
                writer.close();
                } catch (IOException ioe){
                    System.out.println ("(3)"  +ioe.getMessage());
            }
        }
    }



    /**
     * The panel already exists.  The allControl thing exists with the detector
     * names if there are any.  Other wise the panel is blank.  So I the allInfo
     * is not null, then there is information to fill in.
     * @param propertyFile
     */
    private void getMyProperties(File propertyFile)  {
      ArrayList <String> list = new ArrayList<String>();
      String[][] data=null;
      
     // System.out.println("get my properties");
      if (propertyFile == null || ! propertyFile.canRead()){
            System.out.println ("  Can't read the property file !!");
            JOptionPane.showMessageDialog(this, "Cannot read the property file:"+propertyFile.getName(),
                    "Error reading file ", JOptionPane.WARNING_MESSAGE);
            /**
             *   int DETECTOR=0;
         int REAGENT = 1;
        int UNSTAINED =2;
         int STAINED=3;
         int CELLS=4;
             */

        }
        else {
            try {
                BufferedReader in = new BufferedReader (new FileReader (propertyFile));
                String line = in.readLine();
                    while (line != null){
                        String tokens[] = line.split(",");
                        if (tokens == null || tokens.length < 4){
//                            System.out.println ("  no tokens " + line);
                            line= in.readLine();
                            continue;
                        }
                         if (allInfo != null){
                             //find this record by detector name.
                             String reagent="";
                             
                             if (tokens[ControlInformation.REAGENT] != null)
                                  reagent = tokens[ControlInformation.REAGENT];

                             ControlInformation ci = findDetector (tokens[ControlInformation.DETECTOR], reagent);
                             list.add (tokens[ControlInformation.DETECTOR]);
                             if (ci != null ){
                                 ci.addValuesFromMapping (tokens);
                                 
                             }
                             else {
                                 ControlInformation newone = new ControlInformation (tokens);
                                // list.add (tokens[0]);
                                 allInfo.add (newone);
                             }
                         }
                        
                         else {
                             ControlInformation newone = new ControlInformation (tokens);
                             allInfo.add (newone);
                             
                         }
                         
                        line = in.readLine();
                    }

                     data = new String[allInfo.size()][5];
                    int i=0;
                    for (ControlInformation ci: allInfo){
                    	if (!tubeMap.isEmpty()){
                    		TubeInfo tone = tubeMap.get(ci.stainedTubeName);
                    		if (tone !=null)
                    			ci.stainedTube = tone;
                    		if (ci.stainedControlFile.endsWith("fcs")){
                    			
                    		}
                    			
                    	}
                       data[i]=ci.getData();
                       i++;
                    }
                   
                   
            } catch (IOException ioe){
                System.out.println ("getProperties " + ioe.getMessage());
            }
        }
        if (detectorNames == null){
            detectorNames = new String[list.size()];
            detectorNames = list.toArray (detectorNames);
            
        }
      rebuildPanel();

    }
    private void initDetectorsNoUI(){


    for (int i=0; i < detectorNames.length; i++){
            ControlInformation newone = new ControlInformation (detectorNames[i]);
           
            allInfo.add (newone);
    }
    }

    private ControlInformation findDetector (String detector, String reagent){
//        System.out.println ("  find Detector " + detector + ", "+ reagent);
        //in the case of multiples, we can have more than entry for a detector
        //because there is a different reagent.
       // int n=0;
        for (ControlInformation ci: allInfo){
//             System.out.println ("  find detector ci = "+ ci.toString());
            if (ci.detectorName.equalsIgnoreCase (detector)){
                //does this one have data in it and we might have a duplicate?
               // n++;
               if (ci.hasData() )
                   continue;
                ci.reagent = new String (reagent);
                return ci;
                
            }
//            else {
//                System.out.println (" couldn't have any matching detector " + n);
//            }
        }

        return null;
    }

   
    private void displayDropLocation(final String string) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    JOptionPane.showMessageDialog(null, string);
                }
            });
        }
    
    private void getFCSList (File workingdir){

System.out.println ("get fcslist");
        if (workingdir != null && workingdir.exists() && workingdir.isDirectory()){
            MyFilenameFilter filter = new MyFilenameFilter ("fcs");
            String[] files = workingdir.list(filter );
            
            for (int i=0; i < files.length; i++){
               
                TubeInfo tone = getTubeFileAttributes (files[i]);
                
                if (tone != null){
                    listModel.addElement (tone.getTubeName());
                    //System.out.println ("  Add to list model " + tone.getTubeName());
                }
                else
                    listModel.addElement (files[i]);

            }
           
            
        }

       
    }
    private  File showOpenDialog(String title, JFileChooser fileChooser, int mode){

        fileChooser.setDialogTitle(title);
        fileChooser.setFileSelectionMode (mode);

        if (fileChooser.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)
          return null;
        else
          return fileChooser.getSelectedFile();
  }
    
    /**
     * 
     * @param detectors
     */
    public void createDetectorPanel (JPanel leftPanel, String[] detectors){

        GridBagLayout bag = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridwidth = 1;
        constraints.insets = new Insets(2,2,4,2 );
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.weightx = 0.0;
        leftPanel.setLayout (bag);
        leftPanel.setBorder (BorderFactory.createEmptyBorder(4, 4, 4, 4));
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        bag.setConstraints (message, constraints);
        leftPanel.add (message);

        constraints.gridwidth= colNames.length;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.insets = new Insets (1,1,1,1);
        JLabel label0 = new JLabel (colNames[0]);
        bag.setConstraints(label0, constraints);
        leftPanel.add (label0);

        JLabel label1  = new JLabel (colNames[1]);
        constraints.anchor = GridBagConstraints.WEST;  //changed from east
        bag.setConstraints (label1, constraints);
        leftPanel.add (label1);

        JLabel label2 = new JLabel (colNames[2]);
        constraints.anchor = GridBagConstraints.CENTER;
        bag.setConstraints (label2, constraints);
        leftPanel.add (label2);

        JLabel label3 = new JLabel (colNames[3]);
        constraints.gridwidth = GridBagConstraints.RELATIVE;
        bag.setConstraints (label3, constraints);
        leftPanel.add (label3);
        
        JLabel label4 = new JLabel (colNames[4]);
        //constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.gridwidth = GridBagConstraints.RELATIVE;
        bag.setConstraints (label4, constraints );
        leftPanel.add (label4);
        
        JLabel label5 = new JLabel (colNames[5]);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        bag.setConstraints (label5, constraints );
        leftPanel.add (label5);
        
        
        for (int i=0; i < detectors.length; i++){
            if (detectors[i].startsWith("<") && detectors[i].endsWith(">"))
                detectors[i] = detectors[i].substring (1, detectors[i].length()-1);
            ControlInformation newone = new ControlInformation (detectors[i]);
            JButton button = new JButton (addIcon);
            button.setSize (29, 29);
            button.putClientProperty ("rowid", new Integer(i));
            button.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent e){
                    ControlInformation info = null;
                    JButton b = (JButton) e.getSource();
                    Integer rowid = (Integer) b.getClientProperty ("rowid");
//                    System.out.println ("Add Row Action " + rowid + " = row id");
                    ControlInformation ci = allInfo.get(rowid);
                    if (ci != null) {
                        info = new ControlInformation(ci.detectorName);
                        info.setRowId(rowid);
                        allInfo.add (rowid, info);
                    }
                    else{
                        System.out.println ("  could not find the control information for the rowid "+ rowid);
                    }
                    rebuildPanel ();

                }
            });
            constraints.gridwidth = colNames.length;
            constraints.anchor = GridBagConstraints.WEST;
            bag.setConstraints (button, constraints);
            leftPanel.add (button);
            JLabel label = new JLabel (detectors[i]);
           // constraints.anchor = GridBagConstraints.EAST;
            bag.setConstraints (label, constraints);
            leftPanel.add (label);
            JTextField tf1 = new JTextField (12);
            if (fluorochromeList != null && fluorochromeList.length >i && fluorochromeList[i] != null){
                tf1.setText (fluorochromeList[i]);
                newone.reagent = fluorochromeList[i];
            }
            bag.setConstraints(tf1, constraints);
            leftPanel.add (tf1);
            JTextField tf2 = new JTextField(12);
            constraints.gridwidth = GridBagConstraints.RELATIVE;
            bag.setConstraints (tf2, constraints);
            leftPanel.add (tf2);
            JTextField tf3 = new JTextField (12);
            constraints.gridwidth = GridBagConstraints.RELATIVE;
            bag.setConstraints (tf3, constraints);
            leftPanel.add (tf3);
            JCheckBox cb = new JCheckBox();
            cb.putClientProperty("name", "cells");
            cb.setSelected (false);
            cb.addChangeListener(newone);
            
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            bag.setConstraints (cb, constraints);
            leftPanel.add (cb);         
            addListenersTo (tf1, newone,"tf1");
            addListenersTo (tf2, newone,"tf2");
            addListenersTo (tf3, newone,"tf3");
           
            allInfo.add (newone);
        }
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridwidth = 4;
        for (int i=0; i < 4; i++){
        		JLabel la = new JLabel("              ");
        		bag.setConstraints(la, constraints);
        		leftPanel.add(la);
        }	
        
        JCheckBox useUnstained = new JCheckBox("Use one unstained control for all stained controls.");
        useUnstained.putClientProperty("name","useUnstained");
        useUnstained.putClientProperty("dialog", this);
        useUnstained.setSelected(false);
        for (ControlInformation ci: allInfo){
        	useUnstained.addChangeListener (ci);
        }
       /** useUnstained.addChangeListener (new ChangeListener() {
        	public void stateChanged(ChangeEvent e){
        		
        	}
        });**/
         constraints.gridwidth = GridBagConstraints.REMAINDER;
         bag.setConstraints(useUnstained, constraints);
        leftPanel.add(useUnstained);
        	
        	
        
      /*  JLabel la = new JLabel("    ");
        constraints.gridwidth = GridBagConstraints.REMAINDER;
		bag.setConstraints(la, constraints);
		leftPanel.add(la);*/
          }
    /**
     * for each textfield, add the Control Information as a listener
     * @param tf
     * @param ci
     * @param name
     */
     private void addListenersTo (JTextField tf, ControlInformation ci, String name){
//        System.out.println (" add listeners to "+ name);
    	 
    	/** if (useUnstained.isSelected() && singleUnstained == null){
    		 if (name.equalsIgnoreCase("tf2")){
    			 
    	 }
    	 }**/
    		 
        tf.addActionListener (ci);
        Document doc = tf.getDocument();
        doc.putProperty (Document.TitleProperty, name);
        doc.addDocumentListener (ci);

    }
   
     
     public String[][] getMappingInfo () {
System.out.println("FCSDialog get mapping info");
      //   if (Compensation2.CATE){
//tubemap exists,but not continually.  but control info does not have the tube name.  
             if (tubeMap != null && tubeMap.size() > 0){
                 Collection <TubeInfo>tubes = tubeMap.values();
                 Iterator <TubeInfo>it = tubes.iterator();
                 while (it.hasNext()){
                     TubeInfo tone = it.next();
                     System.out.println ("Tube "  +tone.getInfo());
                 }

             }
      //   }
         int n = allInfo.size();
         int m = allInfo.get(0).nfields;
         String[][] data = new String[n][m];
      
         for (int i=0; i < n; i++){
             ControlInformation one = allInfo.get(i);
             System.out.println ("getMappingInfo "+ allInfo.size() + "  "+ one.toString());
             TubeInfo tone = tubeMap.get (one.stainedTubeName);
             if (tone != null){
                 System.out.println("tube = "+ tone.getInfo());
//if the tube is null, 
//             if (one.stainedTube == null && one.stainedControlFile == null){
                 if (one.stainedControlFile == null || !one.stainedControlFile.endsWith (".fcs")){
                    one.stainedControlFile = tone.getFcsFilename();
                 }
                 
                 if (one.unstainedControlFile == null || !one.unstainedControlFile.endsWith (".fcs")){
                     //need to do something here
                     TubeInfo unstained = tubeMap.get (one.unstainedTubeName);
                     if (unstained != null) {
                         one.unstainedTube = unstained;
                         one.unstainedControlFile = unstained.getFcsFilename();
                     }
                 }
                   
             }
             else{
            	 //let's create the tube info now.
             }
                 
             
             data[i] = one.getData();
             for (int j=0; j <data[i].length; j++)
            	 System.out.print (data[i][j] + ", ");
             System.out.println();
         }
        
         return data;
     }

      public ArrayList<ControlInformation> getAllData() {
        return allInfo;
    }
     
     public String[] getDetectors() {
         String [] detectors = new String [allInfo.size()];
         for (int i = 0; i < allInfo.size(); i++){
             detectors[i] = allInfo.get(i).detectorName;
         }
         return detectors;
     }
     
     public String getControlFile (String name, String reag){
         String fn= null;
         for (ControlInformation ci: allInfo){
             if (ci.detectorName.equals (name)){
                 if (reag != null && ci.reagent.contains (reag))
                     fn = ci.stainedControlFile;
                 else
                     fn = ci.unstainedControlFile;
                break;
             }
         }
         return fn;
     }

     protected JPanel createVerticalBoxPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.PAGE_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        return p;
    }

     /* only called in MyDialog.java that is not part of the build */
     protected JPanel createHorizontalBoxPanel() {
         JPanel p = new JPanel();
         p.setLayout(new BoxLayout(p, BoxLayout.LINE_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        return p;
     }

  

        /* rebuild the panel */
    public void rebuildPanel (){
         leftPanel.removeAll();

         GridBagLayout bag3 = (GridBagLayout)leftPanel.getLayout();
         GridBagConstraints constraints3 = bag3.getConstraints(leftPanel);
         //add the message label
         constraints3.gridwidth= 1;
         constraints3.insets = new Insets (2,2,4,2  );
         constraints3.anchor = GridBagConstraints.CENTER;
         constraints3.gridwidth = GridBagConstraints.REMAINDER;
         bag3.setConstraints (message, constraints3);
         leftPanel.add (message );

         //add the column names
         constraints3.gridwidth= colNames.length;
         constraints3.anchor = GridBagConstraints.CENTER;
         constraints3.insets = new Insets (1,1,1,1);
         JLabel label0 = new JLabel (colNames[0]);
         bag3.setConstraints(label0, constraints3);
         leftPanel.add (label0);

         JLabel label1  = new JLabel (colNames[1]);
         constraints3.anchor = GridBagConstraints.EAST;
         bag3.setConstraints (label1, constraints3);
         leftPanel.add (label1);

         JLabel label2 = new JLabel (colNames[2]);
         constraints3.anchor = GridBagConstraints.CENTER;
         bag3.setConstraints (label2, constraints3);
         leftPanel.add (label2);

         JLabel label3 = new JLabel (colNames[3]);
       //  constraints3.gridwidth = GridBagConstraints.RELATIVE;
         bag3.setConstraints (label3, constraints3);
         leftPanel.add (label3);
         JLabel label4 = new JLabel (colNames[4]);
         constraints3.gridwidth = GridBagConstraints.RELATIVE;
         bag3.setConstraints (label4, constraints3 );
         leftPanel.add (label4);
         
         JLabel label5 = new JLabel (colNames[5]);
         constraints3.gridwidth = GridBagConstraints.REMAINDER;
         bag3.setConstraints (label5, constraints3 );
         leftPanel.add (label5);

         Integer index;
         int ii=0;
         for (ControlInformation ci: allInfo){  
             JButton button = new JButton (addIcon);
             button.setSize(29, 29);
             index= new Integer(ii);
             button.putClientProperty ("rowid", index);
             ii++;
             button.addActionListener (new ActionListener () {
                 public void actionPerformed (ActionEvent e){
                     JButton b = (JButton) e.getSource();
                     Integer rowid = (Integer) b.getClientProperty("rowid");
                     ControlInformation ci = allInfo.get(rowid);
                     if (ci != null) {
                        ci = new ControlInformation(ci.detectorName);
                        ci.setRowId(rowid);
                        allInfo.add (rowid, ci);
                    }
                    else{
                        System.out.println ("  could not find the control information for the rowid "+ rowid);
                    }
                    rebuildPanel ();

                 }
             });
             constraints3.anchor = GridBagConstraints.EAST;
             constraints3.gridwidth = colNames.length;
             bag3.setConstraints (button, constraints3);
             leftPanel.add (button);

            // String[] data = ci.copyData();
//             System.out.println (data[0]+ "  "+ data[1] + "  " + data[2]+ "  "+ data[3]);

             JLabel label = new JLabel (ci.detectorName);
             bag3.setConstraints (label, constraints3);
             leftPanel.add (label);
             JTextField tf1 = new JTextField(12);
             tf1.setText (ci.reagent);
             addListenersTo (tf1, ci, "tf1");
             bag3.setConstraints (tf1, constraints3);
             leftPanel.add (tf1);
                

             JTextField tf2 = new JTextField(12);
             tf2.setText (ci.unstainedControlFile);
             addListenersTo (tf2, ci, "tf2");
             bag3.setConstraints (tf2, constraints3);
             leftPanel.add (tf2);

             JTextField tf3 = new JTextField(12);
             tf3.setText (ci.stainedControlFile);
             addListenersTo (tf3, ci, "tf3");
             constraints3.gridwidth = GridBagConstraints.RELATIVE;
             bag3.setConstraints (tf3, constraints3);
             leftPanel.add (tf3);
             
             JCheckBox cb = new JCheckBox();
            // cb.setSelected (false);
             cb.addChangeListener(ci);
             cb.setSelected (ci.compensationCells);
             constraints3.gridwidth = GridBagConstraints.REMAINDER;
             bag3.setConstraints (cb, constraints3);
             leftPanel.add (cb);         


             }
         constraints3.anchor = GridBagConstraints.WEST;
         constraints3.gridwidth = 4;
         for (int i=0; i < 4; i++){
         		JLabel la = new JLabel("              ");
         		bag3.setConstraints(la, constraints3);
         		leftPanel.add(la);
         }	
         
         JCheckBox useUnstained = new JCheckBox("Use one unstained control for all stained controls.");
         useUnstained.putClientProperty("name","useUnstained");
        // useUnstained.setClient
         for (ControlInformation ci: allInfo){
         	useUnstained.addChangeListener (ci);
         }
        /** useUnstained.addChangeListener (new ChangeListener() {
         	public void stateChanged(ChangeEvent e){
         		
         	}
         });**/
          constraints3.gridwidth = GridBagConstraints.REMAINDER;
          bag3.setConstraints(useUnstained, constraints3);
         leftPanel.add(useUnstained);
         

             //leftPanel.validate();
             leftPanel.repaint();
             leftPanel.revalidate();
        }




  class MyFilenameFilter implements FilenameFilter {
        String ext;
        MyFilenameFilter (String ext){
           this.ext = ext;

        }
        public boolean accept (File file, String string) {
            boolean flag = false;
            if (file.isDirectory()  ){
                 if (string.endsWith (ext)){
                     if (string.contains(".chi") || string.contains(".fmo"))
                         flag = false;
                    flag = true;
                 }

            }

            return flag;
        }
    }


    public interface MappingInterface {
        public void putMappingData (String[][] data, File dataFolder);
        public JFrame getFrame();
    }

public static void main (String[] args){
        String[] detectors = { "FITC-A", "Pacific Blue-A", "QDot 605-A","APC-A",
                               "APC-Cy5-5-A", "APC-Cy7-A", "PE-A", "PE-Cy5-A",
                               "PE-CY5-5-A", "PE-Cy7-A"};
        String workingdir = "/Users/cate/FCSData/eliver/CBA-xid";
//       new MyDialog (null, detectors, workingdir);
    }


}
