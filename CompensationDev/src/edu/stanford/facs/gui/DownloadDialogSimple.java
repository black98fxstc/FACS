/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.stanford.facs.gui;

import edu.stanford.facs.exp_annotation.JoFile;
import edu.stanford.facs.exp_annotation.TubeInfo;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

/**
 * $Id: DownloadDialogSimple.java,v 1.2 2012/01/25 01:41:07 beauheim Exp $
 * @author cate
 */
public class DownloadDialogSimple extends JDialog{
    
    
    private HashMap <String, TubeInfo> tubeList;
    private JoFile parent;
    protected boolean saveFiles = false;
    protected File saveJoFileDirectory;
    
    public DownloadDialogSimple (JoFile parent, HashMap <String, TubeInfo> tubeList){
    
        super (parent.getFrame(), "Download Compensation Files");
        this.parent = parent;
        this.tubeList = tubeList;
        JPanel panel = createList ();
        
        setSize (new Dimension (450, 500));
        if (parent.getFrame() != null)
             setLocationRelativeTo( parent.getFrame());
        else
            setLocation (700, 400);
        add(panel);
        setVisible (true);
    }
    
    private JPanel createList (){
    
        setDefaultCloseOperation (JDialog.HIDE_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.setBackground(Color.LIGHT_GRAY);
        
      //  panel.setOpaque(true); //content panes must be opaque
        
        panel.setLayout (new BorderLayout());
        JLabel label = new JLabel ("Tubes in this Experiment");
        label.setAlignmentX (SwingConstants.LEFT);
       
        JPanel checkPanel = new JPanel();
        checkPanel.setLayout (new GridLayout (0,1));
        Collection ctubes = tubeList.values();
        Iterator it = ctubes.iterator();
        
        
        
        while (it.hasNext()){
            final TubeInfo tube = (TubeInfo) it.next();
       System.out.println ("  list "+ tube.getTubeName());     
            JCheckBox cb = new JCheckBox (tube.getTubeName());
            cb.addItemListener (new ItemListener(){
               public void itemStateChanged (ItemEvent e){
                   if (e.getStateChange() == ItemEvent.SELECTED)  {
                       tube.isSelected = true;   
                   }
                   else tube.isSelected = false;
                  
               } 
        });
            
            checkPanel.add (cb);
            
            
        }
        JScrollPane pane = new JScrollPane(checkPanel);
        pane.setBackground(Color.LIGHT_GRAY);
     
       // listModel.addListDataListener ((ListDataListener) list);
        panel.add (label, BorderLayout.NORTH);
        panel.add (pane, BorderLayout.CENTER);
        JPanel lowerpanel = new JPanel ();
        lowerpanel.setBackground (Color.LIGHT_GRAY);
        JButton cancelbutton = new JButton("CANCEL");
        cancelbutton.addActionListener (new ActionListener() {
            public void actionPerformed (ActionEvent e){
                setVisible (false);
            }
        });
        JButton downloadb = new JButton ("DOWNLOAD");
        downloadb.addActionListener (new ActionListener() {
            public void actionPerformed (ActionEvent e){
               System.out.println ("  Download simple:  Selected tubes: ");
               Collection <TubeInfo> tubes = tubeList.values();
               Iterator it = tubes.iterator();
               
               while (it.hasNext()){
                   TubeInfo one = (TubeInfo) it.next();
                   if (one.isSelected ){
                       System.out.println (one.getTubeName() );
                   }
               }
               parent.returnSelections(tubeList, saveJoFileDirectory);
               setVisible (false);
            }
        });
        JCheckBox saveFilesCB = new JCheckBox ("Save FCS files?");
        saveFilesCB.addItemListener (new ItemListener () {
            public void itemStateChanged (ItemEvent e){
                if (e.getStateChange() == ItemEvent.SELECTED){
                    saveFiles = true;
                    String osName = System.getProperty("os.name");
                    if (osName.equalsIgnoreCase("mac os x")){
                        String newdir = dialogForDirectories();  
                        saveJoFileDirectory = new File(newdir);
                    }
                    else {
                        saveJoFileDirectory = showOpenDialog ("Save the FCS Files locally",
                        new JFileChooser(), JFileChooser.DIRECTORIES_ONLY);
                    }
                    parent.setSaveFiles (saveFiles);
                }
                
                 
            }
        });
        lowerpanel.add (saveFilesCB);
        lowerpanel.add (cancelbutton);
        lowerpanel.add (downloadb);
        
        panel.add (lowerpanel, BorderLayout.SOUTH);
        return panel;
 
    }
    private  File showOpenDialog(String title, JFileChooser fileChooser, int mode){

        fileChooser.setDialogTitle(title);
        fileChooser.setFileSelectionMode (mode);

        if (fileChooser.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)
          return null;
        else
          return fileChooser.getSelectedFile();
  }
    
    private String dialogForDirectories() {
        String dirname=null;
        FileDialog chooser = new FileDialog (this, "Select Folder ");
        System.setProperty ("apple.awt.fileDialogForDirectories", "true");
        chooser.setVisible(true);
        
        System.setProperty ("apple.awt.fileDialogForDirectories", "false");
        if (chooser.getDirectory() != null){
            dirname = chooser.getDirectory();
            dirname +=chooser.getFile();
            
        //    jTextField1_TargetFolder.setText (dirname);
        }
        return dirname;
    }
    
    
}
