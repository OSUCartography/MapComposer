package edu.oregonstate.carto.tilemanager;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;

//Every non static, non transient field in a JAXB-bound class will be 
//automatically bound to XML, unless annotated by @XmlTransient
@XmlAccessorType(XmlAccessType.FIELD)

/**
 * A set of web map tiles.
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
     * The cache is a content addressable object that will return a given tile
     * if it already has been created.
     */
    @XmlTransient
    private final Cache cache;
    /**
     * If the source tiles adhere to the TMS tile schema instead of the standard
     * OpenStreetMap tile schema, we need to flip the y coordinate to our
     * internal schema (OpenStreetMap schema)
     */
    private boolean tmsSchema;

    /**
     * Creates a local file based TileSet
     *
     * @param directory
     * @return
     */
    public static TileSet createFileTileSet(File directory) {
        StringBuilder sb = new StringBuilder();
        sb.append("file:///");
        // triple slash is OK.
        // http://superuser.com/questions/352133/what-is-the-reason-that-file-urls-start-with-three-slashes-file-etc
        // From RFC 1738 – Uniform Resource Locators (URL):
        // A file URL takes the form:
        // file://<host>/<path>
        // As a special case, <host> can be the string "localhost" or the empty 
        // string; this is interpreted as 'the machine from which the URL is 
        // being interpreted'.

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
     * false easting: coordinate of central meridian
     * 20037508.342789244
     */
    private static final double ORIGIN_SHIFT = 2 * Math.PI * 6378137 / 2.;

    /**
     * Size of a pixel for given zoom level (measured at Equator)
     *
     * @param zoom zoom level
     * @return Size of a pixel at the equator.
     */
    public static double pixelSize(int zoom) {
        // 156543.03392804062 for tileSize 256 pixels
        final double initialResolution = 2 * Math.PI * 6378137 / Tile.TILE_SIZE;
        return initialResolution / Math.pow(2, zoom);
    }

    /**
     * Converts given lat/lon in WGS84 Datum to XY in Spherical Mercator
     * EPSG:900913
     *
     * @param lat Latitude in degrees
     * @param lon Longitude in degrees
     * @param mxy Resulting coordinates in meters in EPSG:900913
     */
    public static void latLonToMeters(double lat, double lon, double[] mxy) {
        mxy[0] = lon * ORIGIN_SHIFT / 180.0;
        double my = Math.log(Math.tan((90 + lat) * Math.PI / 360.0)) / (Math.PI / 180.0);
        mxy[1] = my * ORIGIN_SHIFT / 180.0;
    }

    /**
     * Converts XY point from Spherical Mercator EPSG:900913 to lat/lon in WGS84
     * Datum
     *
     * @param mx Horizontal coordinate in meters in EPSG:900913
     * @param my Vertical coordinate in meters in EPSG:900913
     * @param latLon Resulting latitude and longitude in degrees
     */
    public static void metersToLatLon(double mx, double my, double[] latLon) {
        double lon = (mx / ORIGIN_SHIFT) * 180.0;
        double lat = (my / ORIGIN_SHIFT) * 180.0;
        lat = 180. / Math.PI * (2 * Math.atan(Math.exp(lat * Math.PI / 180.0)) - Math.PI / 2.0);
        latLon[0] = lat;
        latLon[1] = lon;
    }

    /**
     * Converts pixel coordinates in given zoom level of pyramid to EPSG:900913
     *
     * @param px Horizontal pixel coordinate relative to lower left corner of
     * the tiled map
     * @param py Vertical pixel coordinate relative to lower left corner of the
     * tiled map
     * @param zoom Zoom level
     * @param mxy Resulting EPSG:900913 meters.
     */
    public static void pixelsToMeters(double px, double py, int zoom, double[] mxy) {
        final double res = pixelSize(zoom);
        mxy[0] = px * res - ORIGIN_SHIFT;
        mxy[1] = py * res - ORIGIN_SHIFT;
    }

    /**
     * Converts EPSG:900913 to pyramid pixel coordinates in given zoom level
     *
     * @param mx Horizontal coordinate in EPSG:900913 meters.
     * @param my Vertical coordinate in EPSG:900913 meters.
     * @param zoom Zoom level
     * @param pxy Resulting pixel coordinates relative to lower left corner of
     * the tiled map
     */
    public static void metersToPixels(double mx, double my, int zoom, double[] pxy) {
        final double res = pixelSize(zoom);
        pxy[0] = (mx + ORIGIN_SHIFT) / res;
        pxy[1] = (my + ORIGIN_SHIFT) / res;
    }

    /**
     * Converts pixel coordinates relative to the top-left corner of the tile
     * the pixels falls on.
     *
     * @param px Horizontal pixel coordinate
     * @param py Vertical pixel coordinate
     * @param tltxy Resulting pixel coordinate relative to top-left corner of
     * tile
     */
    public static void pixelsToTopLeftTilePixels(double px, double py, double[] tltxy) {
        tltxy[0] = px % Tile.TILE_SIZE;
        tltxy[1] = Tile.TILE_SIZE - py % Tile.TILE_SIZE;
    }

    /**
     * Finds a TMS tile for a given pixel.
     *
     * @param px Horizontal pixel coordinate
     * @param py Vertical pixel coordinate
     * @param txy Resulting TMS tile coordinates
     */
    public static void pixelsToTMSTile(double px, double py, int[] txy) {
        txy[0] = (int) (Math.ceil(px / (double) Tile.TILE_SIZE) - 1);
        txy[1] = (int) (Math.ceil(py / (double) Tile.TILE_SIZE) - 1);
    }

    /**
     * Finds a Google tile for a given pixel.
     *
     * @param px Horizontal pixel coordinate
     * @param py Vertical pixel coordinate
     * @param zoom Zoom level
     * @param txy Resulting Google tile coordinates
     */
    public static void pixelsToGoogleTile(double px, double py, int zoom, int[] txy) {
        txy[0] = (int) (Math.ceil(px / (double) Tile.TILE_SIZE) - 1);
        int ty = (int) (Math.ceil(py / (double) Tile.TILE_SIZE) - 1);
        // move TMS coordinate origin from bottom-left to top-left corner for Google tile
        txy[1] = (int) Math.pow(2, zoom) - 1 - ty;
    }

    /**
     *
     * @param urlTemplate Examples:
     * http://tile.openstreetmap.org/{z}/{x}/{y}.png
     * file:///C:/Users/nick/Documents/TMS_tiles_MountHood/buildingMask/{z}/{x}/{y}.png
     * @param cache
     * @param tmsSchema
     */
    public TileSet(String urlTemplate, Cache cache, boolean tmsSchema) {
        this.urlTemplate = urlTemplate;
        this.cache = cache;
        this.tmsSchema = tmsSchema;
    }

    public TileSet(String urlTemplate) {
        this(urlTemplate, new DumbCache(), false);
    }

    public TileSet() {
        this(null);
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
     * This creates a new tile and adds it to the cache.
     *
     * @param z coordinate
     * @param x coordinate
     * @param y coordinate
     * @return the new tile
     */
    private Tile createTile(int z, int x, int y) {
        if (isImageURLTemplate()) {
            return new ImageTile(this, z, x, y);
        }
        return new GridTile(this, z, x, y);
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
        }
        return t;
    }

    /**
     * Returns a tile for a geographic location.
     * @param z Zoom level
     * @param lon Longitude in degrees
     * @param lat Latitude in degrees
     * @return The tile
     */
    public Tile getTile(int z, double lon, double lat) {
        double[] mxy = new double[2];
        double[] pxy = new double[2];
        int[] txy = new int[2];

        TileSet.latLonToMeters(lat, lon, mxy);
        TileSet.metersToPixels(mxy[0], mxy[1], z, pxy);
        TileSet.pixelsToGoogleTile(pxy[0], pxy[1], z, txy);
        return getTile(z, txy[0], txy[1]);       
    }

    /**
     * The content of tile has changed, the cache has to be updated if it uses
     * serialized tiles. This method needs to be called when the tile data has
     * been fetched.
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
     * Returns true if the passed URL template string seems to be valid.
     *
     * @param template URL template string to test.
     * @return True if passed URL template string seems to be valid.
     */
    public static boolean isURLTemplateValid(String template) {
        // FIXME use regular expression
        return template != null
                && template.contains("{x}")
                && template.contains("{y}")
                && template.contains("{z}")
                && template.contains("//")
                && template.contains(".");
    }

    /**
     * Returns true if the passed URL template string seems to be valid.
     *
     * @return True if the URL template string seems to be valid.
     */
    public boolean isURLTemplateValid() {
        return isURLTemplateValid(urlTemplate);
    }

    /**
     * Returns true if the URL template is for an image tile set, that is, the
     * URL ends with ".png" or ".jpg"
     *
     * @return
     */
    public boolean isImageURLTemplate() {
        return urlTemplate.endsWith(".png") || urlTemplate.endsWith(".jpg");
    }

    /**
     * @return the tmsSchema
     */
    public boolean isTMSSchema() {
        return tmsSchema;
    }

    /**
     * @param tmsSchema the tmsSchema to set
     */
    public void setTMSSchema(boolean tmsSchema) {
        this.tmsSchema = tmsSchema;
    }
}
