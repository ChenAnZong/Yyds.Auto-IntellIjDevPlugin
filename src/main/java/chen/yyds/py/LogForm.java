package chen.yyds.py;

import chen.yyds.py.impl.ProjectServer;
import chen.yyds.py.impl.ProjectServerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBFont;
import impl.*;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;


public class LogForm implements MouseListener {
    private static final com.intellij.openapi.diagnostic.Logger LOGGER =
            com.intellij.openapi.diagnostic.Logger.getInstance(LogForm.class);

    private JPanel panel1;
    private JScrollPane scrollPanel;
    private JTextPane logPanel;
    private final Project mProject;

    ProjectServerImpl mProjectServer;

    private long lastClick = System.currentTimeMillis();

    @Override
    public void mouseClicked(MouseEvent e) {
        // 一秒內则算双击
        if (System.currentTimeMillis() - lastClick < 400) {
            LOGGER.warn("Double Click clear log!");
            logPanel.setText("");
        } else {
            lastClick = System.currentTimeMillis();
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    interface FetchCallback {
         void appendLog(String content);
         void clear();
    }

    FetchCallback callback = new FetchCallback(){
        @Override
        public void appendLog(String content) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    appendToPane(content);
                    scrollPanel.getVerticalScrollBar().setValue(scrollPanel.getVerticalScrollBar().getMaximum());
                }
            });
        }

        @Override
        public void clear() {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    logPanel.setText("");
                }
            });
        }
    };

    private void tryFetchLog() {
        try {
            while (true) {
                if (mProjectServer.logHasNext()) {
                    callback.appendLog( mProjectServer.nextLog() + "\n");
                }
            }
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    private void appendToPane(String msg) {
        StyleContext sc = StyleContext.getDefaultStyleContext();
        Color c;
        if (msg.contains("python.stdout")) {
            c = JBColor.CYAN;
        } else {
            c = JBColor.GRAY;
        }
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c);
        aset = sc.addAttribute(aset, StyleConstants.FontFamily, "Lucida Console");
        aset = sc.addAttribute(aset, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED);

        int len = logPanel.getDocument().getLength();
        logPanel.setCaretPosition(len);
        logPanel.setCharacterAttributes(aset, false);
        logPanel.replaceSelection(msg);
    }

    public LogForm(Project project, ToolWindow toolWindow) {
        this.mProject = project;
        this.mProjectServer = (ProjectServerImpl) mProject.getService(ProjectServer.class);
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    tryFetchLog();
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        this.logPanel.addMouseListener(this);
        this.logPanel.setFont(JBFont.regular());
    }

    public JPanel getContent() {
        return panel1;
    }
}
