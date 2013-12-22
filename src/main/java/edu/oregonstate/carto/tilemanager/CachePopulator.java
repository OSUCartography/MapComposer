package edu.oregonstate.carto.tilemanager;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author Nicholas Hallahan nick@theoutpost.io
 */
public class CachePopulator {
    
    private final int DEFAULT_NUM_THREADS = 20;
    
    private TileSet tileSet;
    
    private double minLat, minLng, maxLat, maxLng;
    private int minZoom, maxZoom;
    
    public CachePopulator(String httpFormatString,
            double minLat, double minLng, double maxLat, double maxLng, 
            int minZoom, int maxZoom) {
    
        TileSet tileSet = new HTTPTileSet(httpFormatString);
        init(tileSet, minLat, minLng, maxLat, maxLng, minZoom, maxZoom);
    }
    
    public CachePopulator(TileSet tileSet, 
            double minLat, double minLng, double maxLat, double maxLng, 
            int minZoom, int maxZoom) {
        
        init(tileSet, minLat, minLng, maxLat, maxLng, minZoom, maxZoom);
    }
    
    private void init(TileSet tileSet, 
            double minLat, double minLng, double maxLat, double maxLng, 
            int minZoom, int maxZoom) {
        
        this.tileSet = tileSet;
        this.minLat = minLat;
        this.minLng = minLng;
        this.maxLat = maxLat;
        this.maxLng = maxLng;
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
    }
    
    public void populate() {
        populate(DEFAULT_NUM_THREADS);
    }
    
    // NH TODO
    // Redo this so it works with TileIterator
    public void populate(int numThreads) {
//        System.out.println("Populating cache...");
//        Tile[] tiles = tileSet.getTilesForBBoxZoomRange(
//                minLat, minLng, maxLat, maxLng, minZoom, maxZoom);
//        int len = tiles.length;
//        int partitionSize = len / numThreads;
//        ExecutorService pool = Executors.newFixedThreadPool(numThreads);
//        Runnable worker;
//        int i = 0;
//        for(; i < numThreads; ++i) {
//            worker = new CachePopulatorThread(tiles, partitionSize*i, partitionSize*(i+1)-1);
//            pool.execute(worker);
//        }
//        // last one may have a few more tiles than the others
//        worker = new CachePopulatorThread(tiles, partitionSize*i, len-1);
//        pool.execute(worker);
//        pool.shutdown();
//        while (!pool.isTerminated()) {}
//        System.out.println("Finished populating cache!");
    }
            
            
    private class CachePopulatorThread implements Runnable {
        private Tile[] tiles;
        private int idx, endIdx;
        
        public CachePopulatorThread(Tile[] tiles, int startIdx, int endIdx) {
            this.tiles = tiles;
            this.idx = startIdx;
            this.endIdx = endIdx;
        }
        
        @Override
        public void run() {
            while (idx <= endIdx) {
                Tile t = tiles[idx];
                try {
                    t.fetch();
                } catch (IOException ex) {
                    System.out.println("Unable to fetch tile: " + 
                            t.getURL().toString());
                }
                ++idx;
            }
        }
    }
    
}
