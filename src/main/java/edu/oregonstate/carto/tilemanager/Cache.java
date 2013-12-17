package edu.oregonstate.carto.tilemanager;

import java.net.URL;

/**
 * An interface for a tile cache.
 * @author Bernie Jenny
 */
public interface Cache {
    
    /**
     * The URL of a tile serves as the key to the tile in the cache.
     * The cache can call getURL() on a tile to get the corresponding
     * URL.
     * 
     * @param tile 
     */
    public void put(Tile tile);
     
    public Tile get(URL url, TileSet tileSet);
}
