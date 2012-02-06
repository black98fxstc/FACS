/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.stanford.facs.exp_annotation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.Writer;
import java.util.Set;
import org.isac.fcs.FCSFile;
import org.isac.fcs.FCSTextSegment;

/**
 * $Id: FlowJoFiles.java,v 1.1 2012/01/25 01:41:07 beauheim Exp $
 * @author cate
 * all one string with no line breaks.  this is wsp
 * <Keyword name="SPILL" value="18,FITC-A,PerCP-Cy5-5-A,Pacific Blue-A,
 * Aqua Amine-A,Pacific Orange-A,Qdot 585-A,Qdot 605-A,Qdot 655-A,Qdot 705-A,
 * Qdot 800-A,APC-A,APC-Cy5-5-A,APC-Cy7-A,PE-A,PE-Texas-Red-A,PE-Cy5-A,PE-Cy55-A,
 * PE-Cy7-A,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
 * 0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
 * 0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,
 * 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,
 * 0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,
 * 0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,
 * 0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
 * 0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1"/>
 *
 *
 *  there is a tab before SPILL and a tab after SPILL and a tab after the last matrix value.
LIN     SPILL   18,FITC-A,PerCP-Cy5-5-A,Pacific Blue-A,Violet Green-A,Pacific Orange-A,Qdot 585-A,Qdot 605-A,Qdot 655-A,Qdot 705-A,Qdot 800-A,APC-A,APC-Cy5-5-A,APC-Cy7-A,PE-A,PE-Texas-Red-A,PE-Cy5-A,PE-Cy55-A,PE-Cy7-A,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,
0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,
0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,
0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,
0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,
0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1       
 */
public class FlowJoFiles {
    String[]detectorNames;
    Float[][] matrix;
    File newjofile;
    private StreamTokenizer tokens;
    private int ncommas;
    
    public FlowJoFiles (){
        File fcsfile = new File ("/Users/cate/FCSData/KondalaData/MELAS019-011-V03/MELAS019-011-V03/2-1.fcs");
       replaceSpillInFcs(fcsfile, null);
    }
    
    
    public FlowJoFiles (String infile, String matrixfile){
        File mfile = new File (matrixfile);
        File jofile = new File (infile);
        String ext;
        if (mfile.exists() && mfile.canRead()){
            readMatrix (mfile);
        }
        if (jofile.getName().endsWith (".wsp"))
            ext = ".wsp";
        else 
            ext = ".jo";
        String matrixAsString = writeMatrixAsString (matrix, detectorNames, ext);
        System.out.println (matrixAsString);
        openJoFile (jofile, matrixAsString);
    }
    
    public FlowJoFiles (File jofile, Float[][] matrix, String[] detectorNames){
        
        String ext;
        if (jofile.getName().endsWith (".wsp"))
            ext = ".wsp";
        else 
            ext = ".jo";
        String matrixAsString = writeMatrixAsString (matrix, detectorNames, ext);
        openJoFile (jofile, matrixAsString);
    }
    
    public final String writeMatrixAsString (Float[][] matrix, String[] detectorNames, String ext){
        boolean finished = true;
        
        StringBuilder buf=null;
        int dim = detectorNames.length;
        //build the string that is going to get inserted.
        //name="SPILL" value ="18, names, names,..."/>
        if (ext.equals (".wsp")){
            buf = new StringBuilder(" value=\"");
//            buf.append(dim).append(",");
            buf.append (writeSpillData (matrix, detectorNames));
            buf.append ("\"/>").append ("\r");

        }
        else if (ext.equals (".jo")){
            buf= new StringBuilder ();
            buf.append (writeSpillData (matrix, detectorNames));
           
        }
//        System.out.println (buf.toString());
        
        return buf.toString();
        
    }
    
    private String writeSpillData (Float[][] matrix, String[] detectorNames) {
        int dim = detectorNames.length;
        StringBuilder buf = new StringBuilder();
        buf.append(" ").append (dim).append(",");
        for (String s: detectorNames){
            buf.append (s).append(",");
        }
        for (int i=0; i < dim; i++){
            for (int j=0; j < dim; j++){
                buf.append (matrix[i][j].floatValue());
                buf.append(",");
                  
            }
        }
     //   buf.append (matrix[dim-1][dim-1].floatValue());
        
System.out.println ("-------------------------------------");
System.out.println (buf.toString());
System.out.println ("-------------------------------------");
String clip = buf.substring (0, buf.length()-1)+ " ";

System.out.println (clip);
        
        return clip;
        
    }
    
    char[] makeJoMatrixString (Float[][]matrix, String[] detectorNames){
        StringBuilder buf = new StringBuilder();
        
        char[] asChars = new char[buf.toString().length()];
        asChars = buf.toString().toCharArray();
        
        return asChars;
    }
    private void readMatrix (File file){
        int row=0;
        int dim;
        
        if (file.exists() && file.canRead()){
            try {
                BufferedReader reader = new BufferedReader (new FileReader(file));
                String line = reader.readLine();
                line = reader.readLine();  //skip the first two lines.  
                line = reader.readLine();
                detectorNames = line.split("\t");
                dim = detectorNames.length;
                matrix = new Float[dim][dim];
                while ((line = reader.readLine()) != null){
                    String[] data = line.split("\t");
                    for (int i=0; i < data.length; i++){
                        float ff = Float.parseFloat (data[i]);
                        matrix[row][i]= new Float (ff);
                    }
                    row++;
                    
                }
                reader.close();

            } catch (IOException ioe){
                
            }
        }
        
        
        
    }
    
    private void openJoFile (File jofile, String matrixAsString){
        
        
        CharArrayWriter charwriter = new CharArrayWriter (8192);
        File newjo = new File (jofile.getParent() + File.separator + "M_"+ jofile.getName());
        System.out.println (newjo.getPath() + "  " + newjo.getName());
        Writer writer = null;
        Reader reader = null;
        
        
        if (jofile.exists() && jofile.canRead()){ 
            long size = jofile.length();
            System.out.println (size);
            int ndet=0;
            
            try {
                writer = new BufferedWriter (new FileWriter (newjo));
                reader = new BufferedReader (new FileReader (jofile));
                tokens = new StreamTokenizer (reader);
                tokens.resetSyntax();
                tokens.eolIsSignificant(false);
                tokens.wordChars(32, 125);
                tokens.ordinaryChar ('$');

                int type = tokens.nextToken();
                int ii=0;
                int count = 0;
                while (type != StreamTokenizer.TT_EOF){
                     System.out.println (type); 
                    switch (type){
//                        case StreamTokenizer.TT_NUMBER:
//                            charwriter.append (new Float (tokens.nval).toString());
//                            break;   
                      
                        case StreamTokenizer.TT_WORD:
                            
                            if (tokens.sval.equals("SPILL") || tokens.sval.contains ("SPILL")){
                                //in xml, it is the whole line starting with <Keyword name="SPILL"...
                              if (jofile.getName().endsWith(".wsp")){
                                  System.out.println (tokens.sval);
                                  StringBuilder buf = new StringBuilder ("<Keyword name=\"SPILL\" ");
//                                  buf.append ("\"" ).append(detectorNames.length);
                                  buf.append(matrixAsString);
                                //  buf.append ("\"/>\"");
                                  charwriter.append (buf.toString());
                                  charwriter.writeTo(writer);
                                  charwriter.reset();
                              }  
                              else {
                                System.out.println ("Spill keyword " + count++);
                                charwriter.append(tokens.sval); // that is the spill word
                                type = tokens.nextToken(); // get past the keyword.
                                if (tokens.ttype == '\"'){
                                    charwriter.append((char) tokens.ttype);
                                    type = tokens.nextToken();
                                    System.out.println (type + "  "+ (char) tokens.ttype);
                                }
                                System.out.println (type + "  "+ (char) tokens.ttype);  
                                ndet = skipOver (tokens);
                                //in wsp this is a "
                           //     charwriter.append(detectorNames.length+ ",");
                                charwriter.append (matrixAsString);
                             //   tokens = skipOverMatrix ( tokens, ndet);
                                charwriter.writeTo(writer);
//                                System.out.println ("----------->" +charwriter.toString());
                                charwriter.reset();
                            }
                            }

                            else {
                                charwriter.append (tokens.sval);
                            }
                    
                            break;
                        default:
                            char ch = (char) tokens.ttype;
                            charwriter.append(ch);
                            break;

                    
                    }
                    type = tokens.nextToken();
                }
                System.out.println (" --------------end of file--------------");
                charwriter.writeTo (writer);
                
                  
                
            } catch (IOException ioe){
                System.out.println  (ioe.getMessage());
            }finally {
                try{
                    if (writer != null)
                        writer.close();
                    if (reader != null)
                        reader.close();
                } catch (IOException e){
                    System.out.println (e.getMessage());
                }
            }
            
        }
        
        
        
    }
    
    private void replaceSpillInFcs (File fcsfile, String matrixAsString){
        Writer writer = null;
        Reader reader = null;
        boolean flag = false;
        FCSFile fcs = new FCSFile(fcsfile);
        
        if (fcsfile.exists() && fcsfile.canRead()){ 
            try {
                RandomAccessFile raf = new RandomAccessFile (fcsfile, "rw");
                FCSTextSegment segment = fcs.getTextSegment();
                
        Set<String> attrNames = segment.getAttributeNames();
        String split;
        int np;
        for (String s: attrNames){
            System.out.print ("attribute names " + s + "-----");
            if (s.contains ("SPILL") ){
                String[] newone = new String[2];
                newone[0] =s;
                newone[1] = segment.getAttribute(s);
               System.out.println (newone[0] + ", "+ newone[1]);
            }
           
        }
                
            

            } catch (Exception e){
                System.out.println (e.getMessage());
            }
        }
    }
    
    private int skipOver (StreamTokenizer stream ) throws IOException {
        double ndet=0;
        
        int type = stream.nextToken();
        if (type == StreamTokenizer.TT_WORD){
            System.out.println ("(1) " + stream.sval);
            type = stream.nextToken();
            System.out.println ( "now what is the type "+ type);
        }
        else if (type == StreamTokenizer.TT_NUMBER)  {
             ndet = stream.nval;
            System.out.println (ndet + "  where now? ");
            
        }
        
            ndet = stream.nval;
        
        return (int) ndet;
    }
    
    private StreamTokenizer skipOverMatrix (StreamTokenizer stream, int nparameters) throws IOException{
       int i= 0;  
       int type = 0;
       if (stream.sval.equals ("$P6G"))
           return stream;
       if (ncommas == 0){
           ncommas = ( nparameters * (nparameters+1))+ 1 ;
       }
       while (i < ncommas){
           type = stream.nextToken();
           if ((char) stream.ttype == ','){
               i++;
           }
       }
        
//        int type = stream.nextToken();
//        while (type != StreamTokenizer.TT_NUMBER){
//            System.out.println ("["+(char) stream.ttype+"]");
//            if (type == StreamTokenizer.TT_WORD && stream.sval.equalsIgnoreCase("QDOT")){
//                type = stream.nextToken();
//                type = stream.nextToken();
//            }
//            type = stream.nextToken();
//        }
//        if (type == StreamTokenizer.TT_NUMBER){
//            int nparameters = (int) stream.nval;
//            int i=0;
//            if (ncommas == 0)
//            ncommas = ( nparameters *(nparameters +1))- 1;
//          
////            for (int i=0; i < nparameters; i++){
//            while (i < ncommas) {
//                type = stream.nextToken();
//                if ((char) stream.ttype == ','){
//                
//                        i++;
//                }
//            }
//        }
      System.out.println ("  Skip over the matrix ");  
            
        return stream;
        
    }
    
    
    
    public static void main (String[] args){
        
//        String file = "/Users/cate/FCSData/Cate-comp-110303/Cate-compmatrix.jo";
        String file = "/Users/cate/FCSData/Cate-comp-110303/Cate-compmatrix.wsp";
        

        String matrixfile = "/Users/cate/FCSData/Cate-comp-110303/matrix-0707.txt";
       // FlowJoFiles flowjo = new FlowJoFiles(file, matrixfile);
        FlowJoFiles flowjo = new FlowJoFiles();
    }
    
}
