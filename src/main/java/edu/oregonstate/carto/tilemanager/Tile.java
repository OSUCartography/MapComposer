package edu.oregonstate.carto.tilemanager;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

/**
 * A tile is a raster image, 256 x 256 cells large. Data can be stored in an
 * raster image or a raster grid (a single floating point band). The actual
 * storage of data is handled by a concrete subclass.
 *
 * @author Nicholas Hallahan nick@theoutpost.io
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State
 * University
 * @param <TileData> An object to store the actual tile data.
 */
public abstract class Tile<TileData> {

    public static final double MAX_LAT = 85.05112878;
    /**
     * Tiles are always 256 x 256 pixels.
     */
    public final static int TILE_SIZE = 256;
    /**
     * zoom level
     */
    private final int z;
    /**
     * horizontal coordinate of tile
     */
    private final int x;
    /**
     * vertical coordinate of the tile.
     */
    private final int y;
    /**
     * A tile has a TileSet to find its neighbors.
     */
    private final TileSet tileSet;

    /**
     * Creates a new instance of Tile.
     *
     * @param tileSet The tile set for the new tile
     * @param z The zoom level.
     * @param x The horizontal coordinate.
     * @param y The vertical coordinate.
     */
    protected Tile(TileSet tileSet, int z, int x, int y) {
        this.tileSet = tileSet;
        this.z = z;
        this.x = x;
        this.y = y;
    }

    /**
     * Creates a new instance of Tile.
     *
     * @param tileSet The tile set for the new tile.
     * @param coord The zoom level and coordinates.
     */
    protected Tile(TileSet tileSet, TileCoord coord) {
        this(tileSet, coord.Z, coord.X, coord.Y);
    }

    /**
     * Fetches the tile's data from memory, http, or file. If the tile is an
     * image, the BufferedImage is returned. If the tile is a grid, Grid is
     * returned. This may block the calling thread until the image is loaded.
     *
     * @return BufferedImage or Grid
     * @throws java.io.IOException
     */
    public abstract TileData fetch() throws IOException;

    /**
     * Returns the neighbor above and to the left of this tile.
     *
     * @return
     */
    public Tile getTopLeftTile() {
        return tileSet.getTopLeftTile(this);
    }

    /**
     * Returns the neighbor above this tile.
     *
     * @return
     */
    public Tile getTopTile() {
        return tileSet.getTopTile(this);
    }

    /**
     * Returns the neighbor above and to the right of this tile.
     *
     * @return
     */
    public Tile getTopRightTile() {
        return tileSet.getTopRightTile(this);
    }

    /**
     * Returns the neighbor to the left of this tile.
     *
     * @return
     */
    public Tile getLeftTile() {
        return tileSet.getLeftTile(this);
    }

    /**
     * Returns the neighbor to the right of this tile.
     *
     * @return
     */
    public Tile getRightTile() {
        return tileSet.getRightTile(this);
    }

    /**
     * Returns the neighbor below and to the left of this tile.
     *
     * @return
     */
    public Tile getBottomLeftTile() {
        return tileSet.getBottomLeftTile(this);
    }

    /**
     * Returns the neighbor below this tile.
     *
     * @return
     */
    public Tile getBottomTile() {
        return tileSet.getBottomTile(this);
    }

    /**
     * Returns the neighbor below and to the right of this tile.
     *
     * @return
     */
    public Tile getBottomRightTile() {
        return tileSet.getBottomRightTile(this);
    }
    
    /**
     * Return the zoom level of this tile
     *
     * @return the z
     */
    public int getZ() {
        return z;
    }

    /**
     * @return the x
     */
    public int getX() {
        return x;
    }

    /**
     * @return the y
     */
    public int getY() {
        return y;
    }

    /**
     * Returns the URL for this tile.
     *
     * @return URL
     */
    public URL getURL() {
        return tileSet.urlForTile(this);
    }

    /**
     * @return the tileSet
     */
    protected TileSet getTileSet() {
        return tileSet;
    }

    /**
     * Serialize this tile to a binary stream. This should be overridden by
     * subclasses.
     *
     * @param out The stream to write to.
     * @throws IOException
     */
    protected void toBinary(java.io.DataOutputStream out) throws IOException {
        out.writeInt(z);
        out.writeInt(x);
        out.writeInt(y);
    }

    public String toDescription() {
        return "Zoom " + z + ", x: " + x + ", y: " + y;
    }
    
    @Override
    public String toString() {
        return "Tile(z=" + z + ", x=" + x + ", y=" + y + ")";
    }
}
