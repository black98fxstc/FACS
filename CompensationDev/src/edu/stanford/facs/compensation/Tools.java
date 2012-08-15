package edu.stanford.facs.compensation;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import edu.stanford.facs.compensation.Estimator.Point;

public class Tools
{
  public static final double EPSILON = Math.ulp(1.0D);
  public static final double FPMIN = Double.MIN_VALUE / EPSILON;
  public final static boolean DEBUG = false;

  public static class Local
  {
    public final LeastSquaresLine leastSquares;
    public final RobustLine2 robustLine;
    public final Heteroskedastic heteroskedastic;
    public final RobustHeteroskedastic heterobust;
    public final Selector selector = new Selector();
    public final RunTest runs = new RunTest();
    public final NormalityTest normality = new NormalityTest();

    Local ()
    {
      Stack<Point> pool = new Stack<Point>();
      List<Point> data = new ArrayList<Point>();

      leastSquares = new LeastSquaresLine(pool, data);
      robustLine = new RobustLine2(pool, data);
      heteroskedastic = new Heteroskedastic(pool, data);
      heterobust = new RobustHeteroskedastic(pool, data);
    }
  }

  public static final ThreadLocal<Tools.Local> threadLocal = new ThreadLocal<Tools.Local>()
  {
    protected Tools.Local initialValue ()
    {
      return new Tools.Local();
    }
  };

  public static final LeastSquaresLine getLeastSquaresLine ()
  {
    return threadLocal.get().leastSquares;
  }

  public static final RobustLine2 getRobustLine ()
  {
    return threadLocal.get().robustLine;
  }

  public static final Heteroskedastic getHeteroskedastic ()
  {
    return threadLocal.get().heteroskedastic;
  }

  public static final RobustHeteroskedastic getRobustHeteroskedastic ()
  {
    return threadLocal.get().heterobust;
  }

  public static final Selector getSelector ()
  {
    return threadLocal.get().selector;
  }

  public static final RunTest getRunTest ()
  {
    return threadLocal.get().runs;
  }
  
  public static final NormalityTest getNormalityTest ()
  {
  	return threadLocal.get().normality;
  }

  public static void invert (double[][] matrix)
  {
    int row = 0, col = 0, n = matrix.length;
    int pivot[] = new int[n];
    int row_index[] = new int[n];
    int col_index[] = new int[n];

    for (int i = 0; i < n; ++i)
    {
      double big = 0;
      for (int j = 0; j < n; ++j)
      {
        if (pivot[j] != 1)
          for (int k = 0; k < n; ++k)
          {
            if (pivot[k] == 0)
            {
              double abs = Math.abs(matrix[j][k]);
              if (abs >= big)
              {
                big = abs;
                row = j;
                col = k;
              }
            }
            else if (pivot[k] > 1)
              throw new IllegalArgumentException("Matrix is singular.  \nSuggestion:  Rerun the compensation but select the option to pick the controls manually.");
          }
      }
      ++pivot[col];
      row_index[i] = row;
      col_index[i] = col;

      if (row != col)
        for (int k = 0; k < n; ++k)
        {
          double t = matrix[row][k];
          matrix[row][k] = matrix[col][k];
          matrix[col][k] = t;
        }

      if (matrix[col][col] == 0)
        throw new IllegalArgumentException("Matrix is singular.  \nSuggestion:  Rerun the compensation but select the option to pick the controls manually.");
      double inverse = 1 / matrix[col][col];
      matrix[col][col] = 1;
      for (int j = 0; j < n; ++j)
        matrix[col][j] *= inverse;
      for (int j = 0; j < n; ++j)
        if (j != col)
        {
          double t = matrix[j][col];
          matrix[j][col] = 0;
          for (int k = 0; k < n; ++k)
            matrix[j][k] -= matrix[col][k] * t;
        }
    }

    for (int i = n - 1; i >= 0; --i)
      if (row_index[i] != col_index[i])
        for (int j = 0; j < n; ++j)
        {
          double t = matrix[j][row_index[i]];
          matrix[j][row_index[i]] = matrix[j][col_index[i]];
          matrix[j][col_index[i]] = t;
        }
  }

  public static void gaussJordan (double[][] a, double b[], int n)
  {
    int row = 0, col = 0;
    int pivot[] = new int[n];
    int row_index[] = new int[n];
    int col_index[] = new int[n];

    for (int i = 0; i < n; ++i)
    {
      double big = 0;
      for (int j = 0; j < n; ++j)
      {
        if (pivot[j] != 1)
          for (int k = 0; k < n; ++k)
          {
            if (pivot[k] == 0)
            {
              double abs = Math.abs(a[j][k]);
              if (abs >= big)
              {
                big = abs;
                row = j;
                col = k;
              }
            }
            else if (pivot[k] > 1)
              throw new IllegalArgumentException("Matrix is singular.  \nSuggestion:  Rerun the compensation but select the option to pick the controls manually.");
          }
      }
      ++pivot[col];
      row_index[i] = row;
      col_index[i] = col;

      if (row != col)
      {
        for (int k = 0; k < n; ++k)
        {
          double t = a[row][k];
          a[row][k] = a[col][k];
          a[col][k] = t;
        }
        double t = b[row];
        b[row] = b[col];
        b[col] = t;
      }

      if (a[col][col] == 0)
        throw new IllegalArgumentException("Matrix is singular.  \nSuggestion:  Rerun the compensation but select the option to pick the controls manually.");
      double inverse = 1 / a[col][col];
      a[col][col] = 1;
      for (int j = 0; j < n; ++j)
        a[col][j] *= inverse;
      b[col] *= inverse;
      for (int j = 0; j < n; ++j)
        if (j != col)
        {
          double t = a[j][col];
          a[j][col] = 0;
          for (int k = 0; k < n; ++k)
            a[j][k] -= a[col][k] * t;
          b[j] -= b[col] * t;
        }
    }

    for (int i = n - 1; i >= 0; --i)
      if (row_index[i] != col_index[i])
      {
        for (int j = 0; j < n; ++j)
        {
          double t = a[j][row_index[i]];
          a[j][row_index[i]] = a[j][col_index[i]];
          a[j][col_index[i]] = t;
        }
      }
  }

  private static final double[] coef =
    { 57.1562356658629235, -59.5979603554754912, 14.1360979747417471,
      -0.491913816097620199, .339946499848118887e-4, .465236289270485756e-4,
      -.983744753048795646e-4, .15808873224912494e-3, -.210264441724104883e-3,
      .217439618115212643e-3, -.164318106536763890e-3, .844182239838527433e-4,
      -.261908384015814087e-4, .368991826595316234e-5 };

  public static double lnGamma (final double x)
  {
    double y = x, ser = 0.999999999999997092;
    double tmp = x + 5.24218750000000000;
    tmp = (x + .5) * Math.log(tmp) - tmp;
    for (int i = 0; i < 14; i++)
      ser += coef[i] / ++y;
    return tmp + Math.log(2.5066282746310005 * ser / x);
  }

  private static final int ASWITCH = 100;

  private static final double y[] =
    { 0.0021695375159141994, 0.011413521097787704, 0.027972308950302116,
      0.051727015600492421, 0.082502225484340941, 0.12007019910960293,
      0.16415283300752470, 0.21442376986779355, 0.27051082840644336,
      0.33199876341447887, 0.39843234186401943, 0.46931971407375483,
      0.54413605556657973, 0.62232745288031077, 0.70331500465597174,
      0.78649910768313447, 0.87126389619061517, 0.95698180152629142 };
  private static final double w[] =
    { 0.0055657196642445571, 0.012915947284065419, 0.020181515297735382,
      0.027298621498568734, 0.034213810770299537, 0.040875750923643261,
      0.047235083490265582, 0.053244713977759692, 0.058860144245324798,
      0.064039797355015485, 0.068745323835736408, 0.072941885005653087,
      0.076598410645870640, 0.079687828912071670, 0.082187266704339706,
      0.084078218979661945, 0.085346685739338721, 0.085983275670394821 };

  public static double Q (final double a, final double x)
  {
    if (x < 0.0 || a <= 0.0)
      throw new IllegalArgumentException("bad args in gammq");
    if (x == 0.0)
      return 1.0;

    double gln = lnGamma(a);
    if (a >= ASWITCH)
    {
      double a1 = a - 1.0, lna1 = Math.log(a1), sqrta1 = Math.sqrt(a1);
      double xu;
      if (x > a1)
        xu = Math.max(a1 + 11.5 * sqrta1, x + 6.0 * sqrta1);
      else
        xu = Math.max(0., Math.min(a1 - 7.5 * sqrta1, x - 5.0 * sqrta1));

      double sum = 0;
      for (int j = 0; j < y.length; j++)
      {
        double t = x + (xu - x) * y[j];
        sum += w[j] * Math.exp(-(t - a1) + a1 * (Math.log(t) - lna1));
      }
      double ans = sum * (xu - x) * Math.exp(a1 * (lna1 - 1.) - gln);
      if (ans >= 0)
        return ans;
      else
        return 1 + ans;
    }
    else if (x < a + 1)
    {
      double ap = a;
      double del = 1 / a;
      double sum = del;
      for (;;)
      {
        ++ap;
        del *= x / ap;
        sum += del;
        if (Math.abs(del) < Math.abs(sum) * EPSILON)
          return 1 - sum * Math.exp(-x + a * Math.log(x) - gln);
      }
    }
    else
    {
      double b = x + 1 - a;
      double c = 1 / FPMIN;
      double d = 1 / b;
      double h = d;
      for (int i = 1;; i++)
      {
        double an = -i * (i - a);
        b += 2;
        d = an * d + b;
        if (Math.abs(d) < FPMIN)
          d = FPMIN;
        c = b + an / c;
        if (Math.abs(c) < FPMIN)
          c = FPMIN;
        d = 1 / d;
        double del = d * c;
        h *= del;
        if (Math.abs(del - 1) <= EPSILON)
          break;
      }
      return Math.exp(-x + a * Math.log(x) - gln) * h;
    }
  }

  public static final double MEDIAN_CHI_SQUARED_1 = 0.4549364;
}
