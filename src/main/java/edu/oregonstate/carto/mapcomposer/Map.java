package edu.oregonstate.carto.mapcomposer;

import edu.oregonstate.carto.tilemanager.Tile;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
 * Main model class. Contains an array of layers.
 *
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State
 * University
 * @author Nicholas Hallahan nick@theoutpost.io
 * @author Jane Darbyshire, Cartography and Geovisualization Group, Oregon State
 * University
 */
//Defines root element of XML file
@XmlRootElement

//None of the fields or properties is bound to XML unless they are specifically 
//annotated with JAXB annotations (such as @XmlElement).
@XmlAccessorType(XmlAccessType.NONE)

public class Map {

    /**
     * A static reference to the map, required for the "map" URL protocol. This
     * can be extended to an array if multiple maps are to be supported.
     */
    private static Map map;

    /**
     * Access to the static map reference.
     *
     * @return
     */
    public static Map getMap() {
        return map;
    }

    //@XmlElement defines an element is the XML file. name = "" sets the name 
    //in the XML file.
    @XmlElement(name = "layer")
    private final ArrayList<Layer> layers = new ArrayList<>();

    public Map() {
        Map.map = this;
    }

    /**
     * Renders an image for one tile.
     *
     * @param z Zoom level of the tile.
     * @param x X coordinate of the tile.
     * @param y Y coordinate of the tile.
     * @return The rendered image.
     */
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

    private static JAXBContext getJAXBContext() throws JAXBException {
        String packageName = Map.class.getPackage().getName();
        return JAXBContext.newInstance(packageName, Map.class.getClassLoader());
    }

    public static Map unmarshal(InputStream is) throws JAXBException {
        return (Map) getJAXBContext().createUnmarshaller().unmarshal(is);
    }

    public static Map unmarshal(byte[] buf) throws JAXBException {
        return unmarshal(new ByteArrayInputStream(buf));
    }

    public static Map unmarshal(String fileName) throws JAXBException, FileNotFoundException {
        FileInputStream is = null;
        try {
            is = new FileInputStream(fileName);
            return unmarshal(is);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Map.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void marshal(OutputStream os) throws JAXBException {
        Marshaller m = getJAXBContext().createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        m.marshal(this, os);
    }

    public byte[] marshal() throws JAXBException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        marshal(baos);
        return baos.toByteArray();
    }

    public void marshal(String fileName) throws JAXBException, FileNotFoundException {
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(fileName);
            marshal(os);
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Map.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
