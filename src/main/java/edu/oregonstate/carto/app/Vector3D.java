/*
 * Vector3D.java
 *
 * Created on November 21, 2006, 5:13 PM
 *
 */
package edu.oregonstate.carto.app;

/**
 * Vector3D is a vector in three-dimensional space.
 */
public final class Vector3D {

    /**
     * X component of the vector.
     */
    public double x;
    /**
     * Y component of the vector.
     */
    public double y;
    /**
     * Z component of the vector.
     */
    public double z;

    /**
     * Creates a new instance of Vector3D.
     *
     * @param x X component of the vector.
     * @param y Y component of the vector.
     * @param z Z component of the vector.
     */
    public Vector3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Creates a new instance of Vector3D. The new vector has a length of 1. Its
     * orientation is specified by azimuth and a zenith.
     *
     * @param azimuth Azimuth of the direction. Counted from north in counter-
     * clockwise direction. Between 0 and 360 degrees.
     * @param zenith The vertical angle of the light direction from the zenith
     * towards the horizon. Between 0 and 90 degrees.
     */
    public Vector3D(double azimuth, double zenith) {
        double a = azimuth / 180. * Math.PI;
        zenith *= Math.PI / 180.;
        double sinz = Math.sin(zenith);
        x = Math.sin(a) * sinz;
        y = Math.cos(a) * sinz;
        z = Math.cos(zenith);
    }
    
    /**
     * Construct a vector of unary length from an azimuth and a zenith angle.
     * @param azimuthRad From y axis in clockwise direction. In radians.
     * @param zenithRad From z axis towards xy plane. In radians.
     */
    public void direction(double azimuthRad, double zenithRad) {
        double sinz = Math.sin(zenithRad);
        x = Math.sin(azimuthRad) * sinz;
        y = Math.cos(azimuthRad) * sinz;
        z = Math.cos(zenithRad);
    }

    /**
     * Normalize the length of the vector. After calling this method, the vector
     * has a length of 1.
     */
    public void normalize() {
        double length = Math.sqrt(x * x + y * y + z * z);
        x /= length;
        y /= length;
        z /= length;
    }

    /**
     * Normalize the length of a vector. After calling this method, the passed
     * vector has a length of 1.
     * @param v vector to normalize
     * @return passed normalized vector v
     */
    public static Vector3D normalize(Vector3D v) {
        v.normalize();
        return v;
    }

    /**
     * Computes the dot product with another vector.
     *
     * @param v The second vector.
     * @return Returns the scalar product: cos(angle between the vectors).
     */
    public double dotProduct(Vector3D v) {
        return v.x * this.x + v.y * this.y + v.z * this.z;
    }

    /**
     * Compute the dot product of two vectors
     *
     * @param v1 The first vector.
     * @param v2 The second vector.
     * @return The dot product (between 0 and 1)
     */
    public static double dotProduct(Vector3D v1, Vector3D v2) {
        return v1.dotProduct(v2);
    }

    /**
     * Computes a new vector perpendicular to this and a second vector.
     *
     * @param v The second vector.
     * @return A new Vector3D that is perpendicular to the two other vectors.
     */
    public Vector3D vectorProduct(Vector3D v) {
        double vx = y * v.z - z * v.y;
        double vy = z * v.x - x * v.z;
        double vz = x * v.y - y * v.x;
        return new Vector3D(vx, vy, vz);
    }

    /**
     * Adds two vectors together
     *
     * @param v the second vector.
     * @return A new Vector3D that is the sum of the input vectors.
     */
    public Vector3D vectorSum(Vector3D v) {
        double vx = x + v.x;
        double vy = y + v.y;
        double vz = z + v.z;
        return new Vector3D(vx, vy, vz);
    }

    /**
     * Component-wise add of another vector.
     * @param v vector to add
     */
    public void add(Vector3D v) {
        x += v.x;
        y += v.y;
        z += v.z;
    }

    /**
     * Makes the xyz components of a vector accessible from outside this class.
     *
     * @param v the vector whose components we're interested in
     * @return A double array of length 3 containing the x, y, and z components
     * of the vector.
     *
     */
    public static double[] getVectorComponents(Vector3D v) {
        return new double[]{v.x, v.y, v.z};
    }

    /**
     * Compute the vector product of two vectors.
     *
     * @param v1 The first vector.
     * @param v2 The second vector.
     * @return A new vector perpendicular to the plane defined by v1 and v2.
     */
    public static Vector3D vectorProduct(Vector3D v1, Vector3D v2) {
        return v1.vectorProduct(v2);
    }
    
    /**
     * Vector product of two three-dimensional vectors.
     * @param v1x x component of first vector
     * @param v1y y component of first vector
     * @param v1z z component of first vector
     * @param v2x x component of second vector
     * @param v2y y component of second vector
     * @param v2z z component of second vector
     * @param n Vector to store the result.
     */
    public static void vectorProduct(double v1x, double v1y, double v1z, 
            double v2x, double v2y, double v2z, Vector3D n) {
        n.x = v1y * v2z - v1z * v2y;
        n.y = v1z * v2x - v1x * v2z;
        n.z = v1x * v2y - v1y * v2x;
    }
}
