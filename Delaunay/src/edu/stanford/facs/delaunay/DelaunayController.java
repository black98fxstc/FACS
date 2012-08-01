/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.stanford.facs.delaunay;

import java.io.File;
import org.isac.fcs.FCSFile;
import edu.stanford.facs.drawing.DrawingFrame;
import java.util.Random;

/**
 * $Id: Exp $
 * @author cate
 */
public class DelaunayController {

   // private float[][] data;
    private DrawingFrame frame;
    private Delaunay delaunay;
    private String[] detectors;
  //  private FloatingPoint[] initialPoints = new FloatingPoint[4];
  //  private TriangleTree tree;
    private int NPTS=50;
    		

    DelaunayController() {
    	float[][]mydata = new float[NPTS][2];
        
        Random random = new Random (314128759);
        for (int i=0; i < NPTS; i++){
            mydata[i][0] = random.nextFloat()*10;
            mydata[i][1] = random.nextFloat()*10;
            System.out.println (mydata[i][0] + "\t"+ mydata[i][1]);
           /** mydata[i][0] = (float)random.nextGaussian()*10;
            mydata[i][1] = (float)random.nextGaussian()*10;**/

        }
        
      
    //   float [][] sample = dataReader.getRandomSample(data, 8, 13);
         delaunay = new Delaunay ( mydata);
    }
    
    /*
     * Now the data is real, collected by detector.  So maybe 16 x10,000 or something.  To get
     * x,y data, we have to select some parameters.  When I put it in MyPoint, I also want unique
     * points.  So I want to keep track of how many points are the same for each representation in
     * MyPoint
     */
    DelaunayController (FCSFile fcs) {

       DelaunayData dataReader = new DelaunayData (fcs);
 //      frame = new DrawingFrame();
       float[][] mydata= null;
       if (fcs != null && fcs.getFile().canRead()) {
          mydata = dataReader.read();
         //make it transformed
 
          detectors = dataReader.getDetectors(fcs);
       }
       if (mydata == null || mydata.length == 0){
           System.out.println ("  Error reading the file");
          System.exit(1);
      }

//       // 19 x 3000
//       //Get a unique list also.  Make unique first, then get a random sample.
//       FloatingPoint[] asFP = dataReader.getAsFloatingPoints(data, 8, 13);
//       float[][] floatdata = dataReader.getUniqueSamples (asFP);
//       //it isn't necessarily sorted at this point.
//       
//       System.out.println ("  how many data points?  "+ floatdata.length + "  "+ floatdata[0].length);
       System.out.println(" how many data points "+ mydata.length + ",  "+ mydata[0].length);
       MyPoint[] mypts = dataReader.getUniqueSamples (mydata, 14, 15);
       
//       float[][] rsamples = dataReader.getRandomSample ( uniqueFP);
//       
       
         delaunay = new Delaunay ( mypts);
       

    }



 

    public static void main (String[] args){
        String fn = "/Users/cate2/Eclipse/workspace/Delaunay/data/1_ip_C.fcs";//or 5_ivC.fcs
        File file = new File (fn);
        FCSFile fcs = new FCSFile (fn);
        DelaunayController controller = new DelaunayController (fcs);
        //  DelaunayController controller = new DelaunayController();
    }

}
