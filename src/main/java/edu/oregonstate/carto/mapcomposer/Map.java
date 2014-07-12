package edu.oregonstate.carto.mapcomposer;

import edu.oregonstate.carto.tilemanager.Tile;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Nicholas Hallahan nick@theoutpost.io
 */

//Defines root element of XML file
@XmlRootElement

//None of the fields or properties is bound to XML unless they are specifically 
//annotated with JAXB annotations (such as @XmlElement).
@XmlAccessorType(XmlAccessType.NONE)

public class Map {
    
    //@XmlElement defines an element in the XML file. name = "" sets the name 
    //in the XML file.
    @XmlElement (name = "layer")
    private final ArrayList<Layer> layers = new ArrayList<>();

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

        for (int i = layers.size() - 1; i >= 0; i--) {
            Layer layer = layers.get(i);
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
        FileInputStream in = null;
        try {
            JAXBContext context = JAXBContext.newInstance(Map.class);
            Unmarshaller m = context.createUnmarshaller();
            in = new FileInputStream(fileName);
            Map map = (Map)m.unmarshal(in);
            return map;
        } catch (JAXBException | FileNotFoundException ex) {
            Logger.getLogger(Map.class.getName()).log(Level.SEVERE, null, ex);
        } finally  {
            try {
                in.close();
            } catch (IOException ex) {
                Logger.getLogger(Map.class.getName()).log(Level.SEVERE, null, ex);
            }
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
