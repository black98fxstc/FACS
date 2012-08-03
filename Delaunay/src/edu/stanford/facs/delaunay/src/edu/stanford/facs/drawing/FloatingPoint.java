/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.stanford.facs.drawing;

import java.awt.Point;

/**
 * $Id: Exp $
 * @author cate
 */
public class FloatingPoint   extends Point implements Comparable {

        public float xf, yf;
        public String label;
        private boolean transformed = false;


        public FloatingPoint (float x, float y){
           //x,y are the screen coordinates.  They will be transformed in the Frame
            super (-1, -1);
           xf = x;
           yf = y;
           label = (int)x + ", "+ (int)y;
        }

        public boolean isTransformed() {
            return transformed;
        }
        public void setTransformed (boolean t){
            transformed = t;
        }

    @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
        buf = buf.append("(").append(x).append(", ").append(y).append(" (").append(xf).append(", ").append(yf).append (")");
            return buf.toString();
        }

        public void addLabel (String label){
            this.label = label;
        }
        public String getLabel() {

            return label;
        }

        public boolean isNegative() {
            if (xf < 0 || yf < 0)
                return true;
            return false;
        }
        
        public boolean equals (Object t){
            if (t instanceof FloatingPoint){
                FloatingPoint fp = (FloatingPoint)t;
                if (xf == fp.xf && yf == fp.yf)
                    return true;
            }
            return false;
        }
        
        public int hashCode() {
            int hash;   
            hash = java.lang.Float.floatToIntBits (xf) ^ java.lang.Float.floatToIntBits(yf);
//            System.out.println (xf + ",  "+ yf + " == "+ hash);
            
            return hash;
            
        }
    public int compareTo (Object t) {
        
        int ans = 0;
        
        if (t instanceof FloatingPoint){
            FloatingPoint pt = (FloatingPoint)t;
//            System.out.println (" compare "+ xf + " == "+ pt.xf);
            if (xf == pt.xf){
                if (yf == pt.yf)
                    ans=0;
                else if (yf < pt.yf)
                    ans=-1;
                else
                    ans = 1;
            }
            else if (xf < pt.xf)
                ans = -1;
            else if (xf > pt.xf)
                ans = 1;
            
        }
        else {
            ans=0;
        }
        return ans;
    }
  }


