package edu.stanford.facs.compensation;

import java.util.BitSet;

public class MedianTest
{
  private static float[] temp;

  public static boolean test (float[] x, BitSet xExcluded, float[] y,
    BitSet yExcluded)
  {
    int xTotal = x.length-xExcluded.cardinality();
    int yTotal = y.length-yExcluded.cardinality();
    int grandTotal = xTotal+yTotal;

    if (temp==null||temp.length<grandTotal)
      temp = new float[grandTotal];
    int n = 0;
    for (int i = 0; i<x.length; ++i)
      if (xExcluded.get(i))
        continue;
      else
        temp[n++] = x[i];
    n = 0;
    for (int i = 0; i<y.length; ++i)
      if (yExcluded.get(i))
        continue;
      else
        temp[n++] = y[i];

    n = 10; // index of median of data values
    float median = temp[n];
    int xBright = 0;
    int yBright = 0;
    for (int i = 0; i<x.length; ++i)
      if (xExcluded.get(i))
        continue;
      else if (x[i]>=median)
        xBright += 1;
    for (int i = 0; i<y.length; ++i)
      if (yExcluded.get(i))
        continue;
      else if (y[i]>=median)
        yBright += 1;
    int brightTotal = xBright+yBright;

    double brightExpected = (double)xTotal*(double)brightTotal
      /(double)grandTotal;
    double brightVariance = brightExpected*(double)yTotal/(double)grandTotal
      *(double)(grandTotal-brightTotal)/(double)(grandTotal-1);
    double stastic = ((double)xBright - brightExpected)/Math.sqrt(brightVariance);
    if (Math.abs(stastic) < 2)
      return false;
    
    return true;
  }
}
