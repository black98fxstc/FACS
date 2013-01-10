/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.stanford.facs.delaunay;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Map;

/**
 * $Id: Exp $
 * @author cate
 * Container class that will hold all the triangles that are created.
 */
public class TriangleTree {

   
    public Triangle treeRoot;
  
    
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
  //      System.out.println ("containing triangle for point " + xx.getX() + ", "+ xx.getY());
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
                 System.out.println (level + "  "+ tri.toString());
                 traverseTree (tri, level);
             }
         }
    }
    
    private void traverseTree (Triangle parent, int level){
        //System.out.println ("Parent " + level + "  " + parent.toString());
        level++;
        for (Triangle tri:  parent.getDaughters()){
            if (tri != null){
                System.out.println (level + "  " + tri.toString());
                traverseTree (tri, level);
                
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
    
    public void printTree() {
    	/**
    	ArrayList <Triangle>myqueue = new ArrayList<Triangle>();
    	int top = 0;
    		
    	queue.addLast(treeRoot);
    	myqueue.add(treeRoot);
    	while (!myqueue.isEmpty()){
    		Triangle tri = myqueue.get(top);
    		top++;
    		//Triangle tri = queue.pop();
    		
    		System.out.println (tri.toString());
    		Triangle[] daughters = tri.getDaughters();
    		for(Triangle t : daughters){
    			if (t != null)
    			    myqueue.add(t);
    		}
    	}**/
    	
    }

 


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
    
    
}
