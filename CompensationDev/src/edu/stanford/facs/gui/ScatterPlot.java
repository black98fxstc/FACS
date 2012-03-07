/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 * Scatter plot is log FSC vs log SSC.  Data is in the Y arrays in the
 * Stained Control and are already in natural log (ln).  Plot 2.5-3 decades
 * as appropriate.  If gate[] is Gate.RANGE, then data values are undefined.
 *
 * Fluorescence plot :  selected detector (col in table ) vs primary detector
 * in the Stained Control for that row.  Data is  X[row] and X[col] arrays.  Row
 * is the horizontal axis.  Use the Logicle scale with T= 1<<18 and W=.5.
 * the logicle scale() function with these parameters map the data values in X[i][j]
 * into 0 to 1.  Then scale to draw the panel.
 */

package edu.stanford.facs.gui;

//import edu.stanford.facs.compensation.DebuggingInfo;
import java.awt.Graphics;
import java.awt.event.ItemEvent;
import javax.swing.JPanel;
import edu.stanford.facs.logicle.Logicle;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ItemListener;
import java.util.Random;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * $Id: ScatterPlot.java,v 1.15 2011/06/17 00:38:01 beauheim Exp $
 * @author cate
 */
public class ScatterPlot extends JPanel implements ItemListener {
    private static int NVALUES=200;
    double[][]data = new double[2][NVALUES];
    double[][]trueData = new double[2][NVALUES];
	private static final long serialVersionUID = 1L;

    private Random random;
    private int xintmin, xintmax, yintmin, yintmax;
    private double xmin, xmax; // these are screen values
    private double ymin, ymax;

    private int SWIDTH=400;
    private int SHEIGHT=400;

    private static final double BORDER = 0.14;
    private int [][]axis= new int[2][6];
    private String[]axisLabels = new String[6];
    private Logicle logicle;
    private int [][] displayData;
    private int[]gates;
    private GatesColors gateColorMapping = new GatesColors();
    private Rectangle whitebackground;
   // int[][] intdata = null;
    //0 total 1 scatter 2 autofl 3 spectrum gated 4 out of range 5 final
    // gating enumeration does not match the order of the check boxes and the gating
    // enumeration as it stands is used in other places (flow jo and jump).  So my
    //mapping between the check boxes and the colors will be changed to reflect
    //the screen order of the statistics.
    //My order is Scatter Gated = 0 or enum=3
    //            Autofluorescnece = 1 or enum=2
    //            Spectrum gated = 2 or enum (outlier) = 1
    //            Out of range = 3  or enum =4
    //            Total = 4  or enum=0
    //RANGE(4), SCATTER(3), AUTOFLUORESCENCE(2), OUTLIER(1);
//    private Color[] gateColors = {Color.BLACK, Color.CYAN, Color.GREEN, Color.BLUE, Color.YELLOW, Color.RED};

    private String rowLabel, colLabel;
    private int rowlabelstart, collabelstart;
    private int leftMargin=12;
    private int bottomMargin=12;
    private Dimension panelDim;
    private double widthxborder;
    private double heightxborder;
//    private boolean[] colorIt = {true, true, true, true, true};

    ScatterPlot (int swidth, int sheight) {

        double x =  swidth + 2*BORDER*swidth + leftMargin;
        double y = sheight + 2*BORDER*sheight + bottomMargin;
        SWIDTH=swidth;
        SHEIGHT=sheight;
        widthxborder = SWIDTH*BORDER + leftMargin;
        heightxborder = SHEIGHT*BORDER - bottomMargin;
        panelDim = new Dimension ((int)x, (int)y);
        setPreferredSize(panelDim);
        setMaximumSize (panelDim);

    }
    public Dimension getPanelDimension() {
        return panelDim;
    }


    public void setLabels (String rowLabel, String colLabel){
        this.rowLabel = rowLabel;
        this.colLabel = colLabel;
        int len = rowLabel.length() * 10;
        rowlabelstart = SWIDTH/2 - len/2;
        len = colLabel.length() *10;
        collabelstart = SHEIGHT/2 + len/2;

    }
    // W=.5, M=4.5, A=0
      //public Logicle (double T, double W, double M, double A)

    public void plotData (float[] x, float[] y, int[] gates,
                          double T, double W, double M, double A){

        this.gates = gates;
        setXscale (0, SWIDTH);
        setYscale (0, SHEIGHT);
 
        whitebackground = new Rectangle (xintmin+1, yintmax+1, xintmax-xintmin-2, yintmin-yintmax-2);
        if ( x == null || y == null){
            return;
        }
        if (x.length != y.length){
            return;
        }
       // int T = 1<<18;
         logicle = new Logicle(T, W, M, A);
        

        makeAxis();
        displayData = new int[2][x.length];
     

        for ( int i= 0; i< x.length; i++ ){
          
            double xx = logicle.scale(x[i]);
            if (xx < 0.) xx = 0.0;
            else if (xx > 1.0) xx = 1.0;

            double yy = logicle.scale(y[i]);
            if (yy < 0) yy = 0;
            else if (yy > 1.0) yy = 1.0;
  
           displayData[0][i] = (int) scaleX(xx);
           displayData[1][i] = (int) scaleY(yy);

        }
        
     
        repaint();

        // normal scale range, default bins

    }
    /**
     *
     * @param x
     * @param y
     * The scatter plot for the FSC and SSC data is already in natural log.
     * So no reason to pass it through the Logicle scale.  But I need to
     * get the min and max.
     * FSC gets 3 decades == 1000
     * SSC gets 2 decades == 100
     */
    public void plotScatterData (float[]x, float[]y, int[] gates ) { //float minx, float maxx, float miny, float maxy ){

        if ( x == null || y == null){
            return;
        }
        if (x.length != y.length){
            return;
        }
        this.gates = gates;
        // set minx and miny to 3 and see what happens
        setXscale (0, SWIDTH);
        setYscale (0, SHEIGHT);

        double minx = Math.log (1000);
        double maxx = Math.log (100000);
        double miny = Math.log (1000);
        double maxy = Math.log (100000);
        double minn = 1000.;
        double maxn = 1<<18;
        //262144 is the 1<<18

        whitebackground = new Rectangle (xintmin+1, yintmax+1, xintmax-xintmin-1, yintmin-yintmax-1);

        makeNotLogicalAxis(minx, maxx, miny, maxy);
        displayData = new int[2][x.length];
        double logRange = Math.log(1000);
        double minLog = Math.log(1<<18) - logRange;
        for ( int i= 0; i< x.length; i++ ){   
            double xx = (x[i] - minLog)/logRange;
            double yy = (y[i] - minLog)/logRange;
            if (xx < 0.) xx = 0.0;
            else if (xx > 1.0) xx = 1.0;

           if (yy < 0.) yy = 0.0;
           else if (yy > 1.0) yy = 1.0;            
           displayData[0][i] = (int) scaleX(xx);
           displayData[1][i] = (int) scaleY(yy);
           if (edu.stanford.facs.compensation.Compensation2.CATE){
               System.out.println ("(1 "+ x[i] + ", "+ y[i]);
               System.out.println ("(2) "+ xx + ", "+ yy );
               System.out.println ("(3) " + displayData[0][i] + ",  "+ displayData[1][i]);
           }

        }
        repaint();
    }

   


    /**
     * 13.815510557964274 natural log of 1,000,000  10^6
     * 11.512925464970229 natural log of 100,000    10^5
     * 9.210340371976184 natural log of 10,000      10^4
     * 6.907755278982137 natural log of 1000        10^3
     * 4.605170185988092 natural log of 100         10^2
     * 2^18 = 262,144
     * 2^17 = 131,072
     * 2^16 =  65,536
     * 2^15 =  32,768
     * 2^14 =  16,384
     * @param minx
     * @param maxx
     * @param miny
     * @param maxy
     */

    private void makeNotLogicalAxis(double minx, double maxx, double miny, double maxy) {
      
//        System.out.println (" not logical axis minx, maxy, miny, maxy: "+ minx + ", "+ maxx + ". "+ miny + ", "+ maxy);
	      double logRange = Math.log(1000);
	      double minLog = Math.log(1<<18) - logRange;
        axisLabels = new String[3];
        axis = new int[2][3];
        axisLabels[0]="10 3";
        axis[0][0] = (int) scaleX((Math.log(1000)-minLog)/logRange);
        axis[1][0] = (int) scaleY((Math.log(1000)-minLog)/logRange);
        axisLabels[1]="10 4";
        axis[0][1] = (int) scaleX((Math.log(10000)-minLog)/logRange);
        axis[1][1] = (int) scaleY((Math.log(10000)-minLog)/logRange);
        axisLabels[2]="10 5";
        axis[0][2] = (int) scaleX((Math.log(100000)-minLog)/logRange);
        axis[1][2] = (int) scaleY((Math.log(100000)-minLog)/logRange);
   
    }



    private void makeAxis () {
/*Compare */
       
        int index=0;
        axisLabels = new String[6];
        axis = new int[2][6];
        double s = logicle.scale(0);
        axisLabels[index] = "0";
        axis[0][index] = (int) scaleX(s);
        axis[1][index++] = (int)scaleY(s);
 
       
        s = logicle.scale(10);
        axisLabels[index] = "";
        axis[0][index] = (int) scaleX (s);
        axis[1][index++]= (int) scaleY(s);
        s = logicle.scale(100);
        axisLabels[index]="10 2";
        axis[0][index] = (int) scaleX (s);
        axis[1][index++]= (int) scaleY(s);
        s = logicle.scale(1000);
        axisLabels[index]="10 3";
        axis[0][index] = (int)scaleX(s);
        axis[1][index++]= (int) scaleY(s);

        s = logicle.scale(10000);
        axisLabels[index]="10 4";
        axis[0][index] = (int)scaleX(s);
        axis[1][index++]= (int) scaleY(s);


        s = logicle.scale(100000);
        axisLabels[index]="10 5";
        axis[0][index] = (int)scaleX(s);
        axis[1][index++]= (int) scaleY(s);

       

    }

    protected void paintComponent (Graphics g){

        super.paintComponent (g);
        double theta = Math.toRadians (-90);

        Graphics2D g2d = (Graphics2D) g;
//        g2d.clearRect (0, 0, SWIDTH, SHEIGHT);
        g2d.setColor (Color.white);
        if (whitebackground != null)
            g2d.fill (whitebackground);

        g2d.setColor (Color.black);
        g2d.drawLine (xintmin, yintmin, xintmax, yintmin);
        g2d.drawLine (xintmin, yintmin, xintmin, yintmax);
        g2d.drawLine (xintmin, yintmax, xintmax, yintmax);
        g2d.drawLine (xintmax, yintmin, xintmax, yintmax);
      //  g2d.setColor (Color.BLACK);
        if (rowLabel != null ){
            int labely = yintmin+44;

            g2d.drawString(rowLabel, rowlabelstart, labely);
            g2d.rotate (theta, xintmin-36, collabelstart);  //30

            g2d.drawString (colLabel, xintmin-36, (int)collabelstart);
            g2d.rotate (-theta, xintmin-36, collabelstart);

        }
        if (displayData != null){

            for (int i=0; i < displayData[0].length; i++){
                if (gates != null){
                    if (gateColorMapping.cbIsSet (gates[i]) ){
                        g2d.setColor(gateColorMapping.getColorFromGate(gates[i]));
                        g2d.fillOval (displayData[0][i], displayData[1][i], 2,2);
                    }
                    
                }
            }
        }
        g2d.setColor (Color.BLACK);
        for (int i=0; i < axis[0].length; i++){
            g2d.drawLine (axis[0][i], yintmin+4, axis[0][i], yintmin-4);

            g2d.drawLine (xintmin - 4,axis[1][i], xintmin+4, axis[1][i]);
            if (axisLabels != null && axisLabels[i] != null) {

                String[] s2= axisLabels[i].split(" ");

                //xaxis
                g2d.drawString(s2[0], axis[0][i], yintmin+20);//16

                //y-axis
                g2d.drawString(s2[0], xintmin-30, axis[1][i]);

                if (s2.length > 1){
                    g2d.drawString(s2[1], axis[0][i]+16, yintmin+12); //14 and 10
                    g2d.drawString(s2[1], xintmin-14, axis[1][i]-8);
                }
            }
            
        }
       

    }

    /**
     * Set the x-scale (a 14% border is added to the values)
     * where to use xintmin, xintmax, yintmin, yintmax which are the corders
     * of the white background rectangle.
     * @param min the minimum value of the x-scale
     * @param max the maximum value of the x-scale
     */
    public void setXscale(double min, double max) {

        double size = max - min;
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
    public void setYscale(double min, double max) {
        double size = min + max;
        ymax = min + BORDER * size - bottomMargin;
        ymin = max + BORDER * size - bottomMargin;
        yintmin = (int)ymin;
        yintmax = (int)ymax;


    }
    private double scaleX (double x, double xxmin, double xxmax){
        return SWIDTH  * (x - xxmin) / (xxmax - xxmin) + widthxborder;
    }

    private double scaleY (double y, double yymin, double yymax){

        return SHEIGHT * (yymax-y)/ (yymax-yymin) + heightxborder; //try - here  these were -
    }
      // helper functions that scale from user coordinates to screen coordinates and back
    private double  scaleX(double x) {
        float xxmin=0, xxmax=1;
        return SWIDTH  * (x - xxmin) / (xxmax - xxmin) + widthxborder;
    }

    private double  scaleY(double y) {
        double yymin=0, yymax=1;
        return SHEIGHT * (yymax - y) / (yymax - yymin) + heightxborder;
    }

    //override
    public void itemStateChanged (ItemEvent ie) {
        JCheckBox cb = (JCheckBox) ie.getItem();
        String label = cb.getText();
        
        boolean flag= false;
        if (ie.getStateChange() == ItemEvent.SELECTED)
            flag = true;

        if (label.startsWith ("Scatter"))
            gateColorMapping.setCBValue (0, flag);
          
        else if (label.startsWith ("Autofluo"))
            gateColorMapping.setCBValue(1, flag);

        else if (label.startsWith ("Spectrum"))
            gateColorMapping.setCBValue(2, flag);

        else if (label.startsWith ("Out of"))
            gateColorMapping.setCBValue(3,flag);

        else if (label.startsWith ("Final"))
            gateColorMapping.setCBValue(4, flag);



        repaint();
    }


   
    public abstract class Distribution
  {
    public abstract double sample ();
  }

  class Uniform
    extends Distribution
  {
    final double min;
    final double max;

    public Uniform (double min, double max)
    {
      this.min = min;
      this.max = max;
    }

    public double sample ()
    {
      return min + (max - min) * random.nextDouble();
    }
  }

  class Normal
    extends Distribution
  {
    final double mean;
    final double sd;

    public Normal (double mean, double sd)
    {
      this.mean = mean;
      this.sd = sd;
    }

    public double sample ()
    {
      return mean + sd * random.nextGaussian();
    }
  }

  class LogNormal
    extends Distribution
  {
    final double mean;
    final double sd;

    public LogNormal (double mean, double sd)
    {
      this.mean = mean;
      this.sd = sd;
    }

    public double sample ()
    {
      return Math.exp(mean + sd * random.nextGaussian());
    }
  }


    public static void main (String[] args){
        SwingUtilities.invokeLater(new Runnable()
    {
      public void run ()
      {
        try
        {
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
          JFrame frame = new JFrame("Compensation");
          frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
          ScatterPlot plot = new ScatterPlot(400, 400);
          frame.getContentPane().add (plot);
          frame.setSize (740, 740);
          frame.setLocation (600, 600);
          frame.setVisible(true);
        }
        catch (Exception e)
        {
          e.printStackTrace();
          System.exit(0);
        }
      }
    });

    }

}
