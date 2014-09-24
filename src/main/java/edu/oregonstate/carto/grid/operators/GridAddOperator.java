package edu.oregonstate.carto.grid.operators;

import edu.oregonstate.carto.tilemanager.util.Grid;


/**
 * Add a constant value.
 * @author Bernhard Jenny, Oregon State University
 */
public class GridAddOperator extends ThreadedGridOperator {
    
    private float offset;

    public GridAddOperator(float offset) {
        this.offset = offset;
    }
   
    public void operate(Grid src, Grid dst, int startRow, int endRow) {
        float[][] srcGrid = src.getGrid();
        float[][] dstGrid = dst.getGrid();
        final int nCols = src.getCols();
        for (int row = startRow; row < endRow; ++row) {
            float[] srcRow = srcGrid[row];
            float[] dstRow = dstGrid[row];
            for (int col = 0; col < nCols; ++col) {
                dstRow[col] = srcRow[col] + offset;
            }
        }
    }

    @Override
    public String getName() {
        return "Vertical Offset";
    }

}
