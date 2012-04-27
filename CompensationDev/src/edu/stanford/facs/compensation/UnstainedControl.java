package edu.stanford.facs.compensation;

import org.isac.fcs.FCSFile;

public class UnstainedControl
  extends ScatterGatedControl
{
  public double[] A;
  public double[] V;
  public float[] AQ2;
  //protected boolean areCells=false is inherited in ScatterGatedControl

  public UnstainedControl (Compensation2 comp, FCSFile fcsfile)
  {
    super(comp, fcsfile);
  }

  //override
  protected void analyze ()
  {
    Selector selector = Tools.threadLocal.get().selector;

    if (Compensation2.DEBUG)
      System.out.println("Unstained");

    super.analyze();
    int ndetectors = comp.getDetectorLength();
    A = new double[ndetectors];
    V = new double[ndetectors];
    AQ2 = new float[ndetectors];

    double[] median = new double[ndetectors];
    double cov[][] = new double[ndetectors][ndetectors];
    double residual[][] = new double[ndetectors][Nevents];
    int[] outlier = addIntegerAnalysis("TRIM");
    for (int pass = 1; pass <= 5; ++pass)
    {
      for (int j = 0; j < ndetectors; ++j)
      {
        selector.reset();
        for (int k = 0; k < Nevents; ++k)
          if (!censored(k))
            selector.add(X[j][k]);
        median[j] = selector.median();
      }
      for (int j = 0; j < ndetectors; ++j)
      {
        float[] results;
        if (Compensation2.RECORD_RESIDUALS)
          results = addFloatAnalysis(comp.getDetectorList()[j] + "-R" + pass);
        for (int k = 0; k < Nevents; ++k)
        {
          residual[j][k] = X[j][k] - median[j];
          if (Compensation2.RECORD_RESIDUALS)
            results[k] = (float)residual[j][k];
        }
      }

      for (int j = 0; j < ndetectors; ++j)
      {
        selector.reset();
        for (int k = 0; k < Nevents; ++k)
          if (!censored(k))
            selector.add(residual[j][k]);
        median[j] = selector.median();
      }

      for (int jx = 0; jx < ndetectors; ++jx)
      {
        for (int jy = 0; jy < ndetectors && jy <= jx; ++jy)
        {
          selector.reset();
          for (int k = 0; k < Nevents; ++k)
            if (!censored(k))
              selector.add((residual[jx][k] - median[jx])
                * (residual[jy][k] - median[jy]));
          cov[jx][jy] = selector.median() / Tools.MEDIAN_CHI_SQUARED_1;
        }
      }
      for (int jx = 0; jx < ndetectors; jx++)
      {
        for (int jy = 0; jy < ndetectors && jy < jx; jy++)
        {
          cov[jy][jx] = cov[jx][jy];
        }
      }
      if (Compensation2.CATE)
      {
        System.out.println("---------how many detectors?----------"
          + ndetectors);
        for (int i = 0; i < ndetectors; i++)
          System.out.println(i + ". " + comp.getDetectorList()[i]);
        for (int ii = 0; ii < cov.length; ii++)
        {
          for (int jj = 0; jj < cov[ii].length; jj++)
            System.out.print(cov[ii][jj] + " ");
          System.out.println();
        }
      }

      Tools.invert(cov);
      double distance[] = new double[Nevents];
      for (int jx = 0; jx < ndetectors; jx++)
      {
        for (int jy = 0; jy < ndetectors && jy <= jx; jy++)
        {
          for (int k = 0; k < Nevents; ++k)
            distance[k] += cov[jx][jy] * (residual[jx][k] - median[jx])
              * (residual[jy][k] - median[jy]);
        }
      }

      double tol = .001;
      double below = 0;
      double above = Double.MAX_VALUE;
      int Nrejected = 0;
      for (int k = 0; k < Nevents; ++k)
      {
        if (censored(k))
          continue;
        double d = distance[k];
        if (d < below)
          continue;
        if (d < above)
        {
          double P = Tools.Q((double)ndetectors / 2, d / 2);
          if (P > tol)
          {
            below = d;
            continue;
          }
          else
            above = d;
        }
        assert above > below;
        censor(k, Gate.OUTLIER);
        outlier[k] = pass;
        Nrejected++;
      }
      assert Nevents != exclude.cardinality() : "Everything was rejected";
//      if (Compensation2.DEBUG)
//        System.out.println(Nrejected + " rejected at pass " + pass);
    }

    int Nincluded = Nevents - exclude.cardinality();
    for (int j = 0; j < ndetectors; ++j)
    {
      // find the mean and variance
      for (int k = 0; k < Nevents; ++k)
        if (!censored(k))
          A[j] += X[j][k];
      A[j] /= Nincluded;

      selector.reset();
      for (int k = 0; k < Nevents; ++k)
      {
        double Q = X[j][k] - A[j];
        if (!censored(k))
        {
          V[j] += Q * Q;
          selector.add(Q * Q);
        }
      }
      V[j] /= Nincluded - 1;
      AQ2[j] = (float)selector.median();
    }

    if (Compensation2.DEBUG)
      System.out.println();
  }
}
