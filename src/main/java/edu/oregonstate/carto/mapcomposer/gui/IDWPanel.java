package edu.oregonstate.carto.mapcomposer.gui;

import edu.oregonstate.carto.mapcomposer.tilerenderer.IDWPoint;
import edu.oregonstate.carto.utils.ColorUtils;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * An interactive panel for placing color points for IDW color interpolation.
 *
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State
 * University
 */
public class IDWPanel extends IDWPreview {

    private static final int RECT_DIM = 10;

    /**
     * The currently selected point that is being dragged.
     */
    private IDWPoint selectedPoint = null;

    /**
     * horizontal distance between the last mouse click and the center of the
     * selected point.
     */
    private int dragDX = 0;
    /**
     * horizontal distance between the last mouse click and the center of the
     * selected point.
     */
    private int dragDY = 0;

    public IDWPanel() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                IDWPoint pt = findIDWPoint(e.getX(), e.getY());
                if (pt != null) {
                    selectPoint(pt);
                    dragDX = idwAttr1ToPixelX(pt.getAttribute1()) - e.getX();
                    dragDY = idwAttr2ToPixelY(pt.getAttribute2()) - e.getY();
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                IDWPoint pt = findIDWPoint(e.getX(), e.getY());
                if (pt == null) {
                    selectPoint(addIDWPoint(e.getX(), e.getY()));
                }
                repaint();
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (getSelectedPoint() != null) {
                    moveIDWPoint(e.getX(), e.getY());
                }
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                moveIDWPoint(e.getX(), e.getY());
                selectPoint(null);
            }
        });

        // listen to delete and backspace key strokes
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deletePoint");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "deletePoint");
        getActionMap().put("deletePoint", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeSelectedPoint();
                repaint();
            }
        });
    }

    private void removeSelectedPoint() {
        getIdw().getPoints().remove(selectedPoint);
        firePropertyChange("colorChanged", null, null);
        selectPoint(null);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (getIdw() != null) {
            Graphics2D g2d = (Graphics2D) g;
            ArrayList<IDWPoint> points = getIdw().getPoints();
            for (IDWPoint point : points) {
                int px = idwAttr1ToPixelX(point.getAttribute1());
                int py = idwAttr2ToPixelY(point.getAttribute2());
                g2d.setColor(point.getColor());
                g2d.fillRect(px - RECT_DIM / 2, py - RECT_DIM / 2, RECT_DIM, RECT_DIM);
                if (point == selectedPoint) {
                    g2d.setColor(Color.RED);
                } else {
                    if (ColorUtils.getBrightness(point.getColor()) > 100) {
                        g2d.setColor(Color.BLACK);
                    } else {
                        g2d.setColor(Color.WHITE);
                    }
                }
                g2d.drawRect(px - RECT_DIM / 2, py - RECT_DIM / 2, RECT_DIM, RECT_DIM);
            }
        }
    }

    private int idwAttr1ToPixelX(double attr1) {
        int insetX = getInsets().left;
        int w = getWidth() - getInsets().left - getInsets().right;
        return insetX + (int) Math.round(attr1 * w);
    }

    private int idwAttr2ToPixelY(double attr2) {
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
        return (h - y + insetY) / h;
    }

    private IDWPoint addIDWPoint(int pixelX, int pixelY) {
        IDWPoint p = new IDWPoint();
        p.setAttribute1(pixelXToIDWAttr1(pixelX));
        p.setAttribute2(pixelYToIDWAttr2(pixelY));
        Color color = new Color(getIdw().interpolateValue(p.getAttribute1(), p.getAttribute2()));
        p.setColor(color);
        getIdw().getPoints().add(p);
        return p;
    }

    private IDWPoint findIDWPoint(int pixelX, int pixelY) {
        ArrayList<IDWPoint> points = getIdw().getPoints();
        for (IDWPoint point : points) {
            int px = idwAttr1ToPixelX(point.getAttribute1());
            int py = idwAttr2ToPixelY(point.getAttribute2());
            Rectangle rect = new Rectangle(px - RECT_DIM / 2, py - RECT_DIM / 2,
                    RECT_DIM + 1, RECT_DIM + 1);
            if (rect.contains(pixelX, pixelY)) {
                return point;
            }
        }
        return null;
    }

    private void moveIDWPoint(int mouseX, int mouseY) {
        if (selectedPoint == null) {
            return;
        }
        double attr1 = pixelXToIDWAttr1(mouseX + dragDX);
        attr1 = Math.min(Math.max(0d, attr1), 1d);
        selectedPoint.setAttribute1(attr1);
        
        double attr2 = pixelYToIDWAttr2(mouseY + dragDY);
        attr2 = Math.min(Math.max(0d, attr2), 1d);
        selectedPoint.setAttribute2(attr2);
        
        selectedPoint.setLonLat(Double.NaN, Double.NaN);
        
        repaint();
        firePropertyChange("colorChanged", null, null);
    }

    /**
     * @return the dragPoint
     */
    public IDWPoint getSelectedPoint() {
        return selectedPoint;
    }

    /**
     * Sets the selectedPoint field to the passed point and fires a property
     * change event.
     *
     * @param pt
     */
    private void selectPoint(IDWPoint pt) {
        selectedPoint = pt;
        firePropertyChange("selectedPoint", null, selectedPoint);
    }

    public void setSelectedColor(Color color) {
        if (selectedPoint != null) {
            selectedPoint.setColor(color);
            repaint();
            firePropertyChange("colorChanged", null, null);
        }
    }
}
