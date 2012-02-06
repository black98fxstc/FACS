/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.stanford.facs.gui;

import java.awt.Color;

/**
 * $Id: GatesColors.java,v 1.6 2011/04/27 03:11:01 wmoore Exp $
 * @author cate
 */
public class GatesColors {

    private boolean[] cbValues = {true, true, true, true, true};
    private float S = 1F;
    private float B = .5F;
    
//	private Color[] wam_colors = {
//			Color.BLACK,
//			Color.getHSBColor(.5F, S, B),
//			Color.getHSBColor(.333F, S, B),
//			Color.getHSBColor(.166F, S, B),
//			Color.getHSBColor(.0F, S, B) };
   private Color[] wam_colors = { Color.BLACK, Color.BLUE, Color.GREEN, Color.RED, Color.ORANGE };
    //this color sequence matches the checkboxes.
    private Color yellowColor = new Color(233,221,53);
    private Color purpleColor = new Color (76, 15,138);
    private Color greenColor = new Color (15, 138, 36);
    private Color redColor = new Color (138, 15,15);
    private Color[] colors = { purpleColor, greenColor, Color.BLUE, redColor, Color.BLACK};
    private int[] codes = new int[5];//for this gate, give me the matching color index

    private Color background = Color.WHITE;
    //My order is Scatter Gated = 0 ==> enum=3
    //            Autofluorescnece = 1 ==> enum=2
    //            Spectrum gated = 2 ==> enum (outlier) = 1
    //            Out of range = 3  ==> enum =4
    //            Total = 4  ==> enum=0
    //RANGE(4), SCATTER(3), AUTOFLUORESCENCE(2), OUTLIER(1);

    public GatesColors() {
            //the gate code is the array index and the stored value
            //is the color index and the cb value which are the same
            //as in codes[0] or the gate=0 is 4.

            codes[0] = 4;  //Color.BLACK
            codes[1] = 2;  //Color.BLUE
            codes[2] = 1;  //Color.GREEN.darker()
            codes[3] = 0;  //Color.YELLOW.darker
            codes[4] = 3;  //Color.RED

//            URL imageIconUrl = GatesColors.class.getResource ("colorkey3.gif");
//            System.out.println ("(1) " + imageIconUrl.toString());
//            URL url2 = GatesColors.class.getResource ("/edu/stanford/facs/gui/colorkey3.gif");
//            System.out.println ("(2)" + url2.toString());


    }

    public void setCBValue (int i, boolean value){
        cbValues[i] = value;
    }

    public Color[] getColorScheme() {
        return wam_colors;
    }

    public boolean cbIsSet (int gate){
        if (gate > cbValues.length)return false;
        return cbValues[codes[gate]];
    }

    public Color getColorFromGate (int gateCode){

        Color mycolor = background;
        if (gateCode < colors.length){
//            if (cbValues[codes[GATE_INDEX][gateCode]] == true){
                mycolor = wam_colors[gateCode];
              
//            }

        }
        return mycolor;

    }

    public static void main (String[] args){

        GatesColors gc = new GatesColors();
        for (int i=0; i < 5; i++){
            Color c = gc.getColorFromGate (i);
            System.out.println (i + "  "+ c.toString());
        }



    }


}
