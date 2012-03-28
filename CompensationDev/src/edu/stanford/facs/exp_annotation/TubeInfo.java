/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.stanford.facs.exp_annotation;

import java.io.File;
import java.util.ArrayList;

/**
 * $Id: TubeInfo.java,v 1.2 2012/01/31 01:19:01 beauheim Exp $
 * @author cate
 * When reading the tube info from a JO file, we aren't opening the FCS files to
 * get the type of information from the FCSFileDialog.  Otherwise the two classes
 * TubeInfo and TubeFileInfo hold the same information.  The TubeFileInfo is taking
 * information from the FCSFileDialog.  This class wants to gather all the info
 * and put up a FCSFileDialog-type UI to verify that these are the FCS files that
 * the user wants to download.  
 */
public class TubeInfo {
    private String url;
    private String tubeName="";
    
    private String cell="";
    private File fcsfile;
    private String fcsfilename="";
    private String altFcsFilename="";
    //cytogenie labels
    private int tube_id;
    private int lotId;
    private String antibody="";
    private int compensationId= -1;
    private int  tube_type;  
    private int unstained_control_id;
    private ArrayList<String[]> analyte_for;
    private ArrayList<String[]> label_for ;
    private ArrayList<String[]> compensations_for ; // this is the stain set Tube type is analysis
    public boolean isSelected = false;
    private boolean areCells = false;
    //collection of detector id and the antibody name.  
    private ArrayList<String[]>PnS = new ArrayList<String[]>(); 
    
    private static final String [] tubeTypes= 
                {"calibration", "analysis", "cells unstained", "compensation", "beads unstained"};
    
    
    public TubeInfo (String tubename){
        this.tubeName = tubename;
    }
    
    public TubeInfo (String tubename, File fcsfile, String altfilename){
       this.tubeName = tubename;
       this.fcsfile = fcsfile; 
       this.altFcsFilename = altfilename;
       this.fcsfilename = fcsfile.getName();
    }
  
    TubeInfo (String url, String name, String cell){
        this.url = url;
        this.tubeName = name;
        this.cell = cell;
        //file%3D3-2.fcs
        if (url.endsWith (".fcs")){
            int idx = url.indexOf ("file%3D");
            if (idx > 0 ){
                fcsfilename = url.substring (idx+7,url.length() );
                System.out.println ("TubeInfo " + tubeName + "  " + fcsfilename);
            }
        }
    }
    
    public ArrayList<String[]> getAnalyteInfo () {
        return analyte_for;
    }
    
    protected void addAntibodyToDetector (ArrayList<String[]> antibody){
        System.out.println ("\t addAntibodyToDetector ------------------ ");
        PnS = antibody;
        if (!PnS.isEmpty()){
            for (String[] ss: PnS){
                System.out.println (ss[0] + "  "+ ss[1]);
            }
        }
    }
    
    public boolean getAreCells() {
    	return areCells;
    }
    public void setAreCells(boolean cells){
    	areCells = cells;
    }
    
    public ArrayList<String[]> getLabelInfo() {
        return label_for;
    }
    
    public String getTubeName() {
        return tubeName;
    }
    
    public String getTubeType() {
        return tubeTypes[tube_type];
    }
    
    public void addURL (String url){
        this.url = url;
    }
    
    public String getFcsFilename() {
        return fcsfilename;
    }
    
    public void setAltFilename (String fn){
        altFcsFilename = fn;
    }
    
    public void setTubeId (int id){
        tube_id = id;
    }
    
    public void setTubeType (String type){
        System.out.println ("  add tube type "+ type);
        if (type == null || type.equals(""))
        	return;
        for (int i=0; i < tubeTypes.length; i++){
            if (tubeTypes[i].equalsIgnoreCase (type)){
                tube_type = i;
                break;
            }
            if (type.equals(tubeTypes[tubeTypes.length-1]))
            	areCells = true;
        }
       
        
        //tube_type.tubetype = type;
    }
    
    public String getURL() {
        return url;
    }
    
    public String toString() {
        StringBuilder buf = new StringBuilder (tubeName).append("  ");
//        buf.append ("\t").append( url).append ( "\n");
        buf.append(fcsfilename).append ("\n");
//        System.out.println (buf.toString());
//        if (cell != null){
//            buf.append ("\t").append (cell).append("\n");
//        }
        return tubeName;
                
    }
    
    public String getInfo() {
        StringBuilder buf = new StringBuilder (tubeName).append(" ").append (tube_id).append("  ");
        buf.append (fcsfilename).append ("\n");
        buf.append (" tube type = "+ tubeTypes[tube_type] + ", ");
        if (analyte_for != null){
            for (int i=0; i < analyte_for.size(); i++){
                String[] one = analyte_for.get(i);
                buf.append ("\t analyte_for ").append (one[0]).append("  ").append (one[1]).append ("\n");

            }
        }
        if (label_for != null){
            for (int i=0; i < label_for.size(); i++){
                String[] one = label_for.get(i);
                buf.append ("\t label_for ").append (one[0]).append("  ").append (one[1]).append ("\n");

            }
        }
        if (compensations_for != null){
         for (int i=0; i < compensations_for.size(); i++){
            String[] one = compensations_for.get(i);
            buf.append ("\t compensations ").append (one[0]).append("  ").append (one[1]).append ("\n");
            
          }
        }
        
        buf.append ("Compensation tube is ").append( compensationId);
        
        return buf.toString();
    }
    
    public String getAnalyteFor (int i){
        return analyte_for.get(i)[0];
    }
    public String getLabelFor (int i){
        return label_for.get(i)[0];
    }
    
    public boolean equals (Object object){
        TubeInfo tube = null;
        boolean isEqual = true;
        if (object != null && object instanceof TubeInfo){
            tube = (TubeInfo)object;
        }
        else 
            isEqual = false;
        
        
        if (isEqual && tubeName.equals (tube.tubeName)){
            if (tube_id == tube.tube_id){
                if (lotId == tube.lotId){
                    if (antibody.equals( tube.antibody)){
                        isEqual = true;
                    }
                    else isEqual = false;
                }
                else isEqual = false;
            }
            else isEqual = false;
            
        }
        else {
            isEqual = false;
        }
                
        return isEqual;
    }
    
    public void addAlternativeFilename (String altFilename){
        altFcsFilename = altFilename;
    }
    
    public void addAnalyteLabel (String[] fl_labels){
        if (analyte_for == null) analyte_for = new ArrayList<String[]>();
        analyte_for.add(fl_labels);
        
    }
    public void addFcsFilename (String fn){
        fcsfilename = fn;
    }
    
    public void setAnalyteLabelledFor ( ArrayList<String[]> label){
        analyte_for = label;
        for (String s[] : label){
            for (String s1 : s){
                System.out.print ("  setAnalyteLabelledFor " + s1);
            }
            System.out.println ();
        }
    }
    
    public ArrayList<String[]> getCompensations() {
        return compensations_for;
    }
    public void setCompensations (ArrayList<String[]> compensations){
        System.out.println ("  set Compensations ");
        this.compensations_for = compensations;
        for (String s[] : compensations){
            for (String s1 : s){
                System.out.print (" compensation for "+ s1);
            }
            System.out.println();
        }
    }
    
    public void setCompensationTubeId (String id){
       compensationId = Integer.parseInt (id); 
    }
    
    public void setLabelledFor ( ArrayList<String[]> label){
        System.out.println ("setLabel For " );
        label_for = label;
        for (String[] s: label){
            for (String s1 : s){
                System.out.print ("  label for "+ s1);
            }
            System.out.println ();
        }

    }
    public void setLotId (String lotIds){
        lotId = Integer.parseInt (lotIds);
    }
    public void setAntibody (String antibody){
        this.antibody = antibody;
    }
    /**
     * called from DivaXmlParser but don't know what it means..
    */
    public void setSelected (boolean is){
        isSelected = is;
    }
    
}
