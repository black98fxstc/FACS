package edu.stanford.facs.gui;

import edu.stanford.facs.controllers.CompensationController;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JProgressBar;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import edu.stanford.facs.compensation.Compensation2;
import edu.stanford.facs.compensation.Diagnostic;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.JButton;
import javax.swing.SwingWorker;

import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JSplitPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.JTableHeader;


public class CompensationFrame extends JFrame  implements CompensationResults, PropertyChangeListener {
 

  private static final long serialVersionUID = 1L;
  private final SpectrumCellRenderer spectrumRenderer = new SpectrumCellRenderer();
  private JTable spectrumTable = new JTable();
  final Preferences preferences = Preferences.userNodeForPackage(this.getClass());
  private final ButtonHeaderRenderer buttonRenderer = new ButtonHeaderRenderer();
  

 // private String path = "data";
 // private final JFileChooser fileChooser ;

  private String expName;
  private String[] controlList;
  private String[] detectorList;
  private boolean[][] failsLinearityTest;
  private boolean[][] failsMedianTest;
  private boolean[][] failsSignificanceTest;
  private boolean[][] failsInterceptTest;
  private Float[][] sensitivityData;
  private Float[][] spectrumData;
  private Float[][] errorData;

  private SpectrumModel spectrumModel;

  static int PHOTO_ELECTRONS=0;
  static int ERROR_PE=1;
  static int SENSITIVITY=2;
  static int ERROR_SEN=3;
  static int BACKGROUND=4;
  static int ERROR_BACK=5;
  private JPanel mainPanel;
  private JTable rowTable;
  
  private JProgressBar progressBar;
  private ProgressListener progressListener;
  private Compensation2 compensation2;   
  private JButton printButton;

  private File dataFolder;
  private File divaFile;
  private JPanel northPanel;
  private JPanel dataPanel;

  private JPanel statPanel;
  private JLabel[][] statLabels;
  private ScatterPlot scatterPlot;
  private ScatterPlot fluorescencePlot;

//  private boolean firstTime = true;
  private GatesColors colorScheme = new GatesColors();
  private CompensationController controller;
  private DiagnosticPanel diagnosticPanel;
  
  private Color selectionColor = new Color (48, 76, 120);
//  private Color selectionColor = new Color (LIGHT_BLUE);
  private ArrayList <CellSelectionListener> cellListeners = new ArrayList<CellSelectionListener>();
  private ArrayList <RowSelectionListener> rowListeners = new ArrayList<RowSelectionListener>();
  private ArrayList <ColumnSelectionListener> colListeners = new ArrayList<ColumnSelectionListener>();
  private CellSelectionListener cellLis;
  private RowSelectionListener rowLis;
  private ColumnSelectionListener colLis;
  private MouseAdapter cellClick;
  private MouseAdapter rowClick;
  private MouseAdapter colClick;
  private DecimalFormat decimalFormat = new DecimalFormat ("#.####");
  private NumberFormat percentFormat = new DecimalFormat ("##.##%");

    //Override
    public void propertyChange (PropertyChangeEvent pce) {
        System.out.println (" Compensation Frame -- propertyChangeListener ");
    }


    public interface CellSelectionListener {
        public void cellSelected (int row, int col);
    }
    public interface RowSelectionListener {
        public void rowSelected (int row);
    }
    public interface ColumnSelectionListener {
        public void columnSelected (int col);
    }


   // Construct the application
  public CompensationFrame (String title, File mydataFolder, CompensationController controller) {

    super("Automatic Compensation - "+title);
    this.dataFolder = mydataFolder;  
    this.controller = controller;
    
   
  }

  public void initUI (Compensation2 compensation2){
      this.compensation2 =compensation2;
      initUI();
  }


  public void setExperimentName (String name){
      if (name == null) name = new String("");
      this.expName = name;
  }


  
  private void initUI () {
//      Toolkit tk = Toolkit.getDefaultToolkit();
   
    setSize(1200, 1200);
    mainPanel = new JPanel();
   
    mainPanel.setLayout (new BorderLayout());
    JLabel keylabel = null;
    
   // getContentPane().add(mainPanel, BorderLayout.CENTER);
    JPanel messagePanel = new JPanel();
    messagePanel.setBackground (Color.white);
    progressBar = new JProgressBar();
    progressBar.setValue(0);
    progressListener = new ProgressListener(progressBar) ;
    compensation2.addPropertyChangeListener (progressListener);
 
   
    messagePanel.add (progressBar);
    JButton cancelButton = new JButton ("Cancel ");
    cancelButton.addActionListener (new ActionListener() {
        public void actionPerformed (ActionEvent e){
            boolean flag = compensation2.cancel (true);
//            System.out.println (" Cancel the operation " + flag);
        }
    });
    messagePanel.add (cancelButton);
    printButton = new JButton ("Save Matrix");
    printButton.setEnabled (false);
    printButton.addActionListener (new ActionListener() {
        public void actionPerformed (ActionEvent e){
           if (compensation2.getState() == SwingWorker.StateValue.DONE){
              controller.setUpForMatrixPrinting ( spectrumData, detectorList);

           }
           
        }
    });
    messagePanel.add (printButton);
    mainPanel.add (messagePanel, BorderLayout.SOUTH);


    setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);
    

    pack();
    initFrameSize();
       
  }



  public void  errorDialogToExit (String msg){
     System.out.println ("----  Is this being called? CompensaJOptionPanetionFrame "+ msg);
      JOptionPane.showMessageDialog (this, msg,"Fatal Error ", JOptionPane.ERROR_MESSAGE);
      System.exit(1);
  }



  private FileNameExtensionFilter makeFilterForOS() {
      String os = System.getProperty ("os.name");
      FileNameExtensionFilter filter;
      if (os.equalsIgnoreCase ("Mac OS X")){
          filter = new FileNameExtensionFilter ("FlowJo Matrix", "txt");
      }
      else
          filter = new FileNameExtensionFilter ("FlowJo Matrix", "mtx");
      return filter;
  }
  

  
  public void enableButton (boolean flag){
      printButton.setEnabled (flag);
  }

  public String getDataPath (){
      return dataFolder.getPath();
  }

  public File getDataFile() {
      return divaFile;
  }


  public void showMessageDialog (String msg){
      
      System.out.println ("  Is this being called?  Compensation Frame " + msg);
      System.out.println (" why not? ");
      JOptionPane.showMessageDialog (this, msg, " Computation Status ", JOptionPane.INFORMATION_MESSAGE);
//      scatterDialog.setVisible(true);

  }

  /**
   *
   * @param j  integer index into the 2-d array of sensitivity data.
   * @param photoElectrons.  Stored in [0][j]
   * @param error.  Store in [1][j]
   */

  public void setSensitivity (int j, double photoElectrons, double error)
  {
    sensitivityData[PHOTO_ELECTRONS][j] = new Float(photoElectrons);
    sensitivityData[ERROR_PE][j] = new Float(error);
    spectrumModel.setSpectrum();
  }

  /**
   *
   * @param j integer index into the 2-d array of sensitivity data
   * @param sensitivity.  stored in [0][j]
   * @param error.        stored in [1][j]
   */
  public void setTube (int j, double sensitivity, double error)
  {
    sensitivityData[SENSITIVITY][j] = new Float(sensitivity);
    sensitivityData[ERROR_SEN][j] = new Float(error);
    spectrumModel.setSpectrum();
  }
  /**
   *
   * @param j
   * @param background
   * @param error
   */
  public void setBackground (int j, double background, double error)
  {
    sensitivityData[BACKGROUND][j] = new Float(background);
    sensitivityData[ERROR_BACK][j] = new Float(error);
    spectrumModel.setSpectrum();
  }
  /**
   *
   * @param i
   * @param j
   * @param spillover
   * @param uncertainty
   */
  public void setSpectrum (int i, int j, double spillover, double uncertainty)
  {
    spectrumData[i][j] = new Float(spillover);
    errorData[i][j] = new Float(uncertainty);
    spectrumModel.setSpectrum();
  }

  /**
   *
   * @param i
   * @param j
   * @param fails
   */
  public void setFailsLinearityTest (int i, int j, boolean fails)
  {
    failsLinearityTest[i][j] = fails;
    spectrumModel.setSpectrum();
  }

  public void setFailsInterceptTest (int i, int j, boolean fails){
      failsInterceptTest[i][j] = fails;
      spectrumModel.setSpectrum();
  }
  
  public void reportMessage(String msg, boolean flag){
      if (!flag)
          showMessageDialog (msg);
      else {
          StringBuilder buf = new StringBuilder (msg);
          buf.append ("Click Yes to keep the FCS files (stored in home/tempfcs).");
          buf.append ("\nClick No to remove the FCS files");
          int ret = JOptionPane.showConfirmDialog(this, msg, "", JOptionPane.YES_NO_OPTION);
          if (ret == JOptionPane.OK_OPTION){
             showMessageDialog (" Try to rerun compensation algorithm from the tempfcs directory.") ;
             System.exit(1);
          }
          else {
             showMessageDialog ("  The FCS files will be deleted"); 
             controller.deleteTempFiles();
          }
      }
  }

  /**
   *
   * @param i
   * @param j
   * @param fails
   */
  public void setFailsMedianTest (int i, int j, boolean fails)
  {

    failsMedianTest[i][j] = fails;
    spectrumModel.setSpectrum();
  }

  public void spilloverNotSignificantTest (int i, int j, boolean fails){
     // System.out.println ("  Spillover not significant Test");
      failsSignificanceTest[i][j] = fails;
      spectrumModel.setSpectrum();
  }

  public void setFailsSignificanceTest (int i, int j, boolean fails)
  {
      //System.out.println ("set Fails Significance Test");
    failsSignificanceTest[i][j] = fails;
    spectrumModel.setSpectrum();
  }

//  public void passDiagnostics (Diagnostic.List[][]allDiagnostics) {
//      this.allDiagnostics = allDiagnostics;
//      diagnosticPanel.setDiagnostics (allDiagnostics);
//
//  }

  private void initFrameSize() {

      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      Dimension frameSize = getSize();
      if (frameSize.height>screenSize.height)
      {
        frameSize.height = screenSize.height;
      }
      if (frameSize.width>screenSize.width)
      {
        frameSize.width = screenSize.width;
      }
      setLocation(
        (screenSize.width-frameSize.width)/2,
        (screenSize.height-frameSize.height)/2);


  }

  /**
   *
   * @param fluorochrome
   * @param detector
   */
  public void initialize (String[] fluorochrome, String[] detector)
  {
//      Exception e = new Exception();
//      System.out.println ("<--------- Initialize -------->");
//      e.printStackTrace();
    try
    {
      this.detectorList = detector;
      this.controlList = fluorochrome;
      

/*changing the size of these arrays */
      failsLinearityTest = new boolean[controlList.length][detectorList.length+1];
      failsMedianTest = new boolean[controlList.length][detectorList.length+1];
      failsSignificanceTest = new boolean[controlList.length][detectorList.length+1];
      failsInterceptTest = new boolean[controlList.length][detectorList.length+1];

      sensitivityData = new Float[6][controlList.length+1];
      spectrumData = new Float[controlList.length][detectorList.length+1];
      for (int i=0; i < controlList.length; i++) {
          for (int j=0; j < detectorList.length; j++){
              if (i == j) spectrumData[i][j]= (float)1.0;
              else spectrumData[i][j]=(float)0.0;
          }
      }
      errorData = new Float[controlList.length][detectorList.length+1];

      TableColumnModel datacm = new DefaultTableColumnModel(){
          
		private static final long serialVersionUID = 1L;
		boolean first=true;
          public void addColumn (TableColumn tc){
              if (first){
                  first = false;
                  return;
              }
              tc.setMinWidth (130);
             tc.setHeaderRenderer(buttonRenderer);
              super.addColumn (tc);
          }
      };
      TableColumnModel rowcm = new DefaultTableColumnModel(){
			private static final long serialVersionUID = 1L;

          boolean first=true;
          public void addColumn (TableColumn tc){
              if (first){
                  tc.setMaxWidth (150);
                  tc.setHeaderRenderer (buttonRenderer);
                  super.addColumn(tc);
                  first = false;

              }
          }
      };

      spectrumModel = new SpectrumModel();
      spectrumModel.createRowHeaders();
      spectrumModel.setColumnHeaders (detectorList);
      spectrumTable = new JTable (spectrumModel, datacm);
      spectrumTable.setModel(spectrumModel);
      spectrumTable.setDefaultRenderer(Float.class, spectrumRenderer);
      spectrumTable.setGridColor (Color.LIGHT_GRAY);
      spectrumTable.setSelectionBackground (selectionColor);
      spectrumTable.setSelectionForeground (Color.RED);
      

      spectrumModel.setSpectrum();

      rowTable = new JTable (spectrumModel, rowcm);

      rowTable.setMaximumSize (new Dimension (150, 200));
      rowTable.setColumnModel (rowcm);
      rowTable.createDefaultColumnsFromModel();
      rowTable.setRowSelectionAllowed (true);
      rowTable.setSelectionBackground (selectionColor);

      spectrumTable.createDefaultColumnsFromModel();
      spectrumTable.setSelectionModel(rowTable.getSelectionModel());
//      System.out.println ("Spectrum table col, row " + spectrumTable.getColumnSelectionAllowed() + spectrumTable.getRowSelectionAllowed());

      spectrumTable.setColumnSelectionAllowed(true);
      spectrumTable.setRowSelectionAllowed (true);
//    System.out.println ("Spectrum table col, row " + spectrumTable.getColumnSelectionAllowed() + spectrumTable.getRowSelectionAllowed());


      rowTable.getColumnModel().getColumn(0).setCellRenderer (buttonRenderer);
      JViewport jv = new JViewport();
      jv.setView (rowTable);
      jv.setPreferredSize(rowTable.getMaximumSize());
      spectrumTable.setAutoResizeMode (JTable.AUTO_RESIZE_OFF);
      FlowLayout flow = new FlowLayout(FlowLayout.LEFT, 2,0);
      northPanel = new JPanel (new BorderLayout());


      JLabel messageLabel = new JLabel ("Scatter and Fluorescence Plots are displayed here after the computation completes.  Click the table below to select pairs to display.");
      northPanel.add (messageLabel, BorderLayout.NORTH);
     
      JPanel plotPanel = new JPanel (new GridLayout (1,4));
      plotPanel.setAlignmentY(SwingConstants.TOP);
      plotPanel.setBorder (BorderFactory.createLineBorder (Color.LIGHT_GRAY,1));
      plotPanel.setBackground (Color.white);
      Dimension wdim = Toolkit.getDefaultToolkit().getScreenSize();
      int w = wdim.width / 4;
      if (w > 300)
          w = 300;
      else
          w -= w - (5 * 5);
      System.out.println (" dimensions " + wdim.width + "  "+ w);
          
      Dimension panelDim = new Dimension (w, w);
      scatterPlot = new ScatterPlot(panelDim.width, panelDim.height);
      scatterPlot.setBorder (BorderFactory.createLineBorder (Color.LIGHT_GRAY,1));
   
      plotPanel.add (scatterPlot);
      fluorescencePlot = new ScatterPlot(panelDim.width, panelDim.height);
      fluorescencePlot.setBorder (BorderFactory.createLineBorder (Color.LIGHT_GRAY,1));

      plotPanel.add (fluorescencePlot);

      dataPanel = new JPanel();
      dataPanel.setMaximumSize (scatterPlot.getMaximumSize());
      dataPanel.setPreferredSize(scatterPlot.getPreferredSize());
//      dataPanel.setMaximumSize (panelDim);
//      dataPanel.setPreferredSize(panelDim);
      dataPanel.setBorder (BorderFactory.createCompoundBorder (BorderFactory.createLineBorder(Color.LIGHT_GRAY,1),
              BorderFactory.createEmptyBorder (6,12,6,12)));

      plotPanel.add (dataPanel);
      Dimension diagDim = new Dimension (panelDim.width, panelDim.height);
      diagnosticPanel = new DiagnosticPanel (scatterPlot.getPanelDimension());
     // diagnosticPanel.setBorder (BorderFactory.createLineBorder (Color.LIGHT_GRAY,1));
      diagnosticPanel.setBorder (BorderFactory.createCompoundBorder (BorderFactory.createLineBorder(Color.LIGHT_GRAY,1),
              BorderFactory.createEmptyBorder (6,12,6,12)));

      plotPanel.add (diagnosticPanel);
      JScrollPane northscroll = new JScrollPane(plotPanel);

   //   northPanel.add (plotPanel, BorderLayout.CENTER);
    //  northPanel.add (northscroll, BorderLayout.CENTER);
      /**/
      
      /**/

      

      JScrollPane jsp = new JScrollPane (spectrumTable);
      jsp.setRowHeader (jv);
      mainPanel.add (jsp, BorderLayout.CENTER);
   //   setSize (1000, 700);
//      JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, plotPanel, mainPanel  );
      JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, northscroll, mainPanel  );

      //JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, northscroll, mainPanel  );
      getContentPane().add(splitPane, BorderLayout.CENTER);
      pack();
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      Dimension frameSize = getSize();

      initializeTableListeners();
      if (frameSize.height>screenSize.height)
      {
        frameSize.height = screenSize.height;
      }
      if (frameSize.width>screenSize.width)
      {
        frameSize.width = screenSize.width;
      }
      setLocation(
        (screenSize.width-frameSize.width)/2,
        (screenSize.height-frameSize.height)/2);
      setVisible(true);
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
      System.exit(1);
    }
  }
  /********************* a whole bunch of mouse click listener stuff to get the correct behavior on the tables **/
  private void fireCellSelectionEvent (int row, int col){
        for (CellSelectionListener lis: cellListeners){
            lis.cellSelected (row, col);
        }     
  }
  
  private void addCellSelectionListener (CellSelectionListener lis){
      cellListeners.add (lis);
  }
  private void fireRowSelectionEvent (int row){
      for (RowSelectionListener lis: rowListeners)
          lis.rowSelected(row);

  }
  private void addRowSelectionListener (RowSelectionListener lis){
        rowListeners.add (lis);
    }
  private void fireColumnSelectionEvent (int col){
         for (ColumnSelectionListener lis: colListeners){
             lis.columnSelected (col);
         }
  }
  private void addColumnSelectionListener (ColumnSelectionListener lis){
      colListeners.add (lis);
  }

  private void initializeTableListeners() {

      cellClick = new MouseAdapter() {
          public void mouseClicked (MouseEvent e){
              if (e.getSource() instanceof JTable){
                    JTable table = (JTable) e.getSource();
                    int row = table.rowAtPoint (e.getPoint());
                    int col = table.columnAtPoint(e.getPoint());
                    fireCellSelectionEvent (row, col);
                }

          }
      };
      spectrumTable.addMouseListener (cellClick);
      rowClick = new MouseAdapter() {
          public void mouseClicked (MouseEvent e){
                if (e.getSource() instanceof JTable){
                    JTable table = (JTable)e.getSource();
                    int row = table.rowAtPoint (e.getPoint());
                    fireRowSelectionEvent (row);
                }
          }
      };
      rowTable.addMouseListener (rowClick);
      colClick = new MouseAdapter() {
          public void mouseClicked (MouseEvent e){
              if (e.getSource() instanceof JTableHeader){
                  JTableHeader header = (JTableHeader)e.getSource();
                  int col = header.columnAtPoint (e.getPoint());
                  fireColumnSelectionEvent (col);
              }
          }
      };
      spectrumTable.getTableHeader().addMouseListener (colClick);

      rowLis = new RowSelectionListener () {
          public void rowSelected (int row){
              spectrumTable.clearSelection();
              rowTable.clearSelection();
              rowTable.setRowSelectionAllowed (true);
              spectrumTable.setRowSelectionAllowed (true);
              spectrumTable.setColumnSelectionAllowed (false);
              spectrumTable.setRowSelectionInterval(row, row);
              rowTable.setRowSelectionInterval (row, row);
              diagnosticPanel.clearDiagnosticList();
          
              if (row >5){
                  int drow = (row-6)/2;
                  Diagnostic.List[] diagnostics = compensation2.getDiagnosticsForRow(drow);
//                  System.out.println ("  Diagnostics! ");
                  if (diagnostics != null  ){
                      for (int j=0; j < diagnostics.length ; j++ ){
                          if (diagnostics[j] != null){
                               Diagnostic.List list = diagnostics[j];
                               if (list !=null){
                                   diagnosticPanel.appendDiagnosticList(list);
//                                  for (Diagnostic dg: list){
//                                      System.out.println (dg.color() + ", "+ dg.detector + ", "+ dg.reagent + ", "+ dg.importance);
//
//                                  }
                               }
                             
                          }
                      }
                  }
              }
        
                repaint();
          }
      };
      addRowSelectionListener (rowLis);
      cellLis = new CellSelectionListener (){
            public void cellSelected (int row, int col){
                //rowtable.changeSelection(row, 0,false, false);
                rowTable.clearSelection();
                spectrumTable.clearSelection();
                spectrumTable.setColumnSelectionAllowed(true);
                spectrumTable.setRowSelectionAllowed (true);
                spectrumTable.setRowSelectionInterval (row, row);
                spectrumTable.setColumnSelectionInterval (col, col);
                diagnosticPanel.clearDiagnosticList();
                
                if (row >5){
                    int drow = (row-6)/2;
                    displayFCSData (drow, col);

                Diagnostic.List list = compensation2.getDiagnosticsCell(drow, col);

                if (list != null)
                diagnosticPanel.addSimpleList (list);
                repaint();
                }

            }
        };
        addCellSelectionListener (cellLis);

        colLis = new ColumnSelectionListener() {
            public void columnSelected (int col){
                buttonRenderer.setSelected(true);
                rowTable.clearSelection();
                rowTable.setColumnSelectionAllowed(false);

                spectrumTable.clearSelection();
                spectrumTable.setColumnSelectionAllowed (true);
                spectrumTable.setRowSelectionAllowed (false);
                spectrumTable.setColumnSelectionInterval(col, col);
                diagnosticPanel.clearDiagnosticList();
                Diagnostic.List[] diagnostics = compensation2.getDiagnosticsForColumn (col);
                if (diagnostics != null)
                    diagnosticPanel.addDiagnosticsToList2(diagnostics);
//                if (col<0)
//                    return;
              

                repaint();
                buttonRenderer.setSelected (false);
            }
        };
        addColumnSelectionListener (colLis);

  }

  /**
   * Data is in the Y Arrays in Stained Control.  It is log FCS vs log SSC.  It
   * is already in natural log ln.  if gate[] is Gate.Range, the data values are
   * undefined so ignore those.
   * Fluorescence plot is selected detector (col in table) vs primary detector (row).  Data
   * are in the X[R[w] and X[col] arrays and row is the horizontal axis. This is using the
   * Logicle scale with T=1<<18 and W=.5.
   * Dots in both plots should be colored according to gate[].gate[] ==0 is the final data.
   * @param row
   * @param col
   */
  private void displayFCSData (int row, int col){
      
      //have to map the row to the StainedControl

      if (row >= compensation2.stainedControl.length || compensation2.stainedControl[row] == null){
          String msg = "No data was collected on this detector.  ";
          JOptionPane.showMessageDialog (this, msg,"No Data Collected ", JOptionPane.WARNING_MESSAGE);
          return;
      }

          
      
     if (row >-1 && col > -1) {
         try{

          float[] fcsx = compensation2.getDataY(row,0);
          float[] fcsy = compensation2.getDataY (row, 1);
          int size = 2000;
          //should this really be the primary detector, row, primary, not row, row?
          int primary = compensation2.stainedControl[row].getPrimaryDetector();
          float[] xf = compensation2.getDataX(row, primary);
//          float[] xf = compensation2.getDataX (row, row);
          float[] yf = compensation2.getDataX(row, col);
          float[][] data = new float[2][size];
          int[] gates = compensation2.getGates (row, col);

          int[] statistics = compensation2.getStatistics (row, col);
          displayStatistics (statistics, row, col );
//          firstTime = false;
          int max=0;
          if (fcsx != null)
             max = fcsx.length;
          //fluorescent data needs another value for its gate info
          float[][] dataf = new float[2][size];
          int[] mygates = new int[size];
          Random random = new Random(3234567);
    
          for (int i=0; i < size; i++){
              int idx = random.nextInt(max);
//              System.out.println (max + "  "+idx );
              data[0][i] = fcsx[idx];
              data[1][i] = fcsy[idx];
              dataf[0][i] = xf[idx];
              dataf[1][i] = yf[idx];
              mygates[i] = gates[idx];
          }
          int T =1<< 18;
          double W=0.5;
          double M=4.5;
          double A=0;
      // W=.5, M=4.5, A=0
      //public Logicle (double T, double W, double M, double A)
          scatterPlot.plotScatterData (data[0], data[1], mygates ) ; // minx, maxy, miny, maxy);
          scatterPlot.setLabels ("FSC", "SSC");


          fluorescencePlot.plotData (dataf[0], dataf[1], mygates, T, W, M, A);
          fluorescencePlot.setLabels (controlList[row], detectorList[col]);

         } catch (Exception e0) {
             System.out.println ("display FCS data exception (" + row + ", " + col + ") "+e0.getMessage());
             e0.printStackTrace();
         }
     }

  }

  /**0 = n events
   * 1 = scatter gated
   * 2 = autofluorescence
   * 3 = spectrum gates
   * 4 = final
   * these are really ints.
   * selected detector is the column
   * primary detector is the row.
   **/
  private void displayStatistics (int[] statistics, int row, int col){
     

     Color[] gateColors = colorScheme.getColorScheme();
     String[] labels;
//     Insets insetleft = new Insets (0,8,6,8);
//     Insets insetright = new Insets (6,12,6, 12);
     if (compensation2.stainedControl[row] == null){
         String msg = "No data collected on this detector";
          JOptionPane.showMessageDialog (this, msg,"No Data ", JOptionPane.WARNING_MESSAGE);
         return;
     }
     boolean firstTime = false;
     int primary = compensation2.getPrimaryDetector(row);
    // int primary = compensation2.stainedControl[row].getPrimaryDetector();

     if (statPanel == null) {
    	   firstTime = true;
         labels = compensation2.getStatLabels();
         if (labels.length != statistics.length){
             System.out.println (" labels and statistics are unevenly matcheed .");
         }
        statPanel = new JPanel();

        GridBagLayout bag = new GridBagLayout();
        statPanel.setLayout (bag);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridwidth = 2;
        constraints.insets = new Insets(4,4,4,8);
        constraints.anchor = GridBagConstraints.WEST;
        JLabel reagent1 = new JLabel ("Reagent: ");
        bag.setConstraints (reagent1, constraints);
        statPanel.add (reagent1);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        JLabel reagent2 = new JLabel(controlList[row]);

//        JLabel reagent2 = new JLabel(detectorList[col]);
        bag.setConstraints (reagent2, constraints);
        statPanel.add (reagent2);

        JLabel detector1 = new JLabel ("Primary detector: ");
        constraints.gridwidth=2;
      //  constraints.insets = new Insets (0,8,6,8);
        constraints.anchor = GridBagConstraints.WEST;
        bag.setConstraints (detector1, constraints);

        statPanel.add (detector1);

        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor =GridBagConstraints.WEST;
        String primarylabel;
        if (primary < 0)
            primarylabel="";
        else
            primarylabel = detectorList[primary];
        JLabel detector2 = new JLabel (primarylabel);
        
//        System.out.println ("  primary detector?  "+  detectorList[primary]+" " + controlList[primary]+ "  " +prim2 );
        bag.setConstraints (detector2, constraints);
        statPanel.add (detector2);
        
        JLabel spillOver = new JLabel ("Spillover Detector:");
        constraints.gridwidth=2;
        constraints.anchor =GridBagConstraints.WEST;
    //   constraints.insets = new Insets (0,8,6,8);
        bag.setConstraints (spillOver, constraints);
        statPanel.add (spillOver);

        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        JLabel spillOverDetector = new JLabel (detectorList[col]);
        bag.setConstraints (spillOverDetector, constraints);
        statPanel.add (spillOverDetector);
     
        
        constraints.gridwidth=3;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        JLabel spillOverCoef = new JLabel (" Spillover Coefficient:");
        bag.setConstraints (spillOverCoef, constraints);
        statPanel.add (spillOverCoef);
        int r = row*2+6;
        Object v1 = spectrumTable.getValueAt (r, col);
        String sv1 = decimalFormat.format (v1);
       
        v1 = spectrumTable.getValueAt (r+1, col);
        
        String sv2 = percentFormat.format (v1);
        String allsv2 = new String ("+/--"+sv2);
       
      
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.gridwidth = GridBagConstraints.RELATIVE;
        JLabel error = new JLabel (sv1);
       // error.setHorizontalAlignment (SwingConstants.RIGHT);
        bag.setConstraints (error, constraints);
        statPanel.add (error);

        constraints.anchor = GridBagConstraints.CENTER;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
//        JLabel percentLabel = new JLabel ("+/--"+sv2);
//          System.out.println ("Error " + sv1 + "  "+ sv2 + "  " + allsv2);
        JLabel percentLabel = new JLabel (allsv2);
       // percentLabel.setHorizontalAlignment (SwingConstants.RIGHT);
        bag.setConstraints (percentLabel, constraints);
        statPanel.add (percentLabel);


     //   constraints.insets = new Insets(1,4, 1, 0);
        constraints.gridwidth = 3;
        constraints.gridheight = statistics.length;
        
        constraints.ipadx=6;
        constraints.ipady=3;
        constraints.insets = new Insets(1,4, 1, 0);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        statLabels = new JLabel[2][statistics.length];
        /*
         * The statistics array contain values for the total number of events, but
         * this is not a checkbox and has no color effect.  Hence I skip that one.  So
         * there are 6 statistics, but only 5 checkboxes and five colors.
         */
        for (int i=0; i < statistics.length; i++){
//            constraints.fill = GridBagConstraints.NONE;
            constraints.weightx=1.0;
            constraints.gridwidth = 3;
            if (i > 0){
                JCheckBox cb = new JCheckBox (labels[i]);
                if ( i < gateColors.length)
                    cb.setForeground (gateColors[5-i]);
                else
                    cb.setForeground (Color.BLACK);

                    cb.addItemListener (fluorescencePlot);
                    cb.addItemListener (scatterPlot);
                    cb.setSelected (true);
                    bag.setConstraints (cb,constraints);
                    statPanel.add (cb);
                }
            else {

                JLabel label = new JLabel (labels[i]);
                label.setForeground (Color.BLACK);
                bag.setConstraints (label, constraints);
                statPanel.add (label);
            }

            statLabels[0][i] = new JLabel ("" +statistics[i]);

            statLabels[0][i].setHorizontalAlignment(SwingConstants.RIGHT);
            bag.setConstraints (statLabels[0][i], constraints);
            statPanel.add (statLabels[0][i]);

            double percent = (double)statistics[i]/(double) statistics[0] ;

            String spercent = percentFormat.format (percent);
//            System.out.println (statLabels[0][i] + ": " + percent + "  "+ spercent);
            statLabels[1][i] = new JLabel (spercent);
//            statLabels[1][i] = new JLabel (new Float (percent).toString()+"%");
            statLabels[1][i].setHorizontalAlignment(SwingConstants.RIGHT);

            constraints.gridwidth = GridBagConstraints.REMAINDER;
            bag.setConstraints (statLabels[1][i], constraints);
            statPanel.add (statLabels[1][i]);
        }

         dataPanel.setLayout (new BorderLayout ());
         dataPanel.add (statPanel, BorderLayout.CENTER);
     }
//     else{  //update the values based on what I checked on.
//System.out.println ("************************** "+ row + ", "+ col);
    if ( statPanel.getComponentCount()> 8) {
         JLabel label = (JLabel) statPanel.getComponent (1);
//         System.out.println (" label 1 = "+ label.getText() + "  new "+ controlList[row]);
         label.setText (controlList[row]);
         label = (JLabel) statPanel.getComponent (3);
    
         primary = compensation2.getPrimaryDetector(row);
         String primarylabel;
         if (primary < 0)
             primarylabel="";
         else
             primarylabel=detectorList[primary];
         label.setText (primarylabel);
         label = (JLabel) statPanel.getComponent (5);
//     System.out.println (" label 5 = "+ label.getText() + "   new "+ detectorList[col]);
         label.setText (detectorList[col]);

         label = (JLabel) statPanel.getComponent(7);
         int r = row *2 +6;
         Object obj = spectrumTable.getValueAt (r, col);
         String ss = decimalFormat.format (obj);
         label.setText (ss);

         label = (JLabel) statPanel.getComponent(8);
         obj = spectrumTable.getValueAt (r+1, col);
//         System.out.println (obj.getClass().getName() + " "+ obj.toString());
         ss = percentFormat.format (obj);
         label.setText(ss);
    }
        
         //update the values
         for (int i=0; i < statistics.length ; i++){
             statLabels[0][i].setText(""+statistics[i]);
             double percent =  (double) statistics[i]/(double) statistics[0];
             String spercent = percentFormat.format (percent);
            
//             percent = Math.round(percent);
             statLabels[1][i].setText(spercent);

         }
         if (firstTime)
        	 dataPanel.validate();
 
//     }

  }
    


  private final class SpectrumModel
    extends AbstractTableModel
  {
		private static final long serialVersionUID = 1L;

      String[] headers;
      String[] rowHeaders;
    /**
     * getColumnCount
     * 
     * @return int
     */
    public int getColumnCount ()
    {
      return detectorList.length+2;
    }

    protected void createRowHeaders() {
        rowHeaders = new String[2*controlList.length+6];
        rowHeaders[0]= "photons";
        rowHeaders[2]= "tube";
        rowHeaders[4]= "background";
        for (int i=0; i < controlList.length; i++){
            rowHeaders[i*2+6]= controlList[i];
        }
        for (int i=1; i < rowHeaders.length; i+=2){
            rowHeaders[i] = "+-";
        }

    }

    public void setColumnHeaders (String[] detectors){
      headers = new String[detectors.length +2] ;
      headers[0]="Dye/Detectors";
      for (int i=1; i < detectors.length+1; i++){
          headers[i] = detectors[i-1];
      }
      headers[headers.length-1]= "Free Dye";
    }

    /**
     * getRowCount
     * 
     * @return int
     */
    public int getRowCount ()
    {
        
        int row = 2*controlList.length+6;
        
      return 2*controlList.length+6;
    }

    /**
     * getValueAt
     * 
     * @param rowIndex
     *          int
     * @param columnIndex
     *          int
     * @return Object
     */
    public Object getValueAt (int rowIndex, int columnIndex)
    {
      if (columnIndex==0)
      {
          return rowHeaders[rowIndex];

      }

      if (spectrumData==null)
        return null;
       
      if (rowIndex<6)
        return sensitivityData[rowIndex][columnIndex-1];
      else if (rowIndex%2==0)
        return spectrumData[rowIndex/2-3][columnIndex-1];
      else
        return errorData[rowIndex/2-3][columnIndex-1];
    }

    /**
     * Returns the most specific superclass for all the cell values in the
     * column.
     * 
     * @param columnIndex
     *          the index of the column
     * @return the common ancestor class of the object values in the model.
     * @todo Implement this javax.swing.table.TableModel method
     */
    public Class getColumnClass (int columnIndex)
    {
      if (columnIndex==0)
        return String.class;
      else
        return Float.class;
    }

    /**
     * Returns the name of the column at <code>columnIndex</code>.
     * 
     * @param columnIndex
     *          the index of the column
     * @return the name of the column
     * @todo Implement this javax.swing.table.TableModel method
     */
    public String getColumnName (int columnIndex)
    {

        if (columnIndex>=0 && columnIndex < headers.length){
            return headers[columnIndex];
        }
        else
            return"";

    }
    /**
     *
     */
    void setSpectrum ()
    {
      fireTableDataChanged();
    }
  };

  /**
   * This is only applied to the SpectralTable, not the row table.
   * Try to take off the -1.
   */
  class SpectrumCellRenderer extends DefaultTableCellRenderer
  {
    NumberFormat spectrumFormat = NumberFormat.getInstance();
    NumberFormat errorFormat = NumberFormat.getPercentInstance();
	private static final long serialVersionUID = 1L;

    SpectrumCellRenderer ()
    {
      setHorizontalAlignment(SwingConstants.RIGHT);
      if (spectrumFormat instanceof DecimalFormat)
        ((DecimalFormat)spectrumFormat).applyPattern("#.####");
      if (errorFormat instanceof DecimalFormat)
        ((DecimalFormat)errorFormat).applyPattern("##.##%");
    }

    /**
     *
     * @param table
     * @param value
     * @param isSelected
     * @param hasFocus
     * @param row
     * @param column
     * @return
     */
    public Component getTableCellRendererComponent  (JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {

     
      String text = "";
      Float f = (Float)value;
     
      if (value!=null && !f.isNaN()){
        if (row%2==0)
          text = spectrumFormat.format(f.doubleValue());
        else
          text = errorFormat.format(f.doubleValue());
      }
//      super.getTableCellRendererComponent(table, text, isSelected, hasFocus,
//        row, column);
      setValue (text);
 
      if (row>=6)
      {
          int irow = (row-6)/2;
    
          //these three values take off the -1. These must be taken off because
          //this is just the data table, not the rowtable + the datatable as with
          //getValueAt();
          setForeground (Color.black);
          if (compensation2.isSpillOverNotSignificant (irow, column)){
              setForeground (Color.LIGHT_GRAY);
          }
          
          Diagnostic.List diag = compensation2.getDiagnosticsCell(irow, column);
          if (diag != null && diag.size() > 0){
              setBackground (diag.get(0).color());
          }
          else
              setBackground (Color.white);
          
      }
      else {
          setBackground (Color.white);
      }

      if (isSelected)
          setBackground (selectionColor);
     
      return this;
    }
  }

  class ScatterPlotDialog extends JDialog {
		private static final long serialVersionUID = 1L;

      JPanel panel = new JPanel();
      ScatterPlotDialog (JFrame frame){
          super (frame, false);
          init();

          setBackground (Color.lightGray);
          setSize (1000, 500);
        //  setVisible (true);


      }
      private void init() {
          panel.setLayout(new FlowLayout(FlowLayout.LEFT, 2,0));
          panel.setOpaque (false);
         setContentPane (panel) ;

      }
      protected void addPlot (ScatterPlot plot){
          panel.add (plot);
      }
  }





   /**
     * ProgressListener listens to "progress" property
     * changes in the SwingWorkers that search and load
     * images.
     */
    class ProgressListener implements PropertyChangeListener {
        // prevent creation without providing a progress bar
        //private ProgressListener() {}

        ProgressListener(JProgressBar progressBar) {
            this.progressBar = progressBar;
            this.progressBar.setValue(0);
        }

        public void propertyChange(PropertyChangeEvent evt) {
            String strPropertyName = evt.getPropertyName();
            if ("progress".equals(strPropertyName)) {
                progressBar.setIndeterminate(false);
                int progress = (Integer)evt.getNewValue();
                progressBar.setValue(progress);
            }
        }

        private JProgressBar progressBar;
    }

  public static void main (String[] args)
  {
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run ()
      {
        try
        {
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
          new CompensationFrame("Compensation", null, null).setVisible(true);
        }
        catch (Exception e)
        {
          e.printStackTrace();
          System.exit(0);
        }
      }
    });
  }
}
