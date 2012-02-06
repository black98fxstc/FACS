package edu.stanford.facs.mcmc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class State
{
  final List<Point> points;
  final double[][] spillover;

  final Random random = new Random();

  final boolean RECORD = true;
  PrintWriter pw;

  double lnP;
  double logStep = .01;
  double transitionRatio = Double.NaN;
  State proposed;

  public double[] signalPerPhoton;
  public double[] backgroundSignal;
  
  private State (int n, List<Point> points, double[][] spillover)
  {
    signalPerPhoton = new double[n];
    backgroundSignal = new double[n];
    this.points = points;
    this.spillover = spillover;
  }

  public State (int n, List<Point> points, double[][] spillover, double[] estimatedSignal, double[] estimatedBackground, File log)
  {
    this(n, points, spillover);
    
    signalPerPhoton = new double[n];
    for (int i = 0; i < signalPerPhoton.length; ++i)
      signalPerPhoton[i] = estimatedSignal[i];
    backgroundSignal = new double[n];
    for (int i = 0; i < backgroundSignal.length; ++i)
      backgroundSignal[i] = estimatedBackground[i];

    this.lnP = 0;
    Iterator<Point> j = points.iterator();
    while (j.hasNext())
      this.lnP += j.next().lnP(this);
    
    proposed = new State(n, points, spillover);

    try
    {
      if (log != null)
      {
        pw = new PrintWriter(log);
        pw.print("Acceptance,q ratio,log P");
        for (int i = 0; i < signalPerPhoton.length; ++i)
        {
          pw.print(',');
          pw.print("s/pe ");
          pw.print(i);
        }
        for (int i = 0; i < backgroundSignal.length; ++i)
        {
          pw.print(',');
          pw.print("bg ");
          pw.print(i);
        }
        pw.println();
      }
    }
    catch (FileNotFoundException e)
    {
      e.printStackTrace();
    }
  }

  private void record (String value)
  {
    if (RECORD && pw != null)
    {
      pw.print(value);
      pw.print(',');
      pw.print(proposed.transitionRatio);
      pw.print(',');
      pw.print(proposed.lnP);
      for (int i = 0; i < signalPerPhoton.length; i++)
      {
        pw.print(',');
        pw.print(proposed.signalPerPhoton[i]);
      }
      for (int i = 0; i < backgroundSignal.length; i++)
      {
        pw.print(',');
        pw.print(proposed.backgroundSignal[i]);
      }
      pw.println();
    }
  }

  public State propose (double logStep)
  {
    proposed.transitionRatio = 1;
    for (int i = 0; i < signalPerPhoton.length; i++)
    {
      proposed.signalPerPhoton[i] = this.signalPerPhoton[i]
        * Math.exp(logStep * random.nextGaussian());
      proposed.transitionRatio *= proposed.signalPerPhoton[i] / this.signalPerPhoton[i];
    }
    for (int i = 0; i < backgroundSignal.length; i++)
    {
      proposed.backgroundSignal[i] = this.backgroundSignal[i]
        * Math.exp(logStep * random.nextGaussian());
      proposed.transitionRatio *= proposed.backgroundSignal[i] / this.backgroundSignal[i];
    }

    proposed.lnP = 0;
    Iterator<Point> j = points.iterator();
    while (j.hasNext())
      proposed.lnP += j.next().lnP(proposed);
        
    return proposed;
  }

  public void accept ()
  {
    record("ACCEPT");
    for (int i = 0; i < signalPerPhoton.length; i++)
      this.signalPerPhoton[i] = proposed.signalPerPhoton[i];
    for (int i = 0; i < backgroundSignal.length; i++)
      this.backgroundSignal[i] = proposed.backgroundSignal[i];
    this.lnP = proposed.lnP;
  }

  public void reject ()
  {
    record("REJECT");
  }

  public void finalize ()
  {
    if (RECORD && pw != null)
    {
      pw.close();
    }
  }
}
