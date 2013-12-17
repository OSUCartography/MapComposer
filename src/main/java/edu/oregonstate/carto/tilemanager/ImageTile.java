package edu.oregonstate.carto.tilemanager;

import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;

/**
 *
 * @author Nicholas Hallahan nick@theoutpost.io
 */
public class ImageTile extends Tile {

    /**
     * The BufferedImage that is the raster data of this tile. This in memory
     * field is populated by fetch()
     */
    private BufferedImage img;

    public ImageTile(TileSet tileSet, int z, int x, int y) {
        super(tileSet, z, x, y);
    }

    public ImageTile(TileSet tileSet, TileCoord coord) {
        super(tileSet, coord);
    }

    public ImageTile(TileSet tileSet, DataInputStream inStream) throws IOException {
        super(tileSet, inStream.readInt(), inStream.readInt(), inStream.readInt());
        img = ImageIO.read(inStream);
    }

    @Override
    public synchronized BufferedImage fetch() throws IOException {
        if (img == null) {
            URL url = getTileSet().urlForTile(this);
            img = ImageIO.read(url);
            
            // no need to do this for MemCache
            Cache cache = tileSet.getCache();
            if (cache instanceof SQLiteCache) {
                cache.put(this);
            }
            
            System.out.println("fetched: " + url.toString());
        }
        return img;
    }

    @Override
    protected void toBinary(java.io.DataOutputStream out) throws IOException {
        out.writeInt(getZ());
        out.writeInt(getX());
        out.writeInt(getY());
        if (img != null) {
            ImageIO.write(img, "png", out);
        }
        out.flush();
    }
}
