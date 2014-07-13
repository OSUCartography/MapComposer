package edu.oregonstate.carto.mapprotocol;

import edu.oregonstate.carto.mapcomposer.Map;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.UnknownServiceException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 * URLConnection for the map protocol, which is used to render map tiles on the 
 * fly. MapURLConnection forwards getInputStream() requests to a map, which 
 * renders the requested map tile.
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State
 * University
 */
public class MapURLConnection extends java.net.URLConnection {

    private static final Pattern tileCoordinatesPattern = Pattern.compile("[0-9]+");

    public MapURLConnection(URL url) {
        super(url);
    }

    /**
     * {@inheritDoc}
     *
     * @return
     * @throws java.io.IOException
     */
    @Override
    public InputStream getInputStream() throws IOException {
        if (getDoInput()) {
            String urlStr = url.toExternalForm().toLowerCase();
            // sample url: map:4/9/11.png
            
            // extract tile coordinates from URL
            int x = 0;
            int y = 0;
            int z = 0;
            Matcher m = tileCoordinatesPattern.matcher(urlStr);
            if (m.find()) {
                z = Integer.parseInt(m.group());
            }
            if (m.find()) {
                x = Integer.parseInt(m.group());
            }
            if (m.find()) {
                y = Integer.parseInt(m.group());
            }

            // render tile
            BufferedImage image = Map.getMap().generateTile(z, x, y);

            // wrap tile image in InputStream
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(image, "png", os);
            InputStream is = new ByteArrayInputStream(os.toByteArray());
            if (getUseCaches()) {
                is = new BufferedInputStream(is);
            }
            return is;
        }
        throw new UnknownServiceException("URL connection cannot do input"); //$NON-NLS-1$
    }

    @Override
    public void connect() throws IOException {
    }

}
