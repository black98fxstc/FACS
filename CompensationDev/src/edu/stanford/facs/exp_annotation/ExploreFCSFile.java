/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.stanford.facs.exp_annotation;

import org.isac.fcs.FCSException;
import org.isac.fcs.FCSFile;
import org.isac.fcs.FCSHandler;
import org.isac.fcs.FCSParameter;
import org.isac.fcs.FCSTextSegment;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * $Id: ExploreFCSFile.java,v 1.1 2012/01/25 01:41:07 beauheim Exp $
 * @author cate
 */
public class ExploreFCSFile {

    private String[]detectors={  "Pacific Blue-A","Violet Green-A", "Pacific Orange-A",
                               "Qdot 605-A","APC-A", "APC-Cy7-A","PE-A","PE-Texas-Red-A",
                               "PE-Cy5-A","PE-Cy55-A","PE-Cy7-A", "FITC-A"   };
    
    ExploreFCSFile (String filename) {
        System.out.println (filename);
        File f = new File (filename);
        if (f.exists() && f.canRead()){
            FCSFile fcsfile = new FCSFile (f);
            read (fcsfile);
        }
        else
            System.out.println ("file not found " + f.getName());
    }
    
    private void read (FCSFile file){
        try {
             ArrayList <String[]> detectorList = new ArrayList<String[]>();
//            int np= file.getParameters();
            List <FCSParameter> list = file.getParameterList();
          //  FCSHandler handler = file.getOutputIterator();
            Iterator <FCSParameter>it = list.listIterator();
            while (it.hasNext()){
                FCSParameter param =  it.next();
                System.out.println (" parameter is " + param.getIndex() + "  " + param.toString());
               

            }
//            FCSTextSegment segment = file.getTextSegment();
//           Set<String> attrNames = segment.getAttributeNames();
//           it = attrNames.iterator();
//           while (it.hasNext()){
//               String name = (String) it.next();
//               System.out.println (name);
//           }
//            doAsBefore (file);
//            FCSParameter p1 = file.getParameter ("FSC-A");
//            if (p1 == null)
//                System.out.println ("the fsc-a parameter is null here");
//            else
//                System.out.println ("FSC get maximum = " + p1.getMaximum());
//            p1 = file.getParameter ("SSC-A");
//            if (p1 == null)
//                System.out.println ("the ssc-a parameter is null here");
//            else
//                System.out.println ("SSC get maximum" +  p1.getMaximum());


            /**
             * what I want is  $SMNO Specimen (tube or well) label.
             */
            for (int i=0; i < detectors.length; i++){
                FCSParameter p = file.getParameter (detectors[i]);
                if (p != null){
                    System.out.println ("found it " + detectors[i]+"  "+ p.getIndex());

                }
                else
                    System.out.println(" null for " + detectors[i]);
            }
        FCSTextSegment segment = file.getTextSegment();
        Set<String> attrNames = segment.getAttributeNames();
        String split;
        int np;
        for (String s: attrNames){
            System.out.print ("attribute names " + s + "-----");
            if (s.startsWith ("$P") && s.endsWith ("N")){
                String[] newone = new String[2];
                newone[0] = s;
                newone[1] = segment.getAttribute(s);
                detectorList.add (newone);
            }
            else if (s.startsWith ("$P") && s.endsWith("S")){
                String reagent = segment.getAttribute(s);
                System.out.println (s + " " +  reagent);
            }

            System.out.println (segment.getAttribute(s));
        }
        String[] mydetectors = processTheList (detectorList);
        System.out.println ("--------------------------------------");
        for (int i=0; i < mydetectors.length; i++){
            FCSParameter p =file.getParameter (mydetectors[i]);
            System.out.println (p.getIndex() + "  "+ mydetectors[i]);
        }
        System.out.println ("--------------------------------------");

        String instr = segment.getAttribute ("$CYT");
        System.out.println ("Instrument name is  "+ instr);
        String specimen = segment.getAttribute ("$SMNO");
        System.out.println ("Specimen = " + specimen);
        System.out.println ("specimen name = "+ segment.getAttribute("SPECIMEN NAME"));
        
        String spill = segment.getAttribute ("SPILL");
//        if (spill != null){
//            System.out.println (" SPILL ");
//          //  System.out.println (spill);
//            String[]spillmatrix = spill.split(",");
//            try {
//                int n = new Integer(spillmatrix[0]).intValue();
//                int k = n+1;
//                int[][]matrix = new int[n][n];
//                String[] det = new String[n];
//                for (int i=0; i < n; i++){
//                    det[i] = spillmatrix[i+1];
////                    System.out.println (det[i]);
////                    for (int j=0; j < n; j++){
////                        matrix[i][j]= new Integer(spillmatrix[k++]).intValue();
////                    }
//                }
//               // printIntMatrix (matrix);
//
//            }catch (NumberFormatException e){
//                System.out.println (e.getMessage());
//            }
//        }
//        else
//            System.out.println (" no spill ");

        String comp = segment.getAttribute ("COMP");
        if (comp != null){
            System.out.println (spill);
            String[]spillmatrix = comp.split(",");
            try {
                int n = new Integer(spillmatrix[0]).intValue();
                System.out.println (" found n of the COMP value  " + n);
                int k = n+1;
                int[][]matrix = new int[n][n];
                String[] det = new String[n];
                for (int i=0; i < n; i++){
                    det[i] = spillmatrix[i+1];
                    for (int j=0; j < n; j++){
                        matrix[i][j]= new Integer(spillmatrix[k++]).intValue();
                    }
                }
                printIntMatrix (matrix);

            }catch (NumberFormatException e){
                System.out.println (e.getMessage());
            }
        }
        else
            System.out.println (" no comp matrix ");



//        while (it.hasNext()){
//            FCSParameter param = (FCSParameter) it.next();
//            System.out.println (param.toString());
//        }
            file.close();
        } catch (FCSException e){
            System.out.println (" can't get the parameter list");
        } catch (IOException io){
            System.out.println (" io exception");
        }
        
    }

    private String[]  processTheList (ArrayList<String[]>detectorList) {
        //go through the list and throw out the stuff we don't want.
        ArrayList<String> newlist = new ArrayList<String>();
        for (String[] ss: detectorList){
            if (ss[1].equalsIgnoreCase("Time") || ss[1].startsWith("FSC") || ss[1].startsWith ("SSC") )
                System.out.println (" do nothing " + ss[0] + "  " + ss[1]);
            else{
                newlist.add (new String (ss[1]));
                System.out.println (ss[1]);
            }

        }
        String[] detectors= new String[newlist.size()];
        detectors = newlist.toArray(detectors);

        return detectors;

    }

    private void doAsBefore(FCSFile myfcsfile) {

        ArrayList<String> list = new ArrayList<String>();
        ArrayList<String> reagents = new ArrayList<String>();
        String[] detectorList = null;
        String[] reagentNames = null;
         try {
                  //get some information out of an FCS file
          
//                          System.out.println ("Trying to open this file"  + filelist[0]);
          FCSTextSegment segment = myfcsfile.getTextSegment();
//                          System.out.println ("  do we get here ?  ");
          Set<String> attrNames = segment.getAttributeNames();
          String experimentName = segment.getAttribute ("EXPERIMENT NAME");
          if (experimentName == null) experimentName= new String("");
          String tubename = segment.getAttribute ("TUBE NAME");
          System.out.println ("  tubename?  " + tubename);
          int in=0;
          int np;
          String split;
          for (String s: attrNames){

              if (s.startsWith ("$P") && s.endsWith ("N")){
                  String name = segment.getAttribute (s);
//                  split = s.substring (2, s.length()-1);
//
//                  np = Integer.parseInt (split);
//                  System.out.println (" splitting "+ split + "  "+ np);
                  // but was there data collected on this channel?
                  System.out.println (in + " " + s + " ===  "+ name);
                  if (!name.startsWith ("Time") && !name.startsWith ("FSC")&& !name.startsWith("SSC")){
                      // but was there data collected on this channel?
                       if (!name.endsWith("-H")) {
                           FCSParameter p = myfcsfile.getParameter (name);
                           if (p != null ){
                               int index = p.getIndex()-1;
                               System.out.println (name + "  " + index);
                              list.add (name);
                           }
                           else
                               System.out.println (" no data collected for "+ name);
//                                          System.out.println ("Detector Name |" + name + "|");
//                                          FCSParameter  newreagent = fcsfile.getParameter (name);

                       }
                  }
              }
              else if (s.startsWith ("$P") && s.endsWith ("S")){
                  System.out.println (segment.getAttribute(s));
                  //FCSParameter p = myfcsfile.getParameter (segment.getAttribute(s));
                  reagents.add (segment.getAttribute(s));
                  int index = Integer.parseInt(s.substring(2, s.length()-1));
                  System.out.println (in + "  " + s + " :: " +  segment.getAttribute (s) + " "+ index);
              }
              in++;

          }
          //Detectors are a required field in the standard
          //reagents or fluorochromes are not.
          detectorList = new String[list.size()];
          detectorList = list.toArray (detectorList);
          reagentNames = new String[reagents.size()];
          if ( reagents.size() == 0){
              reagentNames = new String[list.size()];
              reagentNames = list.toArray (reagentNames);
          }
          else {
              reagentNames = new String [reagents.size()];
              reagentNames = reagents.toArray (reagentNames);
          }
          FCSParameter p;
          for (int i=0; i < detectorList.length; i++) {
              System.out.print (detectorList[i] + "  ");
              p = myfcsfile.getParameter (detectorList[i]);
              if (p != null){
                  int index = p.getIndex();
                  System.out.print (index + "  ");
                  p = myfcsfile.getParameter (index);
                  if (p != null)
                      System.out.println (" p is not null");
              }
          }

          myfcsfile.close();
        //  }
      } catch (IOException ioe){

          ioe.printStackTrace();

          System.out.println (ioe.getMessage());
      } catch (FCSException fcse){
          fcse.printStackTrace();
          System.out.println (fcse.getMessage());

      }

    }

    private void printIntMatrix (int[][] matrix){
        for (int i=0; i < matrix.length; i++){
            for (int j=0; j < matrix[i].length; j++)
                System.out.print (matrix[i][j] + "  ");
            System.out.println();
        }
    }


    public static void main (String[] args){
//        String fn = new String ("/Users/cate/FCSData/New Comp Data/3Color.fcs");
//        String fn = new String ("/Users/cate/FCSData/070108_LA03_CH/Compensation Controls_R660 Stained Control.fcs");
//          String fn = new String ("/Users/cate/FCSData/004-M-NKFUNCT-JK/496194.fcs");

//      String fn = new String ("/Users/cate/FCSData/Cate-comp-110303/3-2.fcs");

//        String fn = new String("/Users/cate/FCSData"+File.separator+"KondalaData"
//                +File.separator+"MELAS019-011-V03"+File.separator+"MELAS019-011-V03"+File.separator+ "2-1.fcs");
        String fn = "/Users/cate/FCSData/KondalaData/MELAS019-011-V03/MELAS019-011-V03/2-1.fcs";
        new ExploreFCSFile (fn  );
    }
}
