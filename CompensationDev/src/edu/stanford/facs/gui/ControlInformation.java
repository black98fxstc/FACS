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
import javax.swing.JCheckBox;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

/**
 * $Id: ControlInformation.java,v 1.3 2012/01/25 01:41:07 beauheim Exp $
 * @author cate
 */

public class ControlInformation implements ActionListener, ChangeListener, TextListener, DocumentListener  {

		//0 is the detectorName, 1 is the reagent, 2 is unstained file, 3 is stained file, 
	//4 is whether compensationCells are true or false
        String detectorName="";
        String reagent="";
        String unstainedControlFile="", stainedControlFile="";
        TubeInfo unstainedTube, stainedTube;
        boolean compensationCells = false;
        boolean useUnstained = false;
        String unstainedTubeName="", stainedTubeName="";
        FCSFileDialog mydialog;
        Integer rowId;
        public static final int DETECTOR=0;
        public static final int REAGENT = 1;
        public static final int UNSTAINED =2;
        public static final int STAINED=3;
        public static final int CELLS=4;
     
        int nfields=5;

        //tokens are from the mapping file.  there are typically 4 unless the user wants to designate cells as true in 5 token
        ControlInformation (String[] tokens ) throws IllegalArgumentException {
            if (tokens == null || tokens.length < 0 )
                throw new IllegalArgumentException();
            if (tokens[DETECTOR] != null)
                this.detectorName = tokens[DETECTOR];


            reagent = tokens[REAGENT];
            if (tokens[UNSTAINED].endsWith("fcs"))
                unstainedControlFile = tokens[UNSTAINED];
            else unstainedTubeName = tokens[UNSTAINED];
            
            if (tokens[STAINED].endsWith("fcs"))
                stainedControlFile = tokens[STAINED];
            else stainedTubeName = tokens[STAINED];
            
             if (tokens.length == nfields){
            	compensationCells = new Boolean(tokens[CELLS]);
            }
          
        }

        ControlInformation (String detectorName, String reagent){
            this.detectorName = detectorName;
            this.reagent = reagent;
        }

        //picking my own controls leads me here
        ControlInformation (String detectorName){
            this.detectorName = detectorName;
        }
        
        public void setRowId (Integer id){
        	rowId = id;
        }

        public String[] copyData () {
            String[] copy = new String[nfields];

            if (detectorName != null)
                copy[DETECTOR] = detectorName;
            else copy[DETECTOR] = "";
            copy[REAGENT] = reagent;
            
//            if (tubeMap.containsKey (unstainedControlFile))
//                copy[2] = tubeMap.get(unstainedControlFile).getFcsFilename();
//            else
//                copy[2] = unstainedControlFile;
         //   copy[2] = tubeAndFiles.get(unstainedControl);
            
            copy[UNSTAINED] = unstainedControlFile;
           // if (tubeAndFiles.containsKey (stainedControlFile))
                
                
            copy[STAINED] = stainedControlFile;
           // copy[4] = stainedTubeName;
            
            copy[CELLS]="false";
            if (compensationCells)
            	copy[CELLS]="true";

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
  //           System.out.println("add values 117 ");
  //           for (int i = 0; i < tokens.length; i++){
    //        	 System.out.println(i+ ".  "+ tokens[i]);
//             }
             reagent = tokens[REAGENT];
             if (tokens[UNSTAINED].endsWith("fcs"))
                  unstainedControlFile = tokens[UNSTAINED];
             else unstainedTubeName=tokens[UNSTAINED];
             if (tokens[STAINED].endsWith("fcs"))
                 stainedControlFile = tokens[STAINED];
             else stainedTubeName=tokens[STAINED];
            

         }
         
         protected void addValuesFromMapping (String[] tokens){
        	 System.out.println("add valuesfromMapping  117 ");
             for (int i = 0; i < tokens.length; i++){
            	 System.out.println(i+ ".  "+ tokens[i]);
             }
             reagent = tokens[REAGENT];
             if (tokens[UNSTAINED].endsWith("fcs"))
                 unstainedControlFile = tokens[UNSTAINED];
             else
            	 unstainedTubeName=tokens[UNSTAINED];
             if (tokens[STAINED].endsWith("fcs"))
                  stainedControlFile = tokens[STAINED];
             else stainedTubeName=tokens[STAINED];
             
             if (tokens.length ==nfields){                
                 if (tokens[CELLS].equalsIgnoreCase("true")){ //this will fail because of new rule of adding t/f cells
                	 compensationCells = true;
                 } 
             }
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
            buf.append("\n Tube name: ").append( stainedTubeName);
            if (stainedTube != null){
            	buf.append("\n tube info: ").append(stainedTube.getInfo());
            }
            buf.append("\n ");

            return buf.toString();
        }

        public String[] getData() {
            String[] data = new String[nfields];
            data[DETECTOR] = detectorName;
            //hasData returns true if there is a unstained or stained control file
            if (hasData()){
                
                if (reagent ==null || reagent.equals("")){ //fill it in with the detector name                   
                    if (detectorName.startsWith("<") && detectorName.endsWith(">"))
                        detectorName = detectorName.substring (1, detectorName.length()-1);
                        
                    if (detectorName.endsWith ("-A"))
                        reagent = detectorName.substring(0, detectorName.length()-2);
                    else reagent = detectorName;
                    
                }
                data[REAGENT]= reagent;
//                if ()
               
                data[UNSTAINED] = unstainedControlFile;
//                if (stainedControl != null)
//                    data[3] = stainedControl.filename;
//                else
                    data[STAINED] =stainedControlFile;
                    
                   data[CELLS]="false";
                   if (compensationCells)
                	   data[CELLS]="true";
            }
            else{
            	for (int i=0; i < nfields-1; i++)
                data[i]="";
            data[nfields-1]="false";
               
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
  //          System.out.println (" document event insertUpdate "+ de.getDocument().getProperty(Document.TitleProperty));
            String title = (String)de.getDocument().getProperty (Document.TitleProperty);
            if (title.equalsIgnoreCase("tf2")){
            	//System.out.println ("insert update on tf2");
            	if (useUnstained && unstainedTubeName !=null){
            		try{
            		unstainedTubeName = de.getDocument().getText(0, de.getLength());
            	//	System.out.println("set this unstained for all");
            		} catch (BadLocationException e){
                        System.out.println (e.getMessage());
            		}   
                     
            	}
            }
            else if (title.equalsIgnoreCase("tf3") && useUnstained ){
            	if (unstainedTubeName !=null){
            		System.out.println("fill in the unstained file previously selected. "+ unstainedTubeName);
            		if (mydialog !=null){
            			
            		}
            	}
            }
            if (title != null){
                updateValues (title, de.getDocument());
            }

        }

//        //override
        public void removeUpdate (DocumentEvent de) {
          //  System.out.println (" document event removeUpdate "+ de.getDocument().getProperty (Document.TitleProperty));
            String title = (String)de.getDocument().getProperty (Document.TitleProperty);
            if (title != null){
                updateValues (title, de.getDocument());
            }
        }

//        //override
        public void changedUpdate (DocumentEvent de) {            
      //  	System.out.println (" document event changedUpdate "+ de.getDocument().getProperty (Document.TitleProperty));
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

		@Override
		public void stateChanged(ChangeEvent e) {
			if (e.getSource() instanceof JCheckBox) {
	//			System.out.println("compensation cells state change " + compensationCells);
				JCheckBox cb = (JCheckBox)e.getSource();
				String name = (String) cb.getClientProperty("name");
				System.out.println(" checked? "+ cb.isSelected());
				if (name != null && name.equalsIgnoreCase("cells"))
				    compensationCells = cb.isSelected();
				else{
					useUnstained = cb.isSelected();
					if (useUnstained){
					     mydialog = (FCSFileDialog)cb.getClientProperty("dialog");
					}
					
				}
			}
		//	System.out.println("compensation cells state change " + compensationCells);
			
		}
       
}
