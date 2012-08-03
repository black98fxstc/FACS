/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.stanford.facs.drawing;

import edu.stanford.facs.delaunay.Delaunay;
import edu.stanford.facs.delaunay.MyPoint;

import java.awt.Graphics;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;


import edu.stanford.facs.delaunay.Triangle;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import javax.swing.JButton;
import javax.swing.JCheckBox;
//import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.JSplitPane;
import javax.swing.event.TreeModelListener;
import javax.swing.JScrollPane;

/**
 * $Id: Exp $
 * @author cate
 */
public class DrawingFrame  extends JFrame implements TreeModelListener{


    private DrawingPanel panel;
    
    private Delaunay model;
    boolean LabelsOn = true;
    Color almostWhite = new Color (250, 250, 250);
    
   // private ArrayList <Triangle>triangleList = new ArrayList<Triangle>();
    private ArrayList <MyPoint> pointList = new ArrayList<MyPoint>();
//    private ArrayList <Point[]> lineList = new ArrayList <Point[]>();
    private MyPoint[] newpoints = new MyPoint[13];
    private HashMap <Long, MyPoint[]> lineHash = new HashMap <Long, MyPoint[]>();
 //   private HashMap <Long, MyPoint.Float[]> zoomHash = new HashMap<Long, MyPoint.Float[]>();
    private int last=0;
    private ArrayList <MyPoint[]> erasedLines = new ArrayList <MyPoint[]>();
    private File dataFile;
    private int leftMargin = 10;
    private int bottomMargin = 10;
    private static float BORDER = (float)0.10;
    private float xmin, xmax, ymin, ymax;
    private float dataxmin, dataymin, dataxmax, dataymax;
    private int xintmin, xintmax, yintmin, yintmax;
    private int widthxborder=10;
    private int heightxborder=10;
    private static int SWIDTH=900;
    private static int SHEIGHT = 800;
    private JButton addPoint;
    private JTree jtree;
    private JPanel treePanel;
    private DefaultTreeModel treeModel;
    

    public DrawingFrame()  {
        super();
        
        treePanel = new JPanel();
        JScrollPane treeScroll = new JScrollPane(treePanel);
        
        
        panel = new DrawingPanel ();
        JScrollPane drawingScroll = new JScrollPane (panel);
        setXscale(0, SWIDTH);
        setYscale(0, SHEIGHT);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, drawingScroll);
        getContentPane().add (splitPane, BorderLayout.CENTER);
//        getContentPane().add (panel, BorderLayout.CENTER);
//        getContentPane().add (treePanel, BorderLayout.WEST);
        
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel south = new JPanel();
      //  addPoint = new JButton ("Add Point ");
       // addPoint.addActionListener (model);

        final JCheckBox showLabels = new JCheckBox (" Show Labels", true);
        
        showLabels.addItemListener (new ItemListener(){
            public void  itemStateChanged (ItemEvent e){
                
                if (e.getStateChange() == ItemEvent.DESELECTED){
                    LabelsOn = false;  
                }
                else LabelsOn = true;
            }
        });
        JButton readData = new JButton ("Select File");
        readData.addActionListener (new ActionListener() {
            public void actionPerformed (ActionEvent e){
                dataFile = selectFile();
            }
        });

        JButton ready = new JButton ("Print Triangles");
        ready.addActionListener (new ActionListener() {
            public void actionPerformed (ActionEvent e){
              //  model.printForR();

            }
        });
        south.add (addPoint);
        south.add (showLabels);
        south.add (readData);

        south.add (ready);
        add (south, BorderLayout.SOUTH);
        setPreferredSize(new Dimension (1200, 1200));
        setLocation (500, 500);
       
        pack();
       // model = new Delaunay (this);

        setVisible(true);

    }
    
    public void addJTree (DefaultMutableTreeNode root){
        treeModel = new DefaultTreeModel (root);
        treeModel.addTreeModelListener (this);
        jtree = new JTree (treeModel);
        treePanel.add (jtree);
    }
    
    public void addTreeNode (DefaultMutableTreeNode parent, DefaultMutableTreeNode child){
        treeModel.insertNodeInto(child, parent, parent.getChildCount());  
        jtree.scrollPathToVisible (new TreePath(child.getPath()));
    }
    
    
    
    public void addActionListener (ActionListener lis){
        addPoint.addActionListener (lis);
    }

    public void setDataMinMax (float minx, float miny, float maxx, float maxy){
        
        dataxmin = minx;
        dataymin = miny;
        dataxmax = maxx;
        dataymax = maxy;


    }
    
    public void setBoundingBox (Triangle tri){
        System.out.println ("  Bounding Box "+ tri.toString());
    }

    public void setModel (Delaunay model){
        this.model = model;
    }
    
    
    
//    public void addTree (TriangleTree tree){
//        this.tree = tree;
//    }

    public File selectFile () {
        JFileChooser chooser = new JFileChooser();
        File f = chooser.getSelectedFile();
        return f;

    }
   

    public void addLine (MyPoint a, MyPoint b){
       // if (!a.isTransformed())
      //      a = transformPoint (a);
       // if (!b.isTransformed())
       //     b = transformPoint(b);

        MyPoint []line={a, b};
        Long code = new Long (a.hashCode() + b.hashCode());
       // System.out.println ("Frame AddLine: " + code + "  ("+ a.x+","+a.y+") ("+ b.x+ ", "+b.y+")");
        lineHash.put (code, line);
       
    }

    public void addLine (Triangle tri){
        MyPoint[] pts = tri.getPoints();

        if (pts != null && pts.length == 3){
            addLine (pts[0], pts[1]);
            addLine (pts[1], pts[2]);
            addLine (pts[2], pts[0]);
        }
    }

  

    public void eraseLine (MyPoint a, MyPoint b){
        System.out.println (" erase line " + a.toString() + ", "+ b.toString());
//        MyPoint.Float[] line = {a, b};
        MyPoint[] line = new MyPoint[2];
        
      // (!a.isTransformed())
            a = transformPoint (a);
      //  if (!b.isTransformed())
            b = transformPoint (b);
        line[0] = a;
        line[1] = b;
        erasedLines.add (line);

        Long code = new Long (a.hashCode() + b.hashCode());
        System.out.println ("eraseline for this code " + code);
        lineHash.remove (code);
        updateDisplay();
    }
    
    public void updateDisplay() {
        panel.paintComponent(panel.getGraphics());
    }




    public void addPoint(MyPoint p) {
//        System.out.println ("Drawing Frame addPoint " + p.xf + ", " + p.yf);
     //   p = transformPoint (p);
       // String label = new String ("("+ p.x+ ", "+ p.y+ ")");
    	MyPoint newp = new MyPoint ((float) p.getX(), (float)p.getY());
        newp = transformPoint (newp);
//        System.out.println (" Frame Add point " + newp.toString() + " "+ newp.x + ", "+ newp.y);
     //   newp.addLabel (label);
        pointList.add (newp);

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

      // helper functions that scale from user coordinates to screen coordinates and back
    private float  scaleX(float x) {
        //float xxmin=0, xxmax=1;
//        return SWIDTH  * (x - xxmin) / (xxmax - xxmin) + widthxborder;
//        System.out.println ("Scale x dataxmin and dataxmax "+ dataxmin + "  "+ dataxmax);
                
//        System.out.print (" Scale x "+ x + " "+ xmin + " "+ xmax + " ");
        float ans = SWIDTH  * (x - dataxmin) / (dataxmax - dataxmin) + widthxborder;
//        System.out.println ("  ="+ ans);
//          return SWIDTH  * (x - xmin) / (xmax - xmin) + widthxborder;
        return ans;

    }
    private float scaleX(double x){
        float ans = SWIDTH  * (float)(x - dataxmin) / (dataxmax - dataxmin) + widthxborder;
        return ans;
    }

    private float  scaleY(float y) {
       
//        return SHEIGHT * (yymax - y) / (yymax - yymin) + heightxborder;
        
         float ans = SHEIGHT * (dataymax - y) / (dataymax - dataymin) + heightxborder;
//         System.out.println (" = " + ans);
          //return SHEIGHT * (ymax - y) / (ymax - ymin) + heightxborder;
         return ans;

    }
    private float scaleY (double y){
    	 float ans = SHEIGHT * (float)(dataymax - y) / (dataymax - dataymin) + heightxborder;
    	 return ans;
    }

    public MyPoint  transformPoint (MyPoint p){
       // p.x = transformPoint (p.xf);
        p.setX( (int)scaleX (p.getX()));
        p.setY( (int) scaleY (p.getY()));
      //  p.setTransformed (true);
//        if (p.x > dataxmax) p.x = (int) dataxmax;
//        else if (p.x < dataxmin) p.x=(int)dataxmin;
//        if (p.y > dataymax) p.y = (int) dataymax;
//        else if (p.y < dataymin) p.y = (int) dataymin;
    //    p.addLabel (new String ("("+p.xf+","+p.yf+")"));
//        System.out.println ("  Transform point " + p.getLabel() + "  "+ p.x + ", "+ p.y+"  "+p.xf + " "+ p.yf );
        return p;
    }
    
   /** public void zoomData(MyPoint.Float[] boundingBox) {
    //remove the bounding box from the line hash.  Then change the transformation.
           float zoom= 4;
           float shift = 400;
           Integer zoomcode;
           
           Collection <MyPoint.Float[]> ee = lineHash.values();
           Iterator it = ee.iterator();
           while (it.hasNext()){
              MyPoint.Float[] one = (MyPoint.Float[]) it.next();
              //does it connect to a bounding box point?  If so, don't add it.  
              if (!isLineToBoundingBox (boundingBox, one)) {
                  MyPoint.Float[] newline = new MyPoint.Float[2];
                  newline[0] = new MyPoint.Float (one[0].xf * zoom, one[0].yf *zoom);
              //    newline[0] = transformPoint (newline[0]);
                  newline[0].x -= shift;
                  newline[0].y -= shift;
                  newline[1] = new MyPoint.Float(one[1].getX() * zoom, one[1].getY() * zoom);
                //  newline[1] = transformPoint (newline[1]);
                  newline[1].x -= shift;
                  newline[1].y -= shift;
                 // zoomcode = new Integer (newline[0].hashCode() + newline[1].hashCode());
                 // System.out.println ("zooms "  + newline[0].toString() + " -- "+ newline[1].toString());
                //  zoomHash.put(zoomcode, newline);
              }
              
            }
           
           JDialog dialog = new JDialog();
           dialog.setSize(800, 800);
           dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
           ZoomPanel zpanel = new ZoomPanel();
           dialog.getContentPane().add (zpanel);
           dialog.setVisible (true);
           
           zpanel.paintComponent(zpanel.getGraphics());
           
           
    }
    
    **/
    
    private boolean isLineToBoundingBox (MyPoint[] box, MyPoint[] line) {
       boolean isLine = false;
       
       if (line[0].equals(box[0]) || line[0].equals(box[1]) || line[0].equals(box[2]))
           isLine=true;
       if (!isLine){
           if (line[1].equals(box[0]) || line[1].equals (box[1]) || line[1].equals(box[2]))
               isLine = true;
       }
       if (isLine){
           System.out.println ("Line to bounding box " +line[0].toString() +", "+ line[1].toString());
       }
       return isLine;
    }
    
  




    public static void main(String[] args){
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new DrawingFrame();
            }
        });

   
    }

    @Override
    public void treeNodesChanged (TreeModelEvent tme) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void treeNodesInserted (TreeModelEvent tme) {
        System.out.println ("tree node inserted. ");
    }

    @Override
    public void treeNodesRemoved (TreeModelEvent tme) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void treeStructureChanged (TreeModelEvent tme) {
        System.out.println (" tree Structure Changed");
    }
    
   /** class ZoomPanel extends JPanel {
        ZoomPanel() {
            super();
            setPreferredSize (new Dimension (800, 800));
            setBackground (Color.LIGHT_GRAY);
        }
        public void paintComponent (Graphics g){
          g.setColor (Color.BLACK);
          
          Collection <MyPoint.Float[]> ee = zoomHash.values();
          Iterator it = ee.iterator();
          while (it.hasNext()){
              MyPoint.Float[] one = (MyPoint.Float[]) it.next();
              g.drawLine (one[0].x, one[0].y, one[1].x, one[1].y);
              System.out.println (one[0].toString() + "----"+ one[1].toString());
             
          }
        }
    }**/



    class DrawingPanel extends JPanel {
        DrawingPanel() {
            super();
            setPreferredSize(new Dimension(600, 600));
            setBackground (Color.LIGHT_GRAY);


        }
    
        @Override
        public void paintComponent (Graphics g){

          g.setColor (Color.YELLOW);
          System.out.println ("-------Paint Component--erasedLines");
          for (MyPoint[] line: erasedLines){
              g.drawLine (line[0].getSX(), line[0].getSY(), line[1].getSX(), line[1].getSY());
              System.out.println(line[0].getSX() + ", "+ line[0].getSY() + ")  (" + line[1].getSX() + ", "+ line[1].getSY());
          }
          g.setColor (Color.BLUE);
          System.out.println ("---------Point List------");
          for (MyPoint p: pointList){
//              g.drawOval (p.x, p.y, 3, 3);
              g.fillOval (p.getSX(), p.getSY(), 3, 3);
             /**if (LabelsOn && p.getLabel() != null)
                  g.drawString(p.getLabel(), p.x+4, p.y-4);
                   System.out.println (p.getLabel() );**/
            
          }

          g.setColor (Color.BLACK);
          
          Collection <MyPoint[]> ee = lineHash.values();
          System.out.println ("------  Line List ------ ");
          Iterator it = ee.iterator();
          while (it.hasNext()){
              MyPoint[] one = (MyPoint[]) it.next();
              g.drawLine (one[0].getSX(), one[0].getSY(), one[1].getSX(), one[1].getSY());
 System.out.println(one[0].getSX() + ", "+ one[0].getSY() + ")  (" + one[1].getSX() + ", "+ one[1].getSY());

             
          }
         
         
          
        }
    }



}
