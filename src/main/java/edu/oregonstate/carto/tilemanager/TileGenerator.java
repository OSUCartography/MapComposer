package edu.oregonstate.carto.tilemanager;

import edu.oregonstate.carto.mapcomposer.Map;
import edu.oregonstate.carto.mapcomposer.gui.ProgressIndicator;
import edu.oregonstate.carto.utils.FileUtils;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
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

    private static String formatTimeInterval(final long ms) {
        final long hr = TimeUnit.MILLISECONDS.toHours(ms);
        final long min = TimeUnit.MILLISECONDS.toMinutes(ms - TimeUnit.HOURS.toMillis(hr));
        final long sec = TimeUnit.MILLISECONDS.toSeconds(ms - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min));
        return String.format("%02d:%02d:%02d", hr, min, sec);
    }

    public void generateTiles(Map map, ProgressIndicator progress) throws IOException, URISyntaxException {
        long startTimeMillis = System.currentTimeMillis();

        TileSet outputTileSet = TileSet.createFileTileSet(directory);
        TileIterator iterator = outputTileSet.createIterator(south, west, north, east, minZoom, maxZoom);
        while (iterator.hasNext() && !progress.isAborted()) {
            Tile tile = iterator.next();
            long ms = System.currentTimeMillis() - startTimeMillis;
            progress.setMessage("<html>Current tile: " + tile.toDescription()
                    + "<br>Time spent: " + formatTimeInterval(ms) + "</html>");
            BufferedImage img = map.generateTile(tile.getZ(), tile.getX(), tile.getY());
            File file = new File(tile.getURL().toURI());
            // make sure a directory for each zoom level exists
            file.getParentFile().mkdirs();
            ImageIO.write(img, "png", file);
        }
    }

    public File getDirectory() {
        return directory;
    }
}
