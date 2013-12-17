package edu.oregonstate.carto.tilemanager;

import java.util.Iterator;

/**
 * TileIterator iterates over the set of tiles composing of a given bounding
 * box and zoom range. This is specified in the constructor, and the resulting
 * object will spit out each tile in that set by calling the method next().
 * 
 * @author Nicholas Hallahan nick@theoutpost.io
 */
public class TileIterator implements Iterator {

    private static final double INITIAL_RESOLUTION = 2 * Math.PI * 6378137 / Tile.TILE_SIZE;
    private static final double ORIGIN_SHIFT = 2 * Math.PI * 6378137 / 2.0;
    
    private final TileSet tileSet;
    private final double minLat, minLng, maxLat, maxLng;
    private final int minZoom, maxZoom;
    
    private final int minX, minY, maxX, maxY, difX, difY;
    
    private int zIdx = 0;
    private int xIdx = 0;
    private int yIdx = 0;
    
    
    public TileIterator(TileSet tileSet,
            double minLat, double minLng,
            double maxLat, double maxLng,
            int minZoom, int maxZoom) {
        
        if (minLat > maxLat) {
            throw new IllegalArgumentException("minLat cannot be greater than maxLat");
        }
        if (minLng > maxLng) {
            throw new IllegalArgumentException("minLng cannot be greater than maxLng");
        }
        
        this.tileSet = tileSet;
        this.minLat = minLat;
        this.minLng = minLng;
        this.maxLat = maxLat;
        this.maxLng = maxLng;
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
        
        Tile minTile = getTileForLatLngZoom(minLat, minLng, zoom);
        Tile maxTile = getTileForLatLngZoom(maxLat, maxLng, zoom);
        
        minX = minTile.getX();
        minY = minTile.getY();
        maxX = maxTile.getX();
        maxY = maxTile.getY();

        difX = maxX - minX;
        difY = maxY - minY;
    }
            
    @Override
    public boolean hasNext() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Tile next() {
        if (yIdx >= difY) {
            --yIdx;
            
        } else if (xIdx <= difX) {
            yIdx = 0;
            ++xIdx;
        } 
        return tileSet.getTile(zIdx, minX + xIdx, minY + yIdx);
        
        if (xIdx <= difX) {
            if (yIdx >= difY) {
                t = tileSet.getTile(zIdx, minX + xIdx, minY + yIdx);
                --yIdx;
            }
            ++xIdx;
        }
        return null;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Removing a tile makes no sense. You get what you ask for."); 
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
        int xTile = (int) (Math.ceil(xPixels / (double) Tile.TILE_SIZE) - 1);
        int yTile = (int) (Math.ceil(yPixels / (double) Tile.TILE_SIZE) - 1);
        
        // FIXME
        // Convert TMS y coord to Google y coord, should be done in math above...
        yTile = (int) ( (Math.pow(2, zoom) - 1) - (double)yTile );

        return tileSet.getTile(zoom, xTile, yTile);
    }
    
}
