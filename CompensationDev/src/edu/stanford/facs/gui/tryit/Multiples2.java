/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.stanford.facs.gui.tryit;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.ArrayList;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.MatteBorder;
/*0  FITC-A,CD34
  0 FITC-A,cd16
  1 APC-A,C-kit
  2 APC-Cy5-5-A,CD16
  3 APC-Cy7-A,CD11b
  4 PE-A,Flk2
  5 PE-Cy5-A,CD3
  5 PE-Cy5-A,CD5
  6 PE-Cy7-A,CD150
  6 PE-Cy7-A,TNFalpha
  6 PE-Cy7-A,IFN-gamma
  7 APC-Cy7-A,Gr-1
  7 APC-Cy7-A,CD19
 
  8 APC-Cy5-5-A,CD34
  9 PerCP-Cy5-5-A,CD14
  9 PerCP-Cy5-5-A,Sca-1*/
/**
 * $Id: Multiples2.java,v 1.1 2011/11/09 19:49:46 beauheim Exp $
 * @author cate
 */
public class Multiples2 extends JDialog {

//        private Multiples[] multiples;//  0 = primary detector, 1 = reagent.
//        private Multiples[] currentlySelected;
        private int npanels = 0;
        private Color lightBlue = new Color (102, 102, 255);
        private Color lightGreen = new Color (102, 255, 102);
        private Color lighterGreen = new Color (200, 255, 200);
        private Color lighterBlue = new Color (200, 200, 255);
        private Color genieBlue = new Color (20,75,114);
        private JRadioButton []rbs;
        ButtonGroup []groups;
        

//        private CompensationController controller;
    String[] detectors = {"FITC-A", "APC-A", "APC-Cy5-5-A", "APC-Cy7-A","PE-A", "PE-Cy5-A",
                      "PE-Cy7-A", "APC-Cy7-A",  "PE-Texas-Red-A", "APC-Cy5-5-A", "PerCP-Cy5-5-A"};

       ArrayList[] reagents = new ArrayList[detectors.length];
    
    Multiples2() {
        super ();
       // this.pairs = createPairs();
        setTitle (" Multiples ");
       
        createData();
        createDialog(16);
        setSize (400, 600);
        setVisible (true);
    }

    private void createData() {
        reagents[0] = new ArrayList<String>();
        reagents[0].add ("CD34");
        reagents[0].add("CD16");
        reagents[1] = new ArrayList<String>();
        reagents[1].add ("C-kit");
        reagents[2] = new ArrayList<String>();
        reagents[2].add ("CD18");
        reagents[3] = new ArrayList<String>();
        reagents[3].add ("CD11b");
        reagents[4] = new ArrayList<String>();
        reagents[4].add("Flk2");

        reagents[5] = new ArrayList<String>();
        reagents[5].add ("CD3");
        reagents[5].add ("CD5");
        reagents[6] = new ArrayList<String>();
        reagents[6].add ("CD150");
        reagents[6].add ("TNFalpha");
        reagents[6].add ("IFN-gamma");
        reagents[7] = new ArrayList<String>();
        reagents[7].add ("GR-1");
        reagents[7].add ("CD19");
        reagents[8] = new ArrayList<String>();
        reagents[8].add ("");
       
        reagents[9] = new ArrayList<String>();
        reagents[9].add ("CD34a");
        reagents[10] = new ArrayList<String>();
        reagents[10].add ("CD14");
       

    }
    
    
    
    private void createDialog(int nreagents) {
        
        rbs= new JRadioButton[nreagents];
        groups = new ButtonGroup[detectors.length];
        System.out.println (" How many different panels?  " + detectors.length);
        
        setDefaultCloseOperation (JDialog.HIDE_ON_CLOSE);  // change this later
        JComponent panel = new JPanel();
        panel.setOpaque(true); //content panes must be opaque
        
        panel.setLayout (new BorderLayout());
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout (gridbag);
        centerPanel.setBackground (Color.WHITE);
        Border blueBorder = BorderFactory.createMatteBorder (4, 2, 2, 2, genieBlue);
        Border insideBorder = BorderFactory.createMatteBorder (4, 6, 4, 4, Color.WHITE);
        Border compoundBorder = BorderFactory.createCompoundBorder (blueBorder, insideBorder);
        int n=0;
        JLabel detLabel = new JLabel ("Channel");
        detLabel.setHorizontalAlignment (SwingConstants.LEFT);
        detLabel.setBackground (Color.WHITE);
        detLabel.setBorder (compoundBorder);
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.BOTH;
        gridbag.setConstraints (detLabel, constraints);
        centerPanel.add (detLabel);

        JLabel regLabel = new JLabel ("Reagents");
        regLabel.setHorizontalAlignment (SwingConstants.LEFT);
        regLabel.setBackground (Color.WHITE);
        regLabel.setBorder (compoundBorder);
        constraints.gridwidth = GridBagConstraints.REMAINDER;

        gridbag.setConstraints (regLabel, constraints);
        centerPanel.add (regLabel);
       
        for (int i=0; i < detectors.length; i++){
            groups[i] = new ButtonGroup();
          //  constraints.gri
            ArrayList <String> list = reagents[i];
            constraints.gridwidth = list.size() + 1;
            
            JLabel label = new JLabel (detectors[i]);
            label.setBackground (Color.WHITE);
            label.setBorder (compoundBorder);
            label.setHorizontalAlignment (SwingConstants.LEFT);

        
            constraints.fill= GridBagConstraints.BOTH;
            gridbag.setConstraints (label, constraints);
            centerPanel.add (label);
            FlowLayout flow = new FlowLayout(FlowLayout.LEFT);
            
             constraints.weightx = 1.0;


                for (int j=0; j < list.size(); j++ ){
                    rbs[n] = new JRadioButton (list.get(j));
                    rbs[n].setActionCommand (list.get(j).toString());

                    JPanel p = new JPanel(flow);
                    p.setBackground (Color.WHITE);
                    rbs[n].setHorizontalAlignment (SwingConstants.LEFT);
                    p.setBorder (compoundBorder);
                
                    if ( j == list.size()-2)
                        constraints.gridwidth = GridBagConstraints.RELATIVE;
                    else if (j == list.size() -1)
                        constraints.gridwidth = GridBagConstraints.REMAINDER;
                    if (list.size() == 1)
                        rbs[n].setSelected(true);
                    gridbag.setConstraints (p, constraints);
                    groups[i].add (rbs[n]);
                    p.add(rbs[n]);
                    centerPanel.add (p);
                    constraints.weightx = 0.0;
                    n++;
                }


          //  }

        }
        JButton createMore = new JButton (" Create another ");
        createMore.addActionListener (new ActionListener () {
            public void actionPerformed (ActionEvent e){
                createSelectedList();
//                for (int i=0; i < groups.length; i++){
//                    ButtonModel bm = groups[i].getSelection();
//                    System.out.print (" group "+ i + ". "+  "  ");
//                    System.out.println (bm.getActionCommand() );
//
//                }

            }
        });
        JPanel bpanel = new JPanel();
        bpanel.setBorder (compoundBorder);
        bpanel.add (createMore);
        panel.add (bpanel, BorderLayout.SOUTH);
        panel.add (centerPanel, BorderLayout.CENTER);
        getContentPane().add (panel);
       
    }

     private void createSelectedList () {
        boolean all = true;
        for (int i=0; i < groups.length; i++){
            if (groups[i].getSelection() == null){
                System.out.println (" selection i is null ");
                all = false;
            }
            else {
                System.out.println (" selected button in group "+ i + "  "+ groups[i].getSelection().getActionCommand());
                String s = groups[i].getSelection().getActionCommand();
                System.out.println (s);
            }
            if (all == false){
                System.out.println ("  Make a message");
            }


        }


    }


    public static void main (String[] args){

        new Multiples2 ();
    }


}
