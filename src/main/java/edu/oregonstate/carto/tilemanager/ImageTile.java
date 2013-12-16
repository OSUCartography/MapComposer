package edu.oregonstate.carto.tilemanager;

import edu.oregonstate.carto.mapcomposer.Map;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

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

    /**
     * Creates a Buffered image of 9 tiles with this tile being the center. This
     * is needed when a rendering engine needs data of the surrounding
     * neighborhood tiles.
     * 
     * For the tiles around the center tile, rather than throwing an IO exception,
     * I fill them with white. This is to avoid the problem of having the whole
     * mega tile fail if any images around the center image are not available.
     * 
     * It throws IOException only when it cannot load the center image.
     *
     * @return A BufferedImage of the 9 tiles.
     */
    @Override
    public BufferedImage createMegaTile() throws IOException {
        BufferedImage megaTile = new BufferedImage(megaTileSize, megaTileSize, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = megaTile.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        try {
            BufferedImage topLeftTile = (BufferedImage) getTopLeftTile().fetch();
            g2d.drawImage(topLeftTile, 0, 0, null);
        } catch (IOException ex) {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, TILE_SIZE, TILE_SIZE);
        }

        try {
            BufferedImage topTile = (BufferedImage) getTopTile().fetch();
            g2d.drawImage(topTile, TILE_SIZE, 0, null);
        } catch (IOException ex) {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(TILE_SIZE, 0, TILE_SIZE, TILE_SIZE);
        }

        try {
            BufferedImage topRightTile = (BufferedImage) getTopRightTile().fetch();
            g2d.drawImage(topRightTile, TILE_SIZE * 2, 0, null);
        } catch (IOException ex) {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(TILE_SIZE * 2, 0, TILE_SIZE, TILE_SIZE);
        }

        try {
            BufferedImage leftTile = (BufferedImage) getLeftTile().fetch();
            g2d.drawImage(leftTile, 0, TILE_SIZE, null);
        } catch (IOException ex) {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, TILE_SIZE, TILE_SIZE, TILE_SIZE);
        }
        

        
        // The tile in the center is this tile.
        g2d.drawImage(fetch(), TILE_SIZE, TILE_SIZE, null);

        
        try {
            BufferedImage rightTile = (BufferedImage) getRightTile().fetch();
            g2d.drawImage(rightTile, TILE_SIZE * 2, TILE_SIZE, null);
        } catch (IOException ex) {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(TILE_SIZE * 2, TILE_SIZE, TILE_SIZE, TILE_SIZE);
        }
        
        try {
            BufferedImage bottomLeftTile = (BufferedImage) getBottomLeftTile().fetch();
            g2d.drawImage(bottomLeftTile, 0, TILE_SIZE * 2, null);
        } catch (IOException ex) {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, TILE_SIZE * 2, TILE_SIZE, TILE_SIZE);
        }

        try {
            BufferedImage bottomTile = (BufferedImage) getBottomTile().fetch();
            g2d.drawImage(bottomTile, TILE_SIZE, TILE_SIZE * 2, null);
        } catch (IOException ex) {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(TILE_SIZE, TILE_SIZE * 2, TILE_SIZE, TILE_SIZE);
        }

        try {
            BufferedImage bottomRightTile = (BufferedImage) getBottomRightTile().fetch();
            g2d.drawImage(bottomRightTile, TILE_SIZE * 2, TILE_SIZE * 2, null);
        } catch (IOException ex) {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(TILE_SIZE * 2, TILE_SIZE * 2, TILE_SIZE, TILE_SIZE);
        }
        

        return megaTile;
    }
}
