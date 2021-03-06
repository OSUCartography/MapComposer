/*
 * AdobeCurveReader.java
 *
 * Created on July 27, 2007, 11:02 AM
 *
 */

package edu.oregonstate.carto.importer;

import edu.oregonstate.carto.mapcomposer.Curve;
import java.io.*;
import java.net.URL;

/**
 * Reader for Adobe ACV file format
 * http://www.adobe.com/devnet-apps/photoshop/fileformatashtml/PhotoshopFileFormats.htm#50577411_32675
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class AdobeCurveReader {
    
    private Curve curves[];
    
    /** Creates a new instance of AdobeCurveReader */
    public AdobeCurveReader() {
    }
    
    public void readACV(URL acvURL) throws IOException {
        
        final int MAX_CURVES_COUNT = 17;
        final int MAX_POINTS_COUNT = 19;
        
        DataInputStream din = null;
        try {
            din = new DataInputStream(acvURL.openStream());
            
            int version = din.readShort();
            if (version != 1 && version != 4) {
                throw new IOException("Not a ACV file.");
            }
            
            int curvesCount = din.readShort();
            if (curvesCount < 1)
                throw new IOException("File does not contain any curve");
            if (curvesCount > MAX_CURVES_COUNT)
                throw new IOException("File contains too many curves");
            this.curves = new Curve[curvesCount];
            
            for (int curveID = 0; curveID < curvesCount; curveID++) {
                int pointsCount = din.readShort();
                if (pointsCount > MAX_POINTS_COUNT)
                    throw new IOException("Curve contains too many points");
                float[] x = new float[pointsCount];
                float[] y = new float[pointsCount];
                for (int ptID = 0; ptID < pointsCount; ptID++) {
                    y[ptID] = din.readShort();
                    x[ptID] = din.readShort();
                }
                
                this.curves[curveID] = new Curve();
                this.curves[curveID].setKnots(x, y);
            }
        } finally {
            if (din != null)
                din.close();
        }
    }
    
    public int getCurvesCount() {
        return this.curves == null ? 0 : this.curves.length;
    }
    
    public Curve getCurve(int id) {
        return this.curves == null ? null : this.curves[id];
    }
    
    public Curve[] getCurves() {
        return this.curves;
    }
     
}
