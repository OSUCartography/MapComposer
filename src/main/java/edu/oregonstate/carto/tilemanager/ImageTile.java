package edu.oregonstate.carto.tilemanager;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

/**
 *
 * @author Nicholas Hallahan nick@theoutpost.io
 */
public class ImageTile extends Tile {

    /**
     * The BufferedImage that is the raster data of this tile. This in memory
     * field is populated by fetch()
     */
    private BufferedImage img;

    public ImageTile(TileSet tileSet, int z, int x, int y) {
        super(tileSet, z, x, y);
    }

    public ImageTile(TileSet tileSet, TileCoord coord) {
        super(tileSet, coord);
    }

    @Override
    public synchronized BufferedImage fetch() throws IOException {
        if (img == null) {
            URL url = tileSet.urlForTile(this);
            img = sqlite.fetchImage(url);
            System.out.println("fetched: " + url.toString());
        }
        return img;
    }
}
