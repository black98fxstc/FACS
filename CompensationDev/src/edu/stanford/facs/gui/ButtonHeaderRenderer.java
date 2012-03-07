/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.stanford.facs.gui;

import java.awt.Font;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 * $Id: ButtonHeaderRenderer.java,v 1.2 2011/04/12 01:03:56 beauheim Exp $
 * @author cate
 */
public class ButtonHeaderRenderer extends JButton implements
                                  TableCellRenderer {

    public static final int NONE = 0;
    public static final int ASCENDING = 1;
    public static final int DESCENDING = 2;
    private int pressedCol;
    
    private JTable mytable;
	private static final long serialVersionUID = 1L;


    /** Default constructor. */
    public ButtonHeaderRenderer() {
        pressedCol = -1;
        
        setMargin(new Insets(0,0,0,0));
        setFont(new Font(getFont().getFontName(),Font.PLAIN,11));
    }

    /** Implementation of TableCellRenderer. When the column index
        is the index of pressedCol, the button is marked "armed" and
        "pressed". */
    public java.awt.Component getTableCellRendererComponent( JTable  table,
                                       Object  value,
                                       boolean isSelected,
                                       boolean hasFocus,
                                       int row, int col ) {

//        System.out.println ("getTableCennRendererComponent "+ row + ", "+ col);
        this.mytable = table;
        setText(value==null?"":value.toString());
        boolean isPressed = (col==pressedCol);
        getModel().setPressed(isPressed);
        getModel().setArmed(isPressed);
        setToolTipText(value.toString());
//        System.out.println ("ButtonHeaderRender getTableCellRendererComponent");
        return this;
    }

    /** Sets the pressedCol to the specified column.
        @param col the column that needs to look pressed */
    public void setPressed(int col) {
        if (col < 0)
            return;
        else
            pressedCol = col;
    }

    /** Saves the state of sort of the column, defaulting to "ascending".
        @param col the column for which to save the state */
    public void setSelectedColumn(int col) {
        
        if (col >= 0) {
            System.out.println ("ButtonHeaderRenderer  "+ col);
//            mytable.setColumnSelectionAllowed(true);
//            mytable.setRowSelectionAllowed(false);
            mytable.setColumnSelectionInterval(col, col);
           
        }
                
    }




}
