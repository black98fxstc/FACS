/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.stanford.facs.gui;


import java.awt.*;
import java.awt.datatransfer.*;
import javax.swing.*;


public class BasicDnD extends JPanel {//implements ActionListener {
    private static JFrame frame;
	private static final long serialVersionUID = 1L;

    private JList list;


    public BasicDnD() {
        super(new BorderLayout());
        JPanel leftPanel = createVerticalBoxPanel();
        JPanel rightPanel = createVerticalBoxPanel();

        String[][] data = { {"reagent 1", "detector 1"}, {"reagent 2", "detector 2"},
                 {"reagent 3", "detector 3"}, {"reagent 4", "detector 4"} };
        JPanel detectorPanel = createDetectorPanel (4, 2, data);
//        leftPanel.add(detectorPanel, BorderLayout.CENTER);
//        leftPanel.setSize (400, 400);

        //Create a list model and a list.
        DefaultListModel listModel = new DefaultListModel();
        listModel.addElement("Martha Washington");
        listModel.addElement("Abigail Adams");
        listModel.addElement("Martha Randolph");
        listModel.addElement("Dolley Madison");
        listModel.addElement("Elizabeth Monroe");
        listModel.addElement("Louisa Adams");
        listModel.addElement("Emily Donelson");
        list = new JList(listModel);
        list.setVisibleRowCount(-1);
        list.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setDragEnabled(true);
        list.setTransferHandler(new TransferHandler() {

            
			private static final long serialVersionUID = 1L;

			public boolean canImport(TransferHandler.TransferSupport info) {
                // we only import Strings
                if (!info.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    return false;
                }

                JList.DropLocation dl = (JList.DropLocation)info.getDropLocation();
                if (dl.getIndex() == -1) {
                    return false;
                }
                return true;
            }

            public boolean importData(TransferHandler.TransferSupport info) {
                if (!info.isDrop()) {
                    return false;
                }

                // Check for String flavor
                if (!info.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    displayDropLocation("List doesn't accept a drop of this type.");
                    return false;
                }

                JList.DropLocation dl = (JList.DropLocation)info.getDropLocation();
                DefaultListModel listModel = (DefaultListModel)list.getModel();
                int index = dl.getIndex();
                boolean insert = dl.isInsert();
                // Get the current string under the drop.
                String value = (String)listModel.getElementAt(index);

                // Get the string that is being dropped.
                Transferable t = info.getTransferable();
                String data;
                try {
                    data = (String)t.getTransferData(DataFlavor.stringFlavor);
                }
                catch (Exception e) { return false; }

                // Display a dialog with the drop information.
                String dropValue = "\"" + data + "\" dropped ";
                if (dl.isInsert()) {
                    if (dl.getIndex() == 0) {
                        displayDropLocation(dropValue + "at beginning of list");
                    } else if (dl.getIndex() >= list.getModel().getSize()) {
                        displayDropLocation(dropValue + "at end of list");
                    } else {
                        String value1 = (String)list.getModel().getElementAt(dl.getIndex() - 1);
                        String value2 = (String)list.getModel().getElementAt(dl.getIndex());
                        displayDropLocation(dropValue + "between \"" + value1 + "\" and \"" + value2 + "\"");
                    }
                } else {
                    displayDropLocation(dropValue + "on top of " + "\"" + value + "\"");
                }

		
		        return false;
            }

            public int getSourceActions(JComponent c) {
                return COPY;
            }

            protected Transferable createTransferable(JComponent c) {
                JList list = (JList)c;
                Object[] values = list.getSelectedValues();

                StringBuffer buff = new StringBuffer();

                for (int i = 0; i < values.length; i++) {
                    Object val = values[i];
                    buff.append(val == null ? "" : val.toString());
                    if (i != values.length - 1) {
                        buff.append("\n");
                    }
                }
                return new StringSelection(buff.toString());
            }
        });
        list.setDropMode(DropMode.ON_OR_INSERT);

        JScrollPane listView = new JScrollPane(list);
        listView.setPreferredSize(new Dimension(100, 100));
        rightPanel.add(createPanelForComponent(listView, "FCS Files"));

        JScrollPane leftView = new JScrollPane (detectorPanel);
        leftView.setPreferredSize (new Dimension (540, 150));
        leftPanel.add (leftView, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                              leftPanel, rightPanel);
        splitPane.setOneTouchExpandable(true);

        add(splitPane, BorderLayout.CENTER);
//        add(toggleDnD, BorderLayout.PAGE_END);
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        LayoutManager layout = detectorPanel.getLayout();
        Dimension d = layout.preferredLayoutSize (detectorPanel);

        System.out.println (d.width + ", " + d.height);

    }

    protected JPanel createVerticalBoxPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.PAGE_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        return p;
    }

    public JPanel createDetectorPanel (int rows, int cols, String[][] data){
        JPanel panel = new JPanel (new GridLayout(rows+1, cols));
        panel.add (new JLabel ("Reagents"));
        panel.add (new JLabel ("Detectors"));
        panel.add (new JLabel ("FCS for Unstained "));
        panel.add (new JLabel ("FCS for Stained "));
        for (int i=0; i < rows; i++){
            JLabel label1 = new JLabel (data[i][0]);
            JLabel label2 = new JLabel (data[i][1]);
            JTextField tf1 = new JTextField (10);
            JTextField tf2 = new JTextField (10);
            panel.add (label1);
            panel.add (label2);
            panel.add (tf1);
            panel.add (tf2);
        }


        return panel;
    }

    public JPanel createPanelForComponent(JComponent comp,
                                          String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(comp, BorderLayout.CENTER);
        if (title != null) {
            panel.setBorder(BorderFactory.createTitledBorder(title));
        }
        return panel;
    }

    private void displayDropLocation(final String string) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JOptionPane.showMessageDialog(null, string);
            }
        });
    }

//    public void actionPerformed(ActionEvent e) {
//        if ("toggleDnD".equals(e.getActionCommand())) {
//            boolean toggle = toggleDnD.isSelected();
//            textArea.setDragEnabled(toggle);
//            textField.setDragEnabled(toggle);
//            list.setDragEnabled(toggle);
//            table.setDragEnabled(toggle);
////            tree.setDragEnabled(toggle);
////            colorChooser.setDragEnabled(toggle);
//        }
//    }

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
        //Create and set up the window.
        frame = new JFrame("BasicDnD");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create and set up the content pane.
        JComponent newContentPane = new BasicDnD();
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                //Turn off metal's use of bold fonts
	        UIManager.put("swing.boldMetal", Boolean.FALSE);
                createAndShowGUI();
            }
        });
    }
}

