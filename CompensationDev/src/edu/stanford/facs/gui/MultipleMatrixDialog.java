/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.stanford.facs.gui;

import edu.stanford.facs.controllers.CompensationController;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import edu.stanford.facs.controllers.CompensationController.Multiples;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.ScrollPane;
import java.util.ArrayList;
import javax.swing.JLabel;

/**
 * $Id: MultipleMatrixDialog.java,v 1.3 2012/01/25 01:41:07 beauheim Exp $
 * @author cate
 */
public class MultipleMatrixDialog extends JDialog {

        private Multiples[] multiples;//  0 = primary detector, 1 = reagent.
		private static final long serialVersionUID = 1L;

      
//        private Color lightBlue = new Color (202, 202, 255);
     //   private Color genieBlue = new Color (20, 75, 114);
        private JRadioButton []rbs;
        private CompensationController controller;
        private ButtonGroup[] groups;

    
    MultipleMatrixDialog() {
        super ();
       // this.pairs = createPairs();
        setTitle (" Multiples ");
        createDialog(10);
        setSize (600, 600);
        setVisible (true);
    }
    
    public MultipleMatrixDialog (CompensationController controller, Multiples[] multiples, int sumof){
        super();
        this.multiples = multiples;
        this.controller = controller;
        setTitle (" Multiples ");
        setDefaultCloseOperation (JDialog.DO_NOTHING_ON_CLOSE);
        createDialog (sumof);
        setSize(600, 600);
        setLocation (600, 600);
        setVisible(true);

    }
    
    private void createDialog(int nreagents) {
        
        rbs= new JRadioButton[nreagents];
        int nr=0;
        groups = new ButtonGroup[multiples.length];
        
        
        JComponent panel = new JPanel();
        panel.setOpaque(true); //content panes must be opaque
        
        panel.setLayout (new BorderLayout());
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout (gridbag);
        centerPanel.setBackground (Color.WHITE);
        Border blueBorder = BorderFactory.createMatteBorder (4, 2, 2, 2, Color.LIGHT_GRAY);
        Border insideBorder = BorderFactory.createMatteBorder (4, 6, 4, 4, Color.WHITE);
        Border compoundBorder = BorderFactory.createCompoundBorder (blueBorder, insideBorder);

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


       // panel0.setAlignmentX (SwingConstants.LEFT);
        for (int i=0; i < multiples.length; i++){
            groups[i] = new ButtonGroup();
            String[] rlist = multiples[i].getReagentList();
            ArrayList<Multiples.ReagentEntry> relist = multiples[i].getReagentEntryList();
            constraints.gridwidth = rlist.length + 1;

            
            JLabel label = new JLabel (multiples[i].getDetector());
            label.setBackground (Color.WHITE);
            label.setBorder (compoundBorder);
            label.setHorizontalAlignment(SwingConstants.LEFT);
            constraints.fill = GridBagConstraints.BOTH;
            gridbag.setConstraints ( label, constraints);
            centerPanel.add (label);
            FlowLayout leftFlow = new FlowLayout (FlowLayout.LEFT);
            constraints.weightx=1.0;
           if (rlist != null && rlist.length > 0){
               for (int j=0; j < rlist.length; j++){
//                   System.out.print (i + ", ");
//                   System.out.print ( j + ", ");
//                   System.out.println ( nr);
                   rbs[nr] = new JRadioButton (multiples[i].getDetector() + ": "+ rlist[j].toString());
                   rbs[nr].setHorizontalAlignment(SwingConstants.LEFT);
                  // rbs[nr].setActionCommand (rlist[j].toString());
                   rbs[nr].addActionListener (multiples[i].getActionListener (j));
                   rbs[nr].addItemListener (multiples[i].getItemListener (j));
                   if (rlist.length > 0 && j == 0){
                       rbs[nr].setSelected (true);
                       relist.get(j).setSelected (true);
                   }
                   JPanel p = new JPanel (leftFlow);
                   p.setBackground(Color.WHITE);
                   p.setBorder (compoundBorder);
                   if (j == rlist.length -2)
                       constraints.gridwidth = GridBagConstraints.RELATIVE;
                   else if (j == rlist.length -1)
                       constraints.gridwidth = GridBagConstraints.REMAINDER;
                   gridbag.setConstraints (p, constraints);
                   p.add (rbs[nr]);
                   groups[i].add (rbs[nr++]);
                   centerPanel.add (p);
                   constraints.weightx = 0.0;

//                   if (pairs[i].col == group){
//                       groups[group].add (rbs[i]);
//                   }

               }
           }
           else {
                JPanel p = new JPanel (leftFlow);
                p.setBackground (Color.white);
                constraints.gridwidth =  GridBagConstraints.REMAINDER;
                JLabel blank = new JLabel("");
                gridbag.setConstraints (p, constraints);
                p.add (blank);
                centerPanel.add (p);
                constraints.weightx = 0.0;
           }

       }

        JPanel buttonPanel = new JPanel();

        JButton cancelButton = new JButton (" Cancel ");
        cancelButton.addActionListener (new ActionListener() {
            public void actionPerformed (ActionEvent e){
               setVisible (false);
            }
        });
        JButton createAll = new JButton (" Create Matrix ");


        createAll.addActionListener (new ActionListener() {
            public void actionPerformed (ActionEvent e){
               //  boolean okay = createSelectedList();
//                controller.setCurrentlySelected (currentlySelected);
                controller.getSelections (multiples);

            }
        });
//        JButton createMore = new JButton (" Create another ");
//        createMore.addActionListener (new ActionListener () {
//            public void actionPerformed (ActionEvent e){
//                for (int i=0; i < rbs.length; i++)
//                    rbs[i].setSelected (false);
//
//            }
//        });
        JButton doneButton = new JButton (" Done ");
        doneButton.addActionListener (new ActionListener () {
            public void actionPerformed (ActionEvent e){
                setVisible (false);
            }
        });


//        String sep = System.getProperty("line.separator");
        StringBuilder buf = new StringBuilder();
        buf.append (" Please select one control from each color. ");
        JLabel msgLabel = new JLabel (buf.toString());
   
        buttonPanel.setBorder (compoundBorder);
        buttonPanel.add (createAll);
//        buttonPanel.add (createMore);
        buttonPanel.add (cancelButton);
        buttonPanel.add (doneButton);
        ScrollPane scroll = new ScrollPane();
        panel.add (msgLabel, BorderLayout.NORTH);
        panel.add (buttonPanel, BorderLayout.SOUTH);
        scroll.add (centerPanel);
       // panel.add (centerPanel, BorderLayout.CENTER);
        panel.add (scroll,BorderLayout.CENTER);
        panel.setOpaque (true);
        getContentPane().add (panel);
    }

    public static void main (String[] args){
        MultipleMatrixDialog multipleMatrixDialog = new MultipleMatrixDialog ();
    }

    private boolean createSelectedList () {
        boolean all = true;
        for (int i=0; i < groups.length; i++){
            if (groups[i].getSelection() == null){
                System.out.println (" selection i is null ");
                all = false;
            }
            else {
                System.out.println (" selected button in group "+ i + "  "+ groups[i].getSelection().getActionCommand());
                String s = groups[i].getSelection().getActionCommand();
            }
        }
        return all;
 
    }


/**
 * FITC-A,CD34
FITC-A,cd16
APC-A,C-kit
APC-Cy5-5-A,CD16
APC-Cy7-A,CD11b
PE-A,Flk2
PE-Cy5-A,CD3
PE-Cy5-A,CD5
PE-Cy7-A,CD150
PE-Cy7-A,TNFalpha
APC-Cy7-A,Gr-1
APC-Cy7-A,CD19
PE-Cy7-A,IFN-gamma
APC-Cy5-5-A,CD34
PerCP-Cy5-5-A,CD14
PerCP-Cy5-5-A,Sca-1

 */
}
