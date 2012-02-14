/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.stanford.facs.gui;

import edu.stanford.facs.exp_annotation.TubeInfo;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.util.HashMap;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

/**
 * $Id: ControlInformation.java,v 1.3 2012/01/25 01:41:07 beauheim Exp $
 * @author cate
 */

public class ControlInformation implements ActionListener, TextListener, DocumentListener  {
        //0 is the detectorName, 1 is the reagent, 2 is unstained file, 3 is stained file, 4 is the tube name
        String detectorName;
        String reagent;
        String unstainedControlFile, stainedControlFile;
        TubeInfo unstainedTube, stainedTube;
        boolean compensationCells = false;
       
        String unstainedTubeName, stainedTubeName;
        
       
      
        
    
        int nfields=5;

        
        ControlInformation (String[] tokens ) throws IllegalArgumentException {
            if (tokens == null || tokens.length < 0 )
                throw new IllegalArgumentException();
            if (tokens[0] != null)
                this.detectorName = tokens[0];


            reagent = tokens[1];
            unstainedControlFile = tokens[2];
            stainedControlFile = tokens[3];
            if (tokens.length == 5)
                stainedTubeName = tokens[4];
          
        }

        ControlInformation (String detectorName, String reagent){
            this.detectorName = detectorName;
            this.reagent = reagent;
        }

        //picking my own controls leads me here
        ControlInformation (String detectorName){
            this.detectorName = detectorName;
        }
        
        

        public String[] copyData () {
            String[] copy = new String[nfields];

            if (detectorName != null)
                copy[0] = detectorName;
            else copy[0] = "";
            copy[1] = reagent;
            
//            if (tubeMap.containsKey (unstainedControlFile))
//                copy[2] = tubeMap.get(unstainedControlFile).getFcsFilename();
//            else
//                copy[2] = unstainedControlFile;
         //   copy[2] = tubeAndFiles.get(unstainedControl);
            
            copy[2] = unstainedControlFile;
           // if (tubeAndFiles.containsKey (stainedControlFile))
                
                
            copy[3] = stainedControlFile;
            copy[4] = stainedTubeName;

            return copy;
        }
        
        protected void addStainedTube (TubeInfo tube){
            stainedTubeName = tube.getTubeName();
            stainedControlFile = tube.getFcsFilename();
            stainedTube = tube;
        }
        
        protected void addUnstainedTube (TubeInfo tube){
            unstainedTubeName = tube.getTubeName();
            unstainedControlFile = tube.getFcsFilename();
            unstainedTube = tube;
        }

         protected void addValues (String[] tokens){
             Document doc = null;
             reagent = tokens[1];
             unstainedControlFile = tokens[2];
             stainedControlFile = tokens[3];
             if (tokens.length ==5)
                 stainedTubeName = tokens[4];

         }
         
        public void actionPerformed (ActionEvent e){

             if (e.getSource() instanceof JTextField){
                JTextField tf = (JTextField) e.getSource();

                String title = (String) tf.getDocument().getProperty (Document.TitleProperty);
                if (title.equals ("tf1"))
                    reagent = tf.getText().trim();
                else if (title.equals ("tf2"))
                    unstainedControlFile = tf.getText().trim();
                else if (title.equals ("tf3"))
                    stainedControlFile = tf.getText().trim();
                else
                    System.out.println (" Unknown title in actionPerformed "+ title);
                }


        }


        public void textValueChanged (TextEvent e){
            System.out.println ("textValue Changed " + e.getSource());
        }

        //override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            buf.append ("Detector:  ").append (detectorName);
            buf.append (" Reagent: " ).append(reagent);
            buf.append (" Unstained Control:  ").append(unstainedControlFile );
            buf.append (" Stained Control ").append( stainedControlFile);

            return buf.toString();
        }

        public String[] getData() {
            String[] data = new String[nfields];
            data[0] = detectorName;
            //hasData returns true if there is a unstained or stained control file
            if (hasData()){
                
                if (reagent ==null || reagent.equals("")){ //fill it in with the detector name
                    
                    if (detectorName.startsWith("<") && detectorName.endsWith(">"))
                        detectorName = detectorName.substring (1, detectorName.length()-1);
                        
                    if (detectorName.endsWith ("-A"))
                        reagent = detectorName.substring(0, detectorName.length()-2);
                    else reagent = detectorName;
                    
                }
                data[1]= reagent;
//                if ()
               
                data[2] = unstainedControlFile;
//                if (stainedControl != null)
//                    data[3] = stainedControl.filename;
//                else
                    data[3] =stainedControlFile;
                    data[4] = stainedTubeName;
                   
            }
            else{
                data[1]="";
                data[2]="";
                data[3]="";
                data[4]="";
               
            }
            return data;
        }


        public boolean hasData() {
            boolean flag = true;
            //is there a reagent?

            if (stainedControlFile == null || stainedControlFile.equals("")){
                flag = false;
            }
//            else if (stainedControl == null)
//                flag = false;
           return flag;
        }
        public void trimSpaces() {
            if (detectorName != null){
                detectorName = detectorName.trim();
                if (detectorName.startsWith("<") && detectorName.endsWith (">"))
                    detectorName = detectorName.substring (1, detectorName.length()-1);
            }
            
            if (reagent != null) reagent = reagent.trim();
            if (unstainedControlFile != null) unstainedControlFile = unstainedControlFile.trim();
            if (stainedControlFile != null) stainedControlFile = stainedControlFile.trim();
        }

//        //override
        public void insertUpdate (DocumentEvent de) {
//            System.out.println (" document event insertUpdate "+ de.getDocument().getProperty(Document.TitleProperty));
            String title = (String)de.getDocument().getProperty (Document.TitleProperty);
            if (title != null){
                updateValues (title, de.getDocument());
            }

        }

//        //override
        public void removeUpdate (DocumentEvent de) {
//            System.out.println (" document event removeUpdate "+ de.getDocument().getProperty (Document.TitleProperty));
            String title = (String)de.getDocument().getProperty (Document.TitleProperty);
            if (title != null){
                updateValues (title, de.getDocument());
            }
        }

//        //override
        public void changedUpdate (DocumentEvent de) {
//            System.out.println (" document event changedUpdate "+ de.getDocument().getProperty (Document.TitleProperty));
            String title = (String)de.getDocument().getProperty (Document.TitleProperty);
            if (title != null){
                updateValues (title, de.getDocument());
            }
        }
        
        private void updateValues (String title, Document doc){
            try {
                String text = doc.getText(0, doc.getLength());
                if (title.equals ("tf1"))
                    reagent = text;
                else if (title.equals ("tf2")){
                    //unstainedControlFile = text;
                    unstainedTubeName = text;
                    if (text != null){
                        unstainedControlFile = text;
//                        if (tubeMap.containsKey (text)){ 
//                            
//                            TubeInfo tone = tubeMap.get(text);
//                            System.out.println ("--------unstained control file " + tone.getFcsFilename());
//                            unstainedControlFile = tone.getFcsFilename();
//                        }
                    }
                    
//                    System.out.println ("  unstained control file after removeupdate "+ unstainedControlFile);
                }
                else {
                    //stainedControlFile = text;
                    stainedTubeName = text;
                    if (text != null) {
                        stainedControlFile = text;
//                       if (tubeMap.containsKey(text)){
//                           
//                           TubeInfo tone = tubeMap.get(text);
//                           System.out.println ("-------stained control file "+ tone.getFcsFilename());
//                           stainedControlFile = tone.getFcsFilename();
//                       } 
                    }
                    
                }
                } catch (BadLocationException e){
                    System.out.println (e.getMessage());
                    //e.printStackTrace();
            }
        }
       
    

    
}
