package edu.oregonstate.carto.mapprotocol;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

/**
 * A URLStreamHandlerFactory to install the map protocol for rendering map tiles 
 * on the fly. 
 * Usage: URL.setURLStreamHandlerFactory(new MapURLStreamHandlerFactory());
 * URL.setURLStreamHandlerFactory can only be called once per VM.
 * 
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State
 * University
 */
public class MapURLStreamHandlerFactory implements URLStreamHandlerFactory {

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        if (protocol.equals("map")) {
            return new MapURLStreamHandler();
        }        
        // Force the default factory to retreive stream handler.
        return null;
    }
}
