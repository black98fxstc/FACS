/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.stanford.facs.delaunay;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import edu.stanford.facs.drawing.DrawingFrameSimple;
import edu.stanford.facs.logicle.Logicle;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import edu.stanford.facs.delaunay.Triangle.Circle;

/**
 * $Id: Exp $
 * @author cate
 */
public  class Delaunay {

    protected TriangleTree tree;
    protected HashMap <Long, MyPoint> lineHash = new HashMap<Long, MyPoint>();
    protected HashMap <Long, Triangle> triangleHash = new HashMap<Long, Triangle>();
  //  protected HashMap <Long, MyPoint[]>allLines = new HashMap <Long, MyPoint[]>();
    private Map <Long,MyPoint[]>allLines = Collections.synchronizedMap(new HashMap<Long, MyPoint[]>());
    private Onion onion;
    //private DrawingFrame frame;
 //   private ArrayList<Triangle> triangleQueue = new ArrayList<Triangle>();
    private ArrayDeque <Triangle> triangleStack = new ArrayDeque<Triangle>();
    private static int A=0;
    private static int B=1;
    private static int C=2;
    private int nextTask=0;
    
   private float delx, dely;
   
    private float[][] data;

    private MyPoint[] boundingBox;
 
    static final float fuzz  = 1.0e-6F;
    static final float bigscale = 1000.F;
    Random randomfuzz = new Random(314159265);
    private float minx=1000000000, maxx=0;
    private float miny=1000000000, maxy=0;
    private Map<Long, MyPoint> pointlevelList;
    private float[][] tdata;
      		

    /**
     * These hash tables.  lineHash :
     * for vertices A, B, C.  Get vertex A based on the hash key h(B) - h(C)
     * triangleHash:  The key for triangle is H(A) ^ H(B) ^ h(C)
     *
     */

    /*  This one is taking in the FCS data*/
    public Delaunay (MyPoint[] mydata){
    	boundingBox = init(mydata);
    	for (MyPoint pt: boundingBox) {
			System.out.println ("bounding box " + pt.toString());	
    	}
    	processData(mydata, boundingBox);
    	 pruneLineData();
        
    	 printLinesTransformed();
    }
    
    public Delaunay ( float[][] data){
       // System.out.println (data.length + "  " +data[0].length);
        this.data = data;
        
        boundingBox = init();
    
        
        for (MyPoint p:boundingBox)
           System.out.println (p.toString());
        processData(data, boundingBox);
     //   pruneTriangleData();
        pruneLineData();
   
        printLines();
      
    }
    
    /*
     * FCS data*/
    private MyPoint[] init (MyPoint[] mydata){
    	
    	for (int i=0; i < mydata.length; i++){
    		//System.out.println (mydata[i].getX() + ", "+ mydata[i].getY());
            if (mydata[i].getX() < minx) minx=mydata[i].getX();
            if (mydata[i].getX() > maxx) maxx = mydata[i].getX();
            if (mydata[i].getY() < miny) miny = mydata[i].getY();
            if (mydata[i].getY() > maxy) maxy= mydata[i].getY();
            
        }
    	String[]abc={"A", "B", "C"};
    	MyPoint[] trifp = createBoundingBox (minx, maxx, miny,  maxy);
    	for (int i=0; i < trifp.length; i++){
    		trifp[i].setName(abc[i]);
            if (trifp[i].getX() < minx) minx=trifp[i].getX();
            
            if (trifp[i].getX() > maxx) maxx=trifp[i].getX();
            if (trifp[i].getY() < miny) miny = trifp[i].getY();
            if (trifp[i].getY() > maxy) maxy = trifp[i].getY();
        }
        
         
        System.out.println ("  bounding box "+  trifp[0].toString()+ ","+trifp[1].toString()+ ", "+ trifp[2].toString());
  
        try{
	        Triangle tri = new Triangle (trifp[0], trifp[1], trifp[2]);
	        tree = new TriangleTree(tri);
	        addNewLine (trifp[A],trifp[B]);
	        addNewLine (trifp[B], trifp[C]);
	        addNewLine (trifp[C], trifp[A]);
	        hashATriangle (tri);
        } catch (Circle.ColinearPointsException e){
        	System.out.println (e.getMessage());
        	trifp = null;
        }
        
    return trifp;
    }
    
    private MyPoint[] init(){
    	//float minx=10000000, maxx=0;
       // float miny=10000000, maxy=0;
        for (int i=0; i < data.length; i++){
            if (data[i][0] < minx)minx=data[i][0];
            else if (data[i][0] > maxx) maxx = data[i][0];
            if (data[i][1] < miny) miny = data[i][1];
            else if (data[i][1] > maxy) maxy= data[i][1];
            
        }
        
            //create an artificial bounding box based on these values
        MyPoint[] trifp = createBoundingBox (minx, maxx, miny,  maxy);
        
      //  MyPoint[] trifp = createBoundingBox();
        for (int i=0; i < trifp.length; i++){
            if (trifp[i].getX() < minx) minx=trifp[i].getX();
            if (trifp[i].getY() > maxx) maxx=trifp[i].getX();
            if (trifp[i].getY() < miny) miny = trifp[i].getY();
            if (trifp[i].getY() > maxy) maxy = trifp[i].getY();
        }
        
         
  try{
        Triangle tri = new Triangle (trifp[0], trifp[1], trifp[2]);
        tree = new TriangleTree(tri);
    
       /** frame.addPoint (new MyPoint (470, 10));
        frame.addPoint (new Point2D (10, 540));
        frame.addPoint (new Point2D (700, 810));**/
        addNewLine (trifp[A],trifp[B]);
        addNewLine (trifp[B], trifp[C]);
        addNewLine (trifp[C], trifp[A]);
        hashATriangle (tri);
  }catch (Circle.ColinearPointsException e){
	  System.out.println(e.getMessage());
	  return null;
  }
        return trifp;
    }

     


    private MyPoint[] createBoundingBox(float minx, float maxx, float miny, float maxy){
        MyPoint[] boundingtri = new MyPoint[3];
     //   float fbig = (float;
       // System.out.println ("createBoundingBox min max x, min max y "+ minx + ", "+maxx+", "+ miny + ", "+ maxy);
        delx = maxx - minx;
        dely = maxy - miny;
        float x1 = (float)0.5 * (minx + maxx);
        float y1 = maxy + bigscale * dely;
        boundingtri[0] = new MyPoint (x1, y1 );
        boundingtri[0].setName("A");
        
        
       float x2 =minx- (float)0.5* bigscale*delx;
       float y2 = miny- (float)0.5*bigscale*dely;
        boundingtri[1] = new MyPoint (x2, y2);
        boundingtri[1].setName("B");
        
        float x3 = maxx + (float) 0.5*bigscale * delx;
        float y3 = miny - (float) 0.5*bigscale * dely;
        boundingtri[2] = new MyPoint (x3, y3);
        boundingtri[2].setName("C");
        
        
        
        System.out.println ("  Bounding Triangle ");
        for (int i=0; i < boundingtri.length; i++){
            System.out.println (boundingtri[i].toString());

        }

        return boundingtri;

    }
    /**
     * 
     * @param data
     * @param trifp  This is the bounding box. 
     */
    public void processData (float[][] data, MyPoint []trifp) {
       // boundingBox = trifp;
     //   tree.traverseTree();/**????**/
        for (int i=0; i < data.length; i++){
            
            MyPoint fp = new MyPoint (data[i][0], data[i][1]);
            fp.setName(""+ i);
            System.out.println ("Point "+ i + ".  " + data[i][0] + ", "+ data[i][1] + ", "+fp.getFreq());
    //        System.out.println (fp.toString() + "  "+ fp.hashCode());
            addNewPoint (fp);
            
        
        }
        //remove the first big triangle and all of its edges.
     //   dumpLineHash();
        tree.markDead();
      //  Long rootId = tree.treeRoot.getId();
        MyPoint[] rootpts = tree.treeRoot.getPoints();
        removeLineFromHash (rootpts[0], rootpts[1]);
        removeLineFromHash (rootpts[1], rootpts[2]);
        removeLineFromHash (rootpts[2], rootpts[0]);
      
        
    }
   /**
    * this is the FCS Data 
    * @param points
    * @param trifp
    */
    public void processData (MyPoint[] points, MyPoint[] trifp){
    	System.out.println(" how many points are there? "+ points.length);
    	int n =points.length;
    	int size = 10;
    	int []rann = new int[size];
    	Random random = new Random(3234567);
    	for (int i=0; i < size; i++){
    		rann[i] = random.nextInt (n);
    		//System.out.println (rann[i]);
    		System.out.println (points[rann[i]].getX() + "\t" + points[rann[i]].getY());
    	}
    	
    	
    	if (n > size)n=size;
    	for(int i = 0; i < n; i++){
    		System.out.println ("Point "+i + ". " +points[rann[i]].toString()+ " "+ points[rann[i]].getFreq());
    		points[rann[i]].setName(""+ i);
    		addNewPoint(points[rann[i]]);
    	}
    	tree.markDead();
        //  Long rootId = tree.treeRoot.getId();
          MyPoint[] rootpts = tree.treeRoot.getPoints();
          removeLineFromHash (rootpts[0], rootpts[1]);
          removeLineFromHash (rootpts[1], rootpts[2]);
          removeLineFromHash (rootpts[2], rootpts[0]);
          
    }



    public void addNewPoint (MyPoint pt){
        //find the containing triangle

        nextTask = 0;
     //   triangleQueue = null;
    //    triangleQueue = new ArrayList <Triangle>();
        triangleStack = null;
        triangleStack = new ArrayDeque<Triangle>();
        Triangle containing = tree.containingTriangle (pt);
        
        if (containing == null){
            System.out.println ("Oh no -- There is no containing triangle.  "+ pt.toString());
            //try to fuzz it three times before returning
            int j = 0;
            while (j < 2 && containing == null){
            	System.out.println ("fuzzing here " + pt.toString());
            	float fuzzx = (float)(fuzz * delx * randomfuzz.nextFloat() -0.5);
            	float fuzzy = (float)(fuzz * dely * randomfuzz.nextFloat() - 0.5);
            	j++;
            	pt.setX(  pt.getX()+fuzzx);
            	pt.setY( pt.getY()+fuzzy);
            	containing = tree.containingTriangle (pt);
            	
            }
            if (j == 2){
            	System.out.println ("even after fuzzing , no containing triangle found. "+pt.toString());
            return;
            }
        }
        //create 3 new triangles and queue them for testing
//        System.out.println (" containing triangle = " + containing.toString());
        MyPoint[][] newlines = containing.makeDaughters (pt);
        for (int j=0; j < newlines.length; j++){
        	addNewLine (newlines[j][0], newlines[j][1]);
        }
        
       
        Triangle[] daughters = containing.getDaughters();
        
        for (int i=0; i < daughters.length; i++){
            hashATriangle (daughters[i]);
          //  triangleQueue.add (daughters[i]);
            triangleStack.push(daughters[i]);
         
        }


        //erase the old triangle
      //  Point[] conpts = containing.getPoints();
        //update the hash table
        Long key = new Long (containing.getId());
        if (triangleHash.containsKey(key)){
            Triangle t = triangleHash.get (key);
            t.markLiveStatus (false);
            
        }


        //while there are triangles to test
      //  while (nextTask < triangleQueue.size()){
          while ( !triangleStack.isEmpty()){

            Triangle newone=null;
            Triangle newtwo=null;
            // look up the 4th point.  points[A] is this pt what we are testing against
            MyPoint fourth = null;
           // Triangle testTri = triangleQueue.get(nextTask);
            Triangle testTri = triangleStack.pop();
            System.out.println (nextTask + ". testtri = " + testTri.toString());
           /** if (testTri.getLiveStatus() == false){
                System.out.println (" Skip this one.  " + testTri.toString());
                nextTask++;
                continue;
            	
            }**/
            if (testTri.getLiveStatus()) {
            MyPoint[] testpts = testTri.getPoints();
            Long keypt = makeHashCodeForLine (testpts[C], testpts[B]);
            
            
            if (lineHash.containsKey (keypt)) {
                 fourth = lineHash.get (keypt);
                 System.out.print(keypt + "--- ");
                 System.out.println ("Fourth point "+ ptToString(fourth) + " for points "+ptToString(testpts[C])+ " and "+ ptToString(testpts[B]));
                 if (fourth.equals(testpts[A])){
                	 System.out.println ("  This is an error !! ");
                 }
                if (testForDistance (fourth, testTri)){
                    //create two new triangles
//                    System.out.println (testTri.toString());
                	try{
	                    newone = new Triangle(testpts[A], testpts[B], fourth);
	                    newtwo = new Triangle (testpts[A], fourth, testpts[C]);  
                	}catch (Circle.ColinearPointsException e){
                		System.out.println ("(1) " +e.getMessage());
                	}
                	if (newone != null && newtwo != null ){
	                  //  newone = new Triangle (xs, ys);
	                    addNewLine (testpts[A], testpts[B]);
	                    addNewLine (testpts[B], fourth);
	                    addNewLine (fourth, testpts[A]);
	//                    System.out.println ("new one ? " + newone.toString());
	                    hashATriangle (newone);
                	
	                
	                    addNewLine (testpts[A], fourth);
	                    addNewLine (fourth, testpts[C]);
	                    addNewLine (testpts[C], testpts[A]);
	                    
	                    hashATriangle (newtwo);
                
                    
 

                //erase the two old triangles 
                  key = new Long (testTri.getId());
                  eraseTriangle(key, testTri, newone, newtwo);
                  checkTheQueue (key, testTri, nextTask);
                

                 // Triangle oldtriangle = getHashTriangle (testpts[B], fourth, testpts[C]);
                  Triangle oldtriangle = getHashTriangle (fourth, testpts[C], testpts[B]);

   
                  if (oldtriangle != null) {
                	  oldtriangle.markLiveStatus(false);
                      key = oldtriangle.getId();
                      eraseTriangle(key, oldtriangle, newone, newtwo);
                      checkTheQueue (key, oldtriangle, nextTask);
                  }
                   
                  
                //erase the line in both directions
                   removeLineFromHash (testpts[B], testpts[C]);
//                   frame.eraseLine (testpts[B], testpts[C]);
               

                //add two new triangles to the queue.
                  // triangleQueue.add (newone);
                  // triangleQueue.add (newtwo);
                   triangleStack.push (newone);
                   triangleStack.push (newtwo);
                }
            }

            
            }
           /** else {
                System.out.println ("There is no fourth point ");
            }**/
       //     System.out.println ("......... after one iteration of the queue ..........");
           // nextTask++;
            }

        }

    }
    
    /**  
     *******************************************************************************
     */
    private void addNewLine (MyPoint A, MyPoint B){
    	if (A != null && B != null){
	    	Long key = makeHashCodeForLine (A, B);
	    	Long keyneg = 0-key;
	    	MyPoint[] newline={A,B};
	    	if (!allLines.containsKey(key) && !allLines.containsKey(keyneg)){
	    		allLines.put(key, newline);
	    		//System.out.println ("addNewLine "+ ptToString(A) + ", "+ ptToString(B));
	    	}
	    	
    	}
    		
    	
    }
    
    /**
     *  this point is the third in the triangle  
     *  */
    public MyPoint getLineHash (MyPoint a, MyPoint b){
    	
    	
        MyPoint point = null;
        Long hashcode = new Long (a.hashCode() - b.hashCode());

        if (lineHash.containsKey (hashcode)){
            point = lineHash.get (hashcode);
        }
        
      //  if (point != null){
      //  	System.out.print(ptToString (a) + ", "+ ptToString(b));
      //      System.out.println ("--The point for hashcode is " + hashcode+  " "+ ptToString(point));
      //  }
       // else
            System.out.println ("There is no point for this hashcode " + hashcode);
        return point;
        
    }
    
    private void checkTheQueue (Long key, Triangle tri, int nextTask){
    	
    	Iterator<Triangle> it = triangleStack.iterator();
    	System.out.println("Check the Queue ");
    	System.out.println ("\t" + tri.toString() + tri.getId());
    	while (it.hasNext()){
    		Triangle qtri = it.next();
    		System.out.println (qtri.toString() + qtri.getId());
    		
    		if (qtri.getId() == tri.getId()){
    			qtri.setLiveStatus(false);
    			return;
    		}
    	}
    	/**for (int i= nextTask; i < triangleQueue.size(); i++){
    		Triangle qtri = triangleQueue.get(i);
    		if (qtri.getId() == tri.getId()){
    			qtri.setLiveStatus(false);
    			return;
    		}
    		
    	}**/
    }
    
    /**
     *  Erase triangle t1 in the triangleHash and inactivate it in the TriangleTree after setting
     *  its daughters.  
     */
    private void eraseTriangle (Long key, Triangle old, Triangle daughter1, Triangle daughter2){
    	
    	
    	if (old!= null){
	    	old.markLiveStatus( false);
	    	old.makeOneDaughter(daughter1);
	    	old.makeOneDaughter(daughter2);
	    	
    	}	
  /**  	
    	if (triangleHash.containsKey(key)){
            Triangle tri = triangleHash.remove(key);
            System.out.println ("removing this triangle "+ tri.toString());
        }
        **/
    	
    	
    }
    
    public final void hashATriangle(Triangle tri){
        Long hint = new Long (tri.getId());
        
        triangleHash.put (hint, tri);
        MyPoint[] points = tri.getPoints();
 System.out.print (" Hash a Triangle " +  ptToString (points[0]) + "  "+ ptToString(points[1]) + "  " + ptToString(points[2]));
 System.out.println ("  hashcode = " + hint);    
        Long linehash = makeHashCodeForLine (points[A], points[B]);
        lineHash.put (linehash, points[C]);
    System.out.println (linehash + " key for " + points[C].toString());
        linehash = makeHashCodeForLine (points[B], points[C]);
        lineHash.put (linehash, points[A]);
    System.out.println (linehash + " key for " + points[A].toString());
        linehash = makeHashCodeForLine (points[C], points[A]);
        lineHash.put (linehash, points[B]);
    System.out.println (linehash + " key for " + points[B].toString());


    }
    
   

    public void removeLineFromHash (MyPoint a, MyPoint b){

        System.out.println ("..........removeLine from hash " + ptToString(a) + ","+ ptToString(b));
        System.out.println ("............................." + a.hashCode() + ", " + b.hashCode());
        

        Long keyi = new Long (a.hashCode() - b.hashCode());
        
        if (lineHash.containsKey (keyi)){
           MyPoint p = lineHash.remove(keyi);
           System.out.println (keyi + "..... "+ ptToString(p));
           // lineHash.remove (keyi);
           // frame.eraseLine (a, b);
         
        }
        else
            System.out.println (" This key does not exist in the line hash " + keyi.toString());
        
        //remove from the list of all lines
        if (allLines.containsKey(keyi)){
        	allLines.remove(keyi);
        }

        keyi = new Long (b.hashCode() - a.hashCode());
       
        
        if (lineHash.containsKey (keyi)){

            MyPoint p = lineHash.remove (keyi);
            System.out.println (keyi.toString() + "........ "+ ptToString(p));
         //   frame.eraseLine (b, a);

        }
        else {
            System.out.println ("This key does not exist in the lineHash " + keyi.toString());
        }
        //remove from the list of all lines
        if (allLines.containsKey(keyi)){
        	allLines.remove(keyi);
        }
        
  /**      System.out.println (".......................lineHash after removing ..........");
        Set<Long> keys = lineHash.keySet();
        Iterator <Long> it = keys.iterator();
        while (it.hasNext()){
        	Long k = it.next();
        	MyPoint pt = lineHash.get(k);
        	System.out.println(k + "  for "+ ptToString(pt));
        }
        System.out.println (".......................lineHash after removing ..........");**/

    }

    /*
     * Test the radius of this triangle to the point.  Returns true
     * when the distance to the pt is less than the radius, thus
     * signaling the need to flip the edges.
     * positive is inside
     * 0 is on the line
     * negative is outside of the circle.
     */
    private boolean testForDistance (MyPoint pt, Triangle onetri){
        boolean flag= false;
        double radius = 0;
        double distance = 0.;
        double radd = 0.;

        if (pt != null){
        	if(onetri.getCircle() == null)
        		return false;
            radius = onetri.getCircle().getRadius();
            MyPoint center = onetri.getCircle().getCenter();
            if (center == null){
                System.out.println ("  For some reason, this triangle has no center. " + onetri.toString());
                return false;
            }
            radd = (pt.getX() - center.getX())*(pt.getX()-center.getX())+ (pt.getY()-center.getY())*(pt.getY()-center.getY());
            distance = radius*radius - radd;
System.out.println( "distance is "+ distance);
            if (distance == 0){
                System.out.println ("Points are co-linear!!!");
            }
            if (distance > 0)
                flag = true;
        }
        System.out.println ("testForDistance "+ flag + " "+radius + " || "+ distance + " || " + radd);
        return flag;

    }
    /** look through the tree of triangles.  Some of the lines are connected to the points in the
     * boundingBox.  These need to be 'removed' from the final solution.  The other end of a removed
     * line is on the convex hull.  Assign a level to the triangles.  Level 0 is the bounding box.  The
     * daughters are level 1 and so on ...
     * 
     *  */
    private void pruneTriangleData (){
    	System.out.println ("pruneTriangle Data has been commented out.");
    	//tree.pruneLineData ();
    }
    
    private void pruneLineData (){
    	Set<Long> keys = allLines.keySet();
    	Iterator <Long> it = keys.iterator();
    	
    	while (it.hasNext()){
    		Long key = it.next();
    		if (allLines.containsKey(key)){
    			MyPoint[] oneline = allLines.get(key);
    			
    			markPoint (oneline[0], oneline[1]);
    			
    		}
    		
    	}
    	
    }
    
    

       
    private void printLinesTransformed(){
    	Set<Long> keys = lineHash.keySet();
    	Iterator<Long>it = keys.iterator();
    	//MyPoint []pts = (MyPoint[]) allLines.get (key);
    	
    	tdata = transformLineData ();
    	File f = new File ("./printRR.r");
        FileOutputStream fw = null;
        try{
        	fw= new FileOutputStream (f);
        	String h = "x1\ty1\tx2\ty2\n";
        	fw.write(h.getBytes());
        	StringBuilder buf = new StringBuilder();
        	for (int i=0; i < tdata.length; i++){
        		
        			buf.append(tdata[i][0]).append("\t").append(tdata[i][1]).append("\t");
        			buf.append(tdata[i][2]).append("\t").append(tdata[i][3]).append("\n");
        			fw.write(buf.toString().getBytes());
        			buf = new StringBuilder();
        		
        	}
        	fw.close();
        } catch (IOException e){
            System.out.println (" file io exception ");
        } finally {
            
        }
    }
    
    private void printLines() {
    	
    	System.out.println (lineHash.size());
    	Set<Long> keys = lineHash.keySet();
    	Iterator<Long>it = keys.iterator();
    	//MyPoint []pts = (MyPoint[]) allLines.get (key);
    	File f = new File ("./printRR.r");
        FileOutputStream fw = null;
       // printDataFrame
        try{
        	fw= new FileOutputStream (f);
        	String h = "x1\ty1\tLevel1\tx2\ty2\tLevel2\n";
        	//System.out.print(h);
            fw.write(h.getBytes());
            //include the bounding triangle
            while(it.hasNext()){
            	Long key = it.next();
            	if (allLines.containsKey(key)){
	        		MyPoint[] pts = allLines.get(key);
	        		if (pts != null && inBounds(pts)){
	        		 	
	        			//System.out.println (pt1.getLevel() + "-"+pts[0].getLevel() + ", "+ pt2.getLevel() + "-" +pts[1].getLevel() );
	        		    StringBuilder buf = new StringBuilder (pts[0].getX()+"\t"+pts[0].getY());
	        		    buf.append("\t").append(pts[0].getLevel());
		        		buf.append("\t").append(pts[1].getX()).append("\t").append(pts[1].getY());
		        		buf.append("\t").append(pts[1].getLevel()).append("\n");
		        		
		                fw.write(buf.toString().getBytes());
	        		 		
	        		}
            	}
            }
        
        	fw.close();
        	
        } catch (IOException e){
            System.out.println (" file io exception ");
        } finally {
            
        }
    	
    	
    }
    
    private float[][] transformLineData () {
    	//fill the raw data array with transformed lines
    	
    	Collection <MyPoint[]>pts = allLines.values();
    	Iterator<MyPoint[]> it = pts.iterator();
    	ArrayList <float[]> goodlines = new ArrayList<float[]>();
    	
    	while (it.hasNext()){
    		MyPoint[] one = it.next();
    		if (one[0].getLoc() != MyPoint.OUT ){
    			if (one[1].getLoc() != MyPoint.OUT){
    				float[]oneline = new float[4];
    				oneline[0] = one[0].getX();
    				oneline[1] = one[0].getY();
    				oneline[2] = one[1].getX();
    				oneline[3] = one[1].getY();
    				goodlines.add(oneline);
    			}
    		}
    		else
    		    System.out.println ("Line is out " + one[0].toString() + "-"+one[0].getLoc() + ", "+ one[1].toString()+"-"+one[1].getLoc());
    		
    		
    	}
    	float[][] golines = new float[goodlines.size()][4];
    	golines = goodlines.toArray(golines);
    	//tdata = transformToLogicle (golines);
    	
    	
    	//return tdata;
    	return golines;
    }
    
    /*
     * ---------------------------------------------------------------------
     */
    public float[][] transformToLogicle (float[][]data){
        float[][] logdata = new float[data.length][data[0].length];
        int T = 1<<18;
        double W=0.5;
        double M=4.5;
        double A=0;
        System.out.println (" ------------- tranformToLogicle-----------");
        Logicle logicle = new Logicle (T, W, M, A);
        for (int i=0; i < data.length; i++){
            for (int j=0; j < data[i].length; j++){
                logdata[i][j] = (float) logicle.scale(data[i][j]);
                //System.out.println (data[i][j] + "  " + logdata[i][j] );
            }
        }

        return logdata;
    }
    
    public float[][] transformToLog (float[][]data){
        float[][] logdata = new float[data.length][data[0].length];
        System.out.println (" ------------- tranformToLogi-----------");
        for (int i=0; i < data.length; i++){
             for (int j=0; j < data[i].length; j++){
                 logdata[i][j] = (float) Math.log(data[i][j]);
                // System.out.println (data[i][j] + "  "+ logdata[i][j]);
             }
             
        }
        return logdata;
    }
    
    
    
    private void markPoint (MyPoint pt1, MyPoint pt2){
	   // System.out.println (pt1.toString() + ", " + pt2.toString());
	    
		if (pt1.equals(boundingBox[A]) || pt1.equals(boundingBox[B]) || pt1.equals (boundingBox[C])){
			pt1.setLoc(MyPoint.OUT);
		}
		else{
			pt1.setLoc ( MyPoint.IN);
		}
		if (pt2.equals(boundingBox[A]) || pt2.equals(boundingBox[B]) || pt2.equals (boundingBox[C])){
			pt2.setLoc(MyPoint.OUT);
		}
		else{
			pt2.setLoc ( MyPoint.IN);
		}
	
	    if (pt1.getLoc() == MyPoint.IN && pt2.getLoc() == MyPoint.OUT){
	    	pt1.setLoc(MyPoint.ON);
	    }
	    else if (pt2.getLoc() == MyPoint.IN && pt1.getLoc() == MyPoint.OUT){
	    	pt2.setLoc(MyPoint.ON);
	    }
    }
    
   

   /**
    * 
    * @param a
    * @param b
    * @param c
    * @return
    */

    public Triangle getHashTriangle (MyPoint a, MyPoint b, MyPoint c) {
        Triangle tri = null;
//        System.out.println ("getHashTriangle ");
        
        
        long hashcode = a.hashCode() ^ b.hashCode() ^ c.hashCode();
//        System.out.print ("GetHashTriangle Hashcode = " + hashcode + a.toString() + ", "+ b.toString() + ", " + c.toString());
        if (triangleHash.containsKey (new Long (hashcode))){
            tri = triangleHash.get (new Long (hashcode));
            if (tri != null)
                System.out.println ("  Found this triangle " + tri.toString());
            else
            	System.out.println (" No triangle found for key = " + hashcode);
        }
        return tri;
    }
    
    /**
     * Called when printing.  If not inbounds, the line is not printed
     * @param line
     * @return
     */
    
    private boolean inBounds(MyPoint[] line){
    	boolean in = true;
    	if (line[0].getLoc() == MyPoint.OUT){
    		in = false;
    	}
    	else if (line[1].getLoc() == MyPoint.OUT){
    		in = false;
    	}
    	
    	return in;
    }

    private Long makeHashCodeForLine (MyPoint a, MyPoint b){
        int h = 0;

        h = a.hashCode() - b.hashCode();
        return new Long (h);
    }
    
    /* *************************************************************************** */ 
    /*The methods below are not being called*/
    /**
     * used during debugging.  Not being called
     */
     private void dumpLineHash () {
         System.out.println (" dump the all line Hash ");
         Set<Long> keys = allLines.keySet();
         Iterator <Long>it = keys.iterator();
         while (it.hasNext()){
             Long key =  it.next();
             System.out.print ("Key " + key);
             MyPoint []pts = (MyPoint[]) allLines.get (key);
             if (pts != null )
                 System.out.println (" Hashed Line:  "+ lineToString(pts) );
             else
                 System.out.println (" Not enough points in this line");

         }
     }
   
     /**
      * used during debugging.  Not being called.
      */
     private void dumpTriangleHash() {
     	System.out.println (" dump the triangle hash");
         Set<Long> keys = triangleHash.keySet();
         Iterator it = keys.iterator();
         while (it.hasNext()){
             Long key = (Long) it.next();
//             System.out.print ("Key " + key);
             Triangle tri = (Triangle) triangleHash.get (key);
             if (tri != null) {
                 System.out.println ("Hashed Triangle  "+ tri.toString());
             }
             else
                 System.out.println (" No triangle");

         }

     }
     private void printQueue() {
         System.out.println (".........................................................");
         System.out.println ("..................Print Queue............................");
         Iterator<Triangle> it = triangleStack.iterator();
         while (it.hasNext()){
        	 System.out.println (it.next().toString());
         }
        /** for (int i=nextTask; i < triangleQueue.size(); i++){
             System.out.println (triangleQueue.get(i).toString());
         }**/
         
         
     }
     
     
     private void printTriangleHash() {
         System.out.println ("----------------------printTriangleHash-----------------------");
         Set<Long> keys = triangleHash.keySet();
         Iterator<Long> it = keys.iterator();
         while (it.hasNext()){
        	 Long key = it.next();
             Triangle tri = triangleHash.get (key);
             System.out.println (key + " : " + tri.toString());

         }

     }

     private String ptToString(MyPoint pt) {
         String s = "("+pt.getName() + ": " + pt.getX()+","+pt.getY()+")";
         return s;
     }
     
     /**  Used during debugging
      * 
      * @param line
      * @return
      */
     private String lineToString (MyPoint[] line){
     	StringBuilder buf = new StringBuilder(ptToString(line[0]));
     	buf.append(ptToString(line[1]));
     	return buf.toString();
     }


    

}
