package edu.oregonstate.carto.mapcomposer.tilerenderer;

import java.awt.Color;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 *
 * @author Jane Darbyshire and Bernie Jenny, Oregon State University
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class IDWPoint {

    // RGB color values for the point (0-255)
    private int r;
    private int g;
    private int b;
    
    // first attribute
    private double attribute1; //ex: precipitation
    
    // second attribute
    private double attribute2; //ex: elevation

    // longitude of the point
    private double lon = Double.NaN;
    
    // latitude of the point
    private double lat = Double.NaN;

    /**
     * Returns a string description with lon/lat, RGB, and the two attributes.
     * @return The description.
     */
    @Override
    public String toString() {
        return lon + " " + lat + " " + r + " " + g + " " + b + " " + attribute1 + " " + attribute2;
    }
    

    /**
     * Returns the red value.
     * @return the red value
     */
    public double getR() {
        return r;
    }

    /**
     * Set the red value.
     * @param r the red to set
     */
    public void setR(int r) {
        this.r = r;
    }

    /**
     * Returns the green value
     * @return the green value
     */
    public double getG() {
        return g;
    }

    /**
     * Set the green value.
     * @param g the green to set
     */
    public void setG(int g) {
        this.g = g;
    }

    /**
     * Returns the blue value.
     * @return the b
     */
    public double getB() {
        return b;
    }

    /**
     * Set blue value.
     * @param b the blue to set
     */
    public void setB(int b) {
        this.b = b;
    }

    /**
     * Returns the current RGB color as a Color object.
     * @return 
     */
    public Color getColor() {
        return new Color(r, g, b);
    }

    /**
     * The the RGB color.
     * @param rgb 
     */
    public void setColor(Color rgb) {
        this.r = rgb.getRed();
        this.g = rgb.getGreen();
        this.b = rgb.getBlue();
    }

    /**
     * @return the attribute1
     */
    public double getAttribute1() {
        return attribute1;
    }

    /**
     * @param attribute1 the attribute1 to set
     */
    public void setAttribute1(double attribute1) {
        this.attribute1 = attribute1;
    }

    /**
     * @return the attribute2
     */
    public double getAttribute2() {
        return attribute2;
    }

    /**
     * @param attribute2 the attribute2 to set
     */
    public void setAttribute2(double attribute2) {
        this.attribute2 = attribute2;
    }

    /**
     * Set longitude and latitude of the point.
     * @param lon Longitude
     * @param lat Latitude
     */
    public void setLonLat(double lon, double lat) {
        this.lon = lon;
        this.lat = lat;
    }

    /**
     * @return the longitude
     */
    public double getLon() {
        return lon;
    }

    /**
     * @return the latitude
     */
    public double getLat() {
        return lat;
    }

    /**
     * Returns true if the longitude and latitude values are defined.
     * @return True if longitude and latitude are defined, false otherwise.
     */
    public boolean isLonLatDefined() {
        return !(Double.isNaN(lon) || Double.isNaN(lat));
    }
}
