package edu.stanford.facs.fmo;

/**
 * $Id: Detector.java,v 1.4 2011/06/17 00:38:01 beauheim Exp $
 * 
 * @author cate
 */
public class Detector
{
  public final String name;
  public final String laserName;
  public final double nowVolts, nowArea, nowSensitivity, nowBackgroundLight;
  public final double peVolts, peArea, peSensetivity;
  public final double bgVolts, bgArea, bgSensitivity, bgBackgroundLight;
  public final double pmtExponent;
  public final double electronicVariance;

  private float area_scaling;
  private float pmtv;
  private float exponent;
  private float pe_per_unit;
  private float median;
  private float pe_197_est;
  private float rcv;
  private float pmtv0_sd;
  private float blanks_sd;
  private float voltage;
  private float exp_area_scaling;

  /**
   * @param name
   *          String name of the detector. source is the instrument parameter
   *          file
   * @param laser_name
   *          String laser_name from the instrument parameter file
   * @param area
   *          float area is in the instrument parameter file
   * @param pmtv
   *          float pmtv is in the instrument parameter file
   * @param exp
   *          float esp is in the instrument parameter file
   * @param pe
   *          float PE is in the instrument parameter file (photo electrons)
   * @param median
   *          float median is in the instrument parameter file
   * @param pe_est
   *          float PE_est is in the instrument parameter file
   * @param rcv
   *          float rcv is in the instrument parameter file
   */
  public Detector (String name, String laser_name, float area, float pmtv,
    float exp, float pe, float median, float pe_est, float rcv, float pmtv0sd,
    float blanksd, float volts_now, float area_now)
  {
    /*
     * The data as it comes from the .csv file
     */
    this.name = name;
    this.laserName = laser_name;
    this.area_scaling = area;
    this.pmtv = pmtv;
    this.exponent = exp;
    this.pe_per_unit = pe;
    this.median = median;
    this.pe_197_est = pe_est;
    this.rcv = rcv;
    this.pmtv0_sd = pmtv0sd;
    this.blanks_sd = blanksd;
    /*
     * The voltage and area scaling now and when the photoelectron and
     * background estimates were made.
     */
    this.nowVolts = volts_now; // from Diva XML file
    this.nowArea = area_now; // from Diva XML file
    this.peVolts = pmtv;
    this.peArea = area;
    this.bgVolts = this.peVolts;// for now just duplicate
    this.bgArea = this.peArea;
    /*
     * The PMT voltage gain exponent for this detector
     */
    this.pmtExponent = exp;
    /*
     * Calculate the sensetivity, i.e., signal per photoelectron. This is also
     * the variance per signal and we'll need it under all three sets of
     * conditions.
     */
    this.peSensetivity = 1 / this.pe_per_unit;
    double adjust = (nowArea / peArea)
      * Math.pow(nowVolts / peVolts, pmtExponent);
    this.nowSensitivity = adjust * this.peSensetivity;
    adjust = (bgArea / peArea) * Math.pow(bgVolts / peVolts, pmtExponent);
    this.bgSensitivity = adjust * this.peSensetivity;
    /*
     * Get the electronic background and the mean and variance of some hopefully
     * "blank" objects. Compute the estimated background light signal and
     * transform to the current conditions.
     */
    this.electronicVariance = pmtv0sd * pmtv0sd;
    double blankMean = 0;
    double blankVariance = blanksd * blanksd;
    double fluorescenceVariance = bgSensitivity * blankMean;
    double opticalVariance = blankVariance - electronicVariance
      - fluorescenceVariance;
    if (opticalVariance < 0)
      opticalVariance = 0;
    bgBackgroundLight = opticalVariance / bgSensitivity;
    adjust = (nowArea / bgArea) * Math.pow(nowVolts / bgVolts, pmtExponent);
    nowBackgroundLight = adjust * bgBackgroundLight;
  }

  //override
  /**
   * override toString for debugging
   */
  public String toString ()
  {
    String sep = System.getProperty("line.separator");
    StringBuilder buf = new StringBuilder();
    buf.append("Laser : " ).append( laserName);
    buf.append("  Detector : ").append( name);
    buf.append(sep).append( "\t Area Scaling :").append (area_scaling);
    buf.append(sep).append( "\t PMTV : ").append( pmtv);
    buf.append(sep).append("\t PMT Exp : ").append(exponent);
    buf.append(sep).append ("\t PE-/Unit : ").append (pe_per_unit);
    buf.append(sep).append ("\t Median : ").append(median);
    buf.append(sep).append("\t PE-EST : ").append(pe_197_est);
    buf.append(sep).append("\t RCV : ").append (rcv);
    buf.append(sep).append("\t EXP_VOLTAGE: ").append ( voltage);
    buf.append(sep).append( "\t EXP_AREA_SCALING: ").append (exp_area_scaling);
    buf.append(sep).append ("\t PMTV_SD: ").append (pmtv0_sd);
    buf.append(sep).append("\t NOSIGSD:  ").append (blanks_sd);

    buf.append(sep);
    return buf.toString();
  }
}
