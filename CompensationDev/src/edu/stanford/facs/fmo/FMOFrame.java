/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.stanford.facs.fmo;

import com.apple.eio.FileManager;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.UIManager;

//import edu.stanford.facs.fmo.FMOAnalysis.FMOWorker;


/**
 * $Id: FMOFrame.java,v 1.13 2011/06/17 00:38:01 beauheim Exp $
 * @author cate
 */
public class FMOFrame extends JFrame  {
    
  File workingDirectory;
  File spilloverFile;
  final File xmlFile;
  final Preferences preferences = Preferences.userNodeForPackage(this.getClass());
  
  private final JFileChooser fileChooser = new JFileChooser();
  private final JButton printMatrix;
  private  FMOAnalysis analysis;
  private double[][] adjMatrix;
  private String[] detectorNames;
  private String exp_name;
  private JProgressBar progressBar;
  private ProgressListener progressListener;
  private JButton exitButton;

  public FMOFrame ()
  {
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    printMatrix = new JButton("Write matrix as FlowJo File ");
    printMatrix.setEnabled(false);
    // instrumentProp = showOpenDialog ("Choose Instrument Property File",
    // JFileChooser.FILES_AND_DIRECTORIES);
    JPanel basePanel = new JPanel();
    JPanel panel = new JPanel();

    getContentPane().add(basePanel);
    basePanel.setLayout(new BorderLayout());
    JPanel progressPanel = new JPanel();
    progressBar = new JProgressBar();
    progressBar.setValue(0);
    progressPanel.add(progressBar);
    progressListener = new ProgressListener(progressBar);

    basePanel.add(progressPanel, BorderLayout.NORTH);
    basePanel.add(panel, BorderLayout.CENTER);
 

    printMatrix.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e){
        File adjMatrixfile = showSaveDialog("Write the Adjusted Matrix to a File");
        writeMatrixToFile(adjMatrixfile.getPath());
      }
    });
    JPanel bottompanel = new JPanel();
    basePanel.add(bottompanel, BorderLayout.SOUTH);
    exitButton = new JButton("Done");
    exitButton.addActionListener(new ActionListener(){
      public void actionPerformed (ActionEvent e){
        System.exit(0);
      }
    });
    exitButton.setEnabled(false);
    bottompanel.add(printMatrix);
    bottompanel.add(exitButton);

    String p = preferences.get("spillover", null);
    System.out.println (p + "  this is the saved spillover matrix or something ");
    if (p != null) {
      workingDirectory = new File(p).getParentFile();
    }
    else {
        System.out.println (" Preference for spillover is null");
    }
    //select the working directory.  Save this information also.
    workingDirectory = showOpenDialog("Choose Working Data Directory",
                                        workingDirectory,
                                        JFileChooser.DIRECTORIES_ONLY);
    
    if (workingDirectory.exists() && workingDirectory.isDirectory()
      && workingDirectory.canRead()){

      FilenameFilter filter = new FilenameFilter(){
        public boolean accept (File dir, String name){
          return name.endsWith(".xml");
        }
      };
      File[] xmlFiles = workingDirectory.listFiles(filter);
      if (xmlFiles.length == 1){
        xmlFile = xmlFiles[0];
      }
      else{
        throw new IllegalArgumentException(
          "We were expecting just one xml file. ");
      }
    }
    else
      throw new IllegalArgumentException("Bad working directory");
    preferences.put("workingfmo", workingDirectory.getPath());
    System.out.println ("fmo preferences" + preferences.get("workingfmo", null));

    p = preferences.get("spillover", null);
    if (p != null)
      spilloverFile = new File(p);
    spilloverFile = showOpenDialog("Choose Matrix File", spilloverFile,
                                    JFileChooser.FILES_AND_DIRECTORIES);
    if (spilloverFile != null)
      preferences.put("spillover", spilloverFile.getPath());

    File[] fcsfiles = showOpenDialogMulti ("Select the FCS Files ", workingDirectory, JFileChooser.FILES_AND_DIRECTORIES);

    panel.add(new JTextField("Working directory:  "
      + workingDirectory.getName(), 20));
    panel.add(new JTextField("Matrix file : " + spilloverFile.getName(), 20));

    pack();
    setVisible(true);
    
    analysis = new FMOAnalysis(this);
    analysis.setFCSFiles (fcsfiles);
    analysis.worker.addPropertyChangeListener(progressListener);
    analysis.worker.execute();
  }

  public void setButtonEnabled (boolean flag){
        exitButton.setEnabled (true);
        printMatrix.setEnabled (true);
    }

    public void matrixReady (double[][] adjMatrix, String[] detectorNames, String exp_name) {
        printMatrix.setEnabled( true);
        this.detectorNames = detectorNames;
        this.adjMatrix = adjMatrix;
        this.exp_name = exp_name;
    }

    public File getMatrixFile () {
        return spilloverFile;
    }

 

    public File showOpenDialog(String title, File last, int mode){

        fileChooser.setFileSelectionMode (mode);
        fileChooser.setDialogTitle(title);
        if (last != null)
           fileChooser.setCurrentDirectory(last.getParentFile());
//        fileChooser.setSelectedFile(last);
        int res = fileChooser.showOpenDialog (this);
        if (JFileChooser.CANCEL_OPTION == res){
          JOptionPane.showMessageDialog (this, "No File was Selected", "Fatal Error", JOptionPane.ERROR_MESSAGE);
          System.out.println("NO FILE CHOSEN -- The program is closing.");
          System.exit(1);
          return null;
        }
        else
          return fileChooser.getSelectedFile();
  }

    public File[] showOpenDialogMulti (String title, File lastdir, int mode){

        fileChooser.setFileSelectionMode (mode);
        fileChooser.setDialogTitle(title);
        fileChooser.setMultiSelectionEnabled (true);
        MyFilenameFilter filter = new MyFilenameFilter ("fcs", "fmo");
        fileChooser.setFileFilter ( (javax.swing.filechooser.FileFilter) filter);
        if (lastdir != null)
            fileChooser.setCurrentDirectory (lastdir);
        int res = fileChooser.showOpenDialog(this);
        if (JFileChooser.CANCEL_OPTION == res){
            JOptionPane.showMessageDialog (this, "No File was Selected", "Fatal Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            System.out.println (" NO FILES CHOOSEN -- The program is closing.");
            return null;
        }
        
        else
          return fileChooser.getSelectedFiles();

    }

    public File showSaveDialog (String title){
        fileChooser.setDialogTitle (title);
        fileChooser.setCurrentDirectory(workingDirectory);
        fileChooser.setDialogType (JFileChooser.SAVE_DIALOG);
        int ans = fileChooser.showDialog (this, "Save");       
        if (ans == JFileChooser.APPROVE_OPTION){
            File f = fileChooser.getSelectedFile();
            String name = f.getPath() + File.separator + f.getName();
           
            return f;
        }
        else {
            System.out.println ("  else nothing is working ");
            return null;
        }
    }


    /*
     * Write the adjusted matrix out as a Flow Jo Matrix.
     * Here is what one looks like:::
        090806 BeadComp12A,,,,,,,,,,,
        <,>,,,,,,,,,,
        FITC-A,Pacific Blue-A,Violet Green-A,Pacific Orange-A,Qdot 605-A,APC-A,APC-Cy7-A,PE-A,PE-Texas-Red-A,PE-Cy5-A,PE-Cy55-A,PE-Cy7-A
        1,-0.005,0.049,0.055,0.014,0,0,0.022,0.005,0.001,0.001,0
        0,1,0.124,0.062,0.014,0,0,0,0,0,0,0
        0,0,1,0,0,0,0,0,0,0,0,0
        0.001,-0.003,0.255,1,0.608,0,0.002,0,0,0,0,0.001
        0,0.038,0.004,0.008,1,0,0,0.066,0.219,0.002,0,0
        0,0,0,0,0,1,0,0,0,0,0,0
        0,-0.001,-0.001,-0.001,-0.001,0.032,1,0,0,0.004,0.004,0.087
        0.004,-0.019,-0.007,0.0481,0.0332,0,0,1,0.195,0.049,0.034,0.006
        0.001,0,0.000,0.007,0.092,0.003,0.001,0.683,1,0.954,1.041,0.369
        0.002,-0.001,0,0.001,0.001,0.917,0.176,0.061,0.015,1,1.102,0.303
        0.001,0,0,0.008,0.005,0.033,0.066,0.226,0.045,0.042,1,0.323
        0,0,0,0,0,0,0,0,0,0,0,1
     */
    private void writeMatrixToFile (String fn) {
       
        int nc = detectorNames.length-1;
        String nl = System.getProperty ("line.separator");
        if (fn != null){
            
      //  if (adjMatrixfile.canWrite()){
            try {
                PrintWriter fw = new PrintWriter (new BufferedWriter(new FileWriter(fn)));
                
                fw.write(exp_name);
                for (int i=0; i < nc; i++){
                    fw.write(',');
                }
                fw.write(nl);
                fw.write("<,>");
                for (int i=0; i< nc-1; i++){
                    fw.write(',');
                }
                fw.write (nl);
                fw.write (detectorNames[0]);
                for (int i=1; i < detectorNames.length; i++){
                    fw.write(',' );
                    fw.write (detectorNames[i]);
                }
                fw.write(nl);
                int dim = adjMatrix.length;
                for (int i=0; i < dim; i++){
                    for (int j=0; j < dim-1; j++){
                        fw.write (Double.toString(adjMatrix[i][j]));
                        fw.write (',');
                    }
                    fw.write (Double.toString(adjMatrix[i][dim-1]));
                    fw.write (nl);
                }
                fw.close();
                
                String os = System.getProperty ("os.name");
                if (os.equalsIgnoreCase ("Mac OS X")){
                    int t = makeOSType ("TEXT");
                    int c = makeOSType ("ttxt");
                    System.out.println (" Text = " + t + ", creator = "+ c);
                    FileManager.setFileTypeAndCreator (fn, t, c);
                }

            } catch (IOException e){
                System.out.println (" IO Exception " + e.getMessage());
            }
        }
    }

    private static int makeOSType (String type) {
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
                progressBar.setValue(progress);
            }
        }

        private JProgressBar progressBar;
    }

    /**
     * MyFilenameFilter will filter files by the fcs extension and will not
     * accept those files that also contain the notname.  In this case, those
     * with *.fcs and not *fmo*.fcs
     */
    class MyFilenameFilter extends javax.swing.filechooser.FileFilter {
        private String ext;
        private String notMatch;

        MyFilenameFilter (String ext, String notname){
           this.ext = ext;
           this.notMatch= notname;

        }
        public String getDescription (){
            return ext + " Files";
        }


        //override
        public boolean accept (File file) {
            boolean flag = false;
            if (file.isFile()){
                if (file.getName().endsWith (ext)){
                    if (notMatch == null)
                        flag = true;
                    else if (notMatch != null && !file.getName().contains(notMatch))
                        flag = true;

                }
            }
            return flag;
        }
    }

  public static void main (String[] args)
  {
    javax.swing.SwingUtilities.invokeLater(new Runnable()
    {
      public void run ()
      {
        try
        {
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
          new FMOFrame();
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
    });

  }
}
