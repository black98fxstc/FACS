/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.stanford.facs.gui;


import edu.stanford.facs.exp_annotation.TubeInfo;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.Document;
/** !!!! Current this is not being used !!!! **/
/**
 * $Id: DetectorPanel.java,v 1.3 2012/01/25 01:41:07 beauheim Exp $
 * @author cate
 */
public class DetectorPanel extends JPanel{
    private String[] colNames = {"Add Row", "Detector", "Reagent",
                               "Unstained FCSFile ",
                               "Stained FCSFile"};
    private final URL addURL = FCSFileDialog.class.getResource ("/edu/stanford/facs/gui/add.png");
    private final ImageIcon addIcon = new ImageIcon (addURL);
    private JTextField tf1, tf2, tf3;
    private ControlInformation[] allInfo;
    private HashMap <String, TubeInfo> tubeMap;
    
    DetectorPanel (ControlInformation[] controlInfo) {
        super();
        allInfo = controlInfo;
        
    }
    
    public void addTubeInformation (HashMap <String, TubeInfo> tubeMap){
        this.tubeMap = tubeMap;
    }
    
    public void constructPanel (String[] detectors, String[] fluorochromeList) {
        
        
        GridBagLayout bag = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridwidth = 1;
        constraints.insets = new Insets(2,2,4,2 );
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.weightx = 0.0;
        setLayout (bag);
        setBorder (BorderFactory.createEmptyBorder(4, 4, 4, 4));
        constraints.gridwidth = GridBagConstraints.REMAINDER;
//        bag.setConstraints (message, constraints);
//        add (message);

        constraints.gridwidth= colNames.length;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.insets = new Insets (1,1,1,1);
        JLabel label0 = new JLabel (colNames[0]);
        bag.setConstraints(label0, constraints);
        add (label0);

        JLabel label1  = new JLabel (colNames[1]);
        constraints.anchor = GridBagConstraints.EAST;
        bag.setConstraints (label1, constraints);
        add (label1);

        JLabel label2 = new JLabel (colNames[2]);
        constraints.anchor = GridBagConstraints.CENTER;
        bag.setConstraints (label2, constraints);
        add (label2);

        JLabel label3 = new JLabel (colNames[3]);
        constraints.gridwidth = GridBagConstraints.RELATIVE;
        bag.setConstraints (label3, constraints);
        add (label3);
        JLabel label4 = new JLabel (colNames[4]);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        bag.setConstraints (label4, constraints );
        add (label4);
        for (int i=0; i < detectors.length; i++){
            if (detectors[i].startsWith("<") && detectors[i].endsWith(">"))
                detectors[i] = detectors[i].substring (1, detectors[i].length()-1);
      
            JButton button = new JButton (addIcon);
            button.setSize (29, 29);
            button.putClientProperty ("rowid", new Integer(i));
            button.addActionListener (new ActionListener() {
               
                public void actionPerformed (ActionEvent e){
                   
                    JButton b = (JButton) e.getSource();
                    Integer rowid = (Integer) b.getClientProperty ("rowid");
                    System.out.println ("JButton addIcon action performed " + rowid);
                }
            });

     
            constraints.gridwidth = colNames.length;
            constraints.anchor = GridBagConstraints.WEST;
            bag.setConstraints (button, constraints);
            add (button);
            JLabel label = new JLabel (detectors[i]);
            constraints.anchor = GridBagConstraints.EAST;
            bag.setConstraints (label, constraints);
            add (label);
            tf1 = new JTextField (12);
            if (fluorochromeList != null ){
                tf1.setText (fluorochromeList[i]);
                
            }
            bag.setConstraints(tf1, constraints);
            add (tf1);
            tf2 = new JTextField(12);
            constraints.gridwidth = GridBagConstraints.RELATIVE;
            bag.setConstraints (tf2, constraints);
            add (tf2);
            tf3 = new JTextField (12);
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            bag.setConstraints (tf3, constraints);
            add (tf3);
            addListenersTo (tf1, allInfo[i],"tf1");
            addListenersTo (tf2, allInfo[i],"tf2");
            addListenersTo (tf3, allInfo[i],"tf3");
            
        }
    
        
    }
    public String[][] getMappingInfo () {

         int n = allInfo.length;
         int m = 4;
         String[][] data = new String[n][m];
      
         for (int i=0; i < n; i++){
             ControlInformation one = allInfo[i];
//             System.out.println ("getMappingInfo "+ allInfo.size());
             
             data[i] = one.getData();
             
             System.out.println ("Get MappingInformation -- detectorPanel " + data[i][0] + ", "+ data[i][1]+", "+data[i][2]+", "+data[i][3]);
         }
        
         return data;
     }

    
    
    
    public JPanel createVerticalBoxPanel() {
        
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.PAGE_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        return p;
    
    }
    
    public void addTextfieldListener (ControlInformation ci){
        
    }
    private void addListenersTo (JTextField tf, ControlInformation ci, String name){
//        System.out.println (" add listeners to "+ name);
        tf.addActionListener (ci);
        Document doc = tf.getDocument();
        doc.putProperty (Document.TitleProperty, name);
        doc.addDocumentListener (ci);

    }
    
}

