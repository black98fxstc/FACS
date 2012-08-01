package edu.stanford.facs.delaunay;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class ConvexHull extends Delaunay {
	
	private int nhulls=0;
	int[] hullpts;  //indices into the whole set of points in Delaunay
	//thelist is the triangleHash
	int ntree;

	public ConvexHull(float[][]data) {
		
		super(data);
        computeHull();		
	}
	
	private void computeHull(){
		int j,k,pstart;
		ntree = triangleHash.size();
		ArrayList<Integer> nextpt = new ArrayList<Integer>();
		Collection<Triangle> allTriangles = triangleHash.values();
		Iterator<Triangle> it = allTriangles.iterator();
	/**	while (it.hasNext()){
			Triangle onetri = it.next();
			if (onetri.getLiveStatus())   continue;//!= -1 is not the same thing 
			k=1;
			MyPoint[] pts = onetri.getPoints();
			for (int i=0; i<3; i++){
				if (k==3) k=0;
				if (pts[i])
				k++;
			}
		}
		for (j=0; j<ntree; j++) {
			if (thelist[j].stat != -1) continue;
			for (i=0,k=1; i<3; i++,k++) {
				if (k == 3) k=0;
				if (thelist[j].p[i] < npts && thelist[j].p[k] < npts) break;
			}
			if (i==3) continue;
			++nhull;
			nextpt[(pstart = thelist[j].p[k])] = thelist[j].p[i];
		}
		if (nhull == 0) throw("no hull segments found");
		hullpts = new Int[nhull];
		j=0;
		i = hullpts[j++] = pstart;
		while ((i=nextpt[i]) != pstart) hullpts[j++] = i;**/
	}

}
