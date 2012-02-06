Notes for Compensation Users

What the program expects:
    1.  The DiVa xml file.
    2.  The FCS files that are described in the xml file.
    3.  If the DiVa file contains controls that you want to use, the program
        will pick them up.  There will be pop-up that asks if you want to use
        the controls.  If not, see #4, about a mapping file.
    4.  If the Diva file does not contain controls or you don't want to use the
         DiVa controls, you need to map the names of
        the fcs files that serve as the stained and unstained controls for the
        detectors. You can do this interactively by dragging the file names from
        the right side to the appropriate detector shown in the detector table window.
        Please note that when you are using beads for the positive and negative
        controls, do not enter an Unstained Control fcs file.  The Unstained Control file
        as designated by Diva is not used.
    5.  Alternatively you can create a simple, comma-separated mapping file and
        load this file by selecting the browse button on the detector table.

     The mapping file is a comma-separated text file.  The first column is the name
of the detector.  The second column is optional for the antibody name.  The third
column is the optional name of the unstained control file (fcs).  The fourth column is the
name of the stained control file (fcs).  The name of the fcs files are in
reference to the working directory that defaults to the directory the DiVa file
was found.  You can change the working directory by selecting the browse button to
select your data folder.  An example of the mapping file is shown below
(no antibodies are listed):

FITC-A,,7-10.fcs,7-1.fcs
Pacific Blue-A,,7-10.fcs,7-7.fcs
Pacific Orange-A,,7-10.fcs,7-8.fcs
Qdot 605-A,,7-10.fcs,7-6.fcs
APC-Cy7-A,,7-10.fcs,7-5.fcs
PE-A,,7-10.fcs,7-2.fcs
PE-Cy5-A,,7-10.fcs,7-3.fcs
PE-Cy55-A,,7-10.fcs,7-4.fcs

    6.  Click the run button to process with the calculation.
    7.  The program will create a matrix and write it to a file when you click the button "Save Matrix".  
    The matrix is in the form that is FlowJo friendly.  Both the MAC and PC matrices are created and
     saved.  
 

To open the compensation program, click on the application icon.