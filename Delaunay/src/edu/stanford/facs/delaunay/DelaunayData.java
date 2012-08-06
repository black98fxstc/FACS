/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.stanford.facs.delaunay;

import edu.stanford.facs.data.FlowData;
import edu.stanford.facs.logicle.Logicle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import org.isac.fcs.FCSException;
import org.isac.fcs.FCSFile;
import org.isac.fcs.FCSParameter;
import org.isac.fcs.FCSTextSegment;

/**
 * $Id: Exp $
 * @author cate
 */
public class DelaunayData extends FlowData{
    
    float minx = 100000000, maxx=-100000000;
    float miny = 100000000, maxy=-100000000;
    private static int BIN_SPACE=1000;
    MyPointBin[][] bins;
    
    
    //float[][] fcsdata;
    public DelaunayData (FCSFile fcsfile){
        super(fcsfile);
    }
    
    /**
     * The fcs data comes back as the [# of parameters][# of events], such as [19,200000]
     * @return 
     */
    public float[][] read() {
        float[][] fcsdata= null;
        try {
           fcsdata = super.read();
          // System.out.println (fcsdata.length + ", "+ fcsdata[0].length);
        }catch (FCSException e){
            System.out.println (e.getMessage());
        }catch (IOException ioe){
            System.out.println (ioe.getMessage());
        }
        
       
        return fcsdata;
    }
    
    public MyPoint[] getAsFloatingPoints(float[][] data, int xpar, int ypar) {
        MyPoint[] fp = null;
        
        if (xpar >= data.length || ypar >= data.length){
            System.out.println ("  one of the parameters is out of bounds "+ xpar + ", "+ ypar);
            
            return null;
        }
        fp = new MyPoint[data[xpar].length];
        for (int i =0; i < data[xpar].length; i++){
            fp[i] = new MyPoint(data[xpar][i], data[ypar][i]);
        
        }
        return fp;
    }

    public String[] getDetectors (FCSFile myfcsfile) {

        ArrayList<String> list = new ArrayList<String>();

        String[] detectorList = null;
         try {

          FCSTextSegment segment = myfcsfile.getTextSegment();
          Set<String> attrNames = segment.getAttributeNames();
          String experimentName = segment.getAttribute ("EXPERIMENT NAME");
          if (experimentName == null) experimentName= "";
          int in=0;
          int np;
          String split;
          for (String s: attrNames){

              if (s.startsWith ("$P") && s.endsWith ("N")){
                  String name = segment.getAttribute (s);

                  if (!name.startsWith ("Time") && !name.startsWith ("FSC")&& !name.startsWith("SSC")){
                      // but was there data collected on this channel?
                       if (!name.endsWith("-H")) {
                           FCSParameter p = myfcsfile.getParameter (name);
                           if (p != null ){
                               int index = p.getIndex()-1;
                              list.add (name);
                           }
                           else
                               System.out.println (" no data collected for "+ name);

                       }
                  }
              }

              in++;

          }
          //Detectors are a required field in the standard
          //reagents or fluorochromes are not.
          detectorList = new String[list.size()];
          detectorList = list.toArray (detectorList);

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

          System.out.println (ioe.getMessage());
      } catch (FCSException fcse){
          System.out.println (fcse.getMessage());

      }
      return detectorList;
    }
    
    public float[][] getUniqueSamples (MyPoint[] fp){
       
        int []index = new int[2];
        
        Arrays.sort(fp);
        for (int i=0; i < fp.length; i++){
//            System.out.println (fp[i].getX() + ", "+ fp[i].getY());
            if (fp[i].getX() < minx) {
                minx=fp[i].getX();
            }
            else if (fp[i].getX() > maxx) 
                    maxx=fp[i].getX();
            if (fp[i].getY() < miny) miny=fp[i].getY();
            else if (fp[i].getY() > maxy) maxy=fp[i].getY();
        }
        System.out.println ("Min and max:  ("+minx + ","+ miny + ") ("+maxx + ","+ maxy+ ")");
        
        //create bins
      //  createBins (minx, miny, maxx, maxy);
        
        
        //bin the data
        
        int nbins = bins.length;
        //for (int i=0; i < fp.length; i++){
        for (int i=0; i < fp.length; i++){
             index = binarySearch (fp[i]);
//             System.out.println (fp[i].getX() + ", "+ fp[i].getY()+ "----" + index[0] + ", "+ index[1]);
             if (index[0] < nbins && index[1] < nbins){
                 MyPointBin bone = bins[index[0]][index[1]];
                 boolean b = bone.contains (fp[i].getX(), fp[i].getY());
                 if (!b){
                     System.out.println ("--------Not found-------------------");
                     System.out.println ("\t"+ bone.toString());
                     System.out.println ("\t" + fp[i].getX() + ",  "+ fp[i].getY());
                     System.out.println ("----------------------------");
                 }
//                 if (bone.contains (fp[i].getX(), fp[i].getY())){
//                     System.out.println ("..............." + bone.toString());
//                 }
//                 else {
//                     System.out.println ("!!!  not found !!!! " + bone.toString());
//                 }
             }
        }
        System.out.println ("***********************************");
        ArrayList<float[]>finaldata = new ArrayList<float[]>();
        for (int i=0; i < bins.length; i++){
            for (int j=0; j < bins[i].length; j++){
                if (bins[i][j].points.size() > 0){
                    bins[i][j].computeAverage();
                    //System.out.println ( bins[i][j].toString());
                    float[] one = new float[2];
                    one[0] = bins[i][j].avex;
                    one[1] = bins[i][j].avey;
                    finaldata.add (one);
                }
              
            }
        }
        
        
        float[][] data = new float [2][finaldata.size()];
        data = finaldata.toArray (data);
        
        return data;
    }
    
    private void createBins (float minx, float miny, float maxx, float maxy){
        float binmin,  binmax;
        
        if (minx < miny){
           binmin = minx; 
           
        }
        else 
            binmin = miny;
        if (maxx > maxy)
            binmax = maxx;
        else
            binmax = maxy;
        
        int xbins = (int) (binmax - binmin) / BIN_SPACE + 1;
        int ybins = xbins;
        float curx, cury;
        curx= binmin;
        cury = binmin;
        bins = new MyPointBin[xbins][ybins];
       
        for (int i=0; i < xbins; i++){
            for (int j=0; j < ybins; j++){
                bins[i][j] = new MyPointBin (curx, cury, BIN_SPACE, BIN_SPACE); 
                cury += BIN_SPACE;
                
            }
            cury = binmin;
            curx += BIN_SPACE;
        }
        
        
    }
    
   
    public MyPoint[] getUniqueSamples (float[][] data, int xpar, int ypar){
    	
    	int length = data[xpar].length;
       // float[][]logicle = transformToLogicle (data, xpar, ypar);
        Map <Long, MyPoint> uniquePoints = new HashMap <Long,MyPoint>();
        
        for (int i=0; i < length; i++){
        	//if (logicle[xpar][i] < 10000 && logicle[ypar][i]<10000){
        	//MyPoint pt = new MyPoint (logicle[xpar][i], logicle[ypar][i]);
        	//MyPoint pt = new MyPoint (logicle[0][i], logicle[1][i]);
        	//do the calculations before the logicle transformation
        	if (data[xpar][i] < minx) minx=data[xpar][i];
        	else if (data[xpar][i]>maxx) maxx=data[xpar][i];
        	
        	if (data[ypar][i] < miny) miny = data[ypar][i];
        	else if (data[ypar][i] > maxy) maxy= data[ypar][i];
        	
            MyPoint pt = new MyPoint (data[xpar][i], data[ypar][i]);
        	Long key = new Long(pt.hashCode());
        	if (uniquePoints.containsKey(key)){
        		uniquePoints.get(key).addOne();
        		//System.out.println (" duplicate found ");
        	}
        	else {
        		pt.addOne();
        		uniquePoints.put(key, pt);
        	}
        	//}
        		
        }
       
        MyPoint[] unique = new MyPoint[uniquePoints.size()];
        unique = uniquePoints.values().toArray(unique);
//       System.out.println ("Min x,y and max x,y " +minx + ", "+ miny + ", "+ maxx + ", "+ maxy); 
        return unique;
        
    }
    public float[][] getRandomSample (MyPoint[] fp){
        int size=300;
        float[][] sampleOf = new float[2][size]; 
        Random random = new Random (3234567);
        
        int i=0;
        while (i < size){
            int idx = random.nextInt (fp.length);
            sampleOf[0][i] = fp[idx].getX();
            sampleOf[1][i] = fp[idx].getY();
//            System.out.println (i + ". "+ fp[idx].getX() + ","+fp[idx].getY() + " :: "+ sampleOf[0][i] + ", "+ sampleOf[1][i]);
           
            
            i++;
        }
        
        
        return sampleOf;
    }

    public float[][] getRandomSample(float[][] origData, int xpar, int ypar) {

       
        int size = 3000;
        int max=0;
        if (origData != null)
           max = origData[0].length;
          //fluorescent data needs another value for its gate info
        float[][] sample = new float[2][size];
        
        Random random = new Random(3234567);
        int i=0;
        while (i < size){
            int idx = random.nextInt(max);
            if (origData[xpar][idx] > 0 && origData[ypar][idx]>0){
//              System.out.println (max + "  "+idx );
                sample[0][i] = origData[xpar][idx];
                sample[1][i] = origData[ypar][idx];
               i++;
            }
//            if (i < 100){
//                System.out.print (sample[0][i]+", "+ sample[1][i] + "   ");
//                System.out.println (Math.log(sample[0][i])+ "  "+ Math.log(sample[1][i]));
//            }
              
        }
//        System.exit(1);
        return sample;

    }
    
   class MyPointBin extends Rectangle2D.Float {
        ArrayList<MyPoint> points = new ArrayList<MyPoint>();
        float avex=0, avey=0;
        
        //upper left hand corner
       MyPointBin (float x, float y, float width, float height){
           this.x = x;
           this.y = y;
           this.width = width;
           this.height = height;
       }
       
       public float[] computeAverage() {
            
           float[] average = new float[2];
           if (points.size() == 0){
               average[0] = 0;
               average[1] = 0;
           }
           else {
               float sumx=0, sumy=0;
               for (MyPoint fp : points){
                   sumx += fp.getX();
                   sumy += fp.getY();  
               }
               average[0] = sumx / points.size();
               average[1] = sumy / points.size();
               avex = average[0];
               avey = average[1];
           }
           return average;
       }
       
       public String toString() {
           StringBuilder buf = new StringBuilder();
           buf.append (x).append(", ").append(y);
           
           buf.append (" Points in this bin = ").append( points.size());
           buf.append("   Average x, y ").append(avex).append(", ").append (avey);
           return buf.toString();
           
       }
       
       public boolean contains (float xin, float yin){
           boolean in = contains ((double)xin, (double)yin);
           if (!in ){
               float w = x + width;
               float h = y + height;
               System.out.println ("Contains Not in " + xin + ", "+ yin + ":: Min "+ x + ", "+ y + " -- Max "+ w + ", "+ h );
           }
           if (in){
               points.add (new MyPoint (xin, yin));
           }
           return in;
           
       }
       public int containsx (float xin){
           int flag=0;
           if ((xin < x) ){
                   flag = -1;
           }
           else if (xin > x+width)
               flag=1;
           
           return flag;
       }
       public int containsy (float yin){
           
           int flag=0;
           
           if (yin < y)      
               flag=-1;
           
           else if (yin > y+ height)
               flag=1;
         
           return flag;
       }
    }
    /***
      W=.5, M=4.5, A=0
      //public Logicle (double T, double W, double M, double A)
       * 
       * @param data
       * @return
       */
    public float[][] transformToLogicle(float[][]data, int xpar, int ypar){
    	double T=1<<18;
    	double W=0.5;
    	double M=4.5;
    	double A=0;
    	
    	int length = data[xpar].length;
    	float[][] logicalData = new float[2][length];
    	Logicle logicle = new Logicle (T, W, M, A);
    	for (int i=0; i < length; i++){
    		 logicalData[0][i] = (float)logicle.scale (data[xpar][i]);
    		 logicalData[1][i] =(float) logicle.scale (data[ypar][i]); 
    		 
    	}
    	
    	return logicalData;
    	
    	
    }
    public  int[] binarySearch (MyPoint fp){
        int []index = new int[2];
        
        //first search x, then y;
        int lowx=0, lowy = 0; 
        int highx = bins.length-1;
        int highy = bins.length-1;
        int midx, midy;
        
        int flagx=0, flagy=0;
        int LESS = -1;
        int MORE = 1;
//        System.out.println ("---------------------binary search for " + fp.getX() + ", "+ fp.getY());
        while (lowx <= highx && lowy <= highy){
            midx = (lowx + highx)/2;
            midy = (lowy + highy)/2;
            flagx = bins[midx][midy].containsx(fp.getX());
            flagy = bins[midy][midy].containsy (fp.getY());
//            if (fp.getY() > 260000){
//          
//                System.out.println ("______________________________________________");
//                System.out.println ("\t midx,y "+ midx + ", "+ midy + ":  "+ flagx + ", "+ flagy);
//                System.out.println ("\tlow, high "+ lowx + ","+lowy + " :: "+ highx + ", "+highy);
//            }
            if (flagx == LESS){
                highx = midx - 1;
                if (flagy == LESS){ 
                    highy = midy - 1;
                }
                else if (flagy == MORE){   
                    lowy = midy+1;   
                }
                else {
                    index[1] = midy;
                    
                }
                
            }
            else if (flagx == MORE){
//                if (fp.getX() > 200000){
//                   System.out.println (fp.getX() + ", " + fp.getY() );
//                   System.out.println (flagx + ", "+ flagy + " -- "+ midx + ", "+ midy);
//                   
//                }
                lowx = midx + 1;
                if (flagy == LESS){
                    highy = midy - 1; 
//                    System.out.println ("More and less "+ lowx + ", "+ lowy + " -- "+ midx + ", "+ midy + " -- "+ highx + ", "+ highy);
                }
                else if (flagy == MORE){
                    lowy = midy + 1;
                }
                else {
                    index[1] = midy;
                }
                
            }
            else {
                index[0] = midx;
               if (flagy == LESS){
                   highy = midy -1;
               } 
               else if (flagy == MORE){
                   lowy = midy +1;   
               }
               else {
                   index[1] = midy;
                    lowx=highx+1;
                    lowy = highy+1;
               }
//                index[0] = midx;
//                index[1] = midy;
            }          
            
        }
        int nbins = bins.length-1;
        if (flagx != 0){  
            if (flagx == MORE){
                flagx = bins[nbins][index[1]].containsx (fp.getX());
                if (flagx == 0)
                    index[0] = nbins;
            }
            else if (flagx == LESS){
                flagx = bins[0][index[1]].containsx(fp.getX());
                if (flagx == 0) index[0] = 0;
            }
        }
        if (flagy != 0){
            if (flagy == MORE){
                flagy = bins[index[0]][nbins].containsy (fp.getY());
                if (flagy == 0)
                    index[1] = nbins;
            }
            else if (flagy == LESS){
                flagy = bins[index[0]][0].containsy (fp.getY());
                if (flagy == 0)
                    index[1] = 0;
            }
        }
            
        
        return index;
    }
    
    /**
    public void plotData (float[] x, float[] y, int[] gates,
                          double T, double W, double M, double A){

        this.gates = gates;
        setXscale (0, SWIDTH);
        setYscale (0, SHEIGHT);
 
        whitebackground = new Rectangle (xintmin+1, yintmax+1, xintmax-xintmin-2, yintmin-yintmax-2);
        if ( x == null || y == null){
            return;
        }
        if (x.length != y.length){
            return;
        }
       // int T = 1<<18;
         logicle = new Logicle(T, W, M, A);
        

        makeAxis();
        displayData = new int[2][x.length];
     

        for ( int i= 0; i< x.length; i++ ){
          
            double xx = logicle.scale(x[i]);
            if (xx < 0.) xx = 0.0;
            else if (xx > 1.0) xx = 1.0;

            double yy = logicle.scale(y[i]);
            if (yy < 0) yy = 0;
            else if (yy > 1.0) yy = 1.0;
  
           displayData[0][i] = (int) scaleX(xx);
           displayData[1][i] = (int) scaleY(yy);

        }*/
    
    
    
}
