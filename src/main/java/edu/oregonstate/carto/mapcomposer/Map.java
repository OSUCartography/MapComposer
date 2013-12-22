package edu.oregonstate.carto.mapcomposer;

import edu.oregonstate.carto.tilemanager.Tile;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 *
 * @author Nicholas Hallahan nick@theoutpost.io
 */
public class Map {

    private final ArrayList<Layer> layers = new ArrayList<Layer>();

    public BufferedImage generateTile(int z, int x, int y) {

        BufferedImage tileImage = new BufferedImage(
                Tile.TILE_SIZE,
                Tile.TILE_SIZE,
                BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = tileImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE);

        for (Layer layer : layers) {
            if (!layer.isVisible()) {
                continue;
            }
            layer.renderToTile(g2d, z, x, y);

        }
        return tileImage;
    }

    public void addLayer(Layer layer) {
        layers.add(layer);
    }

    public void addLayer(int index, Layer layer) {
        layers.add(index, layer);
    }

    public void removeLayer(Layer layer) {
        layers.remove(layer);
    }

    public Layer removeLayer(int index) {
        return layers.remove(index);
    }

    public Layer getLayer(int index) {
        return layers.get(index);
    }

    public int getLayerCount() {
        return layers.size();
    }

    public Layer[] getLayers() {
        return layers.toArray(new Layer[layers.size()]);
    }
    
    public static Map unmarshal(String fileName) {
        try {
            JAXBContext context = JAXBContext.newInstance(Map.class);
            Unmarshaller m = context.createUnmarshaller();
            Map map = (Map)m.unmarshal(new FileInputStream(fileName));
            return map;
            
        } catch (JAXBException ex) {
            // FIXME
            Logger.getLogger(Map.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Map.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return new Map();
    }
    
    public void marshal(String fileName) {
        try {
            JAXBContext context = JAXBContext.newInstance(Map.class);
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            m.marshal(this, new FileOutputStream(fileName));
            
        } catch (JAXBException ex) {
            // FIXME
            Logger.getLogger(Map.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Map.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
