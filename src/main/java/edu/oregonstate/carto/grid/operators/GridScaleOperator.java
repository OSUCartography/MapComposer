package edu.oregonstate.carto.grid.operators;

import edu.oregonstate.carto.tilemanager.util.Grid;

/**
 * Multiply a grid by a constant factor.
 * @author Bernhard Jenny, Oregon State University
 */
public class GridScaleOperator extends ThreadedGridOperator {
    
    private float scale;

    public GridScaleOperator() {
        this.scale = 1;
    }
    
    public GridScaleOperator(float scale) {
        this.scale = scale;
    }
   
    public void operate(Grid src, Grid dst, int startRow, int endRow) {
        float[][] srcGrid = src.getGrid();
        float[][] dstGrid = dst.getGrid();
        final int nCols = src.getCols();
        for (int row = startRow; row < endRow; ++row) {
            float[] srcRow = srcGrid[row];
            float[] dstRow = dstGrid[row];
            for (int col = 0; col < nCols; ++col) {
                dstRow[col] = srcRow[col] * scale;
            }
        }
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    @Override
    public String getName() {
        return "Scale";
    }

}
