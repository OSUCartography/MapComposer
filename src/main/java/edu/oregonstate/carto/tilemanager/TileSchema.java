package edu.oregonstate.carto.tilemanager;

/**
 *
 * @author Nicholas Hallahan nick@theoutpost.io
 */
public abstract class TileSchema {

    protected static final int TILE_SIZE = 256;
    protected static final double INITIAL_RESOLUTION = 2 * Math.PI * 6378137 / TILE_SIZE;
    protected static final double ORIGIN_SHIFT = 2 * Math.PI * 6378137 / 2.0;

    /**
     * Derived from globalmaptiles.py:
     * http://www.maptiler.org/google-maps-coordinates-tile-bounds-projection/
     *
     * This method is overridden by GoogleTileSchema.java that takes this and
     * converts the y tile to the correct number (coordinate origin is moved
     * from bottom-left to top-left corner of the extent).
     *
     * @param lat
     * @param lng
     * @param zoom
     * @return
     */
    public TileCoord getTileCoordsForLatLngZoom(double lat, double lng, int zoom) {
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
        int xTile = (int) (Math.ceil(xPixels / (double) TILE_SIZE) - 1);
        int yTile = (int) (Math.ceil(yPixels / (double) TILE_SIZE) - 1);

        return new TileCoord(zoom, xTile, yTile);
    }

    public TileCoord[] getTileCoordsForBBoxZoom(double minLat, double minLng,
            double maxLat, double maxLng, int zoom) throws IllegalArgumentException {

        if (minLat > maxLat) {
            throw new IllegalArgumentException("minLat cannot be greater than maxLat");
        }
        if (minLng > maxLng) {
            throw new IllegalArgumentException("minLng cannot be greater than maxLng");
        }

        TileCoord minCoord = getTileCoordsForLatLngZoom(minLat, minLng, zoom);
        TileCoord maxCoord = getTileCoordsForLatLngZoom(maxLat, maxLng, zoom);

        int minX = minCoord.X;
        int minY = minCoord.Y;
        int maxX = maxCoord.X;
        int maxY = maxCoord.Y;

        int difX = maxX - minX;
        int difY = maxY - minY;

        TileCoord[] tileCoords = new TileCoord[(difX + 1) * (Math.abs(difY) + 1)];

        // TMSTileSchema
        if (difY >= 0) {

            int i = 0;
            for (int xIdx = 0; xIdx <= difX; ++xIdx) {
                for (int yIdx = 0; yIdx <= difY; ++yIdx) {
                    tileCoords[i++] = new TileCoord(zoom, minX + xIdx, minY + yIdx);
                }
            }

            // GoogleTileSchema
        } else {

            int i = 0;
            for (int xIdx = 0; xIdx <= difX; ++xIdx) {
                for (int yIdx = 0; yIdx >= difY; --yIdx) {
                    tileCoords[i++] = new TileCoord(zoom, minX + xIdx, minY + yIdx);
                }
            }

        }

        return tileCoords;
    }

    private int numTileCoordsForBBoxZoom(double minLat, double minLng,
            double maxLat, double maxLng, int zoom) {

        TileCoord minCoord = getTileCoordsForLatLngZoom(minLat, minLng, zoom);
        TileCoord maxCoord = getTileCoordsForLatLngZoom(maxLat, maxLng, zoom);

        int minX = minCoord.X;
        int minY = minCoord.Y;
        int maxX = maxCoord.X;
        int maxY = maxCoord.Y;

        int difX = maxX - minX;
        int difY = maxY - minY;

        return (difX + 1) * (Math.abs(difY) + 1);
    }

    /**
     * This gives us a possibly enormous array of all of the tiles we need for a
     * given bbox and zoom range. The returned array is flat, for your convenience!
     *
     * @param minLat of bbox
     * @param minLng of bbox
     * @param maxLat of bbox
     * @param maxLng of bbox
     * @param minZoom of zoom range
     * @param maxZoom of zoom range
     * @return an array of tile coordinates
     * @throws IllegalArgumentException
     */
    public TileCoord[] getTileCoordsForBBoxZoomRange(double minLat, double minLng,
            double maxLat, double maxLng, int minZoom, int maxZoom) throws IllegalArgumentException {

        if (maxZoom < minZoom) {
            throw new IllegalArgumentException("maxZoom must be larger or the same as minZoom!");
        }

        int numTiles = 0;
        for (int z = minZoom; z <= maxZoom; ++z) {
            numTiles += numTileCoordsForBBoxZoom(minLat, minLng, maxLat, maxLng, z);
        }

        TileCoord[] tileCoords = new TileCoord[numTiles];
        int i=0;
        for (int z = minZoom; z <= maxZoom; ++z) {
            TileCoord[] tilesForZoom = getTileCoordsForBBoxZoom(minLat, minLng, maxLat, maxLng, z);
            for (TileCoord coord : tilesForZoom) {
                tileCoords[i++] = coord;
            }
        }

        return tileCoords;
    }

    public abstract TileCoord getTopLeftTile(Tile tile);

    public abstract TileCoord getTopTile(Tile tile);

    public abstract TileCoord getTopRightTile(Tile tile);

    public abstract TileCoord getLeftTile(Tile tile);

    public abstract TileCoord getRightTile(Tile tile);

    public abstract TileCoord getBottomLeftTile(Tile tile);

    public abstract TileCoord getBottomTile(Tile tile);

    public abstract TileCoord getBottomRightTile(Tile tile);
}
