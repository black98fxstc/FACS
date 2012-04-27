package edu.stanford.facs.compensation;

import org.isac.fcs.FCSFile;
import edu.stanford.facs.gui.FCSFileDialog.TubeContents;

public class ScatterGatedControl
  extends BoundedControl
{
  public int NscatterGated;
  
  protected KDTree kdtree;
  protected TubeContents areCells = TubeContents.BEADS_1;
  protected TubeContents tubeContentType = TubeContents.BEADS_1;

  ScatterGatedControl (Compensation2 comp, FCSFile fcsfile)
  {
    super(comp, fcsfile);
  }
  
  public TubeContents getContentType(){
	  return tubeContentType;
  }
  
  public TubeContents areCells() {
	  return areCells;
  }
  
  public void setContentType(TubeContents areCells){
	  this.tubeContentType = areCells;
	  }
  
  public void setAreCells(TubeContents areCells){
	  this.areCells = areCells;
	  }

  //override
  protected void analyze ()
  {
    super.analyze();

    int Noriginal = exclude.cardinality();
    if (kdtree == null)
    {
      kdtree = new KDTree();
      kdtree.init(Nevents, exclude, Y);
      kdtree.computeDistances();
      kdtree.filterPoints(2.5);



      kdtree.filterPoints(exclude, gate);
      float[] nearest = addFloatAnalysis("Nearest");
      for (int i = 0; i < kdtree.Npoints; ++i)
        nearest[kdtree.index[i]] = (float)kdtree.nearest[i];
    }
    else
      kdtree.filterPoints(Nevents, exclude, gate, Y);
    NscatterGated = exclude.cardinality() - Noriginal;
    if (Compensation2.DEBUG){
      System.out.println((100.0 * NscatterGated / Nevents)
          + "% excluded by scatter");
    }
  }
}
