package chen.yyds.py;

import chen.yyds.py.impl.ProjectServer;
import chen.yyds.py.impl.ProjectServerImpl;
import chen.yyds.py.status.YyConnectWidget;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBFont;
import impl.*;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

    private boolean isEndNewLine = false;
    private void tryFetchLog() {
        try {
            while (true) {
                if (mProjectServer.logHasNext()) {
                    String log = mProjectServer.nextLog();
                    log = log.replace("\n\n", "\n");
                    if (!isEndNewLine && !log.isEmpty()) {
                        log = "\n" + log;
                    }
                    if (log.endsWith("\n")) {
                        isEndNewLine = true;
                    }
                    callback.appendLog(log);
                }
            }
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    private void appendToPane(String msg) {
        StyleContext sc = StyleContext.getDefaultStyleContext();
        Color c;

        if (msg.contains("err:") || msg.contains("Error") || msg.contains("Exception")) {
            c = JBColor.RED;
            msg = msg.substring(4);
        } else if(msg.startsWith("out:")   ) {
            c = JBColor.foreground();
            msg = msg.substring(4);
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
        JBPopupMenu popupMenu = new JBPopupMenu();
        JBMenuItem menuItem = new JBMenuItem("清空日志");
        popupMenu.add(menuItem);
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logPanel.setText("");
            }
        });

        JBMenuItem copyItem = new JBMenuItem("复制全部");
        popupMenu.add(copyItem);
        copyItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Toolkit.getDefaultToolkit()
                        .getSystemClipboard()
                        .setContents(
                                new StringSelection(logPanel.getText()),
                                null
                        );
            }
        });

        this.logPanel.setComponentPopupMenu(popupMenu);

        // 显示链接状态
        StatusBar sb =  WindowManager.getInstance().getStatusBar(project);
        sb.addWidget(new YyConnectWidget(project, mProjectServer), new Disposable(){
            @Override
            public void dispose() {

            }
        });
    }

    public JPanel getContent() {
        return panel1;
    }
}
