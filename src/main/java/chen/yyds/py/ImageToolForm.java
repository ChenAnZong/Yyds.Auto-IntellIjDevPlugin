package chen.yyds.py;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import chen.yyds.py.impl.ProjectServer;
import chen.yyds.py.impl.ProjectServerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBFont;

import java.awt.*;
import java.awt.event.*;
import java.util.Collection;

public class ImageToolForm {
    private static final com.intellij.openapi.diagnostic.Logger LOGGER =
            com.intellij.openapi.diagnostic.Logger.getInstance(ImageToolForm.class);
    private final Project mProject;
    ProjectServerImpl projectServer;
    int panelMargin = 10;
    // 三个控件相关
    JBTextArea uiMsgLabel;
    JBScrollPane uiMsgScrollPanel;
    JTree uiDumpTree;
    JBScrollPane uiTreeScrollPanel;

    public String fetchScreenShot() {
        projectServer = (ProjectServerImpl)mProject.getService(ProjectServer.class);
        if (projectServer == null || !projectServer.isClientConnectOk()) {
            ProjectServerImpl.showNotification("连接调试设备失败!" + projectServer);
            return null;
        }
        String imagePath = projectServer.getScreenShot();
        return imagePath;
    }

    public String fetchUiaDump() {
        projectServer = (ProjectServerImpl)mProject.getService(ProjectServer.class);
        if (projectServer == null || !projectServer.isClientConnectOk()) {
            ProjectServerImpl.showNotification("连接调试设备失败!" + projectServer);
            return null;
        }
        String xmlPath = projectServer.getUiaDump();
        return xmlPath;
    }

    public String fetchForeground() {
        projectServer = (ProjectServerImpl)mProject.getService(ProjectServer.class);
        if (projectServer == null || !projectServer.isClientConnectOk()) {
            ProjectServerImpl.showNotification("连接调试设备失败!" + projectServer);
            return null;
        }
        String xmlPath = projectServer.getForeground();
        return xmlPath;
    }

    interface OnScreenRectSelectedEvent {
        void onSelected(Image image, String desc);
    }

    interface OnUiSelectedEvent {
        void onSelected(Collection<DefaultMutableTreeNode> nodes);
    }

    public ImageToolForm(Project project, ToolWindow toolWindow) {
        this.mProject = project;
        panel1.setLayout(null);
        panel1.setBackground(JBColor.background());
        toolWindow.setAutoHide(false);
        JLabel label = new JLabel();
        label.setText("@_@");
        label.setFont(JBFont.h4());
        label.setSize(130, 20);
        label.setVisible(true);
        label.setForeground(JBColor.foreground());
        this.projectServer = (ProjectServerImpl)mProject.getService(ProjectServer.class);
        JLabel imgLabel =  new JLabel("", null, JLabel.LEFT);
        imgLabel.setSize(130, 130);
        imgLabel.setBackground(JBColor.green);
        imgLabel.setForeground(JBColor.green);
        imgLabel.setBorder(new LineBorder(JBColor.green, 2));

        JButton buttonLoadScreen = new JButton("载入截图");
        //buttonLoadScreen.setBackground(new Color(69,73,74));
        buttonLoadScreen.setForeground(JBColor.foreground());
        buttonLoadScreen.setVisible(true);
        buttonLoadScreen.setSize(130, 40);


        JButton buttonLoadUiaDump = new JButton("载入控件信息");
        //buttonLoadUiaDump.setBackground(new Color(69,73,74));
        buttonLoadUiaDump.setForeground(JBColor.foreground());
        buttonLoadUiaDump.setVisible(true);
        buttonLoadUiaDump.setSize(130, 40);


        JButton button = new JButton("保存区域");
        //button.setBackground(new Color(69,73,74));
        button.setForeground(JBColor.foreground());
        button.setVisible(true);
        button.setSize(130, 40);
        // 图片选中裁剪回调
        OnScreenRectSelectedEvent onScreenRectSelectedEvent = new OnScreenRectSelectedEvent() {
            @Override
            public void onSelected(Image image, String desc) {
                imgLabel.setText(null);
                imgLabel.setIcon(new ImageIcon(image));
                imgLabel.invalidate();
                label.setText(desc);
                label.invalidate();
            }
        };
        // 控件选中回调
        OnUiSelectedEvent onUiSelectedEvent = new OnUiSelectedEvent() {
            @Override
            public void onSelected(Collection<DefaultMutableTreeNode> nodes) {
                    if (uiDumpTree == null) return;
                    uiDumpTree.collapsePath(new TreePath(uiDumpTree.getModel().getRoot()));
                    for (DefaultMutableTreeNode n:nodes) {
                        uiDumpTree.setExpandsSelectedPaths(true);
                        uiDumpTree.setScrollsOnExpand(true);
                        TreePath tp = new TreePath(n.getPath());
                        uiDumpTree.expandPath(tp);
                        uiDumpTree.setSelectionPath(tp);
                        uiDumpTree.invalidate();
                        uiDumpTree.requestFocus();
                        LOGGER.warn("OnUiSelectedEvent Select:" + nodes.size() + " Path:" + tp);
                    }
            }
        };
        ScreenPanel screenShotPanel = new ScreenPanel(300, 450, onScreenRectSelectedEvent, onUiSelectedEvent);
        LOGGER.warn("ScreenPanelA w:" + screenShotPanel.getWidth() + "  h:" + screenShotPanel.getHeight());
        screenShotPanel.setLocation(10, panelMargin);
        screenShotPanel.setVisible(true);

        JBTextField textField = new JBTextField();
        textField.setSize(100, 30);


        panel1.add(screenShotPanel);
        panel1.add(label);
        panel1.add(imgLabel);
        panel1.add(textField);
        panel1.add(buttonLoadScreen);
        panel1.add(buttonLoadUiaDump);
        panel1.add(button);
        panel1.addComponentListener(new ComponentListener() {
            int h = 0;
            @Override
            public void componentResized(ComponentEvent e) {
                LOGGER.warn("componentResized Resized w:" + toolWindow.getComponent().getWidth() + "  h:" + toolWindow.getComponent().getHeight() + " panel width:" + panel1.getWidth());
                screenShotPanel.setScreenSize(panel1.getWidth(), panel1.getHeight() - 180);

                LOGGER.warn("Screen  w:" + screenShotPanel.getWidth() + " " + screenShotPanel.getHeight());
                this.h = panel1.getHeight();

                int underY = panelMargin + screenShotPanel.getHeight();
                // 第一排 信息标签
                label.setBounds(panelMargin, underY, screenShotPanel.getWidth(),30);  // 图像信息标签
                // toolWindow.getComponent().setSize(screenShotPanel.getWidth() + panelMargin*2, this.h);
                // ((ToolWindowEx)toolWindow).stretchWidth(toolWindow.getComponent().getWidth());
                // 第二排 图像左, 输入右
                underY += label.getHeight() + 5;

                imgLabel.setLocation(panelMargin, underY); // 左
                int imageRaminRightSpace = screenShotPanel.getWidth() - imgLabel.getWidth() - panelMargin;
                textField.setBounds(imgLabel.getWidth() + panelMargin + 10 , underY, imageRaminRightSpace - panelMargin ,30);
                underY += textField.getHeight() + 5;
                int buttonWidth = (imageRaminRightSpace - panelMargin) / 2;
                button.setBounds(imgLabel.getWidth() + panelMargin * 2, underY, buttonWidth, 40);
                buttonLoadUiaDump.setBounds(imgLabel.getWidth() + panelMargin * 2, underY + button.getHeight() + panelMargin, imageRaminRightSpace - panelMargin, 40);
                buttonLoadScreen.setBounds(imgLabel.getWidth() + button.getWidth() + panelMargin*2 , underY, buttonWidth, 40);

                if (uiDumpTree != null && uiTreeScrollPanel != null && uiMsgScrollPanel != null) {
                    int uiLabelHeight = panel1.getHeight() - screenShotPanel.getHeight() - panelMargin;
                    // 测试控件列表排版
                    uiTreeScrollPanel.setBounds(screenShotPanel.getWidth() + panelMargin, panelMargin,
                            panel1.getWidth() - screenShotPanel.getWidth() - panelMargin,
                            panel1.getHeight() - panelMargin - uiLabelHeight);
                    uiDumpTree.setBounds(0, 0, uiTreeScrollPanel.getWidth() - 20, uiTreeScrollPanel.getHeight() - 20);
                    
                    // 控件信息显示在上面两个窗口下面
                    uiMsgScrollPanel.setBounds(uiTreeScrollPanel.getX(), uiTreeScrollPanel.getHeight() + panelMargin,
                            uiTreeScrollPanel.getWidth() ,
                            uiLabelHeight);
                    uiMsgLabel.setBounds(0, 0, uiMsgScrollPanel.getWidth(), uiMsgScrollPanel.getHeight());
                }
            }

            @Override
            public void componentMoved(ComponentEvent e) {

            }

            @Override
            public void componentShown(ComponentEvent e) {

            }

            @Override
            public void componentHidden(ComponentEvent e) {

            }
        });

        button.setLocation(560, 100);
        buttonLoadScreen.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String fetchScreenShot = fetchScreenShot();
                if (fetchScreenShot != null) {
                    screenShotPanel.resetDrawImage(fetchScreenShot);
                } else {
                    ProjectServerImpl.showNotification("截图失败，请联系开发者处理!");
                    return;
                }
                // 要加载两次图片，尝试调整两次！
                panel1.setSize(screenShotPanel.getWidth() + panelMargin, panel1.getHeight()+1);
                panel1.setSize(screenShotPanel.getWidth() + panelMargin, panel1.getHeight()-1);
                // 如果只加载截图，防止图片对不上控件，那就清空当前控件查找列表
                if (e != null && uiDumpTree != null) {
                    uiDumpTree.removeAll();
                    HierarchyParser.INSTANCE.clearCacheList();
                }
            }
        });
        buttonLoadUiaDump.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 节点窗口 节点窗口 节点窗口 节点窗口 节点窗口 节点窗口 节点窗口 节点窗口 节点窗口 节点窗口 节点窗口 节点窗口 节点窗口 节点窗口 节点窗口
                if (uiDumpTree != null) {
                    uiDumpTree.removeAll();
                }

                if (uiTreeScrollPanel != null) {
                    panel1.remove(uiTreeScrollPanel);
                    panel1.remove(uiMsgScrollPanel);
                    panel1.remove(uiMsgLabel);
                }

                String uiXmlPath = fetchUiaDump();
                if (uiXmlPath == null) {
                    ProjectServerImpl.showNotification("获取控件信息失败！请联系开发者解决");
                    return;
                }
                String foreground = fetchForeground();
                if (uiXmlPath != null && foreground != null) {
                    uiDumpTree = new Tree(HierarchyParser.INSTANCE.parse(uiXmlPath, new DefaultMutableTreeNode("<前台>" + foreground)));
                    uiDumpTree.setForeground(JBColor.green);
                    uiDumpTree.setVisible(true);
                    uiDumpTree.setAutoscrolls(true);
                    uiDumpTree.addTreeSelectionListener(new TreeSelectionListener() {
                        @Override
                        public void valueChanged(TreeSelectionEvent e) {
                            DefaultMutableTreeNode node = (DefaultMutableTreeNode) uiDumpTree.getLastSelectedPathComponent();
                            if (node == null) return;
                            if (node.getUserObject() instanceof NodeObject) {
                                NodeObject selectedNode = (NodeObject) node.getUserObject();
                                screenShotPanel.drawUiDumpRect(null, selectedNode);
                                uiMsgLabel.setText(selectedNode.getInnerXml());
                                System.out.println("Xuan Zhong le:" + node);
                            }
                        }
                    });
                    // 抓取控件的时候同时触发截图操作
                    for (ActionListener ac : buttonLoadScreen.getActionListeners()) {
                        ac.actionPerformed(null);
                    }
                }

                uiTreeScrollPanel = new JBScrollPane(uiDumpTree);
                // treePanel.setLayout(null);
                uiTreeScrollPanel.createVerticalScrollBar();
                uiTreeScrollPanel.createVerticalScrollBar();
                uiTreeScrollPanel.setForeground(JBColor.white);
                uiTreeScrollPanel.setBackground(JBColor.white);
                panel1.add(uiTreeScrollPanel);

                uiMsgLabel = new JBTextArea();
                uiMsgLabel.setText("@_@");
                uiMsgLabel.setFont(JBFont.regular());
                uiMsgLabel.setEditable(false);
                uiMsgLabel.setVisible(true);
                uiMsgLabel.setLineWrap(true);
                uiMsgLabel.setWrapStyleWord(true);
                uiMsgLabel.setForeground(JBColor.green);
                uiMsgLabel.addMouseListener(new MouseListener() {
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
                        uiMsgLabel.setCursor(new Cursor(Cursor.TEXT_CURSOR));
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {

                    }
                });
                uiMsgScrollPanel = new JBScrollPane(uiMsgLabel);
                // treePanel.setLayout(null);
                uiMsgScrollPanel.createVerticalScrollBar();
                uiMsgScrollPanel.createVerticalScrollBar();
                uiMsgScrollPanel.setForeground(JBColor.white);
                uiMsgScrollPanel.setBackground(JBColor.white);
                panel1.add(uiMsgScrollPanel);
                LOGGER.warn("------------------------------------------------------------");
            }
        });

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String tag = textField.getText().replace(" ", "");
                if (mProject.getBasePath() == null) {
                    return;
                }
                if (tag.equals("")) {
                    tag = "1";
                }
                String outFilePath;
                // 自定义保存路径
                if (tag.contains("/")) {
                    outFilePath = String.format("%s/%s.jpg", mProject.getBasePath(), tag);
                } else {
                    outFilePath = String.format("%s/img/%s.jpg",mProject.getBasePath(), tag);
                }
                screenShotPanel.saveRealRectImage(outFilePath);
                label.setText("生成: " + outFilePath.replace(mProject.getBasePath(), ""));
            }
        });
    }
    public JPanel getContent() {
        return panel1;
    }
    private JPanel panel1;
}
