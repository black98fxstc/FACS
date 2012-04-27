package edu.stanford.facs.compensation;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.isac.fcs.FCSException;
import org.isac.fcs.FCSFile;

import edu.stanford.facs.compensation.Compensation2.Point;
import edu.stanford.facs.data.FlowData;
import edu.stanford.facs.gui.FCSFileDialog.TubeContents;

public class StainedControl
  extends ScatterGatedControl
{
  private static final double MINIMUM_GOODNESS = .01;
	final int primary;
  final int reagent;
  public UnstainedControl unstained;
  public int Nautofluorescence;
  public int NspectrumGated;
  public int Nfinal;
  public double[] slope;
  public double[] slopeSigma;
  public double[] intercept;
  public double[] interceptSigma;
  public double[] covariance;
  public double[] varianceSlope;
  public double[] varianceIntercept;
  public double[][] varianceCoefficient;
  public double[] goodnessOfFit;
  public double[] goodnessOfVariance;
  public float[][] chi3;
  public float[][] fit;
  public boolean[] linearity;
  public boolean[] spilloverNotSignificant;
  
  //protected TubeContents tubeContentType = TubeContents.BEADS_1 is inherited from ScatterGatedControl


  public Diagnostic.List[] diagnostics;
  private String parameterName ;

  private final int BURNED_IN = 3;
  private final int ROBUST_PASS = 6;
  private final int FINAL_PASS = 10;
  private final double LN_10 = Math.log(10);
  private final double TOL_STEP = Math.exp(LN_10 / (FINAL_PASS - BURNED_IN));


  public StainedControl (Compensation2 comp, FCSFile fcsfile, int primary,
    int reagent, String parameterName, FlowData unstained, TubeContents type)
  {
    super(comp, fcsfile);
    this.primary = primary;
    this.reagent = reagent;
    this.unstained = (UnstainedControl)unstained;
    this.parameterName =  parameterName;
    this.tubeContentType = type;
    if (Compensation2.CATE){
        System.out.println ("  Stained control constructor "+ primary + ",  "+ reagent + "  "+ parameterName + "  "+ this.tubeContentType);
        System.out.println("fcs file = "+ fcsfile.getFile().getName());
    }
  }

  public StainedControl (Compensation2 comp, FCSFile fcsfile, int primary,
                         int reagent, String parameterName)
  {
    this(comp, fcsfile, primary, reagent, parameterName, null, TubeContents.BEADS_1);
  }
    
	  
	  
	private void addDiagnostic (double importance, int detector, String message,
			Object... arguments)
	{
        String[] detectorList = comp.getDetectorList();
		if (diagnostics == null)
			diagnostics = new Diagnostic.List[detectorList.length];

		if (diagnostics[detector] == null)
			diagnostics[detector] = new Diagnostic.List();

		String formatted = MessageFormat.format(message, arguments);
		Diagnostic diagnostic = new Diagnostic(importance, reagent, detector, parameterName, formatted);

		diagnostics[detector].add(diagnostic);

		if (Compensation2.DEBUG)
		{
			System.out.printf("%.2f %s, %s%n", importance, comp.reagent[reagent],
					detectorList[detector]);
			System.out.println(formatted);
		}
	}

  public String toString ()
  {
      StringBuilder buf = new StringBuilder();
      buf.append ("  --Stained Control--  ");
      String[] detectorList = comp.getDetectorList();
      if (detectorList == null)
          buf.append ("  detectors have not been set yet ");
      else {
          buf.append (" \t Primary:  ").append(primary).append ( ", ").append( detectorList[primary]);
          buf.append (" \t Reagent: ").append(reagent).append("\n");
          buf.append (" \t Parameter name:  ").append(parameterName);
      }

          buf.append ("\t FCSFile : ").append(fcs.getFile().getName());
    return buf.toString();
  }

	public int getPrimaryDetector ()
	{
		return primary;
	}

	public String getParameterName ()
	{
		return parameterName;
	} 

  public void markovData (final int i, final List<Point> points)
  {
    for (int j = 0; j < comp.getDetectorLength(); ++j)
    {
      if (j == primary)
        continue;
      if (slope[j] < slopeSigma[j])
        continue;
      if (slope[j] < MINIMUM_GOODNESS)
        continue;
      for (int k = 0; k < Nevents; ++k)
        if (!censored(k))
        {
          double yHat = intercept[j] + slope[j] * X[primary][k];
          double residual = (X[j][k] - yHat) * (X[j][k] - yHat);
          points.add(comp.new Point(i, j, primary, X[primary][k], X[j][k],
            yHat, residual));
        }
    }
  }
  
  protected void load () throws FCSException, IOException
  {
  	super.load();
  	
  	if (getContentType() == TubeContents.BEADS_1 && unstained != null)//not sure if this is correct
  	{
  		int Nevents = this.Nevents + unstained.Nevents;
  		
  		float[][] X = new float[this.X.length][Nevents];
  		for (int j = 0; j < X.length; j++)
			{
  			System.arraycopy(unstained.X[j], 0, X[j], 0, unstained.Nevents);
  			System.arraycopy(this.X[j], 0, X[j], unstained.Nevents, this.Nevents);
			}
  		
  		float[][] Y = new float[this.Y.length][Nevents];
  		for (int j = 0; j < Y.length; j++)
			{
  			System.arraycopy(unstained.Y[j], 0, Y[j], 0, unstained.Nevents);
  			System.arraycopy(this.Y[j], 0, Y[j], unstained.Nevents, this.Nevents);
			}

  		this.Nevents = Nevents;
  		this.X = X;
  		this.Y = Y;
  	}
  }

  protected void analyze ()
  {
    if (unstained != null && getContentType()!=TubeContents.BEADS_1)
      kdtree = unstained.kdtree;

    super.analyze();

    censorAutofluorescence(unstained);
    int ndetectors = comp.getDetectorLength();
    slope = new double[ndetectors];
    slopeSigma = new double[ndetectors];
    intercept = new double[ndetectors];
    interceptSigma = new double[ndetectors];
    covariance = new double[ndetectors];
    varianceSlope = new double[ndetectors];
    varianceIntercept = new double[ndetectors];
    varianceCoefficient = new double[ndetectors][3];
    spilloverNotSignificant = new boolean[ndetectors];
    goodnessOfFit = new double[ndetectors];
    goodnessOfVariance = new double[ndetectors];
    for (int j = 0; j < ndetectors; ++j)
    {
      varianceSlope[j] = 1;
      varianceIntercept[j] = 0.01 * comp.fluorescenceRange[primary];

      varianceCoefficient[j][0] = varianceIntercept[j];
      varianceCoefficient[j][1] = varianceSlope[j];
      varianceCoefficient[j][2] = 0;
    }
    double mean[] = new double[ndetectors];
    double cov[][] = new double[ndetectors][ndetectors];
    double residual[][] = new double[ndetectors][Nevents];
    double distance[] = new double[Nevents];
    int[] outlier = null;
    if (Compensation2.RECORD_GATES)
      outlier = addIntegerAnalysis("TRIM");

    int Noriginal = exclude.cardinality();
    for (int pass = 1; pass < BURNED_IN; ++pass)
    {
      int Nincluded = Nevents - exclude.cardinality();
      if (Nincluded < 3)
        break;
      fitRobustLine(residual, pass);
    }

    double tol = .0001;
    for (int pass = BURNED_IN; pass < ROBUST_PASS; ++pass)
    {
      int Nincluded = Nevents - exclude.cardinality();
      if (Nincluded < 3)
        break;

      fitRobustLine(residual, pass);
      computeRobustStatistics(mean, cov, residual);
      Tools.invert(cov);
      censorSpectrum(mean, cov, residual, outlier, pass, tol, distance);
      tol *= TOL_STEP;
    }

    for (int pass = ROBUST_PASS; pass < FINAL_PASS; ++pass)
    {
      int Nincluded = Nevents - exclude.cardinality();
      if (Nincluded < 3)
        break;

      fitLeastSquaresLine(residual, pass);
      computeOrdinaryStatistics(mean, cov, residual);
      Tools.invert(cov);
      censorSpectrum(mean, cov, residual, outlier, pass, tol, distance);
      tol *= TOL_STEP;
    }
    NspectrumGated = exclude.cardinality() - Noriginal;
    if (Compensation2.DEBUG)
      System.out.println((100.0 * NspectrumGated / (Nevents))
        + "% excluded for wrong spectrum");
    Nfinal = Nevents - exclude.cardinality();
    if (Compensation2.DEBUG && Nfinal < 100)
      System.out.println("Few events survived pruning");

    fitLeastSquaresLine(residual, FINAL_PASS);

    // Test the linearity of the data
    RunTest runs = Tools.getRunTest();
    linearity = new boolean[ndetectors];
    for (int j = 0; j < ndetectors; ++j)
      if (j == primary)
        linearity[j] = true;
      else if (spilloverNotSignificant[j])
      	continue;
      else if (goodnessOfVariance[j] >= MINIMUM_GOODNESS && goodnessOfFit[j] >= MINIMUM_GOODNESS)
      {
        runs.reset();
        for (int k = 0; k < Nevents; ++k)
          if (!censored(k))
            runs.add(X[primary][k], X[j][k]
              - (slope[j] * X[primary][k] + intercept[j]));
        linearity[j] = Math.abs(runs.standardError()) < 3;
        if (!linearity[j])
        	addDiagnostic(.5, j, "Run test for {0} vs {1} linearity failed {2,number}", comp.getDetectorList()[j], comp.getDetectorList()[primary], runs.standardError());
        if (intercept[j] < 0 && -intercept[j] > 3 * interceptSigma[j])
        	addDiagnostic(.25, j, "{0} spillover from {1} intercept is significantly negative {2,number}.", comp.getDetectorList()[j], comp.getDetectorList()[primary], intercept[j]);
      }
    
		if (diagnostics != null)
			for (int j = 0; j < ndetectors; ++j)
				if (diagnostics[j] != null)
					Collections.sort(diagnostics[j]);

    if (Compensation2.DEBUG)
      System.out.println();
  }

  private void fitRobustLine (double[][] residual, int pass)
  {
    RobustLine2 robustLine = Tools.getRobustLine();
    RobustHeteroskedastic heterobust = Tools.getRobustHeteroskedastic();
    String[] detectorList = comp.getDetectorList();
    for (int j = 0; j < detectorList.length; ++j)
    {
      if (j == primary)
        continue;
      else
      {
        robustLine.reset();
        for (int k = 0; k < Nevents; ++k)
          if (!censored(k))
          {
            assert X[primary][k] >= 0 : "Primary channel signal is negative";
            double variance = PolynomialEstimator.evaluate(
              varianceCoefficient[j], X[primary][k]);
            assert variance > 0 : "Variance is not positive";
            double weight = 1 / Math.sqrt(variance);
            robustLine.data(X[primary][k], X[j][k], weight);
          }
        robustLine.fit();
        slope[j] = robustLine.getSlope();
        intercept[j] = robustLine.getIntercept();

        heterobust.reset();
        float[] results;
        if (Compensation2.RECORD_RESIDUALS)
          results = addFloatAnalysis(detectorList[j] + "-MADR" + pass);
        float[] fit;
        if (Compensation2.RECORD_RESIDUALS)
          fit = addFloatAnalysis(detectorList[j] + "-MADFIT" + pass);
        for (int k = 0; k < Nevents; ++k)
        {
          double estimate = slope[j] * X[primary][k] + intercept[j];
          double variance = PolynomialEstimator.evaluate(
            varianceCoefficient[j], X[primary][k]);
          if (X[primary][k] < 0)
            variance = varianceCoefficient[j][0];
          assert variance > 0 : "Variance is not positive";
          double error = X[j][k] - estimate;
          double weight = 1 / Math.sqrt(variance);
          assert (!Double.isNaN(weight));
          residual[j][k] = error * weight;
          if (Compensation2.RECORD_RESIDUALS)
            fit[k] = (float)estimate;
          if (Compensation2.RECORD_RESIDUALS)
            results[k] = (float)(residual[j][k]);
          if (!censored(k))
            heterobust.data(X[primary][k], error, weight);
        }
        heterobust.aggregate();
        
				if (pass < BURNED_IN)
				{
					heterobust.fit(3);
					if (heterobust.coefficient(2) < heterobust.standardError(2)
							|| heterobust.coefficient(1) < 0
							|| heterobust.coefficient(0) < 0)
						heterobust.fit(2);
					if (heterobust.coefficient(1) < 0 || heterobust.coefficient(0) < 0)
						heterobust.fit(1);
					heterobust.coefficients(varianceCoefficient[j]);
				}
				else
				{
					heterobust.fit(2);
					heterobust.coefficients(varianceCoefficient[j]);
					if (heterobust.coefficient(1) < heterobust.standardError(1)
							 || heterobust.coefficient(0) < 0)
					{
						heterobust.fit(1);
						heterobust.coefficients(varianceCoefficient[j]);
					}
					else
					{
						heterobust.fit(3);
						if (heterobust.coefficient(2) > 2 * heterobust.standardError(2)
								&& heterobust.coefficient(1) > 2 * heterobust.standardError(1)
								&& heterobust.coefficient(0) >= 0)
							heterobust.coefficients(varianceCoefficient[j]);
					}
				}
      }
    }
  }

  private void fitLeastSquaresLine (double residual[][], int pass)
  {
    LeastSquaresLine leastSquares = Tools.getLeastSquaresLine();
    Heteroskedastic heteroskedastic = Tools.getHeteroskedastic();
    String[]detectorList = comp.getDetectorList();
    for (int j = 0; j < detectorList.length; ++j)
    {
      if (j == primary)
        continue;
      else
      {
        leastSquares.reset();
        for (int k = 0; k < Nevents; ++k)
          if (!censored(k))
          {
            double variance = PolynomialEstimator.evaluate(
              varianceCoefficient[j], X[primary][k]);
            assert variance > 0 : "Variance is not positive";
            double weight = 1 / Math.sqrt(variance);
            leastSquares.data(X[primary][k], X[j][k], weight);
          }
        leastSquares.fit();
        slope[j] = leastSquares.getSlope();
        intercept[j] = leastSquares.getIntercept();
        slopeSigma[j] = leastSquares.getSlopeSigma();
        interceptSigma[j] = leastSquares.getInterceptSigma();
        covariance[j] = leastSquares.getCovariance();
				if (pass == FINAL_PASS)
				{
					goodnessOfFit[j] = leastSquares.goodnessOfFit();
					if (goodnessOfFit[j] < MINIMUM_GOODNESS)
						addDiagnostic(.75, j,
								"Fit of spillover line from {0} to {1} is poor {2,number}%.", 
								comp.reagent[reagent], detectorList[j], 100 * goodnessOfFit[j]);
				}

        heteroskedastic.reset();
        float[] results;
        if (Compensation2.RECORD_RESIDUALS)
          results = addFloatAnalysis(detectorList[j] + "-LSR" + pass);
        float[] fit;
        if (Compensation2.RECORD_FIT)
          fit = addFloatAnalysis(detectorList[j] + "-LSFIT" + pass);
        for (int k = 0; k < Nevents; ++k)
        {
          double estimate = slope[j] * X[primary][k] + intercept[j];
          double variance = PolynomialEstimator.evaluate(
            varianceCoefficient[j], X[primary][k]);
          if (X[primary][k] < 0)
            variance = varianceCoefficient[j][0];
          assert variance > 0 : "Variance is not positive";
          double error = X[j][k] - estimate;
          double weight = 1 / Math.sqrt(variance);
          residual[j][k] = error * weight;
          if (Compensation2.RECORD_FIT)
            fit[k] = (float)estimate;
          if (Compensation2.RECORD_RESIDUALS)
            results[k] = (float)(residual[j][k]);
          if (!censored(k))
            heteroskedastic.data(X[primary][k], error, weight);
        }
        heteroskedastic.aggregate();

        heteroskedastic.fit(2);
				heteroskedastic.coefficients(varianceCoefficient[j]);
				if (heteroskedastic.coefficient(1) < heteroskedastic.standardError(1)
						|| heteroskedastic.coefficient(0) < 0)
				{
					heteroskedastic.fit(1);
					heteroskedastic.coefficients(varianceCoefficient[j]);
          if (pass == FINAL_PASS)
          	spilloverNotSignificant[j] = true;
				}
				else
				{
					heteroskedastic.fit(3);
					if (heteroskedastic.coefficient(2) > 2 * heteroskedastic.standardError(2)
							&& heteroskedastic.coefficient(1) > 2 * heteroskedastic.standardError(1)
							&& heteroskedastic.coefficient(0) >= 0)
					{
						heteroskedastic.coefficients(varianceCoefficient[j]);
						if (pass == FINAL_PASS)
						{
							goodnessOfVariance[j] = heteroskedastic.goodnessOfFit();
							if (goodnessOfVariance[j] < MINIMUM_GOODNESS)
								addDiagnostic(.75, j,
										"Parabolic fit of {0} variance by {1} signal level is poor {2,number}%.",
										detectorList[j], comp.reagent[reagent], 100 * goodnessOfVariance[j]);
							else if (heteroskedastic.coefficient(2) > .00001)
								addDiagnostic(.95, j,
										"Large instrument errors {0,number}% detected between {1} and {2}",
										100 * Math.sqrt(heteroskedastic.coefficient(2)),
										detectorList[primary], detectorList[j]);
						}
					}
					else if (pass == FINAL_PASS)
					{
						goodnessOfVariance[j] = heteroskedastic.goodnessOfFit();
						if (goodnessOfVariance[j] < MINIMUM_GOODNESS)
							addDiagnostic(.75, j,
									"Linear fit of {0} variance by {1} signal level is poor {2,number}%.",
									detectorList[j], comp.reagent[reagent], 100 * goodnessOfVariance[j]);
					}
				}
      }
    }
  }

  private void censorSpectrum (double[] mean, double[][] cov,
    double[][] residual, int[] outlier, int pass, double tol, double[] distance)
  {
    int Noriginal = exclude.cardinality();

    Arrays.fill(distance, 0);
    int ndetectors = comp.getDetectorLength();
    for (int jx = 0; jx < ndetectors; jx++)
    {
      if (jx == primary)
        continue;
      for (int jy = 0; jy < ndetectors && jy <= jx; jy++)
      {
        if (jy == primary)
          continue;
        for (int k = 0; k < Nevents; ++k)
          if (!censored(k))
            distance[k] += cov[jx][jy] * (residual[jx][k] - mean[jx])
              * (residual[jy][k] - mean[jy]);
      }
    }

    double below = 0;
    double above = Double.MAX_VALUE;
    for (int k = 0; k < Nevents; ++k)
    {
      if (censored(k))
        continue;
      double d = distance[k];
      if (d < below)
        continue;
      if (d < above)
      {
        double P = Tools.Q((double)(ndetectors - 1) / 2, d / 2);
        if (P > tol)
        {
          below = d;
          continue;
        }
        else
          above = d;
      }
      censor(k, Gate.OUTLIER);
      if (Compensation2.RECORD_GATES)
        outlier[k] = pass;
    }
    if (Compensation2.DEBUG)
      System.out.println(exclude.cardinality() - Noriginal
        + " rejected at pass " + pass);
  }

  private void computeRobustStatistics (double[] mean, double[][] cov,
    double[][] residual)
  {
    Selector selector = Tools.getSelector();
    int ndetectors = comp.getDetectorLength();
    for (int j = 0; j < ndetectors; ++j)
    {
      if (j == primary)
        continue;
      selector.reset();
      for (int k = 0; k < Nevents; ++k)
        if (!censored(k))
          selector.add(residual[j][k]);
      mean[j] = selector.median();
    }
    for (int jx = 0; jx < ndetectors; jx++)
    {
      for (int jy = 0; jy < ndetectors && jy <= jx; jy++)
      {
        if (jx == primary)
        {
          if (jy == primary)
            cov[primary][primary] = 1;
          else
            cov[primary][jy] = 0;
          continue;
        }
        if (jy == primary)
        {
          cov[jx][primary] = 0;
          continue;
        }

        selector.reset();
        for (int k = 0; k < Nevents; ++k)
          if (!censored(k))
            selector.add((residual[jx][k] - mean[jx])
              * (residual[jy][k] - mean[jy]));
        cov[jx][jy] = selector.median() / Tools.MEDIAN_CHI_SQUARED_1;
      }
    }
    for (int jx = 0; jx < ndetectors; jx++)
    {
      for (int jy = 0; jy < ndetectors && jy < jx; jy++)
        cov[jy][jx] = cov[jx][jy];
      if (Compensation2.DEBUG)
      {
        System.out.print(cov[jx][jx]);
        System.out.print(" ");
      }
    }
    if (Compensation2.DEBUG)
      System.out.println();
  }

  private void computeOrdinaryStatistics (double[] mean, double[][] cov,
    double[][] residual)
  {
    int Nincluded = Nevents - exclude.cardinality();
    int ndetectors = comp.getDetectorLength();
    for (int j = 0; j < ndetectors; ++j)
    {
      if (j == primary)
        continue;
      mean[j] = 0;
      for (int k = 0; k < Nevents; ++k)
        if (!censored(k))
          mean[j] += residual[j][k];
      mean[j] /= Nincluded;
    }

    for (int jx = 0; jx < ndetectors; jx++)
    {
      for (int jy = 0; jy < ndetectors && jy <= jx; jy++)
      {
        if (jx == primary)
        {
          if (jy == primary)
            cov[primary][primary] = 1;
          else
            cov[primary][jy] = 0;
          continue;
        }
        if (jy == primary)
        {
          cov[jx][primary] = 0;
          continue;
        }

        cov[jx][jy] = 0;
        for (int k = 0; k < Nevents; ++k)
          if (!censored(k))
            cov[jx][jy] += (residual[jx][k] - mean[jx])
              * (residual[jy][k] - mean[jy]);
        cov[jx][jy] /= Nincluded;
      }
    }

    for (int jx = 0; jx < ndetectors; jx++)
    {
      for (int jy = 0; jy < ndetectors && jy < jx; jy++)
        cov[jy][jx] = cov[jx][jy];
      if (Compensation2.DEBUG)
      {
        System.out.print(cov[jx][jx]);
        System.out.print(" ");
      }
    }
    if (Compensation2.DEBUG)
      System.out.println();
  }

  private void censorAutofluorescence (UnstainedControl unstained)
  {
    int Noriginal = exclude.cardinality();
    float low = 0;
    if (unstained != null && getContentType() == TubeContents.CELLS)
    {
      if (unstained.V[primary] > 0)
        low = (float)(unstained.A[primary] + 2 * Math
          .sqrt(unstained.V[primary]));
      else
        low = (float)unstained.A[primary];
    }

    // only use values that are greater than zero on primary channel
    if (low < 0)
      low = 0;
    for (int k = 0; k < Nevents; ++k)
      if (!censored(k) && X[primary][k] <= low)
        censor(k, Gate.AUTOFLUORESCENCE);
    Nautofluorescence = exclude.cardinality() - Noriginal;
    if (Compensation2.DEBUG)
      System.out.println(100.0 * Nautofluorescence / Nevents
        + "% excluded as autofluorescence");
  }

  public boolean isSpillOverNotSignificant (int detectorIndex){
      boolean flag = false;
      if (spilloverNotSignificant != null && detectorIndex < spilloverNotSignificant.length)
          flag = spilloverNotSignificant[detectorIndex];
      return flag;
  }
}
