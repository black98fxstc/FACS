/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.stanford.facs.gui;

import javax.swing.JPanel;

import java.awt.Dimension;
import javax.swing.JList;

import edu.stanford.facs.compensation.Diagnostic;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

/**
 * $Id: DiagnosticPanel.java,v 1.12 2012/01/25 01:41:07 beauheim Exp $
 * @author cate
 */
public class DiagnosticPanel extends JPanel  {
    JList jlist;
    DefaultListModel model;
    StringBuilder buf;
    MyCellRenderer renderer;
    String[] msgs={"ABCDEF GHIJ  KLMN OPQR STUV WXYZ", 
            "AB CDEF G IJ KLMN OPQR STUV WXYZABCDEF GHIJ KLMN OPQR STUV WXYZ",
            "123456 789 10110 11 12 13 141 5 23 34 1819 1919029 29393940 293049"
        };
    String [] msg2={"One message", "Two Message", "Three Message", "Four Message"};
    int n=0;
   
	private static final long serialVersionUID = 1L;
	private JScrollPane scroll;



    DiagnosticPanel(Dimension dim) {
        super(new BorderLayout());
        setSize (dim.width, dim.height);
       // setBorder (BorderFactory.createEmptyBorder (6, 12, 6, 12));
        init(dim);
//        diagnostics = ListofDiagnostics.getInstance();
    }

    private void init(Dimension dim) {
        model = new DefaultListModel();
        jlist = new JList(model);
        renderer = new MyCellRenderer();
        jlist.setVisibleRowCount (20);
        jlist.setCellRenderer(renderer);
        jlist.setPrototypeCellValue(msgs);
        scroll = new JScrollPane(jlist );
        scroll.setPreferredSize(dim);
        scroll.setVerticalScrollBarPolicy (ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
       
        JLabel label = new JLabel ("Diagnostics");

        add (label, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        
        
    }

    /**
     * Cell selected
     * @param msgs
     */
    protected void addSimpleList (Diagnostic.List msgs){
       // jlist.removeAll();
        model.clear();
        //System.out.println ("  Cell selected--  AddSimpleList.  how many messages "+ msgs.size());
        if (msgs != null && msgs.size()>0) {
            for (int i=0; i < msgs.size(); i++)
               model.addElement (msgs.get(i));

        }
        jlist.ensureIndexIsVisible(0);
       // repaint();
    }
    
    /*
     * this was for testing.
     */
    protected void addSimpleList (String[] msg){
        model.clear();
       // System.out.println ("cell selected. how many messages?  "+ msg.length);
        if (msg != null && msg.length > 0){
            for (String s: msg){
            	//System.out.println (s);
                model.addElement (s);
            }
            repaint();
        }
        jlist.ensureIndexIsVisible(0);
//        System.out.println (model.getSize());
    }


   
    /**
     * Column Selected
     * @param msgs
     */
    protected void addDiagnosticsToList2 (Diagnostic.List[] msgs){
    	//System.out.println ("addDiagnosticsToList2 for column selection. ");
        for (int i=0; i < msgs.length; i++){
            if (msgs[i] != null){
                for (int j=0; j < msgs[i].size(); j++){
                  // System.out.println ("column selection " + msgs[i].get(j).toString());
                        model.addElement (msgs[i].get(j));
                }
            }
        }
        jlist.ensureIndexIsVisible(0);
        repaint();
    }


/**
 * called when a row is selected.
 * @param msgs
 */
    protected void appendDiagnosticList (Diagnostic.List msgs){
    	//System.out.println ("append DiagnosticList ");
        for (Diagnostic dia: msgs){            
            model.addElement (dia);
        }
        jlist.ensureIndexIsVisible(0);
        repaint();
    }

    protected void clearDiagnosticList() {
         model.clear();
//         jlist.removeAll();
         repaint();
    }
    
    class MyCellRenderer extends JTextArea implements ListCellRenderer {
		private static final long serialVersionUID = 1L;

         public MyCellRenderer() {
             super (2, 32);
             setOpaque(true);
           //  System.out.println ("MyCell Renderer Preferred Size  " +getPreferredSize());
           //  System.out.println ("MyCell Renderer Minimum size "+ getMinimumSize());
           //  System.out.println ("MyCell Renderer maximum size "+ getMaximumSize());
             setMaximumSize (getPreferredSize());
         }

     public Component getListCellRendererComponent(JList list,
                                                   Object value,
                                                   int index,
                                                   boolean isSelected,
                                                   boolean cellHasFocus) {
         Color foreground, background;
         JList.DropLocation dropLocation = list.getDropLocation();
         setLineWrap (true);
         setWrapStyleWord(true);
         setBorder (BorderFactory.createLineBorder (Color.LIGHT_GRAY, 1));
         if (dropLocation != null
                 && !dropLocation.isInsert()
                 && dropLocation.getIndex() == index) {

             background = Color.BLUE;
             foreground = Color.WHITE;

         // check if this cell is selected
         } else if (isSelected) {
             background = Color.GRAY;
             foreground = Color.WHITE;

         // unselected, and not the DnD drop location
         } else {
             background = Color.WHITE;
             foreground = Color.BLACK;
         };

         setBackground(background);
         setForeground(foreground);
//         System.out.println (value.getClass().getName());
         
         setText (  value.toString());
         return this;
     
     }

    }
    public static void main (String[] args){
        final String[] msg= {"abcdefghijkl mnopq rstuvwxyz--abcdefghijkl mnopqrstu vwxyz--abcdefghi jklmnopqrstuvwxyz",
            "wxyz---abcd efghijklmn opqrstuvwxyz---abcdefghijklmnopqrstuvwxy z---abcdefghijklmnopqrs tuvwxyz",
            "------abcd efghijklmn opqrstuvwxyz---abcdefghijklmnopqrstuvwxy z---abcdefghijklmnopqrs tuvwxyz-------",

            "amn opqrstuvwxyz===abcde fghijklmnopqrstuvwxyz===abcdef ghijklmnopqrst uvwxyz"};
        
        
        SwingUtilities.invokeLater(new Runnable() {
          public void run ()
          {
            try
            {
              UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
              JFrame frame = new JFrame();
              frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
              Dimension dim = new Dimension(300, 400);
              DiagnosticPanel panel = new DiagnosticPanel(dim);
              panel.addSimpleList (msg);
              frame.getContentPane().add (panel);
              frame.setSize(dim);
              frame.setVisible(true);
              panel.addSimpleList (msg);
              
            }
            catch (Exception e)
            {
              e.printStackTrace();
              System.exit(0);
            }
          }
        });
    }
    }
    




