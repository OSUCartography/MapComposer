package edu.oregonstate.carto.mapcomposer.gui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;

/** @see http://stackoverflow.com/questions/6333464 */
public class RotatePanel extends JPanel {

    public RotatePanel() {
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        int w2 = getWidth() / 2;
        int h2 = getHeight() / 2;
        System.out.println(w2 + " " + h2);
        g2d.rotate(-Math.PI / 2, w2, h2);
        super.paintComponent(g);
    }
}