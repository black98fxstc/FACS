package sff.accounting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Pattern;

public class FilterMachines
{

  /**
   * @param args
   */
  public static void main (String[] args)
  {
    File in = new File(args[0]);
    File out = new File(args[1]);
    try
    {
      Pattern tab = Pattern.compile("\t");
      BufferedReader br = new BufferedReader(new FileReader(in));
      PrintWriter pw = new PrintWriter(new FileWriter(out));

      
      String header = br.readLine();
      String[] column = tab.split(header, -1);
      int canceled = 0;
      int cancel_date = 0;
      int machine = 0;
      for (int i = 0; i < column.length; i++)
      {
        if (column[i].equalsIgnoreCase("\"Canceled\""))
          canceled = i;
        else if (column[i].equalsIgnoreCase("\"Cancel Date\""))
          cancel_date = i;
        else if (column[i].equalsIgnoreCase("\"Machine\""))
          machine = i;
      }
      pw.print(header);
      pw.print('\n');

      for (;;)
      {
        String record = br.readLine();
        if (record == null)
          break;
        String[] entry = tab.split(record, -1);
        if (entry[canceled].equalsIgnoreCase("No"))
          entry[cancel_date] = "";
        if (entry[machine].equalsIgnoreCase("Aida Med"))
          entry[machine] = "Aida";
        for (int i = 0; i < entry.length - 1; i++)
        {
          pw.print(entry[i]);
          pw.print('\t');
        }
        pw.print(entry[entry.length-1]);
        pw.print('\n');
      }
      
      pw.close();
      br.close();
    }
    catch (IOException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    // TODO Auto-generated method stub

  }

}
