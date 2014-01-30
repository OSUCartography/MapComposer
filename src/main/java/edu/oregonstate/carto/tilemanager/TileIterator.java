package edu.oregonstate.carto.tilemanager;

import java.util.Iterator;

/**
 * TileIterator iterates over the set of tiles of a given bounding box and zoom
 * range. This is specified in the constructor, and the resulting object will
 * spit out each tile in that set by calling the method next().
 *
 * @author Nicholas Hallahan nick@theoutpost.io
 */
public class TileIterator implements Iterator {

    private static final double INITIAL_RESOLUTION = 2 * Math.PI * 6378137 / Tile.TILE_SIZE;
    private static final double ORIGIN_SHIFT = 2 * Math.PI * 6378137 / 2.0;

    private final TileSet tileSet;
    private final double minLat, minLng, maxLat, maxLng;
    private final int minZoom, maxZoom;

    private int minX, minY, maxX, maxY, difX, difY, zIdx, xIdx, yIdx;

    public TileIterator(TileSet tileSet,
            double minLat, double minLng,
            double maxLat, double maxLng,
            int minZoom, int maxZoom) {

        if (tileSet == null) {
            throw new IllegalArgumentException("TileSet not valid");
        }
        
        // FIXME BJ
        // not sure what the valid boundary values are
        if (minLat < -90 || maxLat > 90 || minLng < -180 || maxLng > 360) {
            throw new IllegalArgumentException("extent is not valid");
        }
        if (minLat > maxLat) {
            throw new IllegalArgumentException("minLat cannot be greater than maxLat");
        }
        if (minLng > maxLng) {
            throw new IllegalArgumentException("minLng cannot be greater than maxLng");
        }
        
        if (minZoom < 0) {
            throw new IllegalArgumentException("minZoom cannot be smaller than 0");
        }
        if (maxZoom > 31) {
            throw new IllegalArgumentException("maxZoom cannot be greater than 31");
        }

        this.tileSet = tileSet;
        this.minLat = minLat;
        this.minLng = minLng;
        this.maxLat = maxLat;
        this.maxLng = maxLng;
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;

        zIdx = this.minZoom;
        zoom();
    }

    /**
     * This method gets the minTile and maxTile for the current zIdx index. It
     * then sets up the diffs and zeros out the x and y indices. The zIdx
     * iterator is not incremented inside this method, because we want to first
     * get the tiles from the minZoom level in next().
     */
    private void zoom() {
        Tile minTile = getTileForLatLngZoom(minLat, minLng, zIdx);
        Tile maxTile = getTileForLatLngZoom(maxLat, maxLng, zIdx);

        /*
         * minX and minY are for the minTile, so according to the Google / OSM
         * tile schema, maxTile will have a Y value smaller than minTile's Y val.
         */
        minX = minTile.getX();
        minY = minTile.getY();
        maxX = maxTile.getX();
        maxY = maxTile.getY();

        difX = maxX - minX;
        difY = maxY - minY;

        xIdx = 0;
        yIdx = 0;
    }

    /**
     * It is slightly more efficient to create a do while next() does not return
     * null.
     *
     * For example:
     *
     * Tile t = corvallis3.next(); while (t != null) {
     * System.out.println(t.toString()); t = corvallis3.next(); }
     *
     * @return
     */
    @Override
    public boolean hasNext() {
        if (yIdx >= difY) {
            return true;
        }
        if (xIdx < difX) {
            return true;
        }
        return zIdx + 1 <= maxZoom;
    }

    @Override
    public Tile next() {
        if (yIdx >= difY) {
            return tileSet.getTile(zIdx, minX + xIdx, minY + yIdx--);
        }
        if (xIdx < difX) {
            yIdx = 0;
            ++xIdx;
            return tileSet.getTile(zIdx, minX + xIdx, minY + yIdx--);
        }
        // Increments the zoom index and make sure result is not more than
        // the max zoom.
        if (++zIdx <= maxZoom) {
            zoom();
            return next();
        }
        return null;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Removing a tile makes no sense.");
    }

    /**
     * Derived from globalmaptiles.py:
     * http://www.maptiler.org/google-maps-coordinates-tile-bounds-projection/
     *
     * @param lat
     * @param lng
     * @param zoom
     * @return
     */
    private Tile getTileForLatLngZoom(double lat, double lng, int zoom) {
        // convert lat lng to meters
        double xMeters = lng * ORIGIN_SHIFT / 180.0;
        double yMeters = Math.log(Math.tan((90 + lat) * Math.PI / 360.0))
                / (Math.PI / 180.0);
        yMeters = yMeters * ORIGIN_SHIFT / 180.0;

        // resolution of meters/pixel for given zoom level
        double resolution = INITIAL_RESOLUTION / Math.pow(2, zoom);

        // meters to pixels
        double xPixels = (xMeters + ORIGIN_SHIFT) / resolution;
        double yPixels = (yMeters + ORIGIN_SHIFT) / resolution;

        // pixels to tile
        int xTile;
        if (xPixels == 0) {
            xTile = 0;
        } else {
            xTile = (int) (Math.ceil(xPixels / (double) Tile.TILE_SIZE) - 1);
        }

        int yTile = (int) (Math.ceil(yPixels / (double) Tile.TILE_SIZE) - 1);

        // NH FIXME
        // Convert TMS y coord to Google y coord, should be done in math above...
        yTile = (int) ((Math.pow(2, zoom) - 1) - (double) yTile);

        return tileSet.getTile(zoom, xTile, yTile);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("latitude ").append(minLat).append(" to ").append(maxLat).append("\n");
        sb.append("longitude ").append(minLng).append(" to ").append(maxLng).append("\n");
        sb.append("zoom ").append(minZoom).append(" to ").append(maxZoom).append("\n");
        return sb.toString();
    }
}
