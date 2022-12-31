package chen.yyds.py;

import chen.yyds.py.impl.Const;
import chen.yyds.py.impl.ProjectServer;
import chen.yyds.py.impl.ProjectServerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import impl.*;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;


public class LogForm implements MouseListener {
    private static final com.intellij.openapi.diagnostic.Logger LOGGER =
            com.intellij.openapi.diagnostic.Logger.getInstance(LogForm.class);

    private JPanel panel1;
    private JTextArea textArea;
    private JScrollPane scrollPanel;
    private final Project mProject;

    private long lastClick = System.currentTimeMillis();

    @Override
    public void mouseClicked(MouseEvent e) {
        // 一秒內则算双击
        if (System.currentTimeMillis() - lastClick < 1000) {
            LOGGER.warn("Double Click clear log!");
            textArea.setText("");
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
                    textArea.append(content);
                    scrollPanel.getVerticalScrollBar().setValue(scrollPanel.getVerticalScrollBar().getMaximum());
                }
            });
        }

        @Override
        public void clear() {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    textArea.setText("");
                }
            });
        }
    };

    private void tryFetchLog() {
        try {
            byte[] cache = new byte[1024];
            ProjectServerImpl projectServer = (ProjectServerImpl) mProject.getService(ProjectServer.class);
            String deviceIp = projectServer.getProjectProperties(Const.PROP_KEY_DEBUG_DEVICE_IP);
            String projectName = projectServer.getProjectProperties(Const.PROP_KEY_PROJECT_NAME);
            EngineConnector.INSTANCE.setDeviceIp(deviceIp);
            callback.appendLog("当前调试工程:" + projectName + "\n");
            callback.appendLog("当前调试设备IP地址:" + deviceIp + "\n");
            callback.appendLog("正在努力尝试连接设备引擎并读取日志...\n");
            EngineConnector.INSTANCE.notifyCrawlLogcat();
            Socket logSocket = EngineConnector.INSTANCE.getClientLogSocket();
            callback.appendLog("成功连接设备!现在开始抓取运行日志" + "\n");
            InputStream stream = logSocket.getInputStream();
            while (true) {
                try {
                    int readSize = stream.read(cache);
                    if (readSize < 0) {
                        break;
                    }
                    String appendContent = new String(cache, 0, readSize, StandardCharsets.UTF_8);
                    callback.appendLog(appendContent);
                } catch (SocketException ignore) {
                    callback.appendLog("设备断开!");
                    break;
                }
            }
        } catch (ConnectException ignore) {
            callback.clear();
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    public LogForm(Project project, ToolWindow toolWindow) {
        this.mProject = project;
        new Thread(new Runnable() {
            @Override
            public void run() {
                // ApkBuilder.INSTANCE.test();
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
        this.textArea.addMouseListener(this);
    }

    public JPanel getContent() {
        return panel1;
    }
}
