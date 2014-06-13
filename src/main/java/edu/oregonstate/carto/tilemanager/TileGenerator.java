package edu.oregonstate.carto.tilemanager;

import edu.oregonstate.carto.mapcomposer.Map;
import edu.oregonstate.carto.mapcomposer.gui.ProgressIndicator;
import edu.oregonstate.carto.mapcomposer.gui.SwingWorkerWithProgressIndicator;
import edu.oregonstate.carto.utils.FileUtils;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

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

    public void generateTiles(Map map, ProgressIndicator progress) throws IOException, URISyntaxException {
        // Write tiles to a temporary directory if no directory is specified
        if (directory == null) {
            directory = FileUtils.createTempDirectory();

            // FIXME the directory will be deleted when the virtual machine 
            // terminates. This may still result in a huge number of files.
            // A more clever solution is needed here.
            directory.deleteOnExit();
        }
        TileSet outputTileSet = TileSet.createFileTileSet(directory);
        TileIterator iterator = outputTileSet.createIterator(south, west, north, east, minZoom, maxZoom);
        while (iterator.hasNext() && !progress.isAborted()) {
            Tile tile = iterator.next();
            progress.setMessage("Tile: " + tile.toString());
            BufferedImage img = map.generateTile(tile.getZ(), tile.getX(), tile.getY());
            File file = new File(tile.getURL().toURI());
            // make sure a directory for each zoom level exists
            file.getParentFile().mkdirs();
            ImageIO.write(img, "png", file);
            // FIXME
            System.out.println("created tile " + file.getAbsolutePath());
        }
    }

    public File getDirectory() {
        return directory;
    }
}
