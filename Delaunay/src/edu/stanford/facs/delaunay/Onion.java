package edu.stanford.facs.delaunay;

import java.io.File;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.stanford.facs.delaunay.ConvexHullTwo.XCompare;
import edu.stanford.facs.delaunay.MyPoint.PointLevelCompare;
/**
 * Now I have the triangles.  I have to assign the level points.  Then from the bottom up,
 * find the triangles/convex hulls that have different levels.  Those points that are of
 * the same level -- that is the same number of steps to the outside -- are in the same
 * cluster.  The points/lines that have different levels are the boundary points between
 * the clusters.  
 * 1.  sort the points by levels.  
2.  Start with the points with the highest level, level n
2a.  I am keeping a list of clusters that are a list of points.  
3.  Look at each point at level n, one at a time.  If this point has not been assigned, look at my neighbors.  
    If none of my neighbors have been assigned to a cluster, then assign point n and its neighbors to cluster i.  
    If one of my neighbors has been assigned to cluster, this point and all my neighbors will be given the 
    same cluster.  If my neighbors have been assigned to more than one cluster, then this point is a boundary point.  Mark it was such and do nothing with the neighbors.
4.  After all the points at level n have been looked at, let all the clusters take one step out -- 
    by that I mean that each point in each cluster will find its neighbors and decide if the neighbors belong 
    this cluster.  
5.  Now look at the points are level n-1 and repeat.  
 * @author cate2
 *
 */
public class Onion {
	
	Map<Long, MyPoint[]> lineList;
	//OneLine[] singleLines;
	OneLine[] doubleLines;
	TriangleTree tree;
	Map <Long, MyPoint> pointList = new HashMap<Long,MyPoint>();
	static int A=0;
	static int B=1;
	static int C=2;
	static int ONHULL=0;
	static int INSIDE=-1;
	static int POINT_ONE=1;
	static int POINT_TWO=2;
	static int NOT_ASSIGNED=-1;
	
	
	ConvexHullTwo convexHull = new ConvexHullTwo();
	

	//ConvexHullTwo.XCompare xcompare = new ConvexHullTwo.XCompare();
	//static int 
	ArrayDeque <Triangle> queue = new ArrayDeque<Triangle>();
	ArrayDeque <MyPoint> pointQueue = new ArrayDeque<MyPoint>();
	ArrayList<ArrayList<MyPoint>> clusterList = new ArrayList<ArrayList<MyPoint>>();
	
	protected class LevelCompare implements Comparator<OneLine> {
        @Override
        public int compare(OneLine o1, OneLine o2) 
        {
                return (new Integer(o1.one.getLevel())).compareTo(new Integer(o2.one.getLevel()));
        }
}
	
	protected class CompareNames implements Comparator <Object>{
		public int compare (Object o1, Object pt){
			if (o1 instanceof OneLine){
				OneLine line= (OneLine)o1;
				if (pt instanceof MyPoint){
					MyPoint ptt = (MyPoint)pt;
					
					return (line.one.compareTo(ptt));
				}
				else if (pt instanceof OneLine){
					OneLine line2 = (OneLine)pt;
					
                    return (line.one.compareTo(line2.one));
				}
				else throw new ClassCastException();
					
			}
			else throw new ClassCastException();
		}
	}
	
	protected class CompareTwo implements Comparator<OneLine>{
		public int compare (OneLine o1, OneLine o2){
			return (o1.two.getName()).compareTo(o2.two.getName());
		}
	}
	
	class OneLine implements Comparable { //throw ClassCastException{
		MyPoint one, two;
		int status;
		
		OneLine (MyPoint[] line){
			one = line[0];
			two = line[1];
			id = makeHashCodeForLine(one,two);
			status=NOT_ASSIGNED;
		}
		
		OneLine (MyPoint A, MyPoint B){
			one = A;
			two = B;
			id = makeHashCodeForLine(one,two);
			status=NOT_ASSIGNED;

		}
		Long id;

		@Override
		/*This is sorting by the first point.  Sorting by Name*/
		public int compareTo(Object arg0) {
			// TODO Auto-generated method stub
			if (arg0 instanceof OneLine){
				OneLine line = (OneLine) arg0;
				return (one.compareTo(line.one));
				//return (one.compareTo(line.one));
			}
			else if (arg0 instanceof MyPoint){
				MyPoint pt = (MyPoint) arg0;
			
				return (one.compareTo(pt));
			}
			else throw new ClassCastException();
			
		}
		private Long makeHashCodeForLine (MyPoint a, MyPoint b){
	        int h = 0;

	        h = a.hashCode() - b.hashCode();
	        return new Long (h);
	    }

		
		public String toString() {
			StringBuilder buf = new StringBuilder (one.toString()).append(", ").append(two.toString());
			return buf.toString();
		}
		
		/*  Return 0 if this line is a boundary line.
		 *  Return 1 one point is a boundary and one is inside
		 *  Return -1 when neither point is a boundary point
		 *  ONHULL=0;
	     *  INSIDE=-1;
	     *  POINT_ONE=1;
	     *  POINT_TWO=2;
		 *  */
		protected int onBoundary (OneLine[] boundary){
			
			int i=0;
			int flag = INSIDE;
			while ( i < boundary.length){
				System.out.println (boundary[i].id + ", "+ this.id);
				if (Math.abs(boundary[i].id) == Math.abs(this.id) ){
					flag = ONHULL;
					break;
				}
				if (one.getName().equals (boundary[i].one.getName()))
					flag=POINT_ONE;
				else if (one.getName().equals(boundary[i].two.getName())){
					flag = POINT_TWO;
					
				}
				i++;
			}
			return flag;
		}
	}
	
	
	public Onion ( TriangleTree tree,  Map<Long, MyPoint> pointList, Map<Long, MyPoint[]> lineList){
		
		this.pointList = pointList;///don't need this pointList coming in.  
		this.lineList = lineList;
		this.tree = tree;
		//setLevels();
		//constructHull();
		this.pointList = assignLevelsByPoints2();
		//when this method finishes, doubleLines is sorted by Level, not by Names.
		//walkOut is called by Delaunay. 
	//	 printPointsToFile (this.pointList);
		 
		
		
	}
	
	
	/**
	 * the levels have been assigned.  Can I make any sense of the clusters?
	 * singleLines are of type OneLine.  One pt is called one and the other is called two.
	 */
	protected void walkOut() {
		int clusterId = 0;
		
		
		Collection<MyPoint> ptLevels = pointList.values();
		MyPoint[] pointsWithLevels = new MyPoint[ptLevels.size()];
		pointsWithLevels = ptLevels.toArray(pointsWithLevels);
		Arrays.sort(pointsWithLevels, new PointLevelCompare());  // sorted by levels
		
		walkOut4(pointsWithLevels);
		/**while (cur > 0){
			MyPoint curPoint = pointsWithLevels[cur];
			
			if (curPoint.clusterId == 0){
				clusterId++;
				curPoint.addClusterId(clusterId);
			}
			
			if (!curPoint.boundaryPoint){
				ArrayList<MyPoint> neighbors = findMyNeighbors(curPoint);  
				
				assignClusterId (neighbors, curPoint.getClusterId());
				System.out.println ("\t Me and My neighbors " + curPoint.toString());
				for (MyPoint n: neighbors){
					System.out.println ("\t"+ n.toString() );
				}
			}
			else {
				System.out.println (" Boundary point "+ curPoint.toString());
			}
			
			cur--;
		}**/
		printPointsWithClusterIds(pointsWithLevels);
	
	}
	/** Use the clusterList.  Get the point with the highest level.
	 * Main a clusterList of neighbors.  Just one level of neighbors.
	 * Get the next point of the highest level and build out its neighbors.
	 * This is more like a breadth first search.  When there are no more
	 * points are level n, start with the clusterList.  Each clusterList
	 * takes a step out, that is each point gets its neighbors. The clusterList
	 * becomes the neighbors of the neighbors.  When I have looked at all the
	 * clusterLists, see if there are any points at level n-1 that are not in a
	 * clusterList.  Those become new clusterList.
	 * 
	 * @param pointsWithLevels
	 */
	
	protected void walkOut4 (MyPoint[] pointsWithLevels){
		
		//first we have to sort the doubleLines by Names.
		Arrays.sort(doubleLines);
		int cur = pointsWithLevels.length-1;
		int currentLevel = pointsWithLevels[cur].getLevel();
		int clusterId = -1;
		
		while (currentLevel > -1){  
			
		     cur = morePointsInLevel(cur, currentLevel, pointsWithLevels);
		     if (cur>=0 && pointsWithLevels[cur].getLevel() == currentLevel){
		    	 //cur = morePointsInLevel(cur, currentLevel, pointsWithLevels);
		    	 if (pointsWithLevels[cur].getClusterId() == NOT_ASSIGNED && !pointsWithLevels[cur].boundaryPoint){ // not yet assigned
		    		 System.out.println (cur + ".  " + pointsWithLevels[cur].toString());
		    		 //clusterId++;
		    		// pointsWithLevels[cur].addClusterId(clusterId);
		    		 //first get my neighbors.  If one or more are assigned to a cluster,
		    		 //all my neighbors will join that cluster.
		    		 //if there is more than one cluster among my neighbors,then
		    		 //this point is a boundary point or I made a mistake.
		    		 ArrayList<MyPoint>neighbors = findMyNeighbors(pointsWithLevels[cur]);
		    		 boolean merge = true;
		    		 int  assigned = NOT_ASSIGNED;
		    		 for (MyPoint np: neighbors){
		    			 if (np.getClusterId() != NOT_ASSIGNED && !np.boundaryPoint){
		    				 if (assigned == NOT_ASSIGNED)
		    				     assigned = np.getClusterId();
		    				 else {
		    					 //more than one neighbor has been assigned
		    					 if (assigned != np.getClusterId()){
		    						 pointsWithLevels[cur].boundaryPoint = true;
		    						 np.boundaryPoint = true;
		    						 System.out.println ("More than one  neighbor has been assigned to a cluster " + pointsWithLevels[cur].toString());
		    						 System.out.println (" "+ np.toString());
		    						 merge = false;
		    					 }
		    					 else
		    						 merge = true;
		    						 
		    				 }
		    			 }
		    			     //np.addClusterId(clusterId);
		    			 else{
		    				 merge &= true;
		    				 
		    				 System.out.println ("should we merge instead?  " + merge);

		    			 }
		    			 
		    		 }
		    		 // so now I have checked my neighbors.  If merge is false, do nothing.  If assigned
		    		 // is > 0, assigned me and all my neighbors to the cluster 'assigned'
		    		 if (assigned == NOT_ASSIGNED ){
		    			 clusterId++;
		    			 assigned = clusterId;
		    		 }
		    		// else if (merge == true)
		    		//	 clusterId = assigned;
		    		 
		    		 
		    		 if (merge == true){		 
		    			 pointsWithLevels[cur].addClusterId (assigned);
		    			 if (clusterList.size() == assigned )  //assigned is the clusterid and the clusterList is indexed by the clusterId
		    				 clusterList.add(assigned, new ArrayList<MyPoint>());
		    			 clusterList.get(assigned).add(pointsWithLevels[cur]);
		    			 for(MyPoint np: neighbors)
		    				 if (np.getClusterId() == NOT_ASSIGNED){
			    				 np.addClusterId(assigned);
			    		         clusterList.get(assigned).add( np);
		    				 }
		    				 else{
		    					 //my neighbor is assigned
		    					 np.boundaryPoint=true;
		    					// findthispoint (pointsWithLevels, np);
		    				 }
		    		 }
		    	 }
		    	 else { // this point is already assigned
		    		 System.out.println (pointsWithLevels[cur].toString() + " has already been assigned.");
		    		 
		    	 }
		    	 
		    	
		     }
		     else{
		    	 currentLevel--;
		     //all clusters within this level,step out one.
		    	 for( ArrayList<MyPoint> list: clusterList){
		    		 ArrayList<MyPoint> myneighbors=new ArrayList<MyPoint>();
		    		 
		    		 for (MyPoint pt: list){
		    			 myneighbors.addAll( stepOut(pt)); //these have been checked  

		    		 }
		    		 if (myneighbors != null){
		    			 list.addAll(myneighbors);
		    		 }
		    	 }
		     }
		}
		printPointsWithClusterIds (pointsWithLevels);
	}
	
	private ArrayList<MyPoint> stepOut (MyPoint pt){
		
		ArrayList<MyPoint> list = new ArrayList<MyPoint>();
		ArrayList<MyPoint> neighbors = findMyNeighbors (pt);
		for (MyPoint mp: neighbors){
			if (mp.getClusterId() < 0){
				mp.addClusterId (pt.getClusterId());
				list.add(mp);
			}
		}
		return list;
	}
	
	private void findthispoint (MyPoint[] pointsWithLevels, MyPoint np){
		// do I need to change the boundary point here?
		System.out.println (np.toString());
		
	}
	protected void walkOut3 (MyPoint[] pointsWithLevels){
		Arrays.sort(doubleLines);
		
		int cur = pointsWithLevels.length -1;
		int curLevel = pointsWithLevels[cur].getLevel();
		int clusterId = -1;
		cur = morePoints (cur,pointsWithLevels);
		while (cur >= 0){
			
		
			// this point has not been assigned
			if (pointsWithLevels[cur].getLevel() == curLevel){
				if (pointsWithLevels[cur].getClusterId() < 0){ 
					clusterId++;
					pointsWithLevels[cur].addClusterId ( clusterId);
					ArrayList<MyPoint> neighbors = findMyNeighbors(pointsWithLevels[cur]);
					for (MyPoint pt : neighbors)
						pt.addClusterId(clusterId);
					clusterList.add(clusterId, neighbors);
					
				}
			//this point is already in a cluster.  the clusterId is the index of the
			//clusterList.  
				else {
					ArrayList<MyPoint > neighbors = clusterList.get(pointsWithLevels[cur].getClusterId());
					int clusterid = pointsWithLevels[cur].getClusterId();
					ArrayList<MyPoint > newneighbors = new ArrayList<MyPoint>();
					for (MyPoint pt: neighbors){
						ArrayList<MyPoint> stepout = findMyNeighbors(pt);
						for(MyPoint npt: stepout)
							npt.addClusterId(clusterid);
						newneighbors.addAll(stepout);
					}
					clusterList.get(clusterid).clear();
					clusterList.set(clusterid, newneighbors);
			//	}
				}
			cur = morePoints(cur, pointsWithLevels);
			}
			else {
				curLevel--;
				//let all the cluster step out one level.
				int listid=0;
				for (ArrayList<MyPoint> list: clusterList){
					System.out.println (listid + "  listid ");
					ArrayList<MyPoint> newneighbors = new ArrayList<MyPoint>();
					for( MyPoint ptl : list){
						ArrayList<MyPoint>myneighbors = findMyNeighbors(ptl);
						for( MyPoint pt: myneighbors){
							pt.addClusterId(ptl.getClusterId());
							System.out.println (ptl.getClusterId() + " cluster id ");
						}
						 newneighbors.addAll( myneighbors);
						
					}
					clusterList.get(listid).clear();
					clusterList.get(listid).addAll (newneighbors);
					listid++;
				}
				
			}
			
		}
		
	}
	
	protected void walkOut2(MyPoint[] pointsWithLevels) {
		Arrays.sort(doubleLines);
		int cur = pointsWithLevels.length - 1;
		int clusterId=1;
		cur = morePoints(cur, pointsWithLevels);
		while(cur >=0){
			pointsWithLevels[cur].addClusterId(clusterId);
			assignClusterId2 (clusterId, pointsWithLevels[cur], pointsWithLevels);
			clusterId++;
			
			cur = morePoints(cur, pointsWithLevels);
			
		}
	}
	
	private void assignClusterId2 (int clusterid, MyPoint pt, MyPoint[] allpoints){
		
		ArrayList<MyPoint >neighbors = findMyNeighbors(pt);
		if (neighbors.isEmpty()){
			System.out.println ("No neighbors found.!!!  "+ pt.toString());
		}
		System.out.println ("AssignClusterId2 " + pt.toString() + "  size "+ neighbors.size()+ "  "+ clusterid);
		if (! neighbors.isEmpty()){
			for(MyPoint np: neighbors){
				if (np.getClusterId() == 0){   // if the point is not in a cluster
					//System.out.println (np.toString());                          // if the level assignment is okay
					if (np.getLevel() == pt.getLevel() || np.getLevel() == pt.getLevel()-1 ){
					    np.addClusterId(clusterid);
					    assignClusterId2 (clusterid, np, allpoints);
					}
					else{
						System.out.println ("Unassigned but larger than "+ np.toString());
						np.boundaryPoint=true;
						System.out.println("Are they both boundary points?");
					}

				}
				else if (np.getLevel() > pt.getLevel()){
					//np.boundaryPoint = true;
					//pt.boundaryPoint = true;
					pt.boundaryPoint = true;
					System.out.println ("Level is greater "+ np.toString());
				}
			}
		}
	}
	
	private void assignClusterId(ArrayList<MyPoint> neighbors, int clusterid){
		for (MyPoint pt: neighbors){
			if (pt.getClusterId() == 0)    // is it already assigned?
			   pt.addClusterId(clusterid);
			else if (pt.getClusterId() == clusterid)
				System.out.println ("This is a merge");
			else
				pt.boundaryPoint=true;
			
		}
	}
	
	private int morePointsInLevel (int cur, int level, MyPoint[]pts){
		while(cur > -1 ){
			if (pts[cur].getLevel() == level){
				if (pts[cur].clusterId >=0 || pts[cur].boundaryPoint){
					cur--;
				}
				else
					break;
			}
			else
				break;
		}
		return cur;
	}
	
	private int morePoints(int cur, MyPoint[] pts){
		
		while (cur > -1 ){
			if (pts[cur].boundaryPoint)  //skip the boundaryPoints
				cur--;
			else if (pts[cur].clusterId >= 0) //already assigned to a cluster
				cur--;
			else  //not a boundary, not assigned.
				break;
		}
		System.out.println (" more points "+ cur);
		return cur;
	}
	
	private void printPointsWithClusterIds (MyPoint[] pts){
		System.out.println ("Points with cluster ids ");
		File f = new File ("/Users/cate2/Eclipse/workspace/Newone/Delaunay/R/clusters.txt");
		
        FileOutputStream fw = null;
        try{
        	fw= new FileOutputStream (f);
        	String h = "name\tx\ty\tlevel\tcluster\tboundary\n";
        	fw.write(h.getBytes());
        	StringBuilder buf = new StringBuilder();
        	for (int i=0; i < pts.length; i++){
        		if (pts[i].getLevel() > 0){
        			buf.append(pts[i].getName()).append("\t").append(pts[i].getX()).append("\t");
        			buf.append(pts[i].getY()).append("\t").append(pts[i].getLevel()).append("\t");
        			buf.append(pts[i].getClusterId()).append("\t").append(pts[i].boundaryPoint).append("\n");
        			fw.write(buf.toString().getBytes());
        			buf = new StringBuilder();
        		}
        		
        	}
        	fw.close();
        } catch (IOException e){
            System.out.println (" file io exception ");
        } finally {
            
        }
		
	}
	
	
	
	private ArrayList<MyPoint> findMyNeighbors ( MyPoint pt){
		System.out.println ("find My Neighbors "+ pt.getName());
		ArrayList<MyPoint> neighbors = new ArrayList<MyPoint>();
	//	CompareNames compnames = new CompareNames();
		int n = Arrays.binarySearch(doubleLines, pt);
		if (n < 0 )
			return neighbors;
		System.out.println ("find my neighbors :: "+ pt.toString() );
		int[] range= getRange(doubleLines,pt.getName(), n );
		int low = range[0];
		int high = range[1];
		for (int i= low; i < high; i++){
			neighbors.add(doubleLines[i].two);
			System.out.println ("  ,  "  + doubleLines[i].two.toString());
		}
		
		return neighbors;
	}
	
	private boolean withinCluster (MyPoint pt, int level){
		boolean flag = true;
		if (pt.getLevel() < level-1){
			flag=false;
		}
		return flag;
	}
	
	private void assembleClusters (){
		//using the pointList and the OneLine[] singleLines, find clusters?
		
        Collection <MyPoint> points = pointList.values();
        MyPoint[] ppoints = new MyPoint[pointList.size()];
        ppoints = points.toArray(ppoints);
        
        Arrays.sort(ppoints, new PointLevelCompare());
	}
	
	
	
	private void constructHull (){
		Triangle [] daughters = tree.treeRoot.getDaughters();
		int level=1;
		for (Triangle t: daughters){
			MyPoint[] dpts = t.getPoints();
			for (int i=0; i < dpts.length; i++){
				dpts[i].setLevel(level);
			}
			
			ArrayList<MyPoint> connectingPoints = linesConnectedToTriangle(t, lineList);
			connectingPoints = convexHull.execute(connectingPoints);
			for (MyPoint pt: connectingPoints){
				if (pt.getLevel() == NOT_ASSIGNED || pt.getLevel() < (level+1))
					pt.setLevel(level+1);
			}
			System.out.println (" One Daughter ");
			String one = createHullLinesAsStrings(connectingPoints);
			System.out.println (one);
		}
	}
	
	/**private void printResults (ArrayList<String> results){
		FileOutputStream fw=null;
	   try{
		   File f = new File ("./DataOut/hull.txt");
    	   fw= new FileOutputStream (f);
    	   String h = "x1\ty1\tx2\ty2\tLevel\n";
    	   fw.write(h.getBytes());
    	   for (String s: results){
    		   fw.write(s.getBytes());
    	   }
    	   fw.close();
	   } catch (IOException e){
        System.out.println (" file io exception ");
	   } finally {
        
	   }	
	}**/
	
	
	/* Not being called.  It is called by constructHull()*/
	
	private ArrayList<MyPoint> linesConnectedToTriangle(Triangle t, Map<Long, MyPoint[]> lineList){
		
		ArrayList<MyPoint> connectingPoints = new ArrayList<MyPoint>();
		Collection <MyPoint[]> lines = lineList.values();
		
		MyPoint[] tpoints = t.getPoints();
		OneLine[] trilines = new OneLine[6];
		for (int i=0; i < 3; i++){
			int j=i*2;
			trilines[j]= new OneLine (tpoints[i], tpoints[(i+1)%3]);
			trilines[j+1] = new OneLine (tpoints[(i+1)%3], tpoints[i]);
		}
		
		System.out.println ("-----------------------------------------------------");
		System.out.println ("\t"+ tpoints[0].toString() + ", "+ tpoints[0].getLevel());
		System.out.println ( "\t " + tpoints[1].toString() + ", " +tpoints[1].getLevel());
		System.out.println ("\t"+ tpoints[2].toString() + ", "+ tpoints[2].getLevel());
		
		
	    OneLine[] mylines = new OneLine[lines.size()];
	    int i=0;
	    for (MyPoint[] pts : lines){
	    	mylines[i++] = new OneLine (pts[0], pts[1]);
	       // mylines[i++] = new OneLine (pts[1], pts[0]);
	    }
	    int index=0, low, high;
	    Arrays.sort(mylines);
	  //  MyPoint[] pt = new MyPoint[2];
	 //   for ( i=0; i < tpoints.length; i++){
	    for ( i=0; i < trilines.length; i++){
	    	//pt[0] = trilines[i].one;
	    	//pt[1] = trilines[i].two;
	    	index = Arrays.binarySearch(mylines, trilines[i]);
	    	if (index>-1 & index< mylines.length){
	    	low = index;
	    	while (mylines[index].one.getName().equals(mylines[low].one.getName()) && low > 0){
	    		low--;
	    		/**if (low < 0) {
	    			System.out.println("Not found low is < 0 ");
	    			return null;
	    		}**/
	    	}
	    	
	    	high = index;
	    	while (mylines[index].one.getName().equals(mylines[high].one.getName()) && high < mylines.length-1){
	    		high++;
	    		/**if (high == mylines.length){
	    			System.out.println ("Not found, high is > mylines.length");
	    			return null;
	    		}**/
	    	}
	    	System.out.println ("low, high "+ low + ", "+ high);
	    	
	    	for (int j=low+1; j < high; j++){
	    		if (notTrianglePoint (tpoints, mylines[j].two)){
	    			if (mylines[j].two.getLevel() == -1  ){
	    				mylines[j].two.setLevel(mylines[j].one.getLevel()+1);
	    			}
	    			connectingPoints.add(mylines[j].two);
	    		}
	    	}
	    	}
	    	else
	    		System.out.println("index is out of bounds for the line "+ trilines[i].toString());
	    }
	    
    
		return connectingPoints;
	}
	
	private boolean notTrianglePoint (MyPoint[] triangle, MyPoint pt){
		boolean flag=false;
		if (!pt.equals(triangle[0]) ){
			if (!pt.equals(triangle[1])){
				if (!pt.equals(triangle[2]))
					flag = true;
			}
		}
		//System.out.println ("Not a Triangle point returns "+ flag + "  " + triangle[0].toString() + ", "+ triangle[1].toString() + ", "+ triangle[2].toString() + ", "+ pt.toString());
		return flag;
	}
	
	private String createHullLinesAsStrings (ArrayList<MyPoint> hullpts){
		
		StringBuilder buf = new StringBuilder();
		int size = hullpts.size();
		MyPoint A, B;
		for (int i=0; i < size; i++){
			A = hullpts.get(i);
			B = hullpts.get ((i+1)%size);
			buf.append(A.getName() +"\t"+A.getX()+"\t"+ A.getY()+"\t"+ B.getName()+"\t"+ B.getX()+ "\t"+ B.getY()+ "\t"+A.getLevel()+"\n");
		}
		return buf.toString();
	}
	
	
	
	private int[] getRange (OneLine[]lines, String name, int index ){
		int[] range = new int[2];
		int low = index;
		if (index < 0)
		   System.out.println ("name = ");
		while (low >=0 && lines[low].one.getName().equals(name) ) low--;
		//if (low == -1 )
		//	if (!lines[low].one.getName().equals(name)) low++;
		range[0] = low+1;
		int high = index;
		while (lines[high].one.getName().equals(name)&& high < lines.length-1) high++;
		if (high == lines.length-1 && lines[high].one.getName().equals(name))
			range[1] = high;
		else
		    range[1] = high-1;
		
		return range;
	}
	
	/**
	private void assignLevelsByLines2() {
		
		//start with the lines of the bounding box.  They are AB, BC, CA
		MyPoint[] boundingTri = tree.treeRoot.getPoints();
		MyPoint Apt, Bpt, Cpt;
		
		if (pointList.containsKey(boundingTri[0].getId())){
		    Apt = pointList.get(boundingTri[0].getId());
		    Apt.setLevel(0);
		}
		if (pointList.containsKey(boundingTri[1].getId())){
		    Bpt = pointList.get(boundingTri[1].getId());
		    Bpt.setLevel(0);
		}
		if (pointList.containsKey(boundingTri[2].getId())){
		   Cpt = pointList.get(boundingTri[2].getId());
		   Cpt.setLevel(0);
		}
		
		ArrayList<Long> keyarray = new ArrayList<Long>();
		keyarray.addAll(lineList.keySet());
	
		
		int i=0;
		int level=0;
		MyPoint pt1, pt2;
		while (keyarray.size() > 0){
			
			
		for (i = keyarray.size()-1; i > -1 ; i--){
			MyPoint[] line = lineList.get(keyarray.get(i));
			pt1 = pointList.get(line[0].getId());
			pt2 = pointList.get(line[1].getId());
			System.out.println ("\n"+pt1.getName() + ", "+ pt1.getLevel() + ", "+ pt2.getName() + ", "+ pt2.getLevel());

 			if (pt1.getLevel() == pt2.getLevel() ){
				if (pt1.getLevel() != MyPoint.UND)
					keyarray.remove(i);
			}
			else if (pt1.getLevel() == level ){
				pt2.setLevel(level+1);
				keyarray.remove(i);	
			}
			else if (pt2.getLevel() == level){
				pt1.setLevel(level+1);
				keyarray.remove(i);
			}
			
			System.out.println (pt1.getName() + ", "+ pt1.getLevel() + ", "+ pt2.getName() + ", "+ pt2.getLevel());

		}
		level++;
		System.out.println ("Round " + level + "  " + keyarray.size() );

		}
	} **/
	private Map<Long, MyPoint> assignLevelsByPoints2() {
		
		int index=0, low, high;
		int level=0;
		Collection <MyPoint[]> coll = lineList.values();
		doubleLines = new OneLine[coll.size() * 2];
		int i=0, k=0;
	System.out.println ("assignLevels by Points 2");
		Iterator<MyPoint[]> it = coll.iterator();
		while (it.hasNext()){
			MyPoint[] one = it.next();
			doubleLines[i] = new OneLine (one[0], one[1]);
			i++;
			doubleLines[i] = new OneLine(one[1], one[0]);
			i++;
			
		}
		System.out.println("  just before sorting ");
		
		Arrays.sort(doubleLines);
	/**	System.out.println (" -------- double Lines ---------");
		for (OneLine line:  doubleLines){
			System.out.println (line.toString());
		}**/
		
		
		MyPoint[] boundingbox = tree.treeRoot.getPoints();
	    for (MyPoint pt: boundingbox){
	    	pt.setLevel(level);
	    	pointQueue.addLast(pt);
	    }
		
		while (!pointQueue.isEmpty()){
			MyPoint pt = pointQueue.removeFirst();
		//	System.out.println ("Looking for this point in doubleLines " + pt.toString());
			index = Arrays.binarySearch(doubleLines, pt);
		//	System.out.println ("Index = " + index + "  "+ pt.toString());
			//find the range
			if (index >-1 && index < doubleLines.length){
	    		low = index;
	    		while (doubleLines[index].one.getName().equals(doubleLines[low].one.getName())&& low > 0)
	    			low--;
	    		high = index;
	    		while(doubleLines[index].one.getName().equals(doubleLines[high].one.getName())&& high < doubleLines.length-1)
	    			high++;
	    		for (int j=low+1; j < high; j++){
		    		
	    			if (doubleLines[j].two.getLevel() == NOT_ASSIGNED ){
	    				doubleLines[j].two.setLevel(doubleLines[j].one.getLevel()+1);
	    				pointQueue.addLast(doubleLines[j].two);
	    			}
	    			else if (doubleLines[j].two.getLevel()+1 < doubleLines[j].one.getLevel()){
	    				doubleLines[j].one.setLevel(doubleLines[j].two.getLevel()+1);
	    			//	one.setLevel(doubleLines[j].two.getLevel()+1);
	    				pointQueue.addLast(doubleLines[j].one); // this value changed -- do others need to be reevaluated?
	    			}
	    			else if (doubleLines[j].two.getLevel() > doubleLines[j].one.getLevel()+1){
	    				doubleLines[j].two.setLevel(doubleLines[j].one.getLevel()+1);
	    				pointQueue.addLast(doubleLines[j].two);
	    			
	    			}
		    				
		    	}
			}
			else {
	    		System.out.println ("--------------index is out of range for point "+ pt.toString());
	    	}
		
		
	    
		}
		for (  i=0; i < doubleLines.length; i++){
	    	if (doubleLines[i].one.getLevel() == NOT_ASSIGNED && doubleLines[i].two.getLevel() > NOT_ASSIGNED){
	    		doubleLines[i].one.setLevel(doubleLines[i].two.getLevel()+1);
	    		MyPoint one = pointList.get(doubleLines[i].one.id);
	    		one.setLevel(doubleLines[i].two.getLevel()+1);
	    	}
		}
		Arrays.sort(doubleLines, new LevelCompare());
	    System.out.println ("-------------assignLevels by Points2----------------------------------");
	    Map<Long, MyPoint> justPoints = new HashMap<Long, MyPoint>();
	    for ( i=0; i < doubleLines.length; i++){
	    	if (!justPoints.containsValue(doubleLines[i].one)){
	    		justPoints.put(doubleLines[i].one.id, doubleLines[i].one);
	    	}
	    	if (!justPoints.containsValue(doubleLines[i].two)){
	    		justPoints.put(doubleLines[i].two.id, doubleLines[i].two);
	    	}
	    }
	    return justPoints;
	}
	
	
	
	/* Map <Long, MyPoint> pointList Current one!!  */
	private Map<Long, MyPoint> assignLevelsByPoints() {
		Collection<MyPoint[]>lineValues =  lineList.values();
		OneLine[] doubleLines = new OneLine[lineValues.size()];
		doubleLines = lineValues.toArray(doubleLines);
		int i=0;
		Iterator<MyPoint[]> it = lineValues.iterator();
		while (it.hasNext()){
			MyPoint[] one = it.next();
			doubleLines[i] = new OneLine (one[0], one[1]);
			i++;
			
		}
	    
	    int index=0, low, high;
	    int level=0;
	    //pointQueue
	    Arrays.sort(doubleLines);  ///this is for lookup
	    //start with the points in the bounding box.
	   

	  //  ClusterNode clusterRoot = new ClusterNode(tree.treeRoot.getPoints(), mylines);
	    MyPoint[] boundingbox = tree.treeRoot.getPoints();
	    for (MyPoint pt: boundingbox){
	    	pt.setLevel(level);
	    	pointQueue.addLast(pt);
	    }
	    while (!pointQueue.isEmpty()){
	    	MyPoint pt = pointQueue.removeFirst();
	    	System.out.println (pt.toString());
	    	index = Arrays.binarySearch(doubleLines, pt); // this is not predictable, must find range.
	    	if (index >-1 && index < doubleLines.length){
	    		low = index;
	    		while (doubleLines[index].one.getName().equals (doubleLines[low].one.getName()) && low > 0)
	    			low--;
	    	
		    	high = index;
		    	while(doubleLines[index].one.getName().equals(doubleLines[high].one.getName())&& high < doubleLines.length-1)
		    			high++;
		    	//now I have the range
		    	System.out.println ("low, high "+ low + ", "+ high);
		    	for (int j=low+1; j < high; j++){
		    		
	    			if (doubleLines[j].two.getLevel() == NOT_ASSIGNED ){
	    			//	two.setLevel(doubleLines[j].one.getLevel()+1);
	    				doubleLines[j].two.setLevel(doubleLines[j].one.getLevel()+1);
	    				pointQueue.addLast(doubleLines[j].two);
	    			}
	    			else if (doubleLines[j].two.getLevel()+1 < doubleLines[j].one.getLevel()){
	    				doubleLines[j].one.setLevel(doubleLines[j].two.getLevel()+1);
	    			//	one.setLevel(doubleLines[j].two.getLevel()+1);
	    				pointQueue.addLast(doubleLines[j].one); // this value changed -- do others need to be reevaluated?
	    			}
	    			else if (doubleLines[j].two.getLevel() > doubleLines[j].one.getLevel()+1){
	    				doubleLines[j].two.setLevel(doubleLines[j].one.getLevel()+1);
	    				//two.setLevel(doubleLines[j].one.getLevel()+1);
	    				pointQueue.addLast(doubleLines[j].two);
	    			}
		    				
		    	}
		    	
	    	}
	    	else {
	    		System.out.println ("--------------index is out of range for point "+ pt.toString());
	    	}
	    }
	    
	    for ( i=0; i < doubleLines.length; i++){
	    	if (doubleLines[i].one.getLevel() == NOT_ASSIGNED && doubleLines[i].two.getLevel() > NOT_ASSIGNED){
	    		doubleLines[i].one.setLevel(doubleLines[i].two.getLevel()+1);
	    		MyPoint one = pointList.get(doubleLines[i].one.id);
	    		one.setLevel(doubleLines[i].two.getLevel()+1);
	    	}
	    }
	    
	    Arrays.sort(doubleLines, new LevelCompare());
	    System.out.println ("-----------------------------------------------");
	    Map<Long, MyPoint> justPoints = new HashMap<Long, MyPoint>();
	    for ( i=0; i < doubleLines.length; i++){
	    	if (!justPoints.containsValue(doubleLines[i].one)){
	    		justPoints.put(doubleLines[i].one.id, doubleLines[i].one);
	    	}
	    	if (!justPoints.containsValue(doubleLines[i].two)){
	    		justPoints.put(doubleLines[i].two.id, doubleLines[i].two);
	    	}
	    }
	    
	   
	 /**   
	    Iterator <MyPoint>itp = justPoints.values().iterator();
	    while (itp.hasNext()){
	    	MyPoint p= itp.next();
	    	System.out.println (p.getName() + "\t"+p.getX()+"\t"+ p.getY() + "\t"+ p.getLevel());
	    }**/
	    	    
	   return justPoints;
		
	}
	
	
	private void printPointsToFile(Map<Long, MyPoint> pts){
		Iterator <MyPoint> itp = pts.values().iterator();
		FileOutputStream fw = null;
		try {
			File f = new File("/Users/cate2/Eclipse/workspace/Newone/Delaunay/R/points.txt");
			fw = new FileOutputStream(f); 
			String h="name\tx1\ty1\tlevel\n";
			fw.write(h.getBytes());
			StringBuilder buf = new StringBuilder();
			while (itp.hasNext()){
				MyPoint p = itp.next();
				buf.append(p.getName()+"\t"+ p.getX()+"\t"+ p.getY()+"\t"+ p.getLevel()+"\n");
				fw.write(buf.toString().getBytes());
				buf = new StringBuilder();
				
			}
			fw.close();
		} catch (IOException ioe){
			System.out.println (ioe.getMessage());
		} finally {
			
		}
	}
	
	
	
	/*
	 * take the triangle tree and the all lines Map and assign levels to the points. 
	 *  
	 */
	private void assignLevels(){
		MyPoint[] points = tree.treeRoot.getPoints();
    	int level = 1;
    	System.out.println (lineList.size() + "  size of line list ");
    	
    	Triangle curTri;
    	Triangle[] daughters = tree.treeRoot.getDaughters();
    	for (Triangle tri : daughters){
    		MyPoint[] pts = tri.getPoints();
    		for (int i=0; i < 3; i++){
    			pts[i].setLevel(level);
    			pointList.put(pts[i].id, pts[i]	);
    			//do these points need to be added here to the master list?  No
    		}
    	    queue.addLast(tri);
    	}
    	
    	while (!queue.isEmpty()){
    		/* check the points against the triangle*/
    		System.out.println(" size of queue?  "+ queue.size());
    		curTri = queue.removeFirst();
    		
			daughters = curTri.getDaughters();
			for (Triangle tri : daughters){
				if (tri != null)
	    	        queue.addLast(tri);
	    	}
    		points = curTri.getPoints();
    		for (int i=0; i < points.length; i++){
    			for (int j = 0; j < daughters.length; j++){
    				if (daughters[j] != null){
    					MyPoint[] dpts = daughters[j].getPoints();
    					for (int k=0; k < dpts.length; k++){
    						if (dpts[k].equals(points[i])){
    							System.out.println ("  Points are equal "+ i + ", "+j + "," + k);  //points[i].toString() + ", "+ dpts[k].toString());
    							break;
    						}
		    				Long mkey = makeHashCodeForLine (dpts[k], points[i]); //because of the way the triangles are stored
		    				System.out.println (mkey + ", "+ dpts[k].toString() + ", " + points[i].toString());
		    				if (lineList.containsKey (mkey) || lineList.containsKey(mkey*-1)){
		    					//if (allPoints.containsKey(points[i].id)){
		    						//MyPoint mpt = allPoints.get(points[i].id);
		    						//System.out.println(mpt.toString() + ", "+ mpt.getLevel() + "---" + points[i].toString() + ", "+ points[i].getLevel());
		    					//}
		    					int l0 = points[i].getLevel();
		    					if (dpts[k].getLevel() < 0)
		    						dpts[k].setLevel(l0+1);
		    					else if (dpts[k].getLevel() >= l0)
		    						dpts[k].setLevel(l0+1);
		    					pointList.put(dpts[k].id, dpts[k]);
		    						
		    				}
    					}
    				}
    			}
    		} /* end of for loop */
    		
    	}  /*queue is empty */
    	
	}
	
	public Map<Long, MyPoint[]> getLineList() {
		return lineList;
	}
	
	public  boolean lineExists (MyPoint A, MyPoint B){
		boolean exists = false;
		
		Long key = new Long( A.hashCode() - B.hashCode());
		exists = lineList.containsKey(key);
		//if (lineList.containsKey(key)){
		//	System.out.println("lineList contains key " + A.toString() + ", "+ B.toString());
		//}
		return exists;
		
	}
	
	public Map<Long, MyPoint> getPointList(){
		return pointList;
	}
	
	private Long makeHashCodeForLine (MyPoint a, MyPoint b){
        int h = 0;

        h = a.hashCode() - b.hashCode();
        return new Long (h);
    }
	
	private void printLineList(){
		Collection<MyPoint[]> values = lineList.values();
		Iterator <MyPoint[]>it = values.iterator();
		while (it.hasNext()){
			MyPoint[]pt = it.next();
			System.out.println (pt[0].toString()+ ", "+ pt[1].toString() );
		}
	}
	
	

}
