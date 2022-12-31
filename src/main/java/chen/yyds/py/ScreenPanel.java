package chen.yyds.py;

import org.apache.log4j.Level;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import java.util.Random;

public class ScreenPanel extends JLabel implements MouseListener, MouseMotionListener, KeyListener {
    private static final com.intellij.openapi.diagnostic.Logger LOGGER =
            com.intellij.openapi.diagnostic.Logger.getInstance(ScreenPanel.class);
    Point rectStartPoint = new Point(0, 0);
    Point rectEndPoint = new Point();
    Color stokeRectColor = new Color(127,127,127,100);
    int sw;
    int sh;
    String drawScreenImage;
    BufferedImage currentImage;
    BufferedImage subRealImage;
    Rectangle realRectangle = new Rectangle();
    ImageToolForm.OnScreenRectSelectedEvent selectedCallBack;
    ImageToolForm.OnUiSelectedEvent onUiSelectedCallback;
    long lastClickTime;
    double scaleRatio = 0;
    Stroke dashed = new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND,
            0, new float[]{3, 2}, 0);
    Graphics2D g2d;

    int dragCount = 0;
    public ScreenPanel(int w, int h, ImageToolForm.OnScreenRectSelectedEvent call, ImageToolForm.OnUiSelectedEvent call2) {
        this.setVisible(true);
        this.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
        this.sw = w;
        this.sh = h;
        this.setLayout(null);
        this.setBorder(new EtchedBorder(Color.BLACK, Color.GRAY));
        this.selectedCallBack = call;
        this.onUiSelectedCallback = call2;
        addMouseListener(this);
        addMouseMotionListener(this);
        addKeyListener(this);
        setFocusable(true);
        setScreenSize(w, h);
    }

    // 静态函数===========》》》
    static Color[] colors = new Color[]{Color.CYAN, Color.MAGENTA, Color.ORANGE, Color.GRAY, Color.RED, Color.GREEN, Color.BLACK,Color.PINK, Color.BLUE};
    public static Color randomColor() {
        int i = new Random().nextInt(colors.length);
        return colors[i];
    }
    protected static Image zoomImage(BufferedImage image, int maxWidth, int maxHeight) {
        int imgW = image.getWidth();
        int imgH = image.getHeight();
        int newHeight;
        int newWidth;
        if (imgW > imgH) {
            double ratio = (double) maxWidth / (double) imgW;
            newWidth = maxWidth;
            newHeight = (int)Math.round(imgH * ratio);
        } else {
            double ratio = (double) maxHeight / (double) imgH;
            newWidth = (int)Math.round(imgW * ratio);
            newHeight = maxHeight;
        }

        Image resizedImage = image.getScaledInstance(newWidth, newHeight, Image.SCALE_AREA_AVERAGING);
        //BufferedImage resizedImage = new BufferedImage(newWidth , newHeight, BufferedImage.TYPE_INT_ARGB);
        //Graphics2D g2 = resizedImage.createGraphics();
        //g2.drawImage(image, 0, 0, newWidth , newHeight , null); // draw image to resizedImage
        return resizedImage;
    }
    // 静态函数===========《《《
    public void resetDrawImage(String path) {
        drawScreenImage = path;
        try {
            currentImage =  ImageIO.read(new File(drawScreenImage));
            this.drawScreen();
        } catch (Exception ignore) {}
    }
    public void saveRealRectImage(String fileName) {
        if (subRealImage == null) return;
        try {
            File imageFile = new File(fileName);
            if (!imageFile.getParentFile().exists()) imageFile.getParentFile().mkdirs();
            ImageIO.write(subRealImage, "jpg", imageFile);
        } catch (Exception ignore) {}
    }
    public void setScreenSize(int w, int h) {
        if (getGraphics() != null) {
            g2d = (Graphics2D) getGraphics().create();
        }
        if (currentImage == null) {
            // 不强制比例
            setSize(Math.min(h, w), h);
            invalidate();
        } else {
            if (scaleRatio > 0) {
                this.sh = h;
                this.sw = (int)Math.round(Math.floorDiv(this.sh * currentImage.getWidth(), currentImage.getHeight()));
                setSize(this.sw, this.sh);
            }
        }

    }
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (this.getHeight() != this.sh) {
            setSize(this.sw, this.sh);
        }
        if (rectStartPoint.x > 0 && rectStartPoint.y > 0) {
            if (this.getWidth() != this.sw) {
                LOGGER.warn("RESIZE..." + this.getWidth() + "  " + this.sw);
                this.setSize(this.sw, this.sh);
            }
            drawScreenRect(g);
        }
    }
    public void drawScreen() {
        if (currentImage == null) return;
        int screenWidth = currentImage.getWidth();
        int screenHeight = currentImage.getHeight();
        scaleRatio = (double) sh / (double) screenHeight;
        int newSw = (int)Math.round(screenWidth*scaleRatio);
        if (newSw != sw) {
            sw = newSw;
            this.setSize(sw, sh);
        }
        Image resized = currentImage.getScaledInstance(sw, sh, Image.SCALE_AREA_AVERAGING);
        this.setText(null);
        this.setIcon(new ImageIcon(resized));
        this.setSize(this.sw, this.sh);
        this.invalidate();
        g2d.setStroke(dashed);
    }
    public void drawScreenRect(Graphics g) {

        int h = Math.abs(rectEndPoint.y - rectStartPoint.y);
        int w = Math.abs(rectEndPoint.x - rectStartPoint.x);
        int sx = Math.min(rectStartPoint.x, rectEndPoint.x);
        int sy = Math.min(rectStartPoint.y, rectEndPoint.y);
        LOGGER.warn("========Draw ScreenRect:" + rectStartPoint + "  " + sx + "   " + sy);
        if (sx == 0 || sy == 0) return;
        g.setColor(stokeRectColor);
        g.fillRect(sx, sy, w, h);
        g.setColor(Color.PINK);
        g.drawRect(sx, sy, w, h);
        realRectangle = new Rectangle((int)Math.round(sx/scaleRatio), (int)Math.round(sy/scaleRatio), (int)Math.round(w/scaleRatio), (int)Math.round(h/scaleRatio));
    }

    public void drawUiDumpRect(MouseEvent e, NodeObject clickNodes) {
        if (currentImage != null) {
            if (e != null) {
                int realX = (int)Math.round(e.getX()/scaleRatio);
                int realY = (int)Math.round(e.getY()/scaleRatio);
                // 画出控件框
                LOGGER.warn(">> Real Pos x:"  + realX  + " y:" + realY);
                Map<NodeObject, DefaultMutableTreeNode> nodes = HierarchyParser.INSTANCE.allInRectExceptLayout(realX, realY);
                if (nodes.isEmpty()) return;
                // g2d.clearRect(0,0, sw, sh);
                for (NodeObject node : nodes.keySet()) {
                    g2d.setColor(randomColor());
                    g2d.drawRect((int)Math.round(node.getBounds().getP1x()*scaleRatio),
                            (int)Math.round(node.getBounds().getP1y()*scaleRatio),
                            (int)Math.round(node.getBounds().getWidth()*scaleRatio),
                            (int)Math.round(node.getBounds().getHeight()*scaleRatio));
                    LOGGER.warn("Draw:" + node + " # " + node.getBounds());
                }
                onUiSelectedCallback.onSelected(nodes.values());
            } else {
                    g2d.setColor(randomColor());
                    g2d.drawRect((int)Math.round(clickNodes.getBounds().getP1x()*scaleRatio),
                            (int)Math.round(clickNodes.getBounds().getP1y()*scaleRatio),
                            (int)Math.round(clickNodes.getBounds().getWidth()*scaleRatio),
                            (int)Math.round(clickNodes.getBounds().getHeight()*scaleRatio));
                    LOGGER.warn("Draw:" + clickNodes + " # " + clickNodes.getBounds());
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        long cur = System.currentTimeMillis();
        if (cur - lastClickTime < 350) {
            lastClickTime = 0;
            LOGGER.warn("Double Click, load image!!");
        } else {
            lastClickTime = cur;
        }

        // 点击尝试选定控件信息！
        LOGGER.warn("Click--------------------");
        drawUiDumpRect(e, null);
        // 待选状态 +
        return;

    }

    @Override
    public void repaint() {
        super.repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        rectStartPoint.x = e.getX();
        rectStartPoint.y = e.getY();
        setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        LOGGER.warn("mousePressed y=" + rectStartPoint.getY());
        dragCount = 0;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (currentImage == null) {
            return;
        }
        if (rectStartPoint.x == 0 && rectStartPoint.y == 0) return;
        if (realRectangle.height == 0 || realRectangle.width == 0) return;
        if (dragCount < 10) {
            dragCount = 0;
            return;
        } else {
            dragCount = 0;
        }
        // 画到边界外面去了！ java.awt.image.RasterFormatException: (x + width) is outside of Raster
        if (realRectangle.x > currentImage.getWidth() || realRectangle.y > currentImage.getHeight()) return;
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        if (selectedCallBack != null) {
            LOGGER.warn("==========Sub:......"  + scaleRatio + "  " + realRectangle + "  StartXY:" + rectStartPoint.x + "," + rectStartPoint.y);
            // 鼠标释放，计算出真正裁剪图像
            this.subRealImage = currentImage.getSubimage(realRectangle.x, realRectangle.y, realRectangle.width, realRectangle.height);
            String desc = String.format("左上角: %d,%d | 图像大小 %d x %d", realRectangle.x, realRectangle.y, realRectangle.width, realRectangle.height);
            // 缩放显示
            selectedCallBack.onSelected(zoomImage(this.subRealImage,130, 130), desc);
            LOGGER.warn("draw realRectangle: " + realRectangle);
        }
        rectStartPoint.x = 0;
        rectStartPoint.y = 0;
        LOGGER.warn("mouseReleased");
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (rectStartPoint.x > 0 || rectStartPoint.y > 0) {
            rectStartPoint.x = 0;
            rectStartPoint.y = 0;
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (currentImage != null) return;
        if (rectStartPoint.y == 0 && rectStartPoint.x == 0) {
            return;
        }
        rectEndPoint.x = e.getX();
        rectEndPoint.y = e.getY();
        dragCount++;
        if (dragCount > 3)   this.repaint();

        if (selectedCallBack != null) {
            Rectangle edgeRectangle = new Rectangle((int)Math.round(e.getX()/scaleRatio), (int)Math.round(e.getY()/scaleRatio), (int)Math.round(10/scaleRatio), (int)Math.round(10/scaleRatio));
            LOGGER.warn("==========Show Edge:......"  + scaleRatio + "  edgeRectangle:" + edgeRectangle + " size:" + Math.round(10/scaleRatio));
            // 展示周边图像
            this.subRealImage = currentImage.getSubimage(edgeRectangle.x - Math.floorDiv(edgeRectangle.width, 2), edgeRectangle.y - Math.floorDiv(edgeRectangle.height, 2)  , edgeRectangle.width, edgeRectangle.height);
            String desc = String.format("当前坐标:(%s, %s)", edgeRectangle.x, edgeRectangle.y);
            // 缩放显示
            selectedCallBack.onSelected(zoomImage(this.subRealImage,130, 130), desc);
            LOGGER.warn("draw realRectangle: " + realRectangle);
        }

        LOGGER.warn("mouseDragged y=" + rectStartPoint.getY());
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override 
    public void keyTyped(KeyEvent e) {
        LOGGER.warn("keyTyped " + e.getKeyChar() + " " + e.getKeyCode());
    }

    @Override
    public void keyPressed(KeyEvent e) {
        LOGGER.warn("keyPressed " + e.getKeyChar() + " " + e.getKeyCode());
    }

    @Override
    public void keyReleased(KeyEvent e) {
        LOGGER.warn("keyReleased " + e.getKeyChar() + " " + e.getKeyCode());
    }
}