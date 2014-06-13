package edu.oregonstate.carto.tilemanager;

import java.util.Iterator;

/**
 * TileIterator iterates over the set of tiles of a given bounding box and zoom
 * range. This is specified in the constructor, and the iterator will provide
 * each tile in that set by calling the method next().
 *
 * @author Nicholas Hallahan nick@theoutpost.io and Bernie Jenny, Oregon State
 */
public class TileIterator implements Iterator {

    private final static int MAX_Z = 31;
    private static final double INITIAL_RESOLUTION = 2 * Math.PI * 6378137 / Tile.TILE_SIZE;
    private static final double ORIGIN_SHIFT = 2 * Math.PI * 6378137 / 2.0;

    private final TileSet tileSet;
    private final double minLat, minLng, maxLat, maxLng;
    private final int minZoom, maxZoom;
    private int minX, minY, maxX, maxY, zIdx, xIdx, yIdx;

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
        if (maxZoom > MAX_Z) {
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
     * Use this constructor if tiles for a single zoom level are needed and the
     * extent of the tiles is known in columns and rows. Returns all tiles
     * inside a rectangle defined by minX, maxX, minY, and maxY.
     *
     * @param tileSet
     * @param minX first column (westernmost)
     * @param maxX last column (easternmost)
     * @param minY first row (northernmost)
     * @param maxY last row (southernmost)
     * @param zoomIdx zoom level
     */
    public TileIterator(TileSet tileSet, int minX, int maxX, int minY, int maxY, int zoomIdx) {

        if (minX < 0 || minY < 0 || zoomIdx < 0) {
            throw new IllegalArgumentException("invalid tile coordinates");
        }

        this.tileSet = tileSet;
        minLat = maxLat = minLng = maxLng = Double.NaN;
        minZoom = maxZoom = zoomIdx;
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        zIdx = zoomIdx;
        xIdx = minX;
        yIdx = minY;
    }

    /**
     * This method gets the corner tiles for the current zIdx index. It then
     * resets the x and y indices. The zIdx index is not incremented inside this
     * method, because we want to first get the tiles from the minZoom level in
     * next().
     */
    private void zoom() {
        Tile lowerLeftTile = getTileForLatLngZoom(minLat, minLng, zIdx);
        Tile upperRightTile = getTileForLatLngZoom(maxLat, maxLng, zIdx);

        // y increases from north to south in the Google / OSM tile schema
        minX = lowerLeftTile.getX();
        minY = upperRightTile.getY();
        maxX = upperRightTile.getX();
        maxY = lowerLeftTile.getY();
        xIdx = minX;
        yIdx = minY;
    }

    /**
     * It is slightly more efficient to create a do while next() does not return
     * null. For example:
     *
     * Tile t = iterator.next(); while (t != null) {
     * System.out.println(t.toString()); t = iterator.next(); }
     *
     * @return
     */
    @Override
    public boolean hasNext() {
        return yIdx <= maxY || xIdx < maxX || zIdx < maxZoom;
    }

    @Override
    public Tile next() {
        if (yIdx <= maxY) {
            return tileSet.getTile(zIdx, xIdx, yIdx++);
        }
        if (xIdx < maxX) {
            yIdx = minY + 1;
            return tileSet.getTile(zIdx, ++xIdx, minY);
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
     * @param lon
     * @param zoom
     * @return
     */
    private Tile getTileForLatLngZoom(double lat, double lon, int zoom) {
        /*
         // convert lat lng to meters
         double xMeters = lng * ORIGIN_SHIFT / 180.0;
         double yMeters = Math.log(Math.tan((90 + lat) * Math.PI / 360.0)) / (Math.PI / 180.0);
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
         */
        int xtile = (int) Math.floor((lon + 180) / 360 * (1 << zoom));
        int ytile = (int) Math.floor((1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1 << zoom));
        if (xtile < 0) {
            xtile = 0;
        }
        if (xtile >= (1 << zoom)) {
            xtile = ((1 << zoom) - 1);
        }
        if (ytile < 0) {
            ytile = 0;
        }
        if (ytile >= (1 << zoom)) {
            ytile = ((1 << zoom) - 1);
        }

        return tileSet.getTile(zoom, xtile, ytile);
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
