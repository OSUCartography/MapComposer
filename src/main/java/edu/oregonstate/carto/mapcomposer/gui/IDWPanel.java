package edu.oregonstate.carto.mapcomposer.gui;

import edu.oregonstate.carto.mapcomposer.tilerenderer.IDWGridTileRenderer;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

/**
 *
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State
 * University
 */
public class IDWPanel extends JComponent {

    private IDWGridTileRenderer idw = null;

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (idw != null) {
            int x = getInsets().left;
            int y = getInsets().top;
            int w = getWidth() - getInsets().left - getInsets().right;
            int h = getHeight() - getInsets().top - getInsets().bottom;
            if (w > 0 && h > 0) {
                BufferedImage img = idw.getDiagramImage(w, h);
                g.drawImage(img, x, y, null);
            }
        }
    }

    /**
     * @return the idw
     */
    public IDWGridTileRenderer getIdw() {
        return idw;
    }

    /**
     * @param idw the idw to set
     */
    public void setIdw(IDWGridTileRenderer idw) {
        this.idw = idw;
        this.repaint();
    }
}
