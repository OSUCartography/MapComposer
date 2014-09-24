package edu.oregonstate.carto.tilemanager;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;

/**
 * A raster image tile using a BufferedImage for storing tile data.
 *
 * @author Nicholas Hallahan nick@theoutpost.io
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State
 * University
 */
public class ImageTile extends Tile<BufferedImage> {

    /**
     * The image is streamed to lossless PNG format for caching.
     */
    private static final String IMAGE_FORMAT = "png";
    /**
     * The BufferedImage that is the raster data of this tile. This is populated
     * by a call to fetch()
     */
    private BufferedImage img;

    /**
     * Creates a new instance of ImageTile.
     *
     * @param tileSet The tile set for the new tile
     * @param z The zoom level.
     * @param x The horizontal coordinate.
     * @param y The vertical coordinate.
     */
    public ImageTile(TileSet tileSet, int z, int x, int y) {
        super(tileSet, z, x, y);
    }

    /**
     * Creates a new instance of ImageTile.
     *
     * @param tileSet The tile set for the new tile.
     * @param coord The zoom level and coordinates.
     */
    public ImageTile(TileSet tileSet, TileCoord coord) {
        super(tileSet, coord);
    }

    /**
     * Reads a tile from a binary stream. A tile can be written to a stream with
     * toBinary().
     *
     * @param tileSet
     * @param inStream
     * @throws IOException
     */
    public ImageTile(TileSet tileSet, DataInputStream inStream) throws IOException {
        super(tileSet, inStream.readInt(), inStream.readInt(), inStream.readInt());
        img = ImageIO.read(inStream);
    }

    /**
     * Fetches the tile's BuffereImage from memory, http, or file. This may
     * block the calling thread until the image is loaded.
     *
     * @return The image for this tile.
     * @throws IOException
     */
    @Override
    public synchronized BufferedImage fetch() throws IOException {
        if (img == null) {
            TileSet tileSet = getTileSet();
            URL url = tileSet.urlForTile(this);
            img = ImageIO.read(url);
            // replace object in cache
            tileSet.tileChanged(this);
        }
        return img;
    }

    /**
     * Serialize this tile to a binary stream.
     *
     * @param out The stream to write to.
     * @throws IOException
     */
    @Override
    protected void toBinary(java.io.DataOutputStream out) throws IOException {
        super.toBinary(out);
        if (img != null) {
            ImageIO.write(img, IMAGE_FORMAT, out);
        }
        out.flush();
    }

    @Override
    public BufferedImage createMegaTile() throws IOException {
        Color BACKGROUND_COLOR = Color.YELLOW;
        int megaTileSize = Tile.TILE_SIZE * 3;

        BufferedImage megaTile = new BufferedImage(megaTileSize, megaTileSize, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = megaTile.createGraphics();
        g2d.setColor(BACKGROUND_COLOR);
        g2d.fillRect(0, 0, megaTile.getWidth(), megaTile.getHeight());
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        try {
            BufferedImage topLeftTile = (BufferedImage) getTopLeftTile().fetch();
            g2d.drawImage(topLeftTile, 0, 0, null);
        } catch (IOException ex) {
        }

        try {
            BufferedImage topTile = (BufferedImage) getTopTile().fetch();
            g2d.drawImage(topTile, Tile.TILE_SIZE, 0, null);
        } catch (IOException ex) {
        }

        try {
            BufferedImage topRightTile = (BufferedImage) getTopRightTile().fetch();
            g2d.drawImage(topRightTile, Tile.TILE_SIZE * 2, 0, null);
        } catch (IOException ex) {
        }

        try {
            BufferedImage leftTile = (BufferedImage) getLeftTile().fetch();
            g2d.drawImage(leftTile, 0, Tile.TILE_SIZE, null);
        } catch (IOException ex) {
        }

        // The tile in the center is this tile.
        g2d.drawImage((BufferedImage) this.fetch(), Tile.TILE_SIZE, Tile.TILE_SIZE, null);

        try {
            BufferedImage rightTile = (BufferedImage) getRightTile().fetch();
            g2d.drawImage(rightTile, Tile.TILE_SIZE * 2, Tile.TILE_SIZE, null);
        } catch (IOException ex) {
        }

        try {
            BufferedImage bottomLeftTile = (BufferedImage) getBottomLeftTile().fetch();
            g2d.drawImage(bottomLeftTile, 0, Tile.TILE_SIZE * 2, null);
        } catch (IOException ex) {
        }

        try {
            BufferedImage bottomTile = (BufferedImage) getBottomTile().fetch();
            g2d.drawImage(bottomTile, Tile.TILE_SIZE, Tile.TILE_SIZE * 2, null);
        } catch (IOException ex) {
        }

        try {
            BufferedImage bottomRightTile = (BufferedImage) getBottomRightTile().fetch();
            g2d.drawImage(bottomRightTile, Tile.TILE_SIZE * 2, Tile.TILE_SIZE * 2, null);
        } catch (IOException ex) {
        }
        return megaTile;
    }
}
