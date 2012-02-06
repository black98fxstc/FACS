package edu.stanford.facs.compensation;

//import javax.swing.SwingUtilities;


import org.isac.fcs.FCSFile;


public abstract class Compensation
  
{
 

  protected Compensation (String title)
  {
 

  }

  protected abstract void analyze ();

  public final double tubeGain (double volts, double standardVolts,
    double voltageGain)
  {
    double gain = Math.exp(Math.log(volts / standardVolts) * voltageGain);

    return gain;
  }

  public final double tubeGain (double volts)
  {
    return tubeGain(volts, Compensation2.PMT_STANDARD_VOLTS, Compensation2.PMT_EXPONENT);
  }
}
