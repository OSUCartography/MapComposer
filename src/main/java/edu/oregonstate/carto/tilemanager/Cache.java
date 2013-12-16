package edu.oregonstate.carto.tilemanager;

import java.net.URL;

/**
 * An interface for a tile cache.
 * @author Bernie Jenny
 */
public interface Cache {
    public void put(URL url, Tile tile);
     
    public Tile get(URL url);
}
