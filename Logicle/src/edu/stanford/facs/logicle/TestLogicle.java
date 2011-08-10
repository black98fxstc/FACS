/**
 * 
 */
package edu.stanford.facs.logicle;

import static org.junit.Assert.fail;

import java.util.Random;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the Logicle and FastLogicle classes.
 * 
 * @author Wayne A. Moore
 * @version 1.0
 */
public class TestLogicle
{
  final static int NUMBER_OF_VALUES = 1000000;

  private Random random;

  abstract class Distribution
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

  /**
   * @throws java.lang.Exception
   */
  @BeforeClass
  public static void setUpBeforeClass ()
    throws Exception
  {
  }

  /**
   * @throws java.lang.Exception
   */
  @AfterClass
  public static void tearDownAfterClass ()
    throws Exception
  {
  }

  /**
   * Setup random number generators
   * @throws java.lang.Exception
   */
  @Before
  public void setUp ()
    throws Exception
  {
    // use a random number generator with a fixed seed for reproducibility
    random = new Random(31415926);
  }

  /**
   * Tear down random number generators
   * @throws java.lang.Exception
   */
  @After
  public void tearDown ()
    throws Exception
  {
    random = null;
  }

	/**
	 * Test whether the specified Logicle scale is invertible to the stated precision
	 * using pseudorandom data from the supplied distribution.
	 * 
	 * @param logicle
	 * @param distribution
	 * @param precision
	 */
	public void testLogicleScale (Logicle logicle, Distribution distribution,
			double precision)
	{
		for (int i = 0; i < NUMBER_OF_VALUES; ++i)
		{
			double trueScale = distribution.sample();
			double dataValue = logicle.inverse(trueScale);
			double testScale = logicle.scale(dataValue);
			if (Math.abs(trueScale - testScale) > precision)
				fail("scale invertability test failed");
		}
	}

  /**
   * Test whether the inverse of the specified Logicle scale is accurate to
   * the stated tolerance using pseudorandom data from the supplied distribution.
   * Precision is calculated as the tolerance times the slope of the biexponential 
   * at a given point on the scale.
   * 
   * @param logicle
   * @param distribution
   * @param tolerance
   */
  public void testLogicleInverse (Logicle logicle, Distribution distribution,
    double tolerance)
  {
    for (int i = 0; i < NUMBER_OF_VALUES; ++i)
    {
      double trueData = distribution.sample();
      double scale = logicle.scale(trueData);
      double testData = logicle.inverse(scale);
      // data can't be better than slope times tolerance of argument
      if (Math.abs(trueData - testData) > tolerance * logicle.slope(scale))
        fail("inverse data test failed");
    }
  }
  
  /**
   * Test whether two arrays are equal to single precision accuracy.
   * Used to test equivalence of sets of scale coordinates returned by
   * {@link edu.stanford.facs.logicle.Logicle#axisLabels()}.
   * @param actual coordinate values returned
   * @param expected coordinate values
   */
  public void testLabels (double[] actual, double expected[])
  {
  	if (actual.length != expected.length)
  		fail("number of labels differs");
  	for (int i = 0, n = expected.length; i < n; ++i)
  	{
  		double delta = actual[i] - expected[i];
  		if (expected[i] != 0)
  			delta /= expected[i];
  		if (Math.abs(delta) >= Math.ulp(1F))
  			fail("label values differ");
  	}
  }

  /**
   * Test method for {@link edu.stanford.facs.logicle.Logicle#solve(double,double)}.
   * Test whether the solution found actually solves the problem.
   */
  @Test
  public void testSolve ()
    throws Exception
  {
    // use standard 4.5 decade logicle scale
    double b = 4.5 * Logicle.LN_10;
    // since w <= .5 this is precision of about 2ulp
    // seems to be the highest precision that works
    // probably because of the constant 2 in the formula
    double tolerance = Math.ulp(1D);

    for (int i = 0; i < NUMBER_OF_VALUES; ++i)
    {
      double true_w = .5 * random.nextDouble();
      double d = Logicle.solve(b, true_w);
      double test_w = -2 * (Math.log(d) - Math.log(b)) / (b + d);
      if (Math.abs(true_w - test_w) > tolerance)
        fail("solve for d failed");
    }
  }

  /**
   * Test method for {@link edu.stanford.facs.logicle.Logicle#scale(double)}.
   * Test that the scale function really is the inverse of the biexponential
   */
  @Test
  public void testLogicleScale ()
  {
    Logicle logicle;

    logicle = new Logicle(10000, 1);

    // normal scale range
    Distribution normal = new Uniform(0, 1);
    testLogicleScale(logicle, normal, Math.ulp(1D));

    // extended scale range
    Distribution extended = new Uniform(-1, 4);
    testLogicleScale(logicle, extended, 2 * Math.ulp(1D));
    
    // from Josef Spidlen
    logicle = new Logicle(1000, 2, 4);
    testLogicleScale(logicle, normal, 2 * Math.ulp(1D));
    testLogicleScale(logicle, extended, 2 * Math.ulp(1D));
  }

  /**
   * Test method for {@link edu.stanford.facs.logicle.Logicle#inverse(double)}.
   * Check that the inverse function really is the inverse of the scale function
   */
  @Test
  public void testLogicleInverse ()
  {
    Logicle logicle;
    Distribution distribution;

    // worst case for near zero behavior
    logicle = new Logicle(10000, 2.5, 5.0);
    // normally distributed data
    distribution = new Normal(0, 5);
    testLogicleInverse(logicle, distribution, Math.ulp(1D));

    // typical scale
    logicle = new Logicle(10000, 1);
    // normally distributed data
    testLogicleInverse(logicle, distribution, Math.ulp(1D));
    // log normally distributed data
    distribution = new LogNormal(logicle.b / 2, logicle.b / 6);
    testLogicleInverse(logicle, distribution, 2 * Math.ulp(1D) );
    
    // from Josef Spidlen
    logicle = new Logicle(1000, 2, 4);
    distribution = new Normal(0, 1);
    testLogicleInverse(logicle, distribution, Math.ulp(1D));
    distribution = new LogNormal(logicle.b / 2, logicle.b / 6);
    testLogicleInverse(logicle, distribution, 3 * Math.ulp(1D) );
  }
  
	/**
	 * Test method for {@link edu.stanford.facs.logicle.Logicle#axisLabels()}.
	 * Test whether the function returns the expected results in various circumstances.
	 */
	@Test
	public void testAxisLabels ()
	{
		Logicle logicle;

		// typical scale
		logicle = new Logicle(10000, 1);
		double[] typical = { 0, 100, 1000, 10000 };
		testLabels(logicle.axisLabels(), typical);

		// typical Diva
		logicle = new Logicle(300000, .5);
		double[] typical_diva = { -100, 0, 100, 1000, 10000, 100000 };
		testLabels(logicle.axisLabels(), typical_diva);

		// typical EDesk
		logicle = new Logicle(1000, .5);
		double[] typical_desk = { 0, 1, 10, 100 };
		testLabels(logicle.axisLabels(), typical_desk);

		// unit full scale
		logicle = new Logicle(1, .5);
		double[] unit_full_scale = { 0, .001, .01, .1, 1 };
		testLabels(logicle.axisLabels(), unit_full_scale);

		// arcsinh scale
		logicle = new Logicle(10000, 0, Logicle.DEFAULT_DECADES, .5);
		double[] arcsinh = { 0, 1, 10, 100, 1000, 10000 };
		testLabels(logicle.axisLabels(), arcsinh);

		// no negative region
		logicle = new Logicle(10000, 1, Logicle.DEFAULT_DECADES, -1);
		double[] no_negative = { 0, 100, 1000, 10000 };
		testLabels(logicle.axisLabels(), no_negative);

		// worst case
		logicle = new Logicle(10000, 2.5, 5.0);
		double[] worst_case = { -10000, 0, 10000 };
		testLabels(logicle.axisLabels(), worst_case);
	}

  /**
   * Test method for {@link edu.stanford.facs.logicle.FastLogicle#intScale(double)}.
   * Test that the scale function really is the inverse of the biexponential
   */
  @Test
  public void testIntScale ()
    throws Exception
  {
    FastLogicle fast = new FastLogicle(10000, 1);
    for (int i = 0; i < NUMBER_OF_VALUES; ++i)
    {
      double trueScale = random.nextDouble();
      double dataValue = fast.inverse(trueScale);
      int trueInt = (int)Math.floor(fast.bins * trueScale);
      int testInt = fast.intScale(dataValue);
      if (trueInt != testInt)
        fail("fast int scale test failed");
    }
  }
  
  /**
   * Test method for {@link edu.stanford.facs.logicle.FastLogicle#scale(double)}.
   * Test that the scale function really is the inverse of the biexponential
   */
  @Test
  public void testFastScale ()
  {
    FastLogicle logicle;
    Distribution distribution;

    logicle = new FastLogicle(10000, 1);

    // normal scale range, default bins
    distribution = new Uniform(0, 1);
    testLogicleScale(logicle, distribution, 1D/logicle.bins);
    
    // normal range, decimal scale
    logicle = new FastLogicle(10000, 1, 1000);
    testLogicleScale(logicle, distribution, 1D/logicle.bins);
  }
  
  /**
   * Test method for {@link edu.stanford.facs.logicle.FastLogicle#scale(double)}.
   * Test that the scale function really is the inverse of the biexponential
   */
  @Test(expected=LogicleArgumentException.class)
  public void testFastScaleLowerBound ()
  {
    FastLogicle logicle;

    logicle = new FastLogicle(10000, 1);
    
    double bound = logicle.lookup[0];
    bound -= Math.ulp(bound);
    
    logicle.scale(bound);
  }
  
  /**
   * Test method for {@link edu.stanford.facs.logicle.FastLogicle#scale(double)}.
   * Test that the scale function really is the inverse of the biexponential
   */
  @Test(expected=LogicleArgumentException.class)
  public void testFastScaleUpperBoundEqual ()
  {
    FastLogicle logicle;

    logicle = new FastLogicle(10000, 1);
    
    double bound = logicle.lookup[logicle.bins];
    
    logicle.scale(bound);
  }
  
  /**
   * Test method for {@link edu.stanford.facs.logicle.FastLogicle#scale(double)}.
   * Test that the scale function really is the inverse of the biexponential
   */
  @Test(expected=LogicleArgumentException.class)
  public void testFastScaleUpperBoundGreater ()
  {
    FastLogicle logicle;

    logicle = new FastLogicle(10000, 1);
    
    double bound = logicle.lookup[logicle.bins];
    bound += Math.ulp(bound);
    
    logicle.scale(bound);
  }
}