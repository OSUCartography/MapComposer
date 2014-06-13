package edu.oregonstate.carto.tilemanager;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Nicholas Hallahan nick@theoutpost.io
 */
public class TileSet {

    /**
     * Patterns used for replacing z, x, y formatting tokens to create a valid
     * URL for a given tile. \\ is special esc char for regex
     */
    private final static Pattern Z_TOKEN = Pattern.compile("\\{z\\}");
    private final static Pattern X_TOKEN = Pattern.compile("\\{x\\}");
    private final static Pattern Y_TOKEN = Pattern.compile("\\{y\\}");

    /**
     * Format strings for fetching tiles should follow the following format:
     * http://tile.openstreetmap.org/{z}/{x}/{y}.png
     */
    private String urlTemplate;

    /**
     * Tiles in the tile set can only be one type of tile. When constructing
     * tiles, we need to know which type of tile to construct.
     */
    public enum TileType {

        IMAGE, GRID
    }
    /**
     * The constructor sets the type of tiles we will have in the set. This
     * helps us decide what type of tile to construct.
     */
    private final TileType type;

    /**
     * The cache is a content addressable object that will return a given tile
     * if it already has been created.
     */
    private final Cache cache;
    /**
     * If the source tiles adhere to the TMS tile schema instead of the standard
     * OpenStreetMap tile schema, we need to flip the y coordinate to our
     * internal schema (OpenStreetMap schema)
     */
    protected final boolean tmsSchema;

    /**
     * Creates a local file based TileSet
     * @param directory
     * @return 
     */
    public static TileSet createFileTileSet(File directory) {
        StringBuilder sb = new StringBuilder();
        sb.append("file://");
        sb.append(directory.getAbsolutePath());
        sb.append(File.separator);
        sb.append("{z}");
        sb.append(File.separator);
        sb.append("{x}");
        sb.append(File.separator);
        sb.append("{y}.png");
        return new TileSet(sb.toString());
    }

    /**
     *
     * @param urlTemplate Examples:
     * http://tile.openstreetmap.org/{z}/{x}/{y}.png
     * file://C:/Users/nick/Documents/TMS_tiles_MountHood/buildingMask/{z}/{x}/{y}.png
     * @param cache
     * @param type
     * @param tmsSchema
     */
    public TileSet(String urlTemplate, Cache cache, TileType type, boolean tmsSchema) {
        this.urlTemplate = urlTemplate;
        this.type = type;
        this.cache = cache;
        this.tmsSchema = tmsSchema;
    }

    public TileSet(String urlTemplate) {
        this(urlTemplate, MemCache.getInstance(), TileType.IMAGE, false);
    }

    /**
     * Constructs the URL corresponding to a given tile.
     *
     * @param tile
     * @return URL of a tile.
     */
    public URL urlForTile(Tile tile) {
        int z = tile.getZ();
        int x = tile.getX();
        int y = tile.getY();

        return urlForZXY(z, x, y);
    }

    public URL urlForTileCoord(TileCoord coord) {
        int z = coord.Z;
        int x = coord.X;
        int y = coord.Y;

        return urlForZXY(z, x, y);
    }

    /**
     * Returns a URL for tile coordinates (z, x, y).
     *
     * @param z
     * @param x
     * @param y
     * @return URL
     */
    public URL urlForZXY(int z, int x, int y) {
        if (tmsSchema) {
            y = flipY(z, y);
        }

        try {
            Matcher zMatch = Z_TOKEN.matcher(urlTemplate);
            String urlStr = zMatch.replaceAll(String.valueOf(z));

            Matcher xMatch = X_TOKEN.matcher(urlStr);
            urlStr = xMatch.replaceAll(String.valueOf(x));

            Matcher yMatch = Y_TOKEN.matcher(urlStr);
            urlStr = yMatch.replaceAll(String.valueOf(y));

            return new URL(urlStr);
        } catch (MalformedURLException ex) {
            Logger.getLogger(TileSet.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    /**
     * This creates a new tile and puts it in the cache.
     *
     * @param z coordinate
     * @param x coordinate
     * @param y coordinate
     * @return the new tile
     */
    private Tile createTile(int z, int x, int y) {
        if (type == TileType.GRID) {
            return new GridTile(this, z, x, y);
        }
        return new ImageTile(this, z, x, y);
    }

    /**
     * Sometimes the source tile is of the opposite schema than what we want to
     * have represented internal and serve. In this situation, we should fetch a
     * tile with a y coordinate with the alternate schema but internally
     * represent it with our desired schema.
     *
     * @param y
     * @return
     */
    protected int flipY(int z, int y) {
        return (int) ((Math.pow(2, z) - 1) - (double) y);
    }

    /**
     * Gets the tile with the corresponding coordinates from the cache. If not,
     * a new tile is created.
     *
     * @param coord
     * @return the tile we are looking for
     */
    public Tile getTile(TileCoord coord) {
        return getTile(coord.Z, coord.X, coord.Y);
    }

    /**
     * Gets the tile with the corresponding coordinates from the cache. If the
     * tile is not in the cache, a new tile is created.
     *
     * @param z
     * @param x
     * @param y
     * @return the tile we are looking for
     */
    public synchronized Tile getTile(int z, int x, int y) {
        URL url = urlForZXY(z, x, y);
        Tile t = cache.get(url, this);
        if (t == null) {
            t = createTile(z, x, y);
            cache.put(t);
            System.out.println("Cache: put " + t.toString());
        } else {
            System.out.println("Cache: get " + t.toString());
        }
        return t;
    }

    /**
     * The content of tile has changed, the cache has to be updated if if it
     * uses serialized tiles. This method needs to be called when the tile data
     * has been fetched.
     *
     * @param tile
     */
    protected void tileChanged(Tile tile) {
        cache.put(tile);
    }

    public TileIterator createIterator(double minLat, double minLng, double maxLat, double maxLng, int minZoom, int maxZoom) {
        return new TileIterator(this, minLat, minLng, maxLat, maxLng, minZoom, maxZoom);
    }

    public Tile getTopLeftTile(Tile tile) {
        int x = tile.getX() - 1;
        int y = tile.getY() - 1;
        int z = tile.getZ();
        return getTile(z, x, y);
    }

    public Tile getTopTile(Tile tile) {
        int x = tile.getX();
        int y = tile.getY() - 1;
        int z = tile.getZ();
        return getTile(z, x, y);
    }

    public Tile getTopRightTile(Tile tile) {
        int x = tile.getX() + 1;
        int y = tile.getY() - 1;
        int z = tile.getZ();
        return getTile(z, x, y);
    }

    public Tile getLeftTile(Tile tile) {
        int x = tile.getX() - 1;
        int y = tile.getY();
        int z = tile.getZ();
        return getTile(z, x, y);
    }

    public Tile getRightTile(Tile tile) {
        int x = tile.getX() + 1;
        int y = tile.getY();
        int z = tile.getZ();
        return getTile(z, x, y);
    }

    public Tile getBottomLeftTile(Tile tile) {
        int x = tile.getX() - 1;
        int y = tile.getY() + 1;
        int z = tile.getZ();
        return getTile(z, x, y);
    }

    public Tile getBottomTile(Tile tile) {
        int x = tile.getX();
        int y = tile.getY() + 1;
        int z = tile.getZ();
        return getTile(z, x, y);
    }

    public Tile getBottomRightTile(Tile tile) {
        int x = tile.getX() + 1;
        int y = tile.getY() + 1;
        int z = tile.getZ();
        return getTile(z, x, y);
    }

    public Cache getCache() {
        return cache;
    }

    /**
     * @return the urlTemplate
     */
    public String getUrlTemplate() {
        return urlTemplate;
    }

    /**
     * @param urlTemplate the urlTemplate to set
     */
    public void setUrlTemplate(String urlTemplate) {
        this.urlTemplate = urlTemplate;
    }

    /**
     * Returns true if the current URL template string seems to be valid.
     * @return 
     */
    public boolean isURLTemplateValid() {
        // FIXME use regular expression
        return urlTemplate.contains("{x}")
                && urlTemplate.contains("{y}")
                && urlTemplate.contains("{z}")
                && urlTemplate.contains("//")
                && urlTemplate.contains(".")
                && !urlTemplate.contains(" ");
    }
}
