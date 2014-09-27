package edu.oregonstate.carto.mapcomposer.gui;

import edu.oregonstate.carto.mapcomposer.Layer;
import edu.oregonstate.carto.mapcomposer.tilerenderer.IDWPoint;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import javax.swing.AbstractAction;
import javax.swing.JColorChooser;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

/**
 *
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State
 * University
 */
public class IDWPanel extends IDWPreview {

    private static final int RECT_DIM = 10;
    private IDWPoint dragPoint = null;
    
    public IDWPanel() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragPoint = findIDWPoint(e.getX(), e.getY());
                if (dragPoint != null) {
                    moveIDWPoint(e.getX(), e.getY());
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    final IDWPoint p = findIDWPoint(e.getX(), e.getY());
                    Color newColor = JColorChooser.showDialog(getTopLevelAncestor(),
                            "Select New Color", p.getRGB());
                    if (newColor != null) {
                        p.setRGB(newColor);
                        repaint();
                    }
                    dragPoint = null;
                } else if (e.getClickCount() == 1) {
                    dragPoint = findIDWPoint(e.getX(), e.getY());
                    if (dragPoint == null) {
                        dragPoint = addIDWPoint(e.getX(), e.getY());
                    }
                    repaint();
                }
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragPoint != null && e.getClickCount() == 1) {
                    moveIDWPoint(e.getX(), e.getY());
                }
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                moveIDWPoint(e.getX(), e.getY());
                dragPoint = null;
            }
        });

        setFocusable(true);
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deletePoint");
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "deletePoint");
        getActionMap().put("deletePoint", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getIdw().getPoints().remove(dragPoint);
                repaint();
            }
        });
    }

    private int idwAttr1ToPixel(double attr1) {
        int insetX = getInsets().left;
        int w = getWidth() - getInsets().left - getInsets().right;
        return insetX + (int) Math.round(attr1 * w);
    }

    private int idwAttr2ToPixel(double attr2) {
        int insetY = getInsets().top;
        int h = getHeight() - getInsets().top - getInsets().bottom;
        return insetY + (int) Math.round((1 - attr2) * h);
    }

    private double pixelXToIDWAttr1(int x) {
        int insetX = getInsets().left;
        double w = getWidth() - getInsets().left - getInsets().right;
        return (x - insetX) / w;
    }

    private double pixelYToIDWAttr2(int y) {
        int insetY = getInsets().top;
        double h = getHeight() - getInsets().top - getInsets().bottom;
        return (h - y - insetY) / h;
    }

    private IDWPoint addIDWPoint(int pixelX, int pixelY) {
        IDWPoint p = new IDWPoint();
        p.setAttribute1(pixelXToIDWAttr1(pixelX));
        p.setAttribute2(pixelYToIDWAttr2(pixelY));
        Color color = new Color(getIdw().interpolateValue(p.getAttribute1(), p.getAttribute2()));
        p.setRGB(color);
        getIdw().getPoints().add(p);
        return p;
    }

    private IDWPoint findIDWPoint(int pixelX, int pixelY) {
        ArrayList<IDWPoint> points = getIdw().getPoints();
        for (IDWPoint point : points) {
            int px = idwAttr1ToPixel(point.getAttribute1());
            int py = idwAttr2ToPixel(point.getAttribute2());
            int dx = px - pixelX;
            int dy = py - pixelY;
            double d = Math.sqrt(dx * dx + dy * dy);
            if (d < RECT_DIM / 2) {
                return point;
            }
        }
        return null;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (getIdw() != null) {
            Graphics2D g2d = (Graphics2D) g;
            ArrayList<IDWPoint> points = getIdw().getPoints();
            for (IDWPoint point : points) {
                int px = idwAttr1ToPixel(point.getAttribute1());
                int py = idwAttr2ToPixel(point.getAttribute2());
                g2d.setColor(point.getRGB());
                g2d.fillRect(px - RECT_DIM / 2, py - RECT_DIM / 2, RECT_DIM, RECT_DIM);
                if (point == dragPoint) {
                    g2d.setColor(Color.RED);
                } else {
                    g2d.setColor(Color.BLACK);
                }
                g2d.drawRect(px - RECT_DIM / 2, py - RECT_DIM / 2, RECT_DIM, RECT_DIM);
            }
        }
    }

    private void moveIDWPoint(int mouseX, int mouseY) {
        if (dragPoint == null) {
            return;
        }

        int px = idwAttr1ToPixel(dragPoint.getAttribute1());
        int py = idwAttr2ToPixel(dragPoint.getAttribute2());

        if ((px != mouseX) || (py != mouseY)) {
            dragPoint.setAttribute1(pixelXToIDWAttr1(mouseX));
            dragPoint.setAttribute2(pixelYToIDWAttr2(mouseY));
            repaint();
        }
    }

    public static void main(String[] args) {
        Layer layer = new Layer();
        IDWPanel idwPanel = new IDWPanel();
        idwPanel.setIdw(layer.getIdwTileRenderer());
        idwPanel.setPreferredSize(new Dimension(500, 500));
        JOptionPane.showOptionDialog(null, idwPanel, "", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
    }
}
