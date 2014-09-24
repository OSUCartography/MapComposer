package edu.oregonstate.carto.grid.operators;

import edu.oregonstate.carto.tilemanager.util.Grid;


/**
 * Compute slope
 * @author Bernhard Jenny, Oregon State University
 */
public class GridSlopeOperator extends ThreadedGridOperator {
    
    public GridSlopeOperator() {
    }
   
    @Override
    public void operate(Grid src, Grid dst, int startRow, int endRow) {
        float[][] dstGrid = dst.getGrid();
        final int nCols = src.getCols();
        for (int row = startRow; row < endRow; ++row) {
            float[] dstRow = dstGrid[row];
            for (int col = 0; col < nCols; ++col) {
                dstRow[col] = (float)src.getSlope(col, row);
            }
        }
    }

    @Override
    public String getName() {
        return "Slope";
    }

}
