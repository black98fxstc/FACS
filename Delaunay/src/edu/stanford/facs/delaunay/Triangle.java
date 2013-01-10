/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.stanford.facs.delaunay;


import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * $Id: Exp $
 * @author cate
 */
public class Triangle {
    private static int MAX=3;
    private Triangle[] daughters = new Triangle[MAX];
    private Circle circle;
    private static int A=0;
    private static int B=1;
    private static int C=2;
    private boolean LIVE = true;
    private int level;
    private int treeLevel=0;
    private long id;
    private MyPoint[] points = new MyPoint[MAX];
 
    private DefaultMutableTreeNode treeNode;
    private boolean [] validLines = new boolean[3];
    //0 = AB, 1 = BC, 2 = CA.  A line is valid when neither of its end points is connected to
    //the bounding box.  A point is ON, that is on the convex hull, when one end of the line is
    //connected to the bounding box and the other is not.

//
//    public Triangle (){
//      super ();
//    }

    public Triangle (float []xf, float[] yf) throws Circle.ColinearPointsException {
        points[0] = new MyPoint (xf[0], yf[0]);
        points[1] = new MyPoint (xf[1], yf[1]);
        points[2] = new MyPoint (xf[2], yf[2]);
        id = points[A].hashCode() ^ points[B].hashCode() ^ points[C].hashCode();
       
  //      System.out.println ("   --- new Triangle ------");
        try {
           circle = new Circle(this);
        }catch (Circle.ColinearPointsException e){
        	System.out.println ("(2)" + e.getMessage());
        }
        
        treeNode = new DefaultMutableTreeNode (this);
        
    }
    
    public Triangle (MyPoint Aa, MyPoint Bb, MyPoint Cc) throws Circle.ColinearPointsException {
    	points[A] = Aa;
    	points[B] = Bb;
    	points[C] = Cc;
    	id = points[A].hashCode() ^ points[B].hashCode() ^ points[C].hashCode();
    	try {
        circle = new Circle(this);
    	} catch(Circle.ColinearPointsException e){
    		System.out.println ("(2)" + e.getMessage());
    	}
    	    	treeNode = new DefaultMutableTreeNode (this);
    	        
    	
    }
    
    protected void assignLevelToPoints(MyPoint pt, int ptlevel){
    	for (int i=0; i < 3; i++){
    		if (points[i].getLevel() < 0)
    			points[i].setLevel(ptlevel);  
    	
    	ptlevel++;
    	for (Triangle tri: daughters){
    		if (tri != null)
    		   tri.assignLevelToPoints(points[i], ptlevel);
    	}
    	}
    }
    
    public DefaultMutableTreeNode getTreeNode (){
        return treeNode;
    }

    public long getId() {
        return id;
    }

    public MyPoint[] getPoints() {
        return points;
    }
    
    public void setTreeLevel(int i) {
    	treeLevel = i;
    }
    public int getTreeLevel() {
    	return treeLevel;
    }
   

    public boolean containsVertex (MyPoint p){
        boolean flag = false;
        for (int i=0; i < MAX; i++){
            if (p.getX() == points[i].getX() && p.getY() == points[i].getY()){
                flag = true;
                break;
            }
        }
        return flag;
    }

   
    
    public MyPoint[][] makeDaughters (MyPoint xx){
        MyPoint [][] newLines = new MyPoint[3][2];
      
          
        	int i=0;
        	try{
	            daughters[i] = new Triangle(xx, points[0], points[1]);
	            newLines[i][0] = xx;
	            newLines[i][1] = points[0];
	            i++;
        	} catch ( Circle.ColinearPointsException e){
        		System.out.println (e.getMessage());
        	}
           // daughters[0] = new Triangle (x1, y1);
            
         //   float[] x2={(float)xx.getX(), (float)points[1].getX(),(float) points[2].getX()};
         //   float[] y2 = {(float)xx.getY(), (float)points[1].getY(), (float)points[2].getY()};
            try{
	        	daughters[i] = new Triangle(xx, points[1], points[2]);
	            newLines[i][0] =  xx;
	            newLines[i][1]= points[1];
	            i++;
            }catch ( Circle.ColinearPointsException e){
        		System.out.println (e.getMessage());
        	} 
            
         //   daughters[1] = new Triangle (x2, y2);
            
          //  float[] x3 = {(float)xx.getX(), (float)points[2].getX(), (float)points[0].getX()};
          //  float[] y3 = {(float)xx.getY(), (float)points[2].getY(), (float)points[0].getY()};
           try{ 
            daughters[i] = new Triangle(xx, points[2], points[0]);
            newLines[i][0] = xx;
            newLines[i][1]= points[2];
            i++;
           } catch ( Circle.ColinearPointsException e){
        		System.out.println (e.getMessage());
        	}
       // }
        return newLines;
    }
    
    public void makeOneDaughter (Triangle tri) {

        if (daughters[0] != null){
            if (daughters[1] !=null){
               if (daughters[2] != null){
                   System.out.println ("Isn't this an error that there are no empty daughter cells?");
               }  
               else {
//                   daughters[2] = new Triangle (pts[A], pts[B], pts[C]);
                   daughters[2] = tri;
//                   
               }
            }
            else {
//                daughters[1] = new Triangle (pts[A], pts[B], pts[C]);
                daughters[1] = tri;
//                
            }
        }
        else {
//            daughters[0] = new Triangle (pts[A], pts[B], pts[C]);
            daughters[0] = tri;

        }
    }

    public int  contains (MyPoint p){
        
        int j;
        boolean flag = false;
       int ztest=0;
       double d = -1;
//       System.out.println ("------------- MY Contains ---------------------");
//       boolean isIn = mycontains (p);
       // System.out.println (" -----------contains------------- ");
       
       
        for (int i=0; i < points.length; i++){
          j = (i+1) % 3;

          d =( points[j].getX() -points[i].getX()) * (p.getY() - points[i].getY()) -
                   ( points[j].getY() -points[i].getY()) * (p.getX() - points[i].getX());
         
         // System.out.println ("----------ulp  "+ Math.ulp(d) + " "+Float.MIN_VALUE );
          if (d< 0.0) { 
//              System.out.println ("  d is less than 0 " +i + ", "+j);
              return -1;
          }
          else if ( d == 0){
              //System.out.println (" contains d == 0");
              flag = true;
          }
          else if (d > 0){
              flag = false;
          }
          
        }
        
      return flag?0:1;
    }
    
    private double determinant (MyPoint p1, MyPoint p2){
        double d=0;
        
        d = (p1.getX()*p2.getY()) - (p1.getY() * p2.getX());
//        System.out.println (p1.toString() + ", "+ p2.toString()+ " "+ d);
        return d;
    }
    
    public boolean mycontains (MyPoint p){
        boolean isIn = false;
        double a, b;
        
        a = (determinant(p, points[2]) - determinant (points[0], points[2])) / (determinant (points[1], points[2]));
        
        b = -1 * (determinant (p, points[1])- determinant (points[0], points[1])) / determinant (points[1], points[2]);
        
        if ((a > 0 && b > 0) && (a+b < 1))
            isIn = true;
        
//        System.out.println (" My Contains "+ a + ", "+ b+ " "+ isIn);
        return isIn;
        
    }

    public void markLiveStatus (boolean status){
        
        LIVE = status;
    }

    public boolean getLiveStatus () {
        return LIVE;
    }
    
    public void setLiveStatus  (boolean flag){
    	LIVE = flag;
    }

  

    public Triangle containingTriangle (MyPoint xx) {
        Triangle tri = null;
        Triangle dtri = null;
        boolean checking = true;

//        System.out.println ("Containing Triangle " + toString() + xx.x + ", " + xx.y);
        int f = contains (xx);
        if (f == 1){
           tri = this;
//           System.out.println ("Tri containing " + toString());
           checking = true;
        }
        else {
            checking = false;
            return null;
        }

        int i=0;
        while (checking && i < MAX){
            if (daughters[i] != null){
                dtri = daughters[i].containingTriangle (xx);
                if (dtri != null){
//                    System.out.println ("Dtri containing " + dtri.toString());
                    checking=false;
                }
            }
            i++;
        }
        if (dtri == null)
           return tri;
        else
            return dtri;
    }
   
    public Triangle[] getDaughters() {
        return daughters;
    }

    public boolean equals (MyPoint a, MyPoint b, MyPoint c){
        boolean flag = false;
        int hashcode = a.hashCode()^b.hashCode()^c.hashCode();

        if (id == hashcode)
            flag = true;
        return flag;
    }

    @Override
     public String toString() {
        StringBuilder buf = new StringBuilder();
//        buf.append(id).append(" ").append(LIVE).append (" :: ");
        for (int i=0; i < points.length; i++){
        	buf.append("(").append(points[i].getName()).append(",").append(points[i].getName()).append(")");
         //   buf.append("(").append(points[i].getName()).append("  ").append(points[i].getX()).append(", ").append(points[i].getY()).append (") ").append(points[i].loc).append("  ::");
            
        }
        buf.append("  is Live =").append(LIVE);
       // buf.append("   ").append(level);
        buf.append("  ").append(validLines[A]).append(", ").append(validLines[B]).append(", ").append(validLines[C]);
//        buf.append (getLiveStatus());
        return buf.toString();
    }
    
    public void printDataFrame(FileOutputStream fw){
    	
    	/*****In construction****/
    	
    	try {
        StringBuilder buf = new StringBuilder();
        if (LIVE){
        	if (validLines[A]){
		        	buf.append(points[A].getX()).append("\t");
		        	buf.append(points[A].getY()).append("\t");
		        	buf.append(points[B].getX()).append("\t");
		        	buf.append(points[B].getY()).append("\n");
        		
        	}
        	if (validLines[B]){
		        	buf.append(points[B].getX()).append("\t");
		        	buf.append(points[B].getY()).append("\t");
		        	buf.append(points[C].getX()).append("\t");
		        	buf.append(points[C].getY()).append("\n");
        		
        	}
        	if (validLines[C]){
		        	buf.append(points[C].getX()).append("\t");
		        	buf.append(points[C].getY()).append("\t");
		        	buf.append(points[A].getX()).append("\t");
		        	buf.append(points[A].getY()).append("\n");
        		
        	}
        	//System.out.println(buf.toString());	
        	fw.write(buf.toString().getBytes());
        }
       
        for (Triangle t: daughters){
        	if (t != null)
        		t.printDataFrame(fw);
        }
    	} catch (IOException e){
    		System.out.println ("ioexception !! Triangle");
    	}
    }
    
    protected ArrayList<MyPoint[]> getLineData() {
    	ArrayList<MyPoint[] > lines = new ArrayList<MyPoint[]>();
    	//this creates duplicates.  need a hashMap with hashcode
    	if (validLines[A]){
    		MyPoint[] one = new MyPoint[2];
    		one[0] = points[A];
    		one[1] = points[B];
    		lines.add(one);
    	}
		if (validLines[B]){
			MyPoint[] one = new MyPoint[2];
			one[0] = points[B];
			one[1] = points[C];
	        lines.add(one);
			
		}
		if (validLines[C]){
			MyPoint[] one = new MyPoint[2];
			one[0] = points[C];
			one[1] = points[A];
	        lines.add(one);	
		}
	
	for (Triangle t: daughters){
    	if (t != null)
    		lines.addAll( t.getLineData());
    }
	
	return (lines);
    }
    
    protected void prune (int level, MyPoint[] box, boolean pruning){
    	this.level = level;
    	for (MyPoint pt : points)
    		mark (pt, box);
    	
    	if (!pruning){
    		validLines[A] = true;
    		validLines[B] = true;
    		validLines[C]= true;
    	}
    	else{
    	//now is it a good line?
    	validLines[A] = true;
    	if (points[A].loc == MyPoint.OUT){
    		validLines[A] = false;	
    		if (points[B].loc == MyPoint.IN){
    			points[B].loc = MyPoint.ON;	
    		}
    	
    	}
    	else {  //A is in or on
    		if (points[B].loc == MyPoint.OUT){
    			validLines[A]= false;
    		}
    		
    	}
    	validLines[B] = true;
    	if (points[B].loc == MyPoint.OUT){
    		validLines[B] = false;	
    		if (points[C].loc == MyPoint.IN){
    			points[C].loc = MyPoint.ON;	
    		}
    	
    	}
    	else{
    		if (points[C].loc == MyPoint.OUT){
    			validLines[B] = false;
    		}
    	}
    	validLines[C]= true;
    	if (points[C].loc == MyPoint.OUT){
    		validLines[C] = false;	
    		if (points[A].loc == MyPoint.IN){
    			points[A].loc = MyPoint.ON;	
    		}
    	}
    	else {
    		if (points[A].loc == MyPoint.OUT){
    			validLines[C] = false;
    		}
    	}
    	}
    	
    	//System.out.println ("Prune -- "+ toString());
    	for (Triangle d : daughters){
    		if (d!= null)
    			d.prune(level+1, box, pruning);
    	}
    	
    }
    
    
    
    private void mark (MyPoint pt, MyPoint[] box){
    	//System.out.println (pt.toString());
    	if (pt.equals(box[A]) || pt.equals(box[B]) || pt.equals (box[C])){
    		pt.loc=MyPoint.OUT;
    	}
    	else
    		pt.loc = MyPoint.IN;
    }
    
    private void determineStatus (MyPoint A, MyPoint B, float[] minmax){
    	int status = MyPoint.IN;
    	
    	//determine if this line is good by assigning a status to the two pts relative to the bounding box.
    	//if both points do not connect to a bounding box, there are interior., MyPoint.IN
    	//if one of the points is in the bounding box,then return MyPoint.ON
    	if (A.loc == MyPoint.UND ){
    		if ( B.loc == MyPoint.UND){
    		
    		}
    		
    	}
    	
    	
    }
    
    public String printDataFrame2(){
    	
        StringBuilder buf = new StringBuilder();
        if (LIVE){
        	
        	buf.append(points[A].getX()).append("\t");
        	buf.append(points[B].getX()).append("\t");
        	
        	buf.append(points[C].getX()).append("\t");
        	buf.append(points[A].getX()).append("\t");
        	buf.append(points[A].getY()).append("\t");
        	buf.append(points[B].getY()).append("\t");
        	buf.append(points[C].getY()).append("\t");
        	buf.append(points[A].getY()).append("\n");
        	//System.out.println(buf.toString());			
        }
        return buf.toString();
    }
    
    public String printForR() {
        String nl = System.getProperty ("line.sep");
        StringBuilder buf = new StringBuilder();
        if (LIVE){
        
            buf.append ("x").append(id).append("<- c(").append (points[0].getX());
            buf.append (",").append(points[1].getX()).append(",");
            buf.append (points[2].getX()).append(",").append (points[0].getX());
            buf.append(");\n");

            buf.append ("y").append(id).append("<- c(").append (points[0].getY());
            buf.append (",").append(points[1].getY()).append(",");
            buf.append (points[2].getY()).append(",").append (points[0].getY());
            buf.append(");\n");
            buf.append("lines(").append("x").append(id).append(",y").append(id).append(");\n");
        }
        
        return buf.toString();
        
    }

    public Circle getCircle() {
        if (circle == null){
        	try{
            circle = new Circle(this);
        	} catch(Circle.ColinearPointsException e){
        		return null;
        	}
        }
        return circle;
    }
    /**This is the circle that contains this Triangle
     *
     */
    public class Circle   {
    	
    	class ColinearPointsException extends Exception{
    		static final long serialVersionUID=1;
    		ColinearPointsException(String msg){
    			super(msg);
    		}
    	}

        double radius;
        MyPoint center ;
        private Triangle mytri;


        Circle(Triangle mytri) throws ColinearPointsException {
            this.mytri = mytri;
            //try{
                circumcircle();
           // }catch(ColinearPointsException e){
           // 	System.out.println(e.getMessage());
           // }

        }
        
 
        private void circumcircle()  throws ColinearPointsException{
        //xpoints, ypoints, npoints from triangle
            double a0, a1, c0, c1;
            a0 = mytri.points[A].getX() - mytri.points[B].getX();
            a1 = mytri.points[A].getY() - mytri.points[B].getY();
            c0 = mytri.points[C].getX() - mytri.points[B].getX();
            c1 = mytri.points[C].getY() - mytri.points[B].getY();
            
             
            double det = a0*c1-(c0*a1);
//            System.out.println ("Circumcircle " + mytri.toString());
           // double det = mytri.xpoints[A]*mytri.ypoints[C] - mytri.xpoints[C] * mytri.ypoints[A];
            if (det  == 0) {
                System.out.print ("exception colinear points ");// + mytri.points[A].getX()+", "+ mytri.points[C].getY() + ", ");
                System.out.println (mytri.points[A].toString() );
                System.out.println(mytri.points[B].toString());
                System.out.println(mytri.points[C].toString());

                System.out.println ("-------------colinear points-----------");
                String msg=mytri.points[A].toString()+", "+ mytri.points[B].toString()+", "+mytri.points[C].toString();
                throw new ColinearPointsException(msg);
                
            }
            det = 0.5/det;  
            double asq = a0 * a0 + a1 *a1;
            double csq = c0 * c0 + c1 * c1;
            double ctr0 = det * (asq*c1 - csq*a1);
            double ctr1 = det * (csq * a0 - asq*c0);
            radius =  Math.sqrt (ctr0*ctr0 + ctr1*ctr1);


            float xf =  (float) (ctr0 + mytri.points[B].getX());
            float yf =  (float) (ctr1 + mytri.points[B].getY()); 

            center = new MyPoint (xf,yf);
          //  System.out.println ("Printing the radius and center of the circle " + radius + " ("+ xf + ", "+yf + ")");
        }

        public MyPoint getCenter(){
            return center;
        }
        public double getRadius() {
            return radius;
        }

    }


}
