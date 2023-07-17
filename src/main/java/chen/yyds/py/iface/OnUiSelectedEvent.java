package chen.yyds.py.iface;

import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.Collection;

public interface OnUiSelectedEvent {
    void onSelected(Collection<DefaultMutableTreeNode> nodes);
}
