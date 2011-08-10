package edu.stanford.facs.logicle;

/**
 * A fast implementation of the Logicle data scale and its inverse. This version
 * is substantially faster than the more general implementation but will throw a
 * LogicleArgumentException if the arguments are outside of the normal scale
 * range 0 <= scale < 1.0. It should provide suitable speed and resolution for
 * real time graphical applications.
 * 
 * @author Wayne A. Moore
 * @version 1.0
 */
public class FastLogicle
  extends Logicle
{
  /**
   * The default number of entries in the interpolation lookup table
   */
  public static final int DEFAULT_BINS = 1 << 12;

  /**
   * The number of bins for this Logicle scale
   */
  public final int bins;

  /**
   * A lookup table of data values
   */
  protected final double[] lookup;

  /**
   * Constructs a <code>FastLogicle</code> object with all parameters
   * 
   * @param T
   *          the double maximum data value or "top of scale"
   * @param W
   *          the double number of decades to linearize
   * @param M
   *          the double number of decades that a pure log scale would cover
   * @param A
   *          the double additional number of negative decades to include on
   *          scale
   * @param bins
   *          number of data values in the lookup table
   */
  public FastLogicle (double T, double W, double M, double A, int bins)
  {
    super(T, W, M, A, bins);

    this.bins = bins;
    lookup = new double[bins + 1];
    for (int i = 0; i <= bins; ++i)
      lookup[i] = super.inverse((double)i / (double)bins);
  }

  /**
   * Constructs a <code>FastLogicle</code> object no additional negative decades
   * 
   * @param T
   *          the double maximum data value or "top of scale"
   * @param W
   *          the double number of decades to linearize
   * @param M
   *          the double number of decades that a pure log scale would cover
   * @param bins
   *          number of data values in the lookup table
   */
  public FastLogicle (double T, double W, double M, int bins)
  {
    this(T, W, M, 0, bins);
  }

  /**
   * Constructs a <code>FastLogicle</code> object with the default number of
   * decades and no additional negative decades
   * 
   * @param T
   *          the double maximum data value or "top of scale"
   * @param W
   *          the double number of decades to linearize
   * @param bins
   *          number of data values in the lookup table
   */
  public FastLogicle (double T, double W, int bins)
  {
    this(T, W, DEFAULT_DECADES, 0, bins);
  }

  /**
   * Constructs a <code>FastLogicle</code> object with the default number of
   * bins
   * 
   * @param T
   *          the double maximum data value or "top of scale"
   * @param W
   *          the double number of decades to linearize
   * @param M
   *          the double number of decades that a pure log scale would cover
   * @param A
   *          the double additional number of negative decades to include on
   *          scale
   */
  public FastLogicle (double T, double W, double M, double A)
  {
    this(T, W, M, A, DEFAULT_BINS);
  }

  /**
   * Constructs a <code>FastLogicle</code> object with the default number of
   * bins and no additional negative decades
   * 
   * @param T
   *          the double maximum data value or "top of scale"
   * @param W
   *          the double number of decades to linearize
   * @param M
   *          the double number of decades that a pure log scale would cover
   */
  public FastLogicle (double T, double W, double M)
  {
    this(T, W, M, 0, DEFAULT_BINS);
  }

  /**
   * Constructs a <code>FastLogicle</code> object with the default number of
   * bins, the default number of decades and no additional negative decades
   * 
   * @param T
   *          the double maximum data value or "top of scale"
   * @param W
   *          the double number of decades to linearize
   */
  public FastLogicle (double T, double W)
  {
    this(T, W, DEFAULT_DECADES, 0, DEFAULT_BINS);
  }

  /**
   * Looks up a data value in the internal table. Provides a fast method of
   * binning data on the Logicle scale
   * 
   * @param value
   *          a double data value
   * @return the bin for that data value
   * @throws LogicleArgumentException
   *           if the data is out of range
   */
  public int intScale (double value)
  {
    // binary search for the appropriate bin
    int lo = 0;
    int hi = bins;
    while (lo <= hi)
    {
      int mid = (lo + hi) >> 1;
      double key = lookup[mid];
      if (value < key)
        hi = mid - 1;
      else if (value > key)
        lo = mid + 1;
      else if (mid < bins)
        return mid;
      else
        // equal to table[bins] which is for interpolation only
        throw new LogicleArgumentException(value);
    }

    // check for out of range
    if (hi < 0 || lo > bins)
      throw new LogicleArgumentException(value);

    return lo - 1;
  }

  /**
   * Returns the minimum data value for the specified bin
   * 
   * @param index
   *          a bin number
   * @return the double data value
   * @throws LogicalArgumentException
   *           if the index is out of range
   */
  public double inverse (int index)
  {
    if (index < 0 || index >= bins)
      throw new LogicleArgumentException(index);

    return lookup[index];
  }

  /**
   * Computes an approximation of the Logicle scale of the given data value.
   * This method looks up the data in the table then performs an inverse linear
   * interpolation.
   * 
   * @throws LogicleArgumentException
   *           if the data value would be off scale
   * @see edu.stanford.facs.logicle.Logicle#scale(double)
   */
  @Override
  public double scale (double value)
  {
    // lookup the nearest value
    int index = intScale(value);

    // inverse interpolate the table linearly
    double delta = (value - lookup[index])
      / (lookup[index + 1] - lookup[index]);

    return (index + delta) / (double)bins;
  }

  /**
   * Computes the approximate data value corresponding to a scale value. This
   * method uses linear interpolation between tabulated data values.
   * 
   * @throws LogicleArgumentException
   *           if the scale is out of range
   * @see Logicle#inverse(double)
   */
  @Override
  public double inverse (double scale)
  {
    // find the bin
    double x = scale * bins;
    int index = (int)Math.floor(x);
    if (index < 0 || index >= bins)
      throw new LogicleArgumentException(scale);

    // interpolate the table linearly
    double delta = x - index;

    return (1 - delta) * lookup[index] + delta * lookup[index + 1];
  }
}
