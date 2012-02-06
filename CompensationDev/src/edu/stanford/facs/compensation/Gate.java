package edu.stanford.facs.compensation;

public enum Gate
{
  RANGE(4), SCATTER(3), AUTOFLUORESCENCE(2), OUTLIER(1);

  public final int code;

  private Gate (int code)
  {
    this.code = code;
  }
}
