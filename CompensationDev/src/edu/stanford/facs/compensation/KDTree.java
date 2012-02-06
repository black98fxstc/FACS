package edu.stanford.facs.compensation;

import java.util.BitSet;
import java.util.Stack;

public class KDTree
{
  public final static int DIM = 2;
  int Npoints;
  int Nboxes;
  int Nnearest = 32;
  int[] index;
  double[] nearest;
  double median;
  float[][] point;
  int[] neighbor = new int[Nnearest];
  double[] distance = new double[Nnearest];
  int[] taskmom = new int[50];
  int[] taskdim = new int[50];
  Selector selector = new Selector();
  BitSet outlier = new BitSet();

  static class Box
  {
    float[] lo, hi;

    Box ()
    {
      lo = new float[DIM];
      hi = new float[DIM];
    }
  }

  static class BoxNode
    extends Box
  {
    BoxNode mom, dau1, dau2;
    int dim, ptlo, pthi;
  }

  BoxNode[] node;
  Stack<BoxNode> toDo = new Stack<BoxNode>();

  public KDTree ()
  {
    // TODO Auto-generated constructor stub
  }

  public KDTree (GatedControl control)
  {
    super();

    init(control.getNevents(), control.exclude, control.Y);
    computeDistances();
    filterPoints(2.5);
  }

  double dist (float[] x, float[] y)
  {
    double sum = 0;
    for (int i = 0; i < DIM; ++i)
    {
      double d = x[i] - y[i];
      sum += d * d;
    }
    return sum;
  }

  double dist (int i, int j)
  {
    double sum = 0;
    for (int k = 0; k < DIM; ++k)
    {
      double d = point[k][i] - point[k][j];
      sum += d * d;
    }
    return sum;
  }

  double dist (int i, float[][] x, int j, float[][] y)
  {
    double sum = 0;
    for (int k = 0; k < DIM; ++k)
    {
      double d = x[k][i] - y[k][j];
      sum += d * d;
    }
    return sum;
  }

  double dist (Box b, float[] x)
  {
    double sum = 0;
    for (int i = 0; i < x.length; ++i)
    {
      double d;
      if (x[i] < b.lo[i])
        d = x[i] - b.lo[i];
      else if (x[i] > b.hi[i])
        d = x[i] - b.hi[i];
      else
        d = 0;
      sum += d * d;
    }
    return sum;
  }

  double dist (Box b, int p, float[][] x)
  {
    double sum = 0;
    for (int i = 0; i < DIM; ++i)
    {
      double d;
      if (x[i][p] < b.lo[i])
        d = x[i][p] - b.lo[i];
      else if (x[i][p] > b.hi[i])
        d = x[i][p] - b.hi[i];
      else
        d = 0;
      sum += d * d;
    }
    return sum;
  }

  double dist (Box b, int p)
  {
    double sum = 0;
    for (int i = 0; i < DIM; ++i)
    {
      double d;
      if (point[i][p] < b.lo[i])
        d = point[i][p] - b.lo[i];
      else if (point[i][p] > b.hi[i])
        d = point[i][p] - b.hi[i];
      else
        d = 0;
      sum += d * d;
    }
    return sum;

  }

  int select (int l, int k, int r, float[] value)
  {
    int t;
    for (;;)
    {
      if (r <= l + 1)
      {
        if (r == l + 1 && value[index[l]] < value[index[r]])
        {
          t = index[l];
          index[l] = index[r];
          index[r] = t;
        }
        return index[k];
      }
      else
      {
        int mid = (l + r) >> 1;
        t = index[mid];
        index[mid] = index[l + 1];
        index[l + 1] = t;
        if (value[index[l]] > value[index[r]])
        {
          t = index[l];
          index[l] = index[r];
          index[r] = t;
        }
        if (value[index[l + 1]] > value[index[r]])
        {
          t = index[l + 1];
          index[l + 1] = index[r];
          index[r] = t;
        }
        if (value[index[l]] > value[index[l + 1]])
        {
          t = index[l];
          index[l] = index[l + 1];
          index[l + 1] = t;
        }
        int i = l + 1;
        int j = r;
        int ind = index[l + 1];
        float v = value[ind];
        for (;;)
        {
          while (value[index[++i]] < v)
            ;
          while (value[index[--j]] > v)
            ;
          if (j < i)
            break;
          t = index[i];
          index[i] = index[j];
          index[j] = t;
        }
        index[l + 1] = index[j];
        index[j] = ind;
        if (j >= k)
          r = j - 1;
        if (j <= k)
          l = i;
      }
    }
  }

  void init (int Nevents, BitSet exclude, float[][] point)
  {
    Npoints = Nevents - exclude.cardinality();
    if (index == null || index.length < Npoints)
      index = new int[Npoints];
    for (int i = 0, j = 0; i < Nevents; ++i)
      if (!exclude.get(i))
        index[j++] = i;
    this.point = point;

    int m = 1;
    for (int ntmp = Npoints; ntmp > 0; ntmp >>= 1)
      m <<= 1;
    Nboxes = 2 * Npoints - (m >> 1);
    if (m < Nboxes)
      Nboxes = m;
    Nboxes--;

    if (node == null)
    {
      node = new BoxNode[Nboxes];
      for (int i = 0; i < Nboxes; ++i)
        node[i] = new BoxNode();
      node[0].lo[0] = -Float.MAX_VALUE;
      node[0].lo[1] = -Float.MAX_VALUE;
      node[0].hi[0] = Float.MAX_VALUE;
      node[0].hi[1] = Float.MAX_VALUE;
    }
    else if (node.length < Nboxes)
    {
      BoxNode[] bna = new BoxNode[Nboxes];
      System.arraycopy(node, 0, bna, 0, node.length);
      for (int i = node.length; i < Nboxes; ++i)
        bna[i] = new BoxNode();
      node = bna;
    }
    node[0].ptlo = 0;
    node[0].pthi = Npoints - 1;

    int jbox = 0;
    toDo.push(node[0]);
    while (!toDo.isEmpty())
    {
      BoxNode mom = toDo.pop();
      int tdim = mom.dim;
      int ptlo = mom.ptlo;
      int pthi = mom.pthi;
      float[] value = point[mom.dim];
      int ptmid = (ptlo + pthi) >> 1;
      select(ptlo, ptmid, pthi, value);
      BoxNode dau1 = node[++jbox];
      dau1.mom = mom;
      dau1.dim = (mom.dim + 1) % DIM;
      System.arraycopy(mom.lo, 0, dau1.lo, 0, DIM);
      System.arraycopy(mom.hi, 0, dau1.hi, 0, DIM);
      dau1.hi[tdim] = value[index[ptmid]];
      dau1.ptlo = ptlo;
      dau1.pthi = ptmid;
      BoxNode dau2 = node[++jbox];
      dau2.mom = mom;
      dau2.dim = (mom.dim + 1) % DIM;
      System.arraycopy(mom.lo, 0, dau2.lo, 0, DIM);
      System.arraycopy(mom.hi, 0, dau2.hi, 0, DIM);
      dau2.lo[tdim] = value[index[ptmid]];
      dau2.ptlo = ptmid + 1;
      dau2.pthi = pthi;
      mom.dau1 = dau1;
      mom.dau2 = dau2;
      if (ptmid - ptlo > 1)
        toDo.push(dau1);
      else
      {
        dau1.dau1 = null;
        dau1.dau2 = null;
      }
      if (pthi - ptmid > 2)
        toDo.push(dau2);
      else
      {
        dau2.dau1 = null;
        dau2.dau2 = null;
      }
    }
    assert jbox + 1 == Nboxes : "Wrong number of boxes";
  }

  void sift_down (double[] heap, int[] index, int nn)
  {
    int n = nn - 1;
    double a = heap[0];
    int in = index[0];
    int jold = 0;
    int j = 1;
    while (j <= n)
    {
      if (j < n && heap[j] < heap[j + 1])
        ++j;
      if (a >= heap[j])
        break;
      heap[jold] = heap[j];
      index[jold] = index[j];
      jold = j;
      j = 2 * j + 1;
    }
    heap[jold] = a;
    index[jold] = in;

    for (int i = 1; i < Nnearest; ++i)
      assert heap[0] >= heap[i];
  }

  void computeDistances ()
  {
    if (nearest == null || nearest.length < Npoints)
      nearest = new double[Npoints];

    selector.reset();
    for (int i = 0; i < Nboxes; i++)
    {
      BoxNode box = node[i];
      if (box.dau1 != null)
        continue;
      BoxNode initial = box.mom;
      while (initial.pthi - initial.ptlo < Nnearest)
        initial = initial.mom;

      for (int j = box.ptlo; j <= box.pthi; ++j)
      {
        for (int k = 0; k < Nnearest; ++k)
          distance[k] = Double.MAX_VALUE;
        for (int k = initial.ptlo; k <= initial.pthi; ++k)
        {
          if (j == k)
            continue;
          double d = dist(index[j], index[k]);
          if (d < distance[0])
          {
            distance[0] = d;
            neighbor[0] = index[k];
            sift_down(distance, neighbor, Nnearest);
          }
        }

        toDo.push(node[0]);
        while (!toDo.isEmpty())
        {
          BoxNode b = toDo.pop();
          if (b == initial)
            continue;
          if (dist(b, index[j]) >= distance[0])
            continue;
          if (b.dau1 != null)
          {
            toDo.push(b.dau1);
            toDo.push(b.dau2);
          }
          else
          {
            for (int k = b.ptlo; k <= b.pthi; ++k)
            {
              if (j == k)
                continue;
              double d = dist(index[j], index[k]);
              if (d < distance[0])
              {
                distance[0] = d;
                neighbor[0] = k;
                sift_down(distance, neighbor, Nnearest);
              }
            }
          }
        }

        nearest[j] = distance[0];
        selector.add(distance[0]);
      }
    }
    median = selector.median();
  }

  void filterPoints (double scale)
  {
    outlier.clear();
    for (int i = 0; i < Npoints; ++i)
      if (nearest[i] > median * scale * scale)
        outlier.set(i);
  }

  void filterPoints (BitSet exclude, int[] gate)
  {
    for (int i = 0; i < Npoints; ++i)
    {
      if (outlier.get(i))
      {
        exclude.set(index[i]);
        gate[index[i]] = Gate.SCATTER.code;
      }
    }
  }

  void filterPoints (int Nevents, BitSet exclude, int[] gate, float[][] point)
  {
    for (int i = 0; i < Nevents; ++i)
    {
      if (exclude.get(i))
        continue;

      BoxNode box = node[0];
      while (box.dau1 != null)
        if (point[box.dim][i] <= box.dau1.hi[box.dim])
          box = box.dau1;
        else
          box = box.dau2;

      double nearest = Double.POSITIVE_INFINITY;
      int neighbor = -1;
      for (int j = box.ptlo; j <= box.pthi; ++j)
      {
        double d = dist(index[j], this.point, i, point);
        if (d < nearest)
        {
          nearest = d;
          neighbor = j;
        }
      }
      assert !Double.isInfinite(nearest);

      toDo.push(node[0]);
      while (!toDo.isEmpty())
      {
        box = toDo.pop();
        if (dist(box, i, point) < nearest)
          if (box.dau1 != null)
          {
            toDo.push(box.dau1);
            toDo.push(box.dau2);
          }
          else
          {
            for (int j = box.ptlo; j <= box.pthi; ++j)
            {
              double d = dist(index[j], this.point, i, point);
              if (d < nearest)
              {
                nearest = d;
                neighbor = j;
              }
            }
          }
      }

      if (outlier.get(neighbor))
      {
        exclude.set(i);
        gate[i] = Gate.SCATTER.code;
      }
    }
  }

  void filterPoints (BitSet exclude, int[] gate, double scale)
  {
    for (int i = 0; i < Npoints; ++i)
    {
      if (nearest[i] > median * scale * scale)
      {
        exclude.set(index[i]);
        gate[index[i]] = Gate.SCATTER.code;
      }
    }
  }
}
