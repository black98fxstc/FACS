/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.stanford.facs.data;

import com.apple.eio.FileManager;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import org.isac.fcs.FCSFile;

/**
 * Read a matrix from a text file. The matrix file is in the format that
 * FlowJo puts out.  First line is the name of the file or name of the matrix.
 * The second line <   > .
 * Third line is the list of detectors, comma-separated.
 * The next n rows are the matrix values.
 * @author beauheim
 */
public class FluorescenceCompensation {
    private File matrixSource;
    private String name;
   // private String secondLine;
    private double [][] matrix;
    private String [] detectors;
    private double [][] matrixInverted;

    public FluorescenceCompensation (File matrixfile){
        matrixSource = matrixfile;
        readFile();
    }

    public FluorescenceCompensation (String[] detectorNames, double[][] matrix){
        this.detectors = detectorNames;
        this.matrix = matrix;

    }
    /**
     *   Open the fcs file and get the SPILL parameter and get the matrix by
     * some convoluted process of FCS stuff.
     */
    public FluorescenceCompensation (FCSFile fcsfile){

    }
/**
 *
 * @return String[] of detector Names from the matrix file.
 */
    public String[] getDetectorNames() {
        return detectors;
    }
/**
 *
 * @return double[][] data values from the matrix file.
 */
    public double [][] getSpillOverMatrix() {
        return matrix;
    }
/**
 *
 * @param A  double[][]  Print the matrix for debugging
 */
    private void printMatrix (double [][] A){
        for (int i=0; i < A.length; i++){
            for (int j=0; j < A[0].length; j++){
                System.out.print (A[i][j]+ "  ");
            }
        System.out.println();
        }
    }

    /**
     *
     * @return double[][]  Invert the original matrix, returning the inverted one.
     */
    public double[][] getCompensationMatrix() {


        //make a copy of the original matrix in matrixInverted.  Then call invert() with
        //the matrixInverted.

    if (matrixInverted == null){
        int dim = matrix.length;
        matrixInverted = new double[dim][dim];

        for (int i=0; i < dim; i++){
            for (int j=0; j < dim; j++){
                matrixInverted[i][j] = matrix[i][j];
            }
        }
        invert (matrixInverted);
   // printMatrix (matrix);
    }

     return matrixInverted;
    }
    
    /**
     * Invert the matrix.  Taken from Compensation.  
     * @param matrix
     */

 public void invert (double[][] matrix) {

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

 public void writeMatrixToFile (File file, String[] detectors, float[][] matrix){

 }


    /**
     *
     *  read the matrix data file.
     */

    private void readFile() {
        BufferedReader reader= null;
        String line=null;
        int nlines=0;
        String splitOn="\t";
        String secondLine;  //could have something in it, but doesn't right now.
//        File file = new File (matrixSource);
        if (matrixSource.exists() && matrixSource.canRead()){
            try {
                reader = new BufferedReader (new FileReader (matrixSource));
                line = reader.readLine();
                while (line != null){
                   nlines++;
                   if (nlines == 1)
                       name = line;
                   else if (nlines == 2)
                       secondLine = line;
                   else if (nlines == 3){
                       detectors = line.split(splitOn);
                       if (detectors == null || detectors.length == 0){
                           splitOn = ",";
                           detectors = line.split (splitOn);
                       }

                       matrix = new double [detectors.length][detectors.length];
                   }
                   else {

                       String[] row = line.split(splitOn);
                       matrix[nlines-4]= getValues (row);
                   }
                   line = reader.readLine();
                }
//                int type = FileManager.getFileType (matrixSource.getName());
//                int creator = FileManager.getFileCreator (matrixSource.getName());
//                System.out.println ("type = " + type + ", creator "+ creator);
            }  catch (IOException e){
                System.out.println (" can't process the file " + matrixSource);
            }
        }

    }

    /**
     *
     * @param row String[]  Takes the String values from the file
     * and creates double values
     * @return double[] double values from the String values.
     */
     private double[] getValues (String[] row){

            double[] values = new double[row.length];
            for (int i=0; i < row.length; i++){
                try {
                    values[i] = Double.parseDouble(row[i]);
                } catch (NumberFormatException e){
                    values[i] = 0;
                }
            }
            return values;
        }

     public static void main (String[] args){

         FluorescenceCompensation matrix = new FluorescenceCompensation (new File ("data/Comp 090806diva.txt"));
         String[]det = matrix.getDetectorNames();
//         for (int i=0; i < det.length; i++){
//             System.out.print (det[i] + "\t");
//         }
//         System.out.println();
         double[][] data = matrix.getSpillOverMatrix();
//         for (int i=0; i < data.length; i++){
//             for (int j=0; j< data[i].length; j++){
//                 System.out.print (data[i][j] + "\t");
//             }
//             System.out.println ();
//         }
     }

}
