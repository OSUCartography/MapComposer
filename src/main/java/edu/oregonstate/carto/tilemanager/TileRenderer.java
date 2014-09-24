package edu.oregonstate.carto.tilemanager;

import java.awt.image.BufferedImage;

/**
 *
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State
 * University
 */
public interface TileRenderer {
    public BufferedImage render(Tile tile);
}
