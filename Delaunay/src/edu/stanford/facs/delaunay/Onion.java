package edu.stanford.facs.delaunay;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Onion {
	
	Map<Long, MyPoint[]> lineList;
	TriangleTree tree;
	Map <Long, MyPoint> allPoints = new HashMap<Long,MyPoint>();
	static int A=0;
	static int B=1;
	static int C=2;
	ArrayDeque <Triangle> queue = new ArrayDeque<Triangle>();
	
	public Onion (TriangleTree tree, Map<Long, MyPoint[]> lineList){
		this.tree = tree;
		this.lineList = lineList;
		assignLevels();
		
		printLineList();
	}
	/*
	 * take the triangle tree and the all lines Map and assign levels to the points.  
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
    			allPoints.put(pts[i].id, pts[i]	);
    			//do these points need to be added here to the master list?
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
		    					allPoints.put(dpts[k].id, dpts[k]);
		    						
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
		return allPoints;
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
			System.out.println (pt[0].toString()+ ": "+ pt[0].getLevel() + ", "+ pt[1].toString() + ": "+pt[1].getLevel());
		}
	}

}
