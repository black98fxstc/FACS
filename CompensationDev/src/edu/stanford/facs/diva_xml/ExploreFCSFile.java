/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.stanford.facs.diva_xml;

import org.isac.fcs.FCSException;
import org.isac.fcs.FCSFile;
import org.isac.fcs.FCSHandler;
import org.isac.fcs.FCSParameter;
import org.isac.fcs.FCSTextSegment;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * $Id: ExploreFCSFile.java,v 1.1 2010/08/30 22:01:35 beauheim Exp $
 * @author cate
 */
public class ExploreFCSFile {

    private String[]detectors={  "Pacific Blue-A","Violet Green-A", "Pacific Orange-A",
                               "Qdot 605-A","APC-A", "APC-Cy7-A","PE-A","PE-Texas-Red-A",
                               "PE-Cy5-A","PE-Cy55-A","PE-Cy7-A", "FITC-A"   };
    
    ExploreFCSFile (String filename) {
        File f = new File (filename);
        if (f.exists() && f.canRead()){
            FCSFile fcsfile = new FCSFile (f);
            read (fcsfile);
        }
    }
    
    private void read (FCSFile file){
        try {

//            int np= file.getParameters();
            List <FCSParameter> list = file.getParameterList();
            FCSHandler handler = file.getOutputIterator();
            Iterator it = list.listIterator();
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
        for (String s: attrNames){
            System.out.println (s);
        }
        
        String spill = segment.getAttribute ("SPILL");
        if (spill != null){
            System.out.println (spill);
            String[]spillmatrix = spill.split(",");
            try {
                int n = new Integer(spillmatrix[0]).intValue();
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
            System.out.println (" no spill ");

        String comp = segment.getAttribute ("COMP");
        if (comp != null){
            System.out.println (spill);
            String[]spillmatrix = comp.split(",");
            try {
                int n = new Integer(spillmatrix[0]).intValue();
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



        while (it.hasNext()){
            FCSParameter param = (FCSParameter) it.next();
            System.out.println (param.toString());
        }
            file.close();
        } catch (FCSException e){
            System.out.println (" can't get the parameter list");
        } catch (IOException io){
            System.out.println (" io exception");
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
        new ExploreFCSFile("data"+File.separator+"eliver"+File.separator+"NO-in vivo 090806"+File.separator+"6-10.fcs");
    }
}
