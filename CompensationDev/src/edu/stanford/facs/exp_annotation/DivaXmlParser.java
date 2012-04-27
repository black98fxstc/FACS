/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.stanford.facs.exp_annotation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Set;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.jdom.*;
import org.jdom.input.*;

/**
 * $Id: DivaXmlParser.java,v 1.1 2012/01/25 01:41:07 beauheim Exp $
 * @author cate
 * Parse the xml file to obtain the list of detectors, fcs filenames, voltage
 * values and area_scaling factors
 */
public class DivaXmlParser {
    private File datafile;
    private StringBuilder errorMessage = new StringBuilder();
    private ArrayList <String> reagentList = new ArrayList <String>();
    private ArrayList <String[]> controlList = new ArrayList<String[]>();
    private ArrayList <String> detectorList = new ArrayList<String>();
    private String[] fcsFilenames;
    private String dataPath;
    private String[] detectorNamesIn;
    private int compi;
    private String expName;
    private HashMap<String, TubeInfo> tubemap ;
    private ArrayList <String[][]> allStainSets = new ArrayList<String[][]>();
    

//    class SpecimenAreaScaling {
//        //these are the experimental values found in the xml file.  Area scaling
//        //is per specimen, per tube, per laser.  The lasers then have to be
//        //mapped to the detectors.  The lasers can be dropped
//        String specimen;
//        ArrayList<String> tubenames;
//        ArrayList<Double> area_scaling;
//
//        public SpecimenAreaScaling (){
//
//        }
//    }

    public DivaXmlParser() {

    }
    
    public DivaXmlParser (File datafile, HashMap<String, TubeInfo> tubeMap){
        
        this.datafile = datafile;
        this.tubemap = tubeMap;
        if ( datafile.exists() && datafile.canRead()){
//            System.out.println (" let's go");
            dataPath = datafile.getParent();
          
            parseFile();
        }
        else {
            errorMessage.append("Unable to open the datafile ").append ( datafile.getName());
        } 
        
    }
    public DivaXmlParser (File datafile, String[] detectorNames){
        this.datafile = datafile;
        this.detectorNamesIn = detectorNames;
//        if (datafile.exists() && datafile.canRead()){
//
//        }
    }

    public DivaXmlParser (File datafile, Float[][]data, String[]reagentNames, String expname){

        System.out.println (" xml parser constructor");
      //  printFlowJoMatrixforPC (datafile, data, reagentNames, expname);

    }

   


    /**
     * experiment-->specimen-->tube-->lasers-->laser-->area_scaling
     * Parse the xml file looking for the area_scaling value.  The String[][]
     * contains the name of the laser[i][0] and the area_scaling[i][1].  The values
     * returned are matched with the detectors from the calling class.
     * @return String[][]
     */
    public String[][] parseAreaScalingValues (){
        final Document doc;
        String[][] areaScalingValues = null;
        try {
          SAXBuilder builder = new SAXBuilder();
          doc = builder.build(datafile);
          Element root = doc.getRootElement();
          Element exp_ele = root.getChild ("experiment");
          if (exp_ele != null ) {
              List<Element> spec_list = exp_ele.getChildren ("specimen");
              if (spec_list !=null && spec_list.size() > 0){
                  List <Element> tube_list =  ((Element)spec_list.get(0)).getChildren ("tube");
                  if (tube_list != null && tube_list.size() > 0){
                      List <Element>laserS_list = ((Element) tube_list.get(0)).getChildren ("lasers");
                      if (laserS_list != null && laserS_list.size() > 0){
                          List <Element>laser_list = ((Element) laserS_list.get(0)).getChildren("laser");
                          if (laser_list != null && laser_list.size()>0){
                              areaScalingValues = new String[laser_list.size()][2];
                              for (int i=0; i < laser_list.size(); i++){
                                  Element onelaser= (Element) laser_list.get(i);
                                  String lasername = onelaser.getAttribute("name").getValue();
                                  Element ele = onelaser.getChild ("area_scaling");
                                  String value = ele.getText();

                                  areaScalingValues[i][0] = lasername;
                                  areaScalingValues[i][1] = value;
                              }
                          }

                      }
                      }
                  }
              }

          
        } catch(JDOMException e) {
            System.out.println (e.getMessage());
        } catch (IOException e){
            System.out.println (e.getMessage());
        } catch(NullPointerException e) {
              System.out.println (e.getMessage());
        }
        return areaScalingValues;


    }
/**
 * Read the xml read looking for the voltage parameter in the instrument_settings
 * @return String[][]  String[i][0]= the detector name and String[i][1] is the
 *                     voltage as a String.
 */
    public String[][] parseVoltValues() {

        Document doc;
        String[][] voltageValues=new String[detectorNamesIn.length][2];
        try {
          SAXBuilder builder = new SAXBuilder();
          doc = builder.build(datafile);
          Element root = doc.getRootElement();
          Element exp_ele = root.getChild ("experiment");
          int i=0;
          if (exp_ele != null ) {
              List <Element>instrument_list = exp_ele.getChildren ("instrument_settings");
              if (instrument_list != null && instrument_list.size() > 0){
                  List <Element>parameter_list =  instrument_list.get(0).getChildren("parameter");
                  if (parameter_list != null){
                      Iterator <org.jdom.Element> it =  parameter_list.iterator();
                      while (it.hasNext() ){
                          Element oneparam = it.next();
                          String name = oneparam.getAttribute ("name").getName();
                          String att = oneparam.getAttribute("name").getValue();
//             System.out.println (name + ", " + att);
                          //check the detector list
                          if (onDetectorList (att)){
                              String volt = null;
                              List<Element> volt_list = oneparam.getChildren("voltage");
                              if (volt_list != null && volt_list.size() == 1){
                                  Element volt_ele = (Element) volt_list.get(0);
                                  volt = volt_ele.getText();
//                                  System.out.println (att + ",  "+ volt);
                              }
                              voltageValues[i][0]=att;
                              voltageValues[i][1]=volt;
                              i++;

                          }

                  }
              }
              }

          }
       } catch(JDOMException e) {
           System.out.println (e.getMessage());
          
        } catch (IOException e){
            System.out.println (e.getMessage());
        } catch(NullPointerException e) {
          System.out.println (e.getMessage());
        }
        return voltageValues;

    }

    private boolean onDetectorList (String name){
        boolean flag = false;

        for (String s: detectorNamesIn){
            if (s.equals (name))
                flag = true;
        }
//        System.out.println ("on detector list " + name + " " + flag);

        return flag;
    }

    /* the fl_label list is only when we can parsing a file that has
     * multiple fluorescents per detector or at least on one of the
     * detectors.  It is a Diva thing.
     */
    public String[][] getFl_LabelList() {
        String[][] controls = new String[controlList.size()][2];
        for (int i=0; i < controlList.size(); i++)
            controls[i] = controlList.get(i).clone();
         for (int i=0; i < controls.length; i++){
            for (int j=0; j < controls[i].length; j++){
                System.out.print (controls[i][j] + "  ");
            }
         }
        return controls;
    }

    public boolean hasMultipleFluorescents() {
        boolean flag = false;
        if (controlList != null && controlList.size() > 0)
            flag = true;
        return flag;
    }
    /**
     * 
     * @return String array containing the reagentList.
     */

    public String[] getControlList() {
       
        String[] reagents = new String[reagentList.size()];
        return reagents = reagentList.toArray(reagents);
    }

    /**
     *
     * @return String[] return the list of detectors
     */
    public String[] getDetectorList() {
        String[] detectors = new String[detectorList.size()];

        return detectorList.toArray(detectors);
    }

    /*
     * @return String[] return the list of FCS filenames
     */
    public String[] getFCSFilenames() {
        return fcsFilenames;
    }

    public String getExperimentName() {
        return expName;
    }


    
    /**
     * 
     * @return  String
     */
    public String getErrorMessage() {
        return errorMessage.toString();
    }
    
    private void parseFile () {
        Document doc;
        String[] fl_label = null;
        try {
          SAXBuilder builder = new SAXBuilder();
          doc = builder.build(datafile);
          Element root = doc.getRootElement();
          Element exp_ele = root.getChild ("experiment");

          if (exp_ele != null ) {
              expName = exp_ele.getAttributeValue ("name");
              System.out.println ("Experiment name = "+expName);
              List <Element>instrument_list = exp_ele.getChildren ("instrument_settings");
              if (instrument_list != null && instrument_list.size() > 0){
                  List <Element>parameters =  instrument_list.get(0).getChildren ("parameter");
                  if (parameters != null){
//                      System.out.println (" Length of parameter list is " + parameters.size());
                      Iterator <org.jdom.Element> it = parameters.iterator();
                      while (it.hasNext() ){
                          Element oneparam = it.next();
                          int type = oneparam.getAttribute ("type").getIntValue();
                          if (type == 30){
//                              String name = oneparam.getAttribute ("name").getName();
//                              System.out.println (name + ", " + type + ",  "+ oneparam.getAttribute("name").getValue());
                              String att = oneparam.getAttribute("name").getValue();
                              if (!att.startsWith("FSC") && !att.startsWith ("SSC")){
//                                  int index = att.indexOf('-');
//                                  if (index > -1)
//                                      att = att.substring(0, index);
                     //             System.out.println ("Parsing:  att add to detector list " + att);
                                   detectorList.add(att);
                              }
                          }
                          
                      }

                  }
                  else {
                      errorMessage.append (" Unable to get the instrument settings from the file.");
                  }
                  List <Element>specimen_list = exp_ele.getChildren ("specimen");
                  int ith=0;
                  if (specimen_list != null && specimen_list.size() > 0) {
                      for (int i =0; i < specimen_list.size(); i++) {
                          Element ele_i = (Element) specimen_list.get(i);
//                          System.out.println ("Specimen name " + ele_i.getAttribute("name"));
                          ith++;  //where is the comp one in this list?
                          //These are Diva compensation tubes.
                          if (ele_i.getAttribute("name").getValue().equals("Compensation Controls")){
                              compi = ith;
//                              System.out.println (" this one is a comp " + compi);
                              List<Element> tube_list = ele_i.getChildren ("tube");
                          
                              if (tube_list != null && tube_list.size() > 0){
                                  System.out.println (" tube list size?  "+ tube_list.size());
                                  for (int j=0; j < tube_list.size(); j++){
                                       TubeInfo newtube= null;
                                      Element onetube = (Element) tube_list.get(j);
                                      String value = ((Element) tube_list.get(j)).getAttribute("name").getValue();
                                      System.out.println ("Tube name value is " + value);
                                      if (tubemap.containsKey (value)){
                                          newtube = tubemap.get(value);
                                      }
                                      else {
                                          newtube = new TubeInfo (value);
                                          newtube.setSelected (true);
                                      }
                                         
                                      int index =value.indexOf (" Stained Control");
                                      if (index > -1){
                                          newtube.setTubeType ("compensation");
                                        
                                          value = value.substring(0, index);
                                          Element child = onetube.getChild ("data_filename");
                                          if (child != null && child.getContent() != null){
                                                  String alt =  ((Text) child.getContent().get(0)).getText();
                                                  newtube.setAltFilename (alt);

                                          }
                                          
                                          List <Element>labels = onetube.getChildren ("labels");
                                          /*These are the stain sets!! when not a Stained Control*/
                                          for (int p = 0; p < labels.size(); p++){
                                              fl_label = new String[2];
                                              List<Element> fls = labels.get(p).getChildren("fl");
                                              for (int q=0; q < fls.size(); q++){
                                                  Element fl_1 = (Element) fls.get(q);
                                                  String flname = ((Element) fls.get(q)).getAttribute("name").getValue();
                                                  fl_label[0] = flname;
                                                  System.out.println ("fl name = "+ flname);
                                                  Element label_1 = fl_1.getChild ("label");
                                                  if (label_1 != null){
                                                      //content value
                                                      List<List<Text>> labellist = label_1.getContent();
                                                      if (labellist != null && labellist.size()>0){
                                                          String labelis =  ((Text)labellist.get(0)).getText();
                                                          if (labelis == null) labelis="";
                                                          fl_label[1] = labelis;
                                                          System.out.println ("label is " + labelis);
                                                      }
                                                  }
                                                  newtube.addAnalyteLabel (fl_label);
                                                  
                                              }
                                          }
                                          tubemap.put (newtube.getTubeName(), newtube);

                                      }
//                                      else if (value.contains("Unstained")){
//                                          Element child = onetube.getChild ("data_filename");
//                                          if (child != null && child.getContent() != null){
//                                                  String alt =  ((Text) child.getContent().get(0)).getText();
//                                                  newtube.setAltFilename (alt);
//                                                  newtube.setTubeType ("beads unstained");
//
//                                          }
//                                          tubemap.put (newtube.getTubeName(), newtube);
//                                      }
                                      else if (value.contains("capture beads")){
                                          System.out.println (" capture beads " + value);
                                        //  Element onetube = (Element) tube_list.get(j);
                                          List <Element >keys = onetube.getChildren("keywords");
                                          List <Element>keywords = keys.get(0).getChildren("keyword");
                                        //  List keywords = ((Element)tube_list.get(j)).getAttribute("keywords").getChildren("keyword");
                                          for (int k=0; k < keywords.size(); k++){
                                              Element ekey =  keywords.get(k);
                                              System.out.println (ekey.toString());
                                              if (ekey.getAttribute("name").getValue().equals("type")){
                                                 
                                                 List<Element> children = ekey.getChildren("value");
                                                 if (children.size() == 1){
                                                    Element one =  (Element) children.get(0);
                                                    List<List<Text>> contents = one.getContent();
                                                    System.out.println (contents.get(0));
                                                    
                                                 }
                                              }
                                             
                                              
                                          }

                                      }
                                      
                                      reagentList.add (value);
                                      if (value != null){
                                          System.out.print (value );
                                      }
                                      if (fl_label != null){
                                          controlList.add (fl_label);
                                      System.out.print ( "  "+ fl_label[0] + "  "+ fl_label[1]);
                                      }
                                      System.out.println();

                                  }
                              }
                              
                          } //if specimen is a compensation control
                          else {
                              List<Element> tube_list = ele_i.getChildren ("tube");
                             //some other kind of tube.
                              parseDiVaForTubes (tube_list);
                              printTubeMap ();
                              
                          }


                      } //for all the specimens
                      fcsFilenames = new String[reagentList.size()];
                      int j=1;
                      
                      for (int i = 0; i < reagentList.size(); i++){
                          fcsFilenames[i] = dataPath + File.separator + compi + "-" + j + ".fcs";
                          System.out.println (" fcs file = " + fcsFilenames[i]+ " "+ reagentList.get(i)+"|");
                          String tubename = reagentList.get(i);
                          TubeInfo tube = null;
                          if (tubemap.containsKey (tubename) ){
                              tube = tubemap.get(tubename);
                             
                          }
                          else if (tubemap.containsKey (tubename+" Stained Control")){
                              tube = tubemap.get(tubename+ " Stained Control");
                       
                          }
                          if (tube != null){
                              tube.addFcsFilename (fcsFilenames[i]);
                              System.out.println ("Found it " + tube.getTubeName() + "  "+ fcsFilenames[i]);
                          }
                          j++;
                      }
                  }//if specimen list not null
                  //look for the speciment named comp
              //    printTubeMap();

              }
          }
        } catch(JDOMException e) {
          System.out.println (e.getMessage());
        } catch (IOException e){
            System.out.println (e.getMessage());
        } catch(NullPointerException e) {
          System.out.println (e.getMessage());
        }
   
    }
    private  String[][] getStainSet(List<Element> labels ) {
        ArrayList <String[]> astainset = new ArrayList <String[]>();
       
        String[] fl_label;
        String[][] onestainset = null;
        for (int p = 0; p < labels.size(); p++){
                  fl_label = new String[2];
                  List<Element> fls = labels.get(p).getChildren("fl");
                  for (int q=0; q < fls.size(); q++){
                      Element fl_1 = (Element) fls.get(q);
                      String flname = ((Element) fls.get(q)).getAttribute("name").getValue();
                      fl_label[0] = flname;
                      System.out.println ("fl name = "+ flname);
                      Element label_1 = fl_1.getChild ("label");
                      if (label_1 != null){
                          //content value
                          List<List<Text>> labellist = label_1.getContent();
                          if (labellist != null && labellist.size()>0){
                              String labelis =  ((Text)labellist.get(0)).getText();
                              if (labelis == null) labelis="";
                              fl_label[1] = labelis;
                              System.out.println ("label is " + labelis);
                          }
                      }

                  }
                  astainset.add (fl_label);
              }
        if (astainset.size() > 0){
            onestainset = new String[astainset.size()][];
            onestainset = astainset.toArray (onestainset);
        }
       return onestainset;
    }
   
   
    private void parseDiVaForTubes (List<Element> tubeList) {
        
        String alt=null;
        String tube_type = null;
        String tube_id = null;
        String control_name = null;
        String tubeName = null, control_id;
        HashMap <Integer, String> compensationForList = new HashMap<Integer, String>();
        TubeInfo newtube = null;
        System.out.println ("parse diva for tubes " );
        if (tubeList != null && tubeList.size() > 0){
             for (int j=0; j < tubeList.size(); j++){
                 
                  Element onetube = (Element) tubeList.get(j);
                  List<List<Attribute>> li = onetube.getAttributes();
                  Attribute attr = (Attribute) li.get(0);
                  System.out.println (attr.getName() + "  "+ attr.getValue());
                  tubeName = ((Attribute) onetube.getAttributes().get(0)).getValue();
                  if (tubemap.containsKey (tubeName)){
                      newtube = tubemap.get (tubeName);
                  }
                  else {
                      newtube = new TubeInfo (tubeName);
                  }
                  
//                  List list = onetube.getContent();
                  Element child = onetube.getChild ("data_filename");
                  if (child != null){
                      if (child.getContent() != null){
                          alt = ((Text) child.getContent().get(0)).getText();
                          newtube.setAltFilename(alt);
                          System.out.println (tubeName + "-- datafile anme is " + alt);
                      }
                  }
                  
                  List <Element>labels = onetube.getChildren ("labels");
                  String[][] stainset = getStainSet (labels);
                  allStainSets.add (stainset);
                  
/*these are CytoGenie keywords, not DiVA */
                  List <Element>keys = onetube.getChildren("keywords");
                  
                  List <Element>keywords = keys.get(0).getChildren("keyword");
                //  List keywords = ((Element)tube_list.get(j)).getAttribute("keywords").getChildren("keyword");
                  for (int k=0; k < keywords.size(); k++){
                      Element ekey = (Element) keywords.get(k);
                      System.out.println ("All tube keywords " + ekey.getAttribute("name"));
                      
                      if (ekey.getAttribute("name").getValue().startsWith("Compensation-for-")){
                         control_name = ekey.getAttribute("name").getValue();
                          List <Element>children = ekey.getChildren ("value");
                          if (children!= null){
//                              Element one = (Element) children.get(0);  
                              control_id = ((Element) children.get(0)).getText();
//                              System.out.println (control_name + " --- " + one.getName() + "  "+ one.getText());
                              if (compensationForList == null)
                                  compensationForList = new HashMap <Integer,String>();
                              compensationForList.put (Integer.parseInt (control_id), control_name);
                          }   

                      }
                      else if (ekey.getAttribute("name").getValue().equalsIgnoreCase("Tube-identifier")){
                          //only CytoGenie has the tube-identifier as a keyword.
//                          tube_id = ekey.getAttribute("name").getValue();
//                          System.out.print ("\tTube identifier "+ tube_id);
                          List <Element>children = ekey.getChildren ("value");
//                          for (int c=0; c < children.size(); c++){
                            if (children != null && children.size() == 1){
                               System.out.println (" Tube identifier " +  children.get(0).getText());
                               tube_id = children.get(0).getText();
                          }

                      }
                      else if (ekey.getAttribute("name").getValue().equalsIgnoreCase("tube-type")){
//                          tube_type = ekey.getAttribute("name").getValue();
//                          System.out.print ("\t Tube type "+ tube_type);
                          List <Element>children = ekey.getChildren ("value");
                          for (int c=0; c < children.size(); c++) {
                              tube_type = ((Element)children.get(c)).getText();
                              System.out.println (((Element) children.get(c)).getText());
                          }
                      }
                      else if (ekey.getAttribute("name").getValue().startsWith("Label-for-")){
                          getCGKeyword (ekey, newtube);
                      }
                      else if (ekey.getAttribute("name").getValue().startsWith ("Analyte-labeled-by-")){
                          /*keyword name="Analyte-labeled-by-Alexa Fluor 488" type="2">
                                    <value>CD4</value>*/
                          getCGKeyword (ekey, newtube);
                      }
                      else if (ekey.getAttribute ("name").getValue().startsWith ("LOT-ID-")){
                          getCGKeyword (ekey, newtube);
                      }
                      else if (ekey.getAttribute ("name").getValue().startsWith("Antibody-for-")){
                          getCGKeyword (ekey, newtube);
                      }

                  }
                  //get the labels which might be a stain set.
                  
           
                  compensationForList = null;
                   
             }
          }
       
        
    }
    
    private void getCGKeyword (Element keyword, TubeInfo tube){
        /*ekey.getAttribute("name").getValue().startsWith("Label-for-"*/
        
        /*  else if (parts[i].contains ("APPLY COMPENSATION") && i+1 < parts.length){
                            applyCompensation = Boolean.parseBoolean (parts[i+1]);
                        }
                        else if (parts[i].contains("ANALYTE-LABELED") && i+1 < parts.length){
                            String[] ana = parts[i].split ("ANALYTE-LABELED-BY-");
                            if (ana.length == 2){
                                String[] analyte_labeled = new String[2];
                               analyte_labeled[0] = ana[1];
                               analyte_labeled[1] = parts[i+1];
                               analytes.add (analyte_labeled);
                            }
                        } 
                        else if (parts[i].contains ("COMPENSATION-FOR") && i+1 < parts.length){
                            String[] comps = parts[i].split ("COMPENSATION-FOR-");
                            if (comps.length == 2){
                                String[] compensationFor = new String[2];
                               compensationFor[0] = comps[1];
                               compensationFor[1] = parts[i+1];
                               compensations.add (compensationFor);
                            }
                        }
                        else if (parts[i].contains ("LABEL-FOR") && i+1 < parts.length){
                            String[] labelsfor = parts[i].split("LABEL-FOR-");
                            if (labelsfor.length == 2){
                                String[] labeledby = new String[2];
                                labeledby[0] = labelsfor[1];
                                labeledby[1] = parts[i+1];
                                labels.add (labeledby);
                            
                            }
                        }
                        else if (parts[i].contains ("LOT-ID") && i+ 1 < parts.length){
                            String[] lotid = parts[i].split("LOT-ID");
                            if (lotid.length == 2){
                                String[] lotId = new String[2];
                                lotId[0] = lotid[1];
                                lotId[1] = lotid[i+1];
                            }
         **/
    }
    
    /**
     * From the <labels><fl><label> stuff I can identify a stainset.  One 'stain'
     * is made up of a fluor and a label.  String[0] = fluor and String[1] = label
     * The set of fluor are the same, I think.  the set of labels, like CD138 can
     * be different.
     * @param stainset
     * @return 
     */
    private boolean isStainSetUnique (ArrayList<String[]> stainset){
        boolean flag = true;
        
        return flag;
    }
    
    private void printTubeMap (){
        System.out.println (" --------- Print Tube Map---------------" + tubemap.size());
        Set<String> keys = tubemap.keySet();
        Iterator <String>it = keys.iterator();
        while (it.hasNext()){
            String k =  it.next();
            TubeInfo value = (TubeInfo) tubemap.get(k);
            System.out.println (k + " :: ");
            System.out.println (value.toString());
        }
        System.out.println (" --------- Print Tube Map---------------");

    }

    public void printFlowJoMatrixforPC(File fn, Float[][]data, String[] detectorNames, String exname){

        System.out.println (" fn path =" + fn.getPath() +  "   name "+ fn.getName() );
        String myprefix="comp";
        if (exname == null)
            exname = "Experiment";
        if (exname.contains(" ")){
            exname.replaceAll (" ", "_");
        }
        
        
        System.out.println (exname + "  vs "+ expName);
        String uri ="http://www.flowjo.com";
      //  if (fn != null && fn.exists() && fn.canWrite()) {
            try {
                FileOutputStream os = new FileOutputStream (fn);
                XMLOutputFactory factory = XMLOutputFactory.newFactory();
                XMLStreamWriter writer = factory.createXMLStreamWriter(os);
                
                writer.writeStartDocument("1.0");
             
               
                writer.writeStartElement (myprefix, "spilloverMatrix", uri);
                 writer.setPrefix (myprefix, uri);
                writer.writeNamespace(myprefix, uri);
//                writer.writeStartElement(comp:spilloverMatrix version="7.6.1" prefix="Comp-" comp:id="Untitled" color="#0000FF");
                writer.writeAttribute (myprefix,uri, "version", "7.6.1");
                writer.writeAttribute (myprefix, uri, "prefix", "comp");
                writer.writeAttribute (myprefix, uri, "id", exname);
                writer.writeAttribute (myprefix, uri,"color", "#0000FF");
                for (int i=0; i < detectorNames.length; i++){
                    writer.writeStartElement (myprefix, "spillover", uri);
                    writer.writeAttribute ( myprefix, uri,"parameter", detectorNames[i]);
                    for (int j=0; j < detectorNames.length; j++){
                        writer.writeStartElement (myprefix, "coefficient", uri);
                        writer.writeAttribute (myprefix, uri, "parameter", detectorNames[j] );
                        writer.writeAttribute (myprefix, uri, "value", data[i][j].toString());
                        writer.writeEndElement();
                    }
                    writer.writeEndElement();

                }
                writer.writeEndElement();
                writer.writeEndDocument();
                writer.flush();
                writer.close();
            } catch (FileNotFoundException e1){
                System.out.println (e1.getMessage());

            } catch (XMLStreamException e2){
                System.out.println (e2.getMessage());
            }
       // }

    }




    public static void main (String[] args){
        File file = new File ("data/matrixpc-2.xml");
       
      //  if (file.exists() && file.canWrite()){
            String[] names = {"APC Cy7-A", "APC-A","Alexa 680-A","Am Cyan-A",
                          "FITC-A" , "PE Cy5-A", "PE Cy55-A",
                          "PE Cy7-A" ,"PE Green laser-A",
                          "PE Tx RD-A", "Pacific Blue-A","PerCP Cy55 Blue-A" };
           
            float[][] data = {{1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,0},
                          {0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0,0},
                          {0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0,0},
                          {0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0,0},
                          {0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0,0},
                          {0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0,0},
                          {0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0,0},
                          {0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0,0},
                          {0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0,0},
                          {0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0,0},
                          {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1,0},
                          {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,1}};


        Float[][] fdata = new Float[data.length][data.length];
        for (int i=0; i< data.length; i++){
            for (int j=0; j < data.length; j++){
                fdata[i][j] = new Float (data[i][j]);
            }
        }
//        File diva= new File ("/Users/cate/FCSData/KondalaData/BSO-BMS-100204/BSO-BMS-100204.xml");
        File diva = new File ("/Users/cate/FCSData/Autocomp Development-Cate-Cytogenie test/Autocomp Development-Cate-cytogenie test.xml");
//        File diva = new File ("/Users/cate/FCSData/Cate-comp-110303/Cate-comp-110303.xml");
        HashMap <String, TubeInfo> tubeMap = new HashMap<String, TubeInfo>();
        DivaXmlParser parser = new DivaXmlParser (diva, tubeMap);
        String[] controlList = parser.getControlList();
        String[] detectorList = parser.getDetectorList();
        if (parser.hasMultipleFluorescents()){
            String[][] fl_labels = parser.getFl_LabelList();
            for (int i=0; i < fl_labels.length; i++){
                System.out.println ( fl_labels[i][0] + ":  " + fl_labels[i][1]);
            }
        }
        for (String s: controlList)
            System.out.println (" controls "+ s);
        for (String s: detectorList)
            System.out.println (" detector List " + s);
//           XmlParser parser = new XmlParser (file, fdata, names, "My Experiment");
     //   }
    }

}
