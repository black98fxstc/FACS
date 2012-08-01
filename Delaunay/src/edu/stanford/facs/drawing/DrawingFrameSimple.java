package edu.stanford.facs.drawing;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import edu.stanford.facs.delaunay.MyPoint;
import edu.stanford.facs.drawing.DrawingFrame.DrawingPanel;

public class DrawingFrameSimple extends JFrame{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int SWIDTH=900;
	private int SHEIGHT=900;
	private float dataxmin, dataymin, dataxmax, dataymax;
	private int leftMargin = 10;
    private int bottomMargin = 10;
    private float xmin, xmax, ymin, ymax;
    private int xintmin, xintmax, yintmin, yintmax;
    private int widthxborder=10;
    private int heightxborder=10;
    private static float BORDER = (float)0.10;
    private MyPoint[] convexHull;
    private MyPoint[][] lineList;
    private DrawingPanel panel;

	public DrawingFrameSimple(){
		super();
		init();
		pack();
		setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);
		setVisible(true);
		setPreferredSize(new Dimension (1200, 1200));
        setLocation (500, 500);
        
		
	}
	
	public void updateDisplay() {
        panel.paintComponent(panel.getGraphics());
    }
	
	private void init () {
		panel = new DrawingPanel();
        JScrollPane scroll = new JScrollPane(panel);
      
        setXscale(0, SWIDTH);
        setYscale(0, SHEIGHT);
        getContentPane().add (scroll); 
	}
	
	public void setLineList  (MyPoint[][]lines){
		lineList = lines;
		for (MyPoint[] line: lines){
			for (MyPoint pt: line){
				transformPoint (pt);
			}
		}
	}
	
	public void setConvexHull (MyPoint[] hull){
		convexHull = hull;
	}
	
	 public void setDataMinMax (float minx, float miny, float maxx, float maxy){
	        
        dataxmin = minx;
        dataymin = miny;
        dataxmax = maxx;
        dataymax = maxy;

    }
	 
	 private void setXscale(float min, float max) {

	        float size = max - min;
	        xmin =  BORDER * size - min + leftMargin;
	        xmax = max + BORDER * size + leftMargin;
	        xintmin = (int) xmin;
	        xintmax = (int) xmax;
	    }

	    /**
	     * Set the y-scale (a 10% border is added to the values).
	     * @param min the minimum value of the y-scale
	     * @param max the maximum value of the y-scale
	     */
	    private void setYscale(float min, float max) {
	        float size = min + max;
	        ymax = min + BORDER * size - bottomMargin;
	        ymin = max + BORDER * size - bottomMargin;
	        yintmin = (int)ymin;
	        yintmax = (int)ymax;


	    }
	    private float  scaleX(float x) {

	                
//	        System.out.print (" Scale x "+ x + " "+ xmin + " "+ xmax + " ");
	        float ans = SWIDTH  * (x - dataxmin) / (dataxmax - dataxmin) + widthxborder;
//	        System.out.println ("  ="+ ans);
//	          return SWIDTH  * (x - xmin) / (xmax - xmin) + widthxborder;
	        return ans;

	    }
	   

	    private float  scaleY(float y) {
	       
//	        return SHEIGHT * (yymax - y) / (yymax - yymin) + heightxborder;
	        
	         float ans = SHEIGHT * (dataymax - y) / (dataymax - dataymin) + heightxborder;
//	         System.out.println (" = " + ans);
	          //return SHEIGHT * (ymax - y) / (ymax - ymin) + heightxborder;
	         return ans;

	    }
	    public MyPoint  transformPoint (MyPoint p){
	        // p.x = transformPoint (p.xf);
	    	int sx = (int) scaleX (p.getX());
	    	int sy = (int) scaleY(p.getY());
	         p.setSX( (int)scaleX (p.getX()));
	         p.setSY( (int)scaleY (p.getY()));
	         System.out.println ("transformPoint " + p.toString() + "  "+ sx + ", "+ sy);
	         return p;
	     }
	    
	    class DrawingPanel extends JPanel {
	        DrawingPanel() {
	            super();
	            setPreferredSize(new Dimension(600, 600));
	            setBackground (Color.LIGHT_GRAY);
	        }
	    
	        @Override
	        public void paintComponent (Graphics g){

	        
	        /**  for (MyPoint[] line: erasedLines){
	              g.drawLine (line[0].getSX(), line[0].getSY(), line[1].getSX(), line[1].getSY());
	              System.out.println(line[0].getSX() + ", "+ line[0].getSY() + ")  (" + line[1].getSX() + ", "+ line[1].getSY());
	          }**/
	          g.setColor (Color.BLUE);
	         
	          if(convexHull != null){
	          for (MyPoint p: convexHull){
//	              g.drawOval (p.x, p.y, 3, 3);
	              g.fillOval (p.getSX(), p.getSY(), 3, 3);
	             /**if (LabelsOn && p.getLabel() != null)
	                  g.drawString(p.getLabel(), p.x+4, p.y-4);
	                   System.out.println (p.getLabel() );**/
	            
	          }
	          }

	          g.setColor (Color.BLACK);	          
	          
              for (MyPoint[] one: lineList){             
	              g.drawLine (one[0].getSX(), one[0].getSY(), one[1].getSX(), one[1].getSY());
	 System.out.println(one[0].getSX() + ", "+ one[0].getSY() + ")  (" + one[1].getSX() + ", "+ one[1].getSY());
	             
	          }
	          
	        }
	    }


}
