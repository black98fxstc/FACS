/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.stanford.facs.fmo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * $Id: Instrument.java,v 1.3 2011/06/17 00:38:01 beauheim Exp $
 * @author cate
 */
public class Instrument {

        HashMap<String, Detector> detectors;
        float[] adjustedMatrix;
        String name;
        String sep = System.getProperty("line.separator");

        Instrument() {
            detectors = new HashMap<String, Detector>();
           // adjustedMatrix = new float[][];
        }
        public void setName(String name){
            this.name = name;
        }
        protected void addDetector (Detector newdet){
            detectors.put(newdet.name, newdet);
        }
        public HashMap<String, Detector> getDetectors() {
            return detectors;
        }

        public float[] getAdjustedMatrix (){
            return adjustedMatrix;
                      
        }

        //override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            buf.append (sep).append("Instrument:  ").append(name).append(sep);
            Set<String> keys = detectors.keySet();
            Iterator <String>it = keys.iterator();
            while ( it.hasNext()){
                Detector d = (Detector) detectors.get ( it.next());
                buf.append (d.toString());
            }
            return buf.toString();
        }

  public Detector[] getDetectors (String[] detectorNames)
  {
    Detector[] d = new Detector[detectorNames.length];
    for (int i = 0; i < detectorNames.length; ++i)
      d[i] = detectors.get(detectorNames[i]);
    return d;
  }
}
