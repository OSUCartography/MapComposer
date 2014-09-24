/*
 * GridScaleToRangeOperator.java
 *
 * Created on February 2, 2006, 8:33 PM
 *
 */
package edu.oregonstate.carto.grid.operators;

import edu.oregonstate.carto.tilemanager.util.Grid;
import java.util.Arrays;

/**
 *
 * @author Bernhard Jenny, Oregon State University
*/
public class GridScaleToRangeOperator extends ThreadedGridOperator {

    /**
     * New minimum and maximum values in the destination grid.
     */
    private float dstMin, dstMax;

    /**
     * Minimum and maximum values in source grid. Temporary variables to 
     * accelerate computations.
     */
    private float srcMin, srcMax;
    
    /**
     * Creates a new instance of GridScaleToRangeOperator
     */
    public GridScaleToRangeOperator() {
        dstMin = dstMax = 0.f;
    }

    public GridScaleToRangeOperator(float newMin, float newMax) {
        setRange(newMin, newMax);
    }
    
    public GridScaleToRangeOperator(float[] newMinMax) {
        setRange(newMinMax);
    }

    @Override
    public Grid operate(Grid src, Grid dst) {
        final float[] minMax = src.getMinMax();
        srcMin = minMax[0];
        srcMax = minMax[1];
        return super.operate(src, dst);
    }

    @Override
    public void operate(Grid src, Grid dst, int startRow, int endRow) {

        float[][] srcGrid = src.getGrid();
        float[][] dstGrid = dst.getGrid();
        final int nCols = src.getCols();

        final float srcRange = srcMax - srcMin;
        final float dstRange = dstMax - dstMin;
        if (srcRange <= 0 || dstRange <= 0) {
            // set everything in dst to the new minimum
            for (int row = startRow; row < endRow; ++row) {
                Arrays.fill(dstGrid[row], dstMin);
            }
        } else {
            final float f = dstRange / srcRange;
            for (int row = startRow; row < endRow; ++row) {
                float[] srcRow = srcGrid[row];
                float[] dstRow = dstGrid[row];
                for (int col = 0; col < nCols; ++col) {
                    dstRow[col] = (srcRow[col] - srcMin) * f + dstMin;
                }
            }
        }
    }

    public float getNewMin() {
        return dstMin;
    }

    public void setNewMin(float newMin) {
        this.dstMin = newMin;
    }

    public float getNewMax() {
        return dstMax;
    }

    public void setNewMax(float newMax) {
        this.dstMax = newMax;
    }

    final public void setRange(float newMin, float newMax) {
        this.dstMin = newMin;
        this.dstMax = newMax;
    }
    
    final public void setRange(float[] minMax) {
        this.dstMin = minMax[0];
        this.dstMax = minMax[1];
    }

    @Override
    public String getName() {
        return "Scale To Range";
    }
}
