/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.oregonstate.carto.mapcomposer;

import com.jhlabs.composite.MultiplyComposite;
import edu.oregonstate.carto.tilemanager.TileSet;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author Nicholas Hallahan nick@theoutpost.io
 */
public class Map {

    public final static int TILE_SIZE = 256;
    private ArrayList<Layer> layers = new ArrayList<Layer>();

    public BufferedImage generateTile(int z, int x, int y) {

        BufferedImage tileImage = new BufferedImage(
                TILE_SIZE,
                TILE_SIZE,
                BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = tileImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, TILE_SIZE, TILE_SIZE);

        for (Layer layer : layers) {
            if (!layer.isVisible()) {
                continue;
            }
            layer.renderToTile(g2d, z, x, y);

        }
        return tileImage;
    }

    public void addLayer(Layer layer) {
        layers.add(layer);
    }
}
