package edu.oregonstate.carto.tilemanager;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 *
 * @author Nicholas Hallahan nick@theoutpost.io
 */
public class CachePopulator {
    
    private final int DEFAULT_NUM_THREADS = 20;
    
    private final double minLat, minLng, maxLat, maxLng;
    private final int minZoom, maxZoom;
    
    public CachePopulator(String httpFormatString,
            double minLat, double minLng, double maxLat, double maxLng, 
            int minZoom, int maxZoom) {
    
        this.minLat = minLat;
        this.minLng = minLng;
        this.maxLat = maxLat;
        this.maxLng = maxLng;
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
        
    }
    
    public CachePopulator(TileSet tileSet, 
            double minLat, double minLng, double maxLat, double maxLng, 
            int minZoom, int maxZoom) {
        
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
    
    public void populate(int numThreads) {
        
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
                int z = t.getZ();
                int x = t.getX();
                int y = t.getY();
               
                try {
                    BufferedImage img = (BufferedImage) t.fetch();
                    File f = new File("test/output/testGetTilesChicagoThread-" + z + "-" + x + 
                            "-" + y + ".png");
                    ImageIO.write(img, "png", f);
                    System.out.println("test/output/testGetTilesChicagoThread-" + z + "-" + x + 
                            "-" + y + ".png");
                } catch (IOException ex) {
                    System.out.println("Could not write test/output/testGetTilesChicagoThread-" 
                            + z + "-" + x + "-" + y + ".png");
                }
                ++idx;
            }
        }
    }
    
}
