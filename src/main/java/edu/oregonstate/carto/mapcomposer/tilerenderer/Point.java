package edu.oregonstate.carto.mapcomposer.tilerenderer;

import java.awt.geom.Rectangle2D;

/**
 *
 * @author darbyshj
 */
public class Point {

    //create fields for the class
    private double x;
    private double y;
    
    //color values for the point (0-255)
    private double r; 
    private double g; 
    private double b;
    
    private double attribute1; //ex: precipitation
    private double attribute2; //ex: elevation

    //constructor
    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    //Getters & Setters
    /**
     * @return the x
     */
    public double getX() {
        return x;
    }

    /**
     * @param x the x to set
     */
    public void setX(double x) {
        this.x = x;
    }

    /**
     * @return the y
     */
    public double getY() {
        return y;
    }

    /**
     * @param y the y to set
     */
    public void setY(double y) {
        this.y = y;
    }

    public String toString() {
        return " " + x + "/" + y + " ";
    }

    //Return a bounding box of size 0,0 for points
    public Rectangle2D getBoundingBox() {
        Rectangle2D.Double rectangle = new Rectangle2D.Double();
        rectangle.setRect(x, y, 0, 0);
        return rectangle;
    }

    /**
     * @return the r
     */
    public double getR() {
        return r;
    }

    /**
     * @param r the r to set
     */
    public void setR(double r) {
        this.r = r;
    }

    /**
     * @return the g
     */
    public double getG() {
        return g;
    }

    /**
     * @param g the g to set
     */
    public void setG(double g) {
        this.g = g;
    }

    /**
     * @return the b
     */
    public double getB() {
        return b;
    }

    /**
     * @param b the b to set
     */
    public void setB(double b) {
        this.b = b;
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
}
