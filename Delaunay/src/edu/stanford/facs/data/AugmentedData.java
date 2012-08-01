package edu.stanford.facs.data;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import org.isac.fcs.FCSException;
import org.isac.fcs.FCSFile;
import org.isac.fcs.FCSHandler;
import org.isac.fcs.FCSParameter;

public abstract class AugmentedData
  extends FlowData
{
  public final String tag;

  protected AugmentedData (FCSFile fcsfile, String tag)
  {
    super(fcsfile);
    this.tag = tag;
  }

  public void writeAugmented ()
    throws IOException, FCSException
  {
    FCSFile fcsIn = this.fcs;
    File f = fcsIn.getFile();
    String fn = f.getName();
    fn = fn.substring(0, fn.length() - 4) + tag + fn.substring(fn.length() - 4);
    f = new File(f.getParentFile(), fn);
    FCSFile fcsOut = new FCSFile(f);

    fcsOut.getTextSegment().readFrom(this.fcs.getTextSegment());
    augmentHeader(fcsOut);

    FCSHandler itIn = fcsIn.getInputIterator();
    FCSHandler itOut = fcsOut.getOutputIterator();
    int dim = fcsIn.getInteger("$PAR");  //number of parameters in an event
    for (int k = 0; itIn.hasMoreEvents(); ++k)
    {
      for (int n = 0; n < dim; ++n)
        itOut.writeFloat(itIn.readFloat());
      augmentData(itOut, k);
    }
    itIn.close();
    itOut.close();

    fcsOut.close();
    fcsIn.close();
  }

  protected abstract void augmentData (FCSHandler itOut, int k)
    throws FCSException, IOException;

  protected abstract void augmentHeader (FCSFile fcsOut)
    throws FCSException, IOException;

  public void writeAugmentedCSV ()
    throws IOException, FCSException
  {
    FCSFile fcsIn = this.fcs;
    File f = fcsIn.getFile();
    String fn = f.getName();
    fn = fn.substring(0, fn.length() - 4) + tag + ".csv";
    f = new File(f.getParentFile(), fn);

    PrintWriter pw = new PrintWriter(f);
    Iterator<FCSParameter> i = (Iterator<FCSParameter>)fcsIn.getParameterList()
      .iterator();
    FCSParameter p = i.next();
    pw.print(p.getAttribute("$P", "N"));
    while (i.hasNext())
    {
      p = (FCSParameter)i.next();
      pw.print(',');
      pw.print(p.getAttribute("$P", "N"));
    }

    augmentHeaderCSV(pw);
    pw.println();

    FCSHandler itIn = fcsIn.getInputIterator();
    int dim = fcsIn.getInteger("$PAR");
    for (int k = 0; itIn.hasMoreEvents(); ++k)
    {
      pw.printf("%10e", itIn.readFloat());
      for (int n = 1; n < dim; ++n)
      {
        pw.print(',');
        pw.printf("%10e", itIn.readFloat());
      }
      augmentDataCSV(pw, k);
      pw.println();
    }
    itIn.close();

    pw.close();
    fcsIn.close();
  }

  protected abstract void augmentDataCSV (PrintWriter pw, int k)
    throws FCSException, IOException;

  protected abstract void augmentHeaderCSV (PrintWriter pw)
    throws FCSException, IOException;
}