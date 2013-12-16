package edu.oregonstate.carto.tilemanager;

import java.net.URL;
import com.google.common.cache.*;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author Nicholas Hallahan nick@theoutpost.io
 */
public class MemCache implements Cache {

    /**
     * The cache is using the Guava cache. We are only using the most basic
     * functionality from this library. We are limiting the cache size to 5000
     * entries, and the least recently used (LRU) object is removed from the
     * cache once the maximum size is reached.
     */
    private static int MAX_SIZE = 5000;

    /* This is the one instance that exists throughout the entire life-cycle
     * of the program.  The can be retrieved at any time using the static
     * singleton method.  This is to be used to prevent many instances of
     * the same cache hanging around.
     */
    private static MemCache singleton = new MemCache();
//    private ConcurrentHashMap<String, Tile> map = new ConcurrentHashMap<String, Tile>();
    private ConcurrentMap map = CacheBuilder.newBuilder().maximumSize(MAX_SIZE).build().asMap();

    private MemCache() {
    }

    /**
     * Call this method to get the cache, not the constructor. We only want a
     * single cache to exist for the application.
     *
     * @return the singleton instance
     */
    public static MemCache getInstance() {
        return singleton;
    }

    @Override
    public void put(URL url, Tile tile) {
        String urlStr = url.toString();
        map.put(urlStr, tile);
    }

    /**
     * Returns a tile if it is in the cache, returns null otherwise.
     *
     * @param url (the key)
     * @return a tile or null
     */
    @Override
    public Tile get(URL url) {
        Tile tile = (Tile) map.get(url.toString());
//        System.out.println( "MemCache Size: " + map.size() );
//        Tile tile = map.getIfPresent(url.toString());
        return tile;
    }
}
