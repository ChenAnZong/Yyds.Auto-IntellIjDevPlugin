package chen.yyds.py;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

import chen.yyds.py.iface.OnScreenDoubleClickEvent;
import chen.yyds.py.iface.OnScreenRectSelectedEvent;
import chen.yyds.py.iface.OnUiSelectedEvent;

import chen.yyds.py.iface.IOnSearchTreeEvent;
import chen.yyds.py.impl.EngineImplement;
import chen.yyds.py.impl.ProjectServer;
import chen.yyds.py.impl.ProjectServerImpl;
import chen.yyds.py.util.XmlEditKit;
import chen.yyds.py.util.XmlFormatter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.JBColor;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.UIUtil;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.*;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ImageToolForm {
    private static final com.intellij.openapi.diagnostic.Logger LOGGER =
            com.intellij.openapi.diagnostic.Logger.getInstance(ImageToolForm.class);
    private final Project mProject;
    ProjectServerImpl projectServer;
    int panelMargin = 10;
    // 三个控件相关
    JTextPane uiMsgTextArea;

    SearchTextField uiSearchField;

    JBScrollPane uiMsgTextScrollPanel;
    JTree uiDumpTree;

    JBScrollPane uiTreeScrollPanel;

    String loadImageFromDisk;

    IOnSearchTreeEvent onSearchTreeEvent;


    public String fetchScreenShot() {
        // if (checkDeviceConnectFailed()) return null;
        return projectServer.getScreenShot();
    }

    public String fetchUiaDump() {
        // if (checkDeviceConnectFailed()) return null;
        return projectServer.getUiaDump();
    }

    public String fetchForeground() {
        // if (checkDeviceConnectFailed()) return null;
        return projectServer.getForeground();
    }



    public ImageToolForm(Project project, ToolWindow toolWindow) {
        this.mProject = project;
        this.projectServer = (ProjectServerImpl)mProject.getService(ProjectServer.class);
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
        imgLabel.setBackground(JBColor.CYAN);
        imgLabel.setForeground(JBColor.background());
        imgLabel.setBorder(new LineBorder(JBColor.green, 2));

        JButton buttonLoadScreen = new JButton("截图载入");
        //buttonLoadScreen.setBackground(new Color(69,73,74));
        buttonLoadScreen.setForeground(JBColor.foreground());
        buttonLoadScreen.setVisible(true);
        buttonLoadScreen.setSize(130, 40);

        JButton buttonLoadUiaDump = new JButton("控件载入");
        //buttonLoadUiaDump.setBackground(new Color(69,73,74));
        buttonLoadUiaDump.setForeground(JBColor.foreground());
        buttonLoadUiaDump.setVisible(true);
        buttonLoadUiaDump.setSize(130, 40);


        JButton saveAreaButton = new JButton("保存区域");
        //button.setBackground(new Color(69,73,74));
        saveAreaButton.setForeground(JBColor.foreground());
        saveAreaButton.setVisible(true);
        saveAreaButton.setSize(130, 40);
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
                    if (uiDumpTree == null || nodes.isEmpty()) return;
                    uiDumpTree.collapsePath(new TreePath(uiDumpTree.getModel().getRoot()));
                    // 最终会选中深度最深的控件
                    int maxDepth = 0;
                    int lastArea = 0;
                    TreePath last = null;
                    for (DefaultMutableTreeNode treeNode: nodes) {
                        NodeObject node = (NodeObject) ((DefaultMutableTreeNode) treeNode).getUserObject();
                        if (node.getDepth() >= maxDepth || node.getBounds().getHeight() * node.getBounds().getWidth() < lastArea) {
                            uiDumpTree.setExpandsSelectedPaths(true);
                            uiDumpTree.setScrollsOnExpand(true);
                            TreePath tp = new TreePath(treeNode.getPath());
                            uiDumpTree.expandPath(tp);
                            LOGGER.warn("OnUiSelectedEvent Select:" + nodes.size() + " Path:" + tp);
                            maxDepth = node.getDepth();
                            last = tp;
                            lastArea = node.getBounds().getHeight() * node.getBounds().getWidth();
                        }
                    }
                    if (last != null) uiDumpTree.setSelectionPath(last);
            }
        };
        ScreenPanel screenShotPanel = new ScreenPanel(300, 450, onScreenRectSelectedEvent, onUiSelectedEvent);
        screenShotPanel.onScreenDoubleClickEvent = new OnScreenDoubleClickEvent() {
            @Override
            public void onDoubleClick(int x, int y) {
                LOGGER.warn("Double Click call back parent" + "x= " + x + "y=" + y);
                new Thread(()-> {
                    boolean isSuccess = projectServer.click(x, y);
                    if (isSuccess) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                for (ActionListener ac : buttonLoadScreen.getActionListeners()) {
                                    ac.actionPerformed(null);
                                }
                            }
                        });
                    }
                }).start();
            }
        };

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
        panel1.add(saveAreaButton);
        panel1.addComponentListener(new ComponentListener() {
            int h = 0;
            // 位置位置
            @Override
            public void componentResized(ComponentEvent e) {
                LOGGER.warn("componentResized Resized w:" + toolWindow.getComponent().getWidth() + "  h:" + toolWindow.getComponent().getHeight() + " panel width:" + panel1.getWidth());
                screenShotPanel.setScreenSize(panel1.getWidth(), panel1.getHeight() - 180);

                LOGGER.warn("Screen  w:" + screenShotPanel.getWidth() + " h:" + screenShotPanel.getHeight());
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
                saveAreaButton.setBounds(imgLabel.getWidth() + panelMargin * 2, underY, buttonWidth, 40);
                buttonLoadUiaDump.setBounds(imgLabel.getWidth() + panelMargin * 2, underY + saveAreaButton.getHeight() + panelMargin, imageRaminRightSpace - panelMargin, 40);
                buttonLoadScreen.setBounds(imgLabel.getWidth() + saveAreaButton.getWidth() + panelMargin*2 , underY, buttonWidth, 40);

                if (uiDumpTree != null && uiTreeScrollPanel != null && uiMsgTextScrollPanel != null) {
                    // 测试控件列表排版
                    uiTreeScrollPanel.setBounds(screenShotPanel.getWidth() + panelMargin, panelMargin,
                            panel1.getWidth() - screenShotPanel.getWidth() - panelMargin,
                            (panel1.getHeight() - panelMargin*2)*3/5);
                    uiDumpTree.setBounds(0, 0, uiTreeScrollPanel.getWidth() - 20, (panel1.getHeight() - panelMargin*2)*3/5);

                    // XML信息占用 2/5 的高度
                    int uiMsgScrollPanelHeight = (panel1.getHeight() - panelMargin*2)*2/5;

                    // 搜索栏 XML 上面
                    uiSearchField.setBounds(
                            uiTreeScrollPanel.getX(), uiTreeScrollPanel.getHeight() + panelMargin,
                            uiTreeScrollPanel.getWidth(), 30);

                    // 控件信息显示在上面两个窗口下面
                    uiMsgTextScrollPanel.setBounds(
                            uiTreeScrollPanel.getX(), uiTreeScrollPanel.getHeight() + panelMargin + uiSearchField.getHeight(),
                            uiTreeScrollPanel.getWidth(), uiMsgScrollPanelHeight - uiSearchField.getHeight());
                    uiMsgTextArea.setBounds(0, 0, uiMsgTextScrollPanel.getWidth(), uiMsgTextScrollPanel.getHeight());
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

        saveAreaButton.setLocation(560, 100);

        screenShotPanel.setDropTarget(new DropTarget() {
            public synchronized void drop(DropTargetDropEvent evt) {
                try {
                    evt.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> droppedFiles = (List<File>)
                            evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    loadImageFromDisk = droppedFiles.get(0).getAbsolutePath();
                    for (ActionListener ac : buttonLoadScreen.getActionListeners()) {
                        ac.actionPerformed(null);
                    }
                } catch (Exception ex) {
                    LOGGER.error(ex.getMessage());
                }
            }
        });
        buttonLoadScreen.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String fetchScreenShot;
                boolean isLoadFromDisk = false;
                if (loadImageFromDisk != null) {
                    fetchScreenShot = loadImageFromDisk;
                    loadImageFromDisk = null;
                    isLoadFromDisk = true;
                } else {
                    fetchScreenShot = fetchScreenShot();
                }

                try {
                    if (fetchScreenShot != null) {
                        screenShotPanel.resetDrawImage(fetchScreenShot);
                    } else {
                        Notifyer.notifyError("开发助手", "截图失败，无法获取图片, 请尝试多点两此");
                        return;
                    }
                } catch (Exception resetError) {
                    Notifyer.notifyError("开发助手", "截图错误，请多点几次或者联系开发者处理!\n" + resetError.getMessage());
                }

                // 要加载两次图片，尝试调整两次！
                panel1.setSize(screenShotPanel.getWidth() + panelMargin, panel1.getHeight() + 1);
                panel1.setSize(screenShotPanel.getWidth() + panelMargin, panel1.getHeight() - 1);
                // 如果只加载截图，防止图片对不上控件，那就清空当前控件查找列表 | 点击截图 or 或者拉取外部图片， 即非同时拉取截图与控件
                if ((e != null || isLoadFromDisk) && uiDumpTree != null) {
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
                    panel1.remove(uiMsgTextScrollPanel);
                    panel1.remove(uiMsgTextArea);
                    panel1.remove(uiSearchField);
                }

                new Thread(()-> {
                    String uiXmlPath = fetchUiaDump();
                    if (uiXmlPath == null) {
                        Notifyer.notifyError("开发助手","获取控件信息失败！请多点两次或者联系开发者解决");
                        return;
                    }
                    String foreground = fetchForeground();

                    SwingUtilities.invokeLater(()-> {
                        if (foreground != null) {
                            DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("<前台>" + foreground);
                            DefaultMutableTreeNode treeNode = HierarchyParser.INSTANCE.parse(uiXmlPath, rootNode);
                            DefaultTreeModel myModel = new DefaultTreeModel(treeNode);
                            uiDumpTree = new JTree(myModel);
                            uiDumpTree.setForeground(UIUtil.getTreeForeground());
                            uiDumpTree.setBackground(UIUtil.getTreeBackground());
                            uiDumpTree.setVisible(true);
                            uiDumpTree.setAutoscrolls(true);
                            uiDumpTree.setCellRenderer(new NodeTreeCellRender());
                            onSearchTreeEvent = new IOnSearchTreeEvent() {
                                @Override
                                public void search() {
                                    myModel.nodeStructureChanged(treeNode);
                                }
                            };

                            uiDumpTree.addTreeSelectionListener(new TreeSelectionListener() {
                                @Override
                                public void valueChanged(TreeSelectionEvent e) {
                                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) uiDumpTree.getLastSelectedPathComponent();
                                    if (node == null) return;
                                    if (node.getUserObject() instanceof NodeObject) {
                                        NodeObject selectedNode = (NodeObject) node.getUserObject();
                                        screenShotPanel.drawUiDumpRect(null, selectedNode);
                                        uiMsgTextArea.setText(XmlFormatter.format(selectedNode.getInnerXml()));
                                        System.out.println("Your Selected Node:" + node);
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

                        // 控件XML搜索
                        uiSearchField = new SearchTextField();
                        panel1.add(uiSearchField);
                        uiSearchField.setToolTipText("输入关键词并按下回车键进行搜索");
                        uiSearchField.setSize(100, 30);
                        uiSearchField.setEnabled(true);
                        uiSearchField.setVisible(true);
                        uiSearchField.addKeyboardListener(new KeyListener() {
                            @Override
                            public void keyTyped(KeyEvent e) {
                            }
                            @Override
                            public void keyPressed(KeyEvent e) {
                            }
                            @Override
                            public void keyReleased(KeyEvent e) {
                                TreeCellRenderer cellRenderer =  uiDumpTree.getCellRenderer();
                                // 按下回车键开始搜索
                                if (cellRenderer != null && onSearchTreeEvent != null && e.getKeyCode() == 10) {
                                    ((NodeTreeCellRender) cellRenderer).setFilterString(uiSearchField.getText());
                                    LOGGER.warn("keyFilterSet:" + uiSearchField.getText());
                                    onSearchTreeEvent.search();
                                    screenShotPanel.drawUiSearchNode(uiSearchField.getText());
                                }
                            }
                        });

                        // 控件XML显示对照
                        uiMsgTextArea = new JTextPane();
                        uiMsgTextArea.setText("@_@");
                        uiMsgTextArea.setFont(JBFont.regular());
                        uiMsgTextArea.setEditable(false);
                        uiMsgTextArea.setVisible(true);
                        uiMsgTextArea.setEditorKitForContentType("text/xml", new XmlEditKit());
                        uiMsgTextArea.setContentType("text/xml");
                        uiMsgTextArea.setForeground(JBColor.foreground());
                        uiMsgTextArea.addMouseListener(new MouseListener() {
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
                                uiMsgTextArea.setCursor(new Cursor(Cursor.TEXT_CURSOR));
                            }

                            @Override
                            public void mouseExited(MouseEvent e) {

                            }
                        });
                        JBPopupMenu popupMenu = new JBPopupMenu();
                        JBMenuItem copyAllItem = new JBMenuItem("复制全部");
                        popupMenu.add(copyAllItem);

                        copyAllItem.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                Toolkit.getDefaultToolkit()
                                        .getSystemClipboard()
                                        .setContents(
                                                new StringSelection(uiMsgTextArea.getText()),
                                                null
                                        );
                            }
                        });
                        JBMenuItem copySelectionItem = new JBMenuItem("复制选中");
                        popupMenu.add(copySelectionItem);
                        uiMsgTextArea.setComponentPopupMenu(popupMenu);
                        copySelectionItem.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                Toolkit.getDefaultToolkit()
                                        .getSystemClipboard()
                                        .setContents(
                                                new StringSelection(uiMsgTextArea.getSelectedText()),
                                                null
                                        );
                            }
                        });

                        uiMsgTextScrollPanel = new JBScrollPane(uiMsgTextArea);
                        // treePanel.setLayout(null);
                        uiMsgTextScrollPanel.createVerticalScrollBar();
                        uiMsgTextScrollPanel.createVerticalScrollBar();
                        uiMsgTextScrollPanel.setForeground(JBColor.white);
                        uiMsgTextScrollPanel.setBackground(JBColor.white);
                        panel1.add(uiMsgTextScrollPanel);
                        LOGGER.warn("------------------------------------------------------------");
                    });

                }).start();
            }
        });
        saveAreaButton.addActionListener(new ActionListener() {
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
