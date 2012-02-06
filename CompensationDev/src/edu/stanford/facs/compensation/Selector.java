package edu.stanford.facs.compensation;

public class Selector
{
  private double[] data = new double[1000];
  private int count;

  public void reset ()
  {
    count = 0;
  }

  public void add (double x)
  {
    if (Double.isNaN(x))
      throw new IllegalArgumentException();
    if (count == data.length)
    {
      double[] temp = new double[data.length * 2];
      System.arraycopy(data, 0, temp, 0, count);
      data = temp;
    }
    data[count++] = x;
  }

  private void swap (int i, int j)
  {
    double t = data[i];
    data[i] = data[j];
    data[j] = t;
  }

  public double select (int rank)
  {
    int l = 0, r = count - 1;
    for (;;)
    {
      if (r <= l + 1)
      {
        if (r == l + 1 && data[r] < data[l])
          swap(l, r);
        return data[rank];
      }
      else
      {
        int mid = (l + r) / 2;
        swap(mid, l + 1);
        if (data[l] > data[r])
          swap(l, r);
        if (data[l + 1] > data[r])
          swap(l + 1, r);
        if (data[l] > data[l + 1])
          swap(l, l + 1);

        int i = l + 1;
        int j = r;
        double split = data[i];
        for (;;)
        {
          do
          {
            ++i;
          } while (data[i] < split);
          do
          {
            --j;
          } while (data[j] > split);
          if (j < i)
            break;
          swap(i, j);
        }
        data[l + 1] = data[j];
        data[j] = split;
        
        if (j >= rank)
          r = j - 1;
        if (j <= rank)
          l = i;
      }
    }
  }
  
  public double median ()
  {
    return select(count / 2);
  }
}
