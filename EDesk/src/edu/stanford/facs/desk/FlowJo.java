package edu.stanford.facs.desk;

import java.io.*;

import org.isac.fcs.*;
import edu.stanford.facs.flowjo.*;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:      Stanford University
 * @author Wayne A. Moore
 * @version 1.0
 */

public class FlowJo
{
  public static final double LN_10 = Math.log(10.0);

  static FCSTextSegment getFCSTextSegment(
      DataDrawer drawer,
      int index)
      throws FCSException
  {
    FCSTextSegment fcs_text = new FCSTextSegment();
    FacsSample sample = drawer.sample[index];
    FacsChannel[] channel = sample.channel;

    fcs_text.setDelimiter('`');

    fcs_text.setAttribute("$BYTEORD", "1,2,3,4");
    fcs_text.setAttribute("$DATATYPE", "I");
    fcs_text.setAttribute("$ENCRYPT", "NONE");
    fcs_text.setAttribute("$KWMAXCHARS", "63");
    fcs_text.setAttribute("$MODE", "L");
    fcs_text.setAttribute("$NEXTDATA", "0");
    fcs_text.setAttribute("$VALMAXCHARS", "63");
    fcs_text.setAttribute("$SYS", "PASCAL; VMS_VAX Desk Export");

    fcs_text.setAttribute("$CELLS", sample.label);
    fcs_text.setAttribute("$CYT", drawer.cytometer);
    fcs_text.setAttribute("$DATE", drawer.date);
    fcs_text.setAttribute("$EXP", drawer.author);
    fcs_text.setAttribute("$PROJ", drawer.title);
    fcs_text.setAttribute("$SMNO", sample.coord);
    fcs_text.setAttribute("$TOT", sample.events);
    fcs_text.setAttribute("#EXPID", drawer.protocol);
    //	fcs_text.setAttribute( "#HEADID",	"????1091713" );
    fcs_text.setAttribute("#SMPID", sample.warehouse);
    fcs_text.setAttribute("$PAR", channel.length);

    for (int j = 0, n = 1; j < channel.length; ++j, ++n)
    {
      fcs_text.setAttribute("$P", n, "N", channel[j].sensor);
      fcs_text.setAttribute("$P", n, "S", channel[j].reagent);
      int bits = channel[j].bits;
      fcs_text.setAttribute("$P", n, "B", bits);
      fcs_text.setAttribute("$P", n, "R", 1 << bits);
      double low = channel[j].scaleMinimum;
      double high = channel[j].scaleMaximum;
      if (!channel[j].isLogScale)
      {
        high = (float)(high / 1.024);
//        fcs_text.setAttribute("$P", n, "G", 1600.0 / high);
        fcs_text.setAttribute("$P", n, "E", "0.0,0.0");
      }
      else
      {
        double range = (Math.log(high) - Math.log(low)) / 1.024;
        high = low * Math.exp(range);
        String d = String.valueOf((float)(range / LN_10));
        String m = String.valueOf((float)low);
        fcs_text.setAttribute("$P", n, "E", d + "," + m);
      }
      fcs_text.setAttribute("#P", n, "LO", low);
      fcs_text.setAttribute("#P", n, "HI", high);
    }

    return fcs_text;
  }

  public static FlowJoSummaryFile getExportFile(
      DataDrawer drawer,
      String ftpHost)
      throws IOException, FCSException
  {
    int n = drawer.sample.length;
    FCSTextSegment[] headers = new FCSTextSegment[n];
    String[] urls = new String[n];
    for (int i = 0; i < n; ++i)
    {
      urls[i] = "ftp://" + ftpHost + "/"
          + FacsDesk.getNinjaVMS(drawer.sample[i].warehouse);
      headers[i] = FlowJo.getFCSTextSegment(drawer, i);
    }

    return new FlowJoSummaryFile(urls, headers, drawer.title, FlowJoSummaryFile.V3);
  }

  public static FlowJoSummaryFile getSummaryFile(
      DataDrawer drawer,
      String httpHost)
      throws IOException, FCSException
  {
    int n = drawer.sample.length;
    FCSTextSegment[] headers = new FCSTextSegment[n];
    String[] urls = new String[n];
    for (int i = 0; i < n; ++i)
    {
      urls[i] = "http://" + httpHost + "/EDesk/Darwin/CSampl"
          + FacsDesk.getNinjaURI(drawer.sample[i].warehouse);
      headers[i] = FlowJo.getFCSTextSegment(drawer, i);
    }

    return new FlowJoSummaryFile(urls, headers, drawer.title, FlowJoSummaryFile.V4);
  }
}
