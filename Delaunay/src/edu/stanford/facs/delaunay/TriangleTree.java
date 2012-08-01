/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.stanford.facs.delaunay;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

/**
 * $Id: Exp $
 * @author cate
 * Container class that will hold all the triangles that are created.
 */
public class TriangleTree {

   
    public Triangle treeRoot;
    private MyPoint[][] validLines;
    private MyPoint[] convexHull;
  
    
    public TriangleTree (Triangle tri){
        
        this.treeRoot = tri;

    }
    
    public void markDead() {
    	treeRoot.setLiveStatus(false);
    }
    
    public Triangle containingTriangle(MyPoint xx) {
        Triangle tri=null;
//        if (treeRoot != null){
//            tri = treeRoot.containingTriangle (xx);
//        }
  //      traverseTree (treeRoot, 0);

        tri = treeRoot;
        System.out.println ("containing triangle for point " + xx.getX() + ", "+ xx.getY());
        int ans = tri.contains (xx);
 //       System.out.println ("\t(1) " + ans );

        
        if (tri.getLiveStatus() && ans == 1){ 
            return tri;
        }
        tri = tryDaughters (xx, tri.getDaughters());
        
        return tri;
        
    }
   /**
    * I have to figure out if there exists a line between each point in the parent 
    * triangle with each point in the daughter triangles.  Then if a line exists,
    * and no level has been assigned, assign a level.
    */
    protected void assignLevelToPoints(Map <Long,MyPoint[]>allLines) {
    	int level=0;
    	MyPoint[] points = treeRoot.getPoints();
    	for (MyPoint pt: points){
    		pt.setLevel(level);
    	
	    	for (Triangle tri: treeRoot.getDaughters()){
	    		tri.assignLevelToPoints(pt, level+1);
	    	}
    	}
    }

    
    public void traverseTree () {
        System.out.println ();
         System.out.println ("------------------------  Traverse Tree  -----------------");
         System.out.println ("  Root 0 "+ treeRoot.toString());
         int level = 1;
         for (Triangle tri: treeRoot.getDaughters()){
             if (tri != null){
//                 System.out.println (level + "  "+ tri.toString());
                 traverseTree (tri, level);
             }
         }
    }
    
    private void traverseTree (Triangle parent, int level){
        //System.out.println ("Parent " + level + "  " + parent.toString());
        level++;
        for (Triangle tri:  parent.getDaughters()){
            if (tri != null){
//                System.out.println (level + "  " + tri.toString());
                traverseTree (tri, level);
                System.out.println (level + "  "+ tri.toString());
            }
        }
        
        
    }
    
    private Triangle tryDaughters (MyPoint fpt, Triangle[] daughters){
        Triangle tri=null;
        int i=0;
        try {
        while (i < daughters.length){
            
//            if (daughters[i] != null){
////               System.out.println ("containing triangle for  " + fpt.xf + ", "+ fpt.yf + " -- "+ daughters[i].toString());     
//               System.out.println ( daughters[i].toString() );
//               System.out.println ("\t (1) "+ daughters[i].contains (fpt));
//
//            }
            if (daughters[i] == null) i++;
            else if (!daughters[i].getLiveStatus()){
                tri = tryDaughters(fpt, daughters[i].getDaughters());
                if (tri == null) i++;
                else
                    break;
            }
            else if(daughters[i].contains (fpt)==1 ){
//              else if(daughters[i].contains (fpt.xf, fpt.yf) ){
                tri = daughters[i];
                break;
                
            }
            else i++;
           // if (i == daughters.length) tri = null;

        }
//        if (tri != null)
//            System.out.println (tri.toString());
//        else
//            System.out.println (" no containing triangle was found ");
        } catch (StackOverflowError e){
            System.out.println (" Stack overflow");
            tri = null;
        }
        
        return tri;
    }

 

/**

    private Triangle findTriangleInTree (Point a, Point b, Point c, Triangle root){
        Triangle tri = null;
        boolean found = false;

        int id = a.hashCode() ^ b.hashCode() ^ c.hashCode();
        while (!found){
            if (root != null){
                Triangle[] daughters = root.getDaughters();
                for (int i=0; i < daughters.length; i++){
                    if (daughters[i] != null){
                        if (daughters[i].getId() == id){
                            found = true;
                            tri = daughters[i];
                            break;
                        }
                        else {
                            Point aa = new Point (daughters[i].xpoints[0], daughters[i].ypoints[0]);
                            Point bb = new Point (daughters[i].xpoints[1], daughters[i].ypoints[1]);
                            Point cc = new Point (daughters[i].xpoints[2], daughters[i].ypoints[2]);
                            findTriangleInTree (aa, bb, cc, daughters[i]);
                        }
                    }
                }
            }
        }
//        System.out.println ("findTriangle in Tree " + root.toString());
//        if (tri != null)
//        System.out.println  (tri.toString());
        return tri;
    }

    private Triangle whichDaughter (Triangle parent, Point a, Point b, Point c){
        Triangle[]daughters = parent.getDaughters();
        Triangle one = null;
        int id = a.hashCode()^ b.hashCode()^ c.hashCode();

            if (daughters[0] != null && daughters[0].getId() == id){
                one= daughters[0];
            }
            else if (daughters[1] != null && daughters[1].getId() == id){
                one =daughters[1];
            }
            else if (daughters[2] != null && daughters[2].getId() == id){
                one = daughters[2];
            }
            else {
                System.out.println (" couldn't find it ");
            }
        return one;
    }

   **/
    /* Return the list of valid lines and save the list of points on the convex hull
     * if remove is true, then do not include the outer triangle and its connecting
     * lines.  if false, all lines are retained.
     * file format is four columns x1 y1 x2 y2 representing all lines in a live triangle
     * */
    /** not being called  **/
    public void printDataFrame(boolean remove){
    	File f = new File ("./printRR2.r");
        FileOutputStream fw = null;
    	try{
    		if (treeRoot != null){
    			fw = new FileOutputStream (f);
    			String h = "x1\ty1\tx2\ty2\n";
    			fw.write(h.getBytes());
    		}
    	}catch (IOException e){
            System.out.println (" file io exception ");
        } finally {
            
        }
    	
    }
    
    public void pruneLineData (){
    	
    	
    	treeRoot.prune(0, treeRoot.getPoints(), true);
    }
    
    public MyPoint[][] getLineData() {
    	ArrayList<MyPoint[]> lines = treeRoot.getLineData();
    	MyPoint[][] asarray = new MyPoint[lines.size()][2];
    	asarray = lines.toArray(asarray);
    	return asarray;
    }
    
    public void printDataFrame (){
    	File f = new File ("./printRR.r");
        FileOutputStream fw = null;
       // printDataFrame
        try{
        	if (treeRoot != null){
	        	fw= new FileOutputStream (f);
	        	String h = "x1\ty1\tx2\ty2\n";
	        	System.out.print(h);
	            fw.write(h.getBytes());
	            //include the bounding triangle
	            treeRoot.printDataFrame(fw);
	            
	          //  printTree(treeRoot.getDaughters(), fw);
	        	fw.close();
        	}
        } catch (IOException e){
            System.out.println (" file io exception ");
        } finally {
            
        }
        
       
    }
    
    /** not called
    public void printTreeForR (Triangle root, MyPoint[] bbox){
        
        File f = new File ("./printR.r");
        FileOutputStream fw = null;
        try {
        fw = new FileOutputStream (f);
        if (root != null){
        	String s ="plot.new();\n";
        	fw.write (s.getBytes());
        	String xx = "xb <-c("+bbox[0].getX()+","+bbox[1].getX()+","+bbox[2].getX()+","+bbox[0].getX()+");\n";
        	String yy = "yb <-c("+bbox[0].getY()+","+bbox[1].getY()+","+bbox[2].getY()+","+bbox[0].getY()+");\n";
        	fw.write(xx.getBytes());
        	fw.write(yy.getBytes());
        	String plot = "plot (xb,yb,type=\"n\");\n";
        	fw.write(plot.getBytes());
        			

        	printTree (root.getDaughters(), fw);
//            Triangle[] daughters = root.getDaughters();
//            
//            for (Triangle tri: daughters){
//                // (tri != null && tri.getLiveStatus()){
//                if (tri != null){
//                    printTree (tri, fw);
////                    String s = tri.printForR();
////                    if (s != null || !s.equals(""))
////                        fw.write (s.getBytes());
//                }
//            }
        }
        fw.close();
        } catch (IOException e){
            System.out.println (" file io exception ");
        } finally {
            
        }
    }**/

/**
    public void printTree(Triangle[] daughters, FileOutputStream fw) throws IOException {
      

//        if (root != null  && !root.getLiveStatus()) {
//            System.out.println ("PrintTree Parent" + root.toString());
//            Triangle[] daughters = root.getDaughters();
          
            for (Triangle tri : daughters){
                if ( tri != null) {
                    if (!tri.getLiveStatus()){
                       // System.out.println ("daughters " +  " " +tri.toString());
                        printTree (tri.getDaughters(), fw);
//                    String s = tri.printForR();
//                    System.out.println (s);
//                    fw.write (s.getBytes());
                    }
               
                    else {
                     //   String s = tri.printForR();
                    	tri.printDataFrame(fw);
                      
                    }
                }
            }
    }**/
    
  

    
}
