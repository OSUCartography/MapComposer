/*
 * AdobeCurveReader.java
 *
 * Created on July 27, 2007, 11:02 AM
 *
 */

package edu.oregonstate.carto.importer;

import edu.oregonstate.carto.mapcomposer.imageFilters.CurvesFilter;
import java.io.*;
import java.net.URL;

/**
 *
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class AdobeCurveReader {
    
    private CurvesFilter.Curve curves[];
    
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
            //if (version != 1) // CS2 and CS3 write undocumented 4 instead of 1
            //    throw new IOException("Not a ACV file.");
            
            int curvesCount = din.readShort();
            if (curvesCount < 1)
                throw new IOException("File does not contain any curve");
            if (curvesCount > MAX_CURVES_COUNT)
                throw new IOException("File contains too many curves");
            this.curves = new CurvesFilter.Curve[curvesCount];
            
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
                
                this.curves[curveID] = new CurvesFilter.Curve();
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
    
    public CurvesFilter.Curve getCurve(int id) {
        return this.curves == null ? null : this.curves[id];
    }
    
    public CurvesFilter.Curve[] getCurves() {
        return this.curves;
    }
     
}
