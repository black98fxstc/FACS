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
   * @throws java.lang.Exception
   */
  @After
  public void tearDown ()
    throws Exception
  {
    random = null;
  }

  /**
   * Test that the specified Logicle scale is invertible to the stated precision
   * over the stated range
   * 
   * @param logicle
   * @param min
   * @param max
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
   * Test that the specified Logicle scale is invertible to the stated precision
   * over the stated range
   * 
   * @param logicle
   * @param min
   * @param max
   */
  public void testLogicleScaleToReference (Logicle logicle, Logicle reference,
    Distribution distribution, double precision, boolean censor)
  {
    double minimum = logicle.scale(0);
    double maximum = logicle.scale(1);
    
    for (int i = 0; i < NUMBER_OF_VALUES; ++i)
    {
      double trueScale = distribution.sample();
      double dataValue = logicle.inverse(trueScale);
      double testScale = logicle.scale(dataValue);
      double referenceScale = reference.scale(dataValue);
      if (Math.abs(referenceScale - testScale) > precision)
        fail("scale invertability test failed");
    }
  }

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

  /*
   * Test that the solution found actually solves the problem
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
   * Test method for {@link edu.stanford.facs.logicle.Logicle#scale(double)}.
   * Test that the scale function really is the inverse of the biexponential
   */
  @Test
  public void testSimpleScale ()
  {
    Logicle logicle;
    Distribution distribution;

    logicle = new SimpleLogicle(10000, 1);

    // normal scale range
    distribution = new Uniform(0, 1);
    testLogicleScale(logicle, distribution, 1.0E-14);

    // extended scale range
    distribution = new Uniform(-1, 4);
    testLogicleScale(logicle, distribution, 2.0E-14);
  }

  /**
   * Test method for {@link edu.stanford.facs.logicle.Logicle#inverse(double)}.
   * Check that the inverse function really is the inverse of the scale function
   */
  @Test
  public void testSimpleInverse ()
  {
    Logicle logicle;
    Distribution distribution;

    // worst case for near zero behavior
    logicle = new SimpleLogicle(10000, 2.5, 5.0);
    // normally distributed data
    distribution = new Normal(0, 5);
    testLogicleInverse(logicle, distribution, 1.0E-14);

    // typical default scale
    logicle = new Logicle(10000, 1);
    // normally distributed data
    testLogicleInverse(logicle, distribution, 1.0E-14);
    // log normally distributed data
    distribution = new LogNormal(logicle.b / 2, logicle.b / 6);
    testLogicleInverse(logicle, distribution, 1.0E-14 );
  }

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
   * Test method for {@link edu.stanford.facs.logicle.Logicle#scale(double)}.
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
   * Test method for {@link edu.stanford.facs.logicle.Logicle#scale(double)}.
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
   * Test method for {@link edu.stanford.facs.logicle.Logicle#scale(double)}.
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
   * Test method for {@link edu.stanford.facs.logicle.Logicle#scale(double)}.
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