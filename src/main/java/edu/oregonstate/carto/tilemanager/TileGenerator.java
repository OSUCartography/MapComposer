package edu.oregonstate.carto.tilemanager;

import edu.oregonstate.carto.mapcomposer.Map;
import edu.oregonstate.carto.utils.FileUtils;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import javax.imageio.ImageIO;

/**
 *
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State
 * University
 */
public class TileGenerator {

    private File directory;

    private double west = - 180;
    private double east = 180;
    private double south = - 90;
    private double north = 90;
    private int minZoom = 0;
    private int maxZoom = 4;

    public TileGenerator(File directory) {
        this.directory = directory;
    }

    public TileGenerator() {
        this.directory = null;
    }
    
    public void setExtent(double west, double east, double south, double north) {
        this.west = west;
        this.east = east;
        this.south = south;
        this.north = north;
    }
    
    public void setZoomRange(int minZoom, int maxZoom) {
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
    }

    public void generateTiles(Map map) throws IOException, URISyntaxException {
        // Write tiles to a temporary directory if no directory is specified
        if (directory == null) {
            directory = FileUtils.createTempDirectory();
        }
        TileSet outputTileSet = TileSet.createFileTileSet(directory);
        TileIterator iterator = outputTileSet.createIterator(south, west, north, east, minZoom, maxZoom);
        while (iterator.hasNext()) {
            Tile tile = iterator.next();
            BufferedImage img = map.generateTile(tile.getZ(), tile.getX(), tile.getY());
            File file = new File(tile.getURL().toURI());
            file.getParentFile().mkdirs();
            ImageIO.write(img, "png", file);
            // FIXME
            System.out.println("created tile " + file.getAbsolutePath());
        }
    }

    public URL generateHTMLMapViewer() throws IOException {
        if (directory == null) {
            throw new IllegalStateException("no directory for HTML map");
        }
        String path = directory.getAbsolutePath();
        URL inputUrl = getClass().getResource("/Local_Tiles_TMS.html");
        File dest = new File(path + "/Local_Tiles_TMS.html");
        org.apache.commons.io.FileUtils.copyURLToFile(inputUrl, dest);
        return dest.toURI().toURL();
    }
}
