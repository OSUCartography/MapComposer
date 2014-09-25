package edu.oregonstate.carto.grid.operators;

import edu.oregonstate.carto.tilemanager.util.Grid;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Convert to binary grid with 0 and 1 values.
 *
 * @author Bernhard Jenny, Oregon State University
 */
public class GridBinarizeOperator extends ThreadedGridOperator {

    /**
     * sorted array with values to be converted to 1
     */
    private float[] maskValues;

    public GridBinarizeOperator() {
        maskValues = new float[]{0};
    }

    public GridBinarizeOperator(String maskStr) {
        maskValues = new float[]{0};
        ArrayList<Float> nbrs = new ArrayList<>();
        String delims = "[ ,]+";
        String[] tokens = maskStr.split(delims);
        for (String token : tokens) {
            try {
                float v = Float.parseFloat(token);
                nbrs.add(v);
            } catch (NumberFormatException ex) {
                // ignore
            }
        }
        maskValues = new float[nbrs.size()];
        for (int i = 0; i < maskValues.length; i++) {
            maskValues[i] = nbrs.get(i);
        }
        Arrays.sort(maskValues);
    }

    public GridBinarizeOperator(float[] maskValues) {
        this.maskValues = maskValues;
        Arrays.sort(maskValues);
    }

    public void operate(Grid src, Grid dst, int startRow, int endRow) {
        float[][] srcGrid = src.getGrid();
        float[][] dstGrid = dst.getGrid();
        final int nCols = src.getCols();
        for (int row = startRow; row < endRow; ++row) {
            float[] srcRow = srcGrid[row];
            float[] dstRow = dstGrid[row];
            for (int col = 0; col < nCols; ++col) {
                float v = srcRow[col];
                if (Arrays.binarySearch(maskValues, v) < 0) {
                    dstRow[col] = 0;
                } else {
                    dstRow[col] = 1;
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Binarize";
    }

    /**
     * @return the maskValues
     */
    public float[] getMaskValues() {
        return maskValues;
    }

    /**
     * @param maskValues the maskValues to set
     */
    public void setMaskValues(float[] maskValues) {
        Arrays.sort(maskValues);
        this.maskValues = maskValues;
    }
    
    public float getMinMaskValue() {
        return (maskValues != null && maskValues.length > 0) ? maskValues[0] : 0;
    }

    public float getMaxMaskValue() {
        return (maskValues != null && maskValues.length > 0) ? maskValues[maskValues.length - 1] : 0;
    }
}
