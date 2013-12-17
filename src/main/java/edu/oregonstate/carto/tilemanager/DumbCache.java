package edu.oregonstate.carto.tilemanager;

import java.net.URL;

/**
 * A dumb cache that does not cache anything.
 * @author Bernie Jenny
 */
public class DumbCache implements Cache {

    @Override
    public void put(Tile tile) {
        // nowhere to write to
    }

    @Override
    public Tile get(URL url, TileSet tileSet) {
        return null;
    }
    
}
