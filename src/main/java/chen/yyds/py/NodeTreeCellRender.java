package chen.yyds.py;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;


public class NodeTreeCellRender extends DefaultTreeCellRenderer {

    private Logger LOGGER = Logger.getInstance(
            NodeTreeCellRender.class
    );

    private String filterString;

    public void setFilterString(String f) {
        filterString = f;
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean sel, boolean exp, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, exp, leaf, row, hasFocus);


        if (((DefaultMutableTreeNode) value).getUserObject() instanceof NodeObject) {
            // Assuming you have a tree of Strings
            NodeObject node = (NodeObject) ((DefaultMutableTreeNode) value).getUserObject();

//            if (filterString != null && filterString.length() >= 2) {
//                if (!node.toString().contains(filterString)) {
//                    setPreferredSize(new Dimension( 0, 0));
//                    setVisible(false);
//                    setBackground(Color.lightGray);
//                } else {
//                    LOGGER.warn("Show__:" + node);
//                }
//            }

            // If the node is a leaf and ends with "xxx"
            if (node.getCls().contains("Layout")) {
                // Paint the node in blue
                setForeground(UIUtil.getTextFieldForeground());
            } else if (node.getCls().endsWith("TextView")) {
                setForeground(JBColor.CYAN);
            } else if (node.getCls().contains("Button")) {
                setBackground(JBColor.PINK);
            }

            if (leaf) {
                setBackground(JBColor.PINK);
            }
        }

        return this;
    }
}
