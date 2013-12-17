package edu.oregonstate.carto.tilemanager;

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
public class HTTPTileSet extends TileSet {

    /**
     * Patterns used for replacing z, x, y formatting tokens to create a valid
     * URL for a given tile.
     */
    private final Pattern Z_TOKEN = Pattern.compile("\\{z\\}"); // \\ is special esc char for regex
    private final Pattern X_TOKEN = Pattern.compile("\\{x\\}");
    private final Pattern Y_TOKEN = Pattern.compile("\\{y\\}");
    /**
     * Format strings for fetching tiles should follow the following format:
     * http://tile.openstreetmap.org/{z}/{x}/{y}.png
     */
    private final String httpFormatString;

    /**
     * Creates a standard HTTPTileSet with the given format string.
     * 
     * Format strings for fetching tiles should follow the following format:
     * http://tile.openstreetmap.org/{z}/{x}/{y}.png
     * 
     * @param httpFormatString 
     */
    public HTTPTileSet(String httpFormatString) {
        super();
        this.httpFormatString = httpFormatString;
    }
 
    public HTTPTileSet(String httpFormatString, boolean sourceSchemaOpposite) {
        super(sourceSchemaOpposite);
        this.httpFormatString = httpFormatString;
    }
    
    public HTTPTileSet(String httpFormatString, 
            Cache cache,
            TileType type, 
            boolean sourceSchemaOpposite) {
        
        super(cache, type, sourceSchemaOpposite);
        this.httpFormatString = httpFormatString;
    }
    
    @Override
    public URL urlForZXY(int z, int x, int y) {
        if (tmsSchema) {
            y = flipY(z, y);
        }
        
        try {
            Matcher zMatch = Z_TOKEN.matcher(httpFormatString);
            String urlStr = zMatch.replaceAll(String.valueOf(z));

            Matcher xMatch = X_TOKEN.matcher(urlStr);
            urlStr = xMatch.replaceAll(String.valueOf(x));

            Matcher yMatch = Y_TOKEN.matcher(urlStr);
            urlStr = yMatch.replaceAll(String.valueOf(y));

            return new URL(urlStr);
        } catch (MalformedURLException ex) {
            Logger.getLogger(HTTPTileSet.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    /**
     * @return the httpFormatString
     */
    public String getHttpFormatString() {
        return httpFormatString;
    }

}
