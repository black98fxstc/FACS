/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.stanford.facs.gui;

/**
 * $Id: CompensationResults.java,v 1.2 2011/11/09 19:49:46 beauheim Exp $
 * @author cate
 */
public interface CompensationResults  {
    
    public void setFailsLinearityTest(int i, int j, boolean fails);
    public void setFailsInterceptTest(int i, int j, boolean fails);
    public void setSpectrum(int i, int j, double spillover, double uncertainty);
    public void reportMessage (String msg, boolean flag);

    public void spilloverNotSignificantTest (int reagent, int j, boolean b);
    public void setFailsSignificanceTest (int i, int j, boolean fails);

    public void enableButton (boolean b);
    
    
}
