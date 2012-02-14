/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.stanford.facs.gui.tryit;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.text.*;
/**
 * $Id: AutoWrapTest.java,v 1.1 2011/11/09 19:49:46 beauheim Exp $
 * @author cate
 */
public class AutoWrapTest {
    LineWrapCellRenderer linerenderer;

 public JComponent makeUI() {
    String[] columnNames = {"TextAreaCellRenderer"};
    Object[][] data = {
      {"1234 5678 9012 34567 89012345678 90"},
      {"ddddddddd dddddddddd dddddddd dddddddddd ddddd dddd ddddd ddddddx"},
      {"----- -------------- --------- ------- -----------0"},
      {"xxxxxxxxxx xxxxxxxxxxxxxx xxxxxxxxxxxxxxxxxxxx xxxxxx xxxxxxx|"},
      {">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>|"},
    };
    String[] listdata = {
      "1234567 890123456 7890123 4567890",
      "dddd ddddddd dddddd dddddddd ddddddddddddddd dddddddddd dddddddx",
      "------------------------- ----------------- ----0",
      "xxxxxxxxxx xxxxxxxxxx xxxxxxxx xxxxxxxxxx xxxxxxxxxxxxx xxxxxx|",
      ">>>>>> >>>>>> >>>>>>>>> >>>>>> >>>>>>>>> >>|"
    };
    TableModel model = new DefaultTableModel(data, columnNames) {
        public boolean isCellEditable(int row, int column) {
        return false;
      }
    };
    JTable table = new JTable(model) {
      //Override 
      public void doLayout() {
        TableColumn col = getColumnModel().getColumn(0);
        for(int row=0; row<getRowCount(); row++) {
          Component c = prepareRenderer(col.getCellRenderer(), row, 0);
          if(c instanceof JTextArea) {
            JTextArea a = (JTextArea)c;
            int h = getPreferredHeight(a) +
                    getIntercellSpacing().height;
            if(getRowHeight(row)!=h) setRowHeight(row, h);
          }
        }
        super.doLayout();
      }
      //http://tips4java.wordpress.com/2008/10/26/text-utilities/
      private int getPreferredHeight(JTextComponent c) {
        Insets insets = c.getInsets();
        View view = c.getUI().getRootView(c).getView(0);
        int preferredHeight = (int)view.getPreferredSpan(View.Y_AXIS);
        return preferredHeight + insets.top + insets.bottom;
      }
    };
    table.setEnabled(false);
    table.setShowGrid(false);
    table.setTableHeader(null);
    table.getColumnModel().getColumn(0).setCellRenderer(
      new TextAreaCellRenderer());
    JScrollPane sp = new JScrollPane(table);
    sp.setVerticalScrollBarPolicy(
      ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    JPanel p = new JPanel(new BorderLayout());

    p.add(sp, BorderLayout.CENTER);

    JList list = new JList (listdata);

    list.setCellRenderer(new LineWrapCellRenderer());
    p.add (list, (BorderLayout.EAST));
    return p;
  }
  public static void main(String[] args) {
    EventQueue.invokeLater(new Runnable() {
      //Override 
      public void run() { createAndShowGUI(); }
    });
  }
  public static void createAndShowGUI() {
    JFrame f = new JFrame();
    f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    f.getContentPane().add(new AutoWrapTest().makeUI());
    f.setSize(200, 200);
    f.setLocationRelativeTo(null);
    f.setVisible(true);
  }
}
class TextAreaCellRenderer extends JTextArea implements TableCellRenderer {
  private final Color evenColor = new Color(230, 240, 255);
  public TextAreaCellRenderer() {
    super();
    setLineWrap(true);
    setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
  }
  //Override 
  public Component getTableCellRendererComponent(
    JTable table, Object value, boolean isSelected,
    boolean hasFocus, int row, int column) {
    if(isSelected) {
      setForeground(table.getSelectionForeground());
      setBackground(table.getSelectionBackground());
    } else {
      setForeground(table.getForeground());
      setBackground(table.getBackground());
      setBackground((row%2==0)?evenColor:getBackground());
    }
    setFont(table.getFont());
    setText((value ==null) ? "" : value.toString());
    return this;
  }

}

class LineWrapCellRenderer extends JTextArea implements ListCellRenderer {
    private final Color evenColor = new Color(230, 240, 255);
    public LineWrapCellRenderer() {
        super();
        setLineWrap(true);
        setWrapStyleWord (true);
        setOpaque (true);

    }
    public Component getListCellRendererComponent (JList jlist, Object o, int index, 
                                                   boolean isSelected, boolean hasFocus) {
        System.out.println (index + " Value of cell " + o);
        setBackground((index%2==0)?evenColor:Color.WHITE);
        if (isSelected)
            setBackground (Color.YELLOW);
        
        setText (o.toString());
        System.out.println ("wrapping policy? "+ getLineWrap() + " "+ getWrapStyleWord());
        return this;
    }

}
