/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.stanford.facs.fmo;

import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 *  Read the file. Create an instrument parameter object for each
 *  instrument in the file.  Each instrument has a set of detectors.
 *  Each detector has some values.
 * @author beauheim
 */
public class InstrumentParameters {
    private ArrayList <Instrument> allInstruments;
    private final String col_area = "ref AreaSC";
    private final String col_volt = "ref PMTV";
    private final String col_exp = "PMT exponent";
    private final String col_pe_unit = "pe-/unit est";
    private final String col_median = "197 Median";
    private final String col_pe_est = "197pe- est";
    private final String col_rcv = "197 RCV";
    private final String col_det = "Detector";
    private final String col_label = "Label used";
    private final String col_pmtv_sd = "PMTV 0 SD";
    private final String col_no_sig_sd = "PMTon NoSigSD";
    String sep = System.getProperty ("line.separator");
    private String[] detectorsOfInterest;
    private String[][] voltValues;
    private String[][] scalingValues;
// ref AreaSc	ref PMTV	PMT exponent	pe-/unit est	197 Median	197pe- est	197 RCV


    

    /**
     * This Instrument contains a map of detectors, keyed on the detector name.
     */

    /*
     * Read the file of instrument configurations, given the name of the csv
     * file and the list of detectors of interest.  The detectorNames are gleamed
     * from the spectral overlap matrix csv file.
     */
    public InstrumentParameters (String instr_filename, String[] detectorNames,
                                 String[][]scalingValues, String[][]voltValues) {
        detectorsOfInterest = detectorNames;
        allInstruments = new ArrayList<Instrument>();
        this.scalingValues = scalingValues;
        this.voltValues = voltValues;
        Class c = null;
        try {
           c = Class.forName ("edu.stanford.facs.fmo.InstrumentParameters");
        } catch (Exception e){
            System.out.println (e.getMessage());
            System.out.println (" Can't read the Resource file !!");
            System.exit(1);
        }
        System.out.println (instr_filename + "  " );
        InputStream is = c.getResourceAsStream (instr_filename);
        if (is != null)
            readResourceAsStream(is);
        else {
            System.out.println (" Input Stream was null");
            System.exit(1);
        }

    }
    
    public ArrayList<Instrument> getAllInstruments() {
        for (Instrument ii : allInstruments){
            System.out.println (ii.name);
        }
        return allInstruments;
    }

    public Instrument getInstrument (String name){
        if (allInstruments.size() == 1)
            return allInstruments.get(0);
        else
            return null;

    }

    //override
    /**
     * for debugging
     */
    public String toString() {
        StringBuilder buf = new StringBuilder();

        for (Instrument inst: allInstruments){
           buf.append (inst.toString());
        }

        return buf.toString();
    }

//Use ClassLoader with getSystemResource() and getSystemResourceAsStream()
 //Class.getResource()
    /**
     * public InputStream getResourceAsStream(String name) {
          name = resolveName(name);
          ClassLoader cl = getClassLoader();
          if (cl==null) {
             return ClassLoader.getSystemResourceAsStream(name); // A system class.
          }
        return cl.getResourceAsStream(name);
}

     * @param paramFile
     */
    private void readResourceAsStream (InputStream is ){
        
        int colarea=-1, colvolt=-1, colexp=-1, colpe=-1;
        int colmed=-1, colpeEst=-1,colrcv=-1, col_detector=-1;
        int collabel=-1;
        int col_pmtvsd=-1, colnosigsd=-1;
        String laser_name=null;
        boolean DETECTOR_MODE = false;
        Instrument newInst = null;
        try {    
            BufferedReader in = new BufferedReader (new InputStreamReader (is, "UTF-8"));
            String line = in.readLine();
            while (line != null){

                if (line.startsWith("Cytometer Configuration")){
                    //new instrument
                    if (newInst != null)
                        allInstruments.add (newInst);
                    DETECTOR_MODE = false;
                    newInst = new Instrument();    

                }
                else if (line.startsWith ("Configuration Name")){
                    String[]parts = line.split(",");
                    if (parts.length > 1)
                        newInst.setName (parts[1]);
                    
                }
                else if (line.startsWith ("Laser Name")){
                    DETECTOR_MODE = true;
                   String[] parts = line.split(",");
                   int ncol = parts.length;


                   colarea= findMatchingColumn (parts, col_area);
                   colvolt= findMatchingColumn (parts, col_volt);
                   colexp = findMatchingColumn (parts, col_exp);
                   colpe =  findMatchingColumn ( parts, col_pe_unit);
                   colmed = findMatchingColumn ( parts, col_median);
                   colpeEst=findMatchingColumn ( parts, col_pe_est);
                   colrcv = findMatchingColumn ( parts, col_rcv);
                   col_detector = findMatchingColumn (parts, col_det);
                   collabel = findMatchingColumn (parts, col_label);
                   col_pmtvsd = findMatchingColumn (parts, col_pmtv_sd);
                   colnosigsd = findMatchingColumn (parts, col_no_sig_sd);
                   if (col_detector == -1){
                       System.out.println (" Can't find the detector column");
                       System.exit(1);
                   }
                   
                   //get the detectors

                }
                else if (DETECTOR_MODE){
                    float area=0, volt=0,exp=0;
                    float pe=0,med=0,peEst=0,rcv=0;
                    float pmtvsd=0, nosigsd=0;
                    float volts_now, area_now;
                    String[]parts = line.split(",");
                    if (parts != null && parts.length > 1){
                        //not a blank line
                        if (parts[0] != null && !parts[0].equals(""))
                            laser_name = parts[0];
                        //sometimes we have a 'detector' column, but nothing else is filled in.
                        //first is this a detector we are interested in?
                        if (parts.length > collabel && detectorInteresting (parts[collabel])){
//                        if (detectorInteresting (parts[col_detector])){
                            if (parts[collabel] !=null && !parts[collabel].equals("") && parts.length>20){ //new detector
                                String name=parts[collabel];
                                area = findValue (parts, colarea);
                                volt = findValue (parts, colvolt);
                                exp = findValue (parts, colexp);
                                pe =  findValue (parts,colpe);
                                med = findValue (parts, colmed);
                                peEst = findValue (parts, colpeEst);
                                rcv = findValue (parts, colrcv);
                                pmtvsd = findValue (parts, col_pmtvsd);
                                nosigsd = findValue (parts, colnosigsd);
                                volts_now = matchVoltageValue(name);
                                area_now = matchScalingValue(laser_name);
                                Detector newdet = new Detector (name, laser_name, area, volt, exp,
                                                                pe, med, peEst, rcv, pmtvsd, nosigsd, volts_now, area_now);
                                newInst.addDetector (newdet);
                            }
                        }
                    }

                }
                line = in.readLine();
            }
            if (newInst != null){
                allInstruments.add (newInst);
            }

       // }
    } catch (Exception e){
        System.out.println (e.getMessage());
        e.printStackTrace();
        System.exit(1);
    }
    }
         /**
     * in the xml file the scaling value is per laser, not per detector.
     */
    private float matchScalingValue (String laser){
        float value=0;
        for (int i=0; i < scalingValues.length; i++){
            if (scalingValues[i][0].equals (laser)){
                try {
                    value = Float.parseFloat (scalingValues[i][1]);
                } catch (NumberFormatException e){
                    System.out.println (laser + "  " + scalingValues[i]+ "  " +e.getMessage() );
                }
            }
        }
        System.out.println (" laser " + value);
        return value;

    }

    private boolean detectorInteresting (String det){
        boolean flag = false;
//System.out.println (" ------------ detectorInteresting?  " + det);
        if (det != null && !det.equals("")){
            for (String s: detectorsOfInterest){
              //  System.out.println ("detectors of interest?  " + s + ", "+ det);
                if (s.equals (det)){
                    flag = true;
                }
            }
        }
//System.out.println (" retruning flag is " + flag);
        return flag;
    }

    private float findValue (String[] line, int index){
        float value=0;

        if (index > -1 && index < line.length){
            if (line[index] != null && !line[index].equals("")){
                try {
                value = Float.parseFloat (line[index]);
                } catch( NumberFormatException e) {
                    value=0;
                }
            }
        }


        return value;
    }

    private int findMatchingColumn (String[] line, String heading){
        int i=0;
        int ret=-1;
        boolean flag = false;
        while (!flag){
            if (i > line.length){
                ret=-1;
                flag=true;
            }
            else if (line[i].equalsIgnoreCase (heading)){
                ret=i;
                flag =true;
            }
            else i++;
        }
        return ret;
    }
     private int matchDetectorName (String name){
        boolean flag = false;
        int index=-1;
        for (int i=0; i < detectorsOfInterest.length; i++){
            if (detectorsOfInterest[i].equals (name))
                index=i;

        }

        return index;
    }

    private float matchVoltageValue (String detectorname){
        float value=0;
        for (int i=0; i < voltValues.length; i++){
            if (voltValues[i][0].equals (detectorname)){
                try {
                    value = Float.parseFloat(voltValues[i][1]);
                } catch (NumberFormatException e){
                    System.out.println (detectorname + " " + e.getMessage());
                }
            }

        }
        return value;
    }



}
