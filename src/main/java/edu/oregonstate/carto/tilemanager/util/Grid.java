package edu.oregonstate.carto.tilemanager.util;

import java.awt.geom.*;
import java.text.DecimalFormat;

/**
 * A georeferenced raster grid.
 *
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class Grid {

    private float[][] grid;
    /**
     * Size of a pixel.
     */
    private double cellSize;
    /**
     * Vertical coordinate of top left corner of this image.
     */
    private double north;
    /**
     * Horizontal coordinate of top left corner of this image.
     */
    private double west;

    public class GeoGridStatistics {

        public float min;
        public float max;
        public float mean;
        public int voidCount;

        public GeoGridStatistics(Grid geoGrid) {
            float[][] grid = geoGrid.getGrid();
            min = Float.MAX_VALUE;
            max = -Float.MAX_VALUE;
            double tot = 0;
            int rows = grid.length;
            int cols = getCols();
            voidCount = 0;
            for (int r = 0; r < rows; ++r) {
                float row[] = grid[r];
                for (int c = 0; c < cols; ++c) {
                    float v = row[c];
                    if (Float.isInfinite(v) || Float.isNaN(v)) {
                        ++voidCount;
                    } else {
                        tot += v;
                        if (v < min) {
                            min = v;
                        }
                        if (v > max) {
                            max = v;
                        }
                    }
                }
            }
            mean = (float) (tot / (cols * rows));

            // test for grid with only void values
            if (cols * rows == voidCount) {
                min = max = mean = Float.NaN;
            }
        }

        @Override
        public String toString() {
            return this.toString("\n");
        }

        public String toString(String newLine) {
            return this.toString(true, newLine);
        }

        public String toString(boolean includeNumberOfVoidPoints, String newLine) {
            DecimalFormat format = new DecimalFormat(max - min > 1 ? "#,##0.0" : "#,##0.0#####");
            StringBuilder sb = new StringBuilder();
            sb.append("Minimum: ");
            sb.append(format.format(min));
            sb.append(newLine);
            sb.append("Maximum: ");
            sb.append(format.format(max));
            sb.append(newLine);
            sb.append("Mean: ");
            sb.append(format.format(mean));
            if (includeNumberOfVoidPoints) {
                sb.append(newLine);
                sb.append("Number of void points: ");
                sb.append(new DecimalFormat("#,###").format(voidCount));
                if (voidCount > 0) {
                    sb.append(" (");
                    int rows = grid.length;
                    int cols = getCols();
                    sb.append(new DecimalFormat("0.#").format(voidCount * 100 / (cols * rows)));
                    sb.append("%)");
                }
            }
            return sb.toString();
        }
    }

     /**
     * Creates a new instance of Grid
     */
    public Grid(int cols, int rows, double cellSize) {
        this.initGrid(cols, rows, cellSize);
    }

    public Grid(int cols, int rows, double cellSize, float initialValue) {
        this.initGrid(cols, rows, cellSize);
        for (int r = 0; r < rows; ++r) {
            java.util.Arrays.fill(this.grid[r], initialValue);
        }
    }

    public Grid(float[][] grid, double cellSize) {
        if (grid == null
                || grid.length < 2
                || grid[0] == null
                || grid[0].length < 2
                || cellSize < 0) {
            throw new IllegalArgumentException();
        }

        this.grid = grid;
        this.cellSize = cellSize;
    }

    private void initGrid(int cols, int rows, double cellSize) {
        this.cellSize = cellSize;
        this.grid = new float[rows][cols];
    }

    /**
     * Returns true if the grid has non-zero dimensions and position.
     *
     * @return
     */
    public boolean isWellFormed() {
        return getCols() > 0
                && getRows() > 0
                && getCellSize() > 0
                && !Double.isNaN(getWest())
                && !Double.isNaN(getNorth());
    }

    public void resetGeoreference() {
        this.west = 0;
        this.north = getRows();
        this.cellSize = 1;
    }

    public double getCellSize() {
        return cellSize;
    }

    public void setCellSize(double cellSize) {
        if (cellSize <= 0) {
            throw new IllegalArgumentException();
        }
        this.cellSize = cellSize;
    }

    /**
     * Returns vertical coordinate of top left corner.
     */
    public double getNorth() {
        return north;
    }

    /**
     * Set vertical coordinate of top left corner.
     */
    public void setNorth(double north) {
        this.north = north;
    }

    /**
     * Returns horizontal coordinate of top left corner.
     */
    public double getWest() {
        return west;
    }

    /**
     * Set horizontal coordinate of top left corner.
     */
    public void setWest(double west) {
        this.west = west;
    }

    public boolean hasSameExtensionAndResolution(Grid grid) {

        if (grid == null) {
            return false;
        }
        return getCols() == grid.getCols()
                && getRows() == grid.getRows()
                && getWest() == grid.getWest()
                && getNorth() == grid.getNorth()
                && getCellSize() == grid.getCellSize();

    }

    public Rectangle2D getBounds2D(double scale) {
        int rows = grid.length;
        int cols = getCols();
        final double width = cellSize * (cols - 1);
        final double height = cellSize * (rows - 1);
        final double x = west;
        final double y = north - height;
        return new Rectangle2D.Double(x, y, width, height);
    }

    public boolean isIntersectedByRectangle(java.awt.geom.Rectangle2D rect, double scale) {
        // Test if if the passed rectangle and the bounding box of this object
        // intersect.
        // Use GeometryUtils.rectanglesIntersect and not Rectangle2D.intersects!
        final Rectangle2D bounds = this.getBounds2D(scale);
        return ika.utils.GeometryUtils.rectanglesIntersect(rect, bounds);
    }

    public boolean isPointOnSymbol(java.awt.geom.Point2D point, double tolDist, double scale) {
        Rectangle2D bounds = this.getBounds2D(scale);
        ika.utils.GeometryUtils.enlargeRectangle(bounds, tolDist);
        return bounds.contains(point);
    }

    public void transform(AffineTransform affineTransform) {
    }

    public final float getValue(int col, int row) {
        return grid[row][col];
    }

    public final float getNearestNeighbor(double x, double y) {
        int rows = grid.length;
        int cols = getCols();
        // round to nearest neighbor
        int col = (int) ((x - this.west) / this.cellSize + 0.5);
        int row = (int) ((this.north - y) / this.cellSize + 0.5);
        if (col < 0 || col >= cols || row < 0 || row >= rows) {
            return Float.NaN;
        }
        return grid[row][col];
    }

    /**
     * Bilinear interpolation. See
     * http://www.geovista.psu.edu/sites/geocomp99/Gc99/082/gc_082.htm "What's
     * the point? Interpolation and extrapolation with a regular grid DEM"
     */
    public final float getBilinearInterpol(double x, double y) {

        float h1, h2, h3, h4;
        int rows = grid.length;
        int cols = getCols();
        // column and row of the top left corner
        int col = (int) ((x - this.west) / this.cellSize);
        int row = (int) ((this.north - y) / this.cellSize);

        if (col < 0 || col >= cols || row < 0 || row >= rows) {
            return Float.NaN;        // relative coordinates in the square formed by the four points, scaled to 0..1.
            // The origin is in the lower left corner.
        }
        double relX = (x - this.west) / this.cellSize - col;
        final double south = this.getSouth();
        double relY = (y - south) / this.cellSize - rows + row + 2;

        if (row + 1 < rows) {
            // value at bottom left corner
            h1 = this.getValue(col, row + 1);
            // value at bottom right corner
            h2 = col + 1 < cols ? this.getValue(col + 1, row + 1) : Float.NaN;
        } else {
            h1 = Float.NaN;
            h2 = Float.NaN;
        }

        // value at top left corner
        h3 = this.getValue(col, row);

        // value at top right corner
        h4 = col + 1 < cols ? this.getValue(col + 1, row) : Float.NaN;

        // start with the optimistic case: all values are valid
        return Grid.bilinearInterpolation(h1, h2, h3, h4, relX, relY);
    }

    /**
     * compute a bilinear interpolation.
     *
     * @param h1 value bottom left
     * @param h2 value bottom right
     * @param h3 value top left
     * @param h4 value top right
     * @param relX relative horizontal coordinate (0 .. 1) counted from left to
     * right
     * @param relY relative vertical coordinate (0 .. 1) counted from bottom to
     * top
     * @return The interpolated value
     */
    private static float bilinearInterpolation(float h1, float h2, float h3, float h4, double relX, double relY) {
        return (float) (h1 + (h2 - h1) * relX + (h3 - h1) * relY + (h1 - h2 - h3 + h4) * relX * relY);
    }

    /**
     * This has not been tested or verified !!! ??? From Grass:
     * raster/r.resamp.interp and lib/gis/interp.c
     */
    public final float getBicubicInterpol(double x, double y) {
        
        int rows = grid.length;
        int cols = getCols();
        // column and row of the top left corner
        int col1 = (int) ((x - this.west) / this.cellSize);
        int col0 = col1 - 1;
        int col2 = col1 + 1;
        int col3 = col1 + 2;
        // mirror values along the edges
        if (col1 == 0) {
            col0 = col2;
        } else if (col1 == cols - 1) {
            col2 = col0;
            col3 = col0 - 1;
        }

        int row1 = (int) ((this.north - y) / this.cellSize);
        int row0 = row1 - 1;
        int row2 = row1 + 1;
        int row3 = row1 + 2;
        // mirror values along the edges
        if (row1 == 0) {
            row0 = row2;
        } else if (row1 == rows - 1) {
            row2 = row0;
            row3 = row0 - 1;
        }

        double u = ((x - this.west) - col1 * this.cellSize) / cellSize;
        double v = ((this.north - y) - row1 * this.cellSize) / cellSize;

        double c00 = this.getValue(col0, row0);
        double c01 = this.getValue(col1, row0);
        double c02 = this.getValue(col2, row0);
        double c03 = this.getValue(col3, row0);

        double c10 = this.getValue(col0, row1);
        double c11 = this.getValue(col1, row1);
        double c12 = this.getValue(col2, row1);
        double c13 = this.getValue(col3, row1);

        double c20 = this.getValue(col0, row2);
        double c21 = this.getValue(col1, row2);
        double c22 = this.getValue(col2, row2);
        double c23 = this.getValue(col3, row2);

        double c30 = this.getValue(col0, row3);
        double c31 = this.getValue(col1, row3);
        double c32 = this.getValue(col2, row3);
        double c33 = this.getValue(col3, row3);

        return (float) interp_bicubic(
                u, v,
                c00, c01, c02, c03,
                c10, c11, c12, c13,
                c20, c21, c22, c23,
                c30, c31, c32, c33);
    }

    private double interp_cubic(double u, double c0, double c1, double c2, double c3) {
        return (u * (u * (u * (c3 - 3 * c2 + 3 * c1 - c0) + (-c3 + 4 * c2 - 5 * c1 + 2 * c0)) + (c2 - c0)) + 2 * c1) / 2;
    }

    private double interp_bicubic(double u, double v,
            double c00, double c01, double c02, double c03,
            double c10, double c11, double c12, double c13,
            double c20, double c21, double c22, double c23,
            double c30, double c31, double c32, double c33) {
        double c0 = interp_cubic(u, c00, c01, c02, c03);
        double c1 = interp_cubic(u, c10, c11, c12, c13);
        double c2 = interp_cubic(u, c20, c21, c22, c23);
        double c3 = interp_cubic(u, c30, c31, c32, c33);

        return interp_cubic(v, c0, c1, c2, c3);
    }

    public double getAspect(double x, double y) {

        final float w = this.getBilinearInterpol(x - this.cellSize, y);
        final float e = this.getBilinearInterpol(x + this.cellSize, y);
        final float s = this.getBilinearInterpol(x, y - this.cellSize);
        final float n = this.getBilinearInterpol(x, y + this.cellSize);
        return Math.atan2(n - s, e - w);

    }

    /**
     * Returns the slope for a point in radians. Not tested, probably not
     * correct !!! ???
     *
     * @param x
     * @param y
     * @return
     */
    public double getSlope(double x, double y) {

        final float w = this.getBilinearInterpol(x - this.cellSize, y);
        final float e = this.getBilinearInterpol(x + this.cellSize, y);
        final float s = this.getBilinearInterpol(x, y - this.cellSize);
        final float n = this.getBilinearInterpol(x, y + this.cellSize);
        return Math.atan(Math.hypot((e - w), (n - s)) / this.cellSize); // / 2 !!! ???

    }

    /**
     * Returns the slope for a cell. Slope is computed with four neighbors. Not
     * tested.
     *
     * @param col
     * @param row
     * @return Slope in radians
     */
    public double getSlope(int col, int row) {

        if (row < 1 || row >= this.grid.length - 1 || col < 1 || col >= this.grid[0].length - 1) {
            return Double.NaN;
        }
        final float w = this.grid[row][col - 1];
        final float e = this.grid[row][col + 1];
        final float s = this.grid[row + 1][col];
        final float n = this.grid[row - 1][col];
        return Math.atan(Math.hypot(e - w, n - s) / (2 * this.cellSize));

    }

    /**
     * Change a value in the grid.
     * <B>Important: This will not generate a MapChange event!</B>
     *
     * @param value The new value
     * @param col The column of the value to change.
     * @param row The row of the value to change
     */
    public void setValue(float value, int col, int row) {
        grid[row][col] = value;
    }

    /**
     * Returns the minimum and maximum value of the grid. This can potentially
     * be expensive as the whole grid is parsed.
     */
    public float[] getMinMax() {
        final int rows = grid.length;
        final int cols = getCols();
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        for (int r = 0; r < rows; ++r) {
            float[] row = grid[r];
            for (int c = 0; c < cols; ++c) {
                final float v = row[c];
                if (v < min) {
                    min = v;
                }
                if (v > max) {
                    max = v;
                }
            }
        }
        return new float[]{min, max};
    }

    public GeoGridStatistics getStatistics() {
        return new GeoGridStatistics(this);
    }

    public void cut(Rectangle2D extension) {
        final double w = extension.getMinX();
        final double e = extension.getMaxX();
        final double s = extension.getMinY();
        final double n = extension.getMaxY();
        final int firstRow = (int) ((this.north - n) / this.cellSize);
        final int nbrRows = this.getRows() - firstRow
                - (int) ((s - this.getSouth()) / this.cellSize);
        final int firstCol = (int) ((w - this.west) / this.cellSize);
        final int nbrCols = this.getCols() - firstCol
                - (int) ((this.getEast() - e) / this.cellSize);

        this.cut(firstRow, firstCol, nbrRows, nbrCols);
    }

    public void cut(int firstRow, int firstCol, int newRows, int newCols) {
        float[][] newGrid = new float[newRows][newCols];

        // copy section of grid
        for (int i = 0; i < newRows; i++) {
            System.arraycopy(this.grid[i + firstRow], firstCol, newGrid[i], 0, newCols);
        }

        this.west += firstCol * this.cellSize;
        this.north -= firstRow * this.cellSize;
        this.grid = newGrid;
    }

    public int getCols() {
        return grid.length == 0 ? 0 : grid[0].length;
    }

    public int getRows() {
        return grid.length;
    }

    public double getSouth() {
        return this.north - (grid.length - 1) * this.cellSize;
    }

    public double getEast() {
        return this.west + (getCols() - 1) * this.cellSize;
    }

    public float[][] getGrid() {
        return grid;
    }

    /**
     * Converts an horizontal x coordinate to the next column id to the left of
     * x.
     *
     * @return The column (starting with 0) or -1 if x is not on the grid.
     */
    public int xToColumn(double x) {
        if (x < this.west || x > this.getEast()) {
            return -1;
        }
        return (int) ((x - this.west) / this.cellSize);
    }

    /**
     * Converts an vertical y coordinate to the next row id above y.
     *
     * @return The row (starting with 0) or -1 if y is not on the grid.
     */
    public int yToRow(double y) {
        if (y > this.north || y < this.getSouth()) {
            return -1;
        }
        return (int) ((this.north - y) / this.cellSize);
    }

    @Override
    public String toString() {
        return this.toString("\n");
    }

    public String toString(String newLine) {
        DecimalFormat f = new DecimalFormat(cellSize < 1 ? "#,##0.0#####" : "#,##0.0");
        DecimalFormat intFormat = new DecimalFormat("#,###");
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append(newLine);
        sb.append("Dimension: ");
        sb.append(intFormat.format(this.getCols()));
        sb.append("\u2006\u00D7\u2006"); // multiplication sign surrounded by small spaces
        sb.append(intFormat.format(this.getRows()));
        sb.append(newLine);
        sb.append("Cell size: ");
        sb.append(f.format(cellSize));
        sb.append(newLine);
        sb.append("West: ");
        sb.append(f.format(this.getWest()));
        sb.append(newLine);
        sb.append("North: ");
        sb.append(f.format(this.getNorth()));
        return sb.toString();
    }

    public String toStringWithStatistics() {
        return this.toStringWithStatistics("\n");
    }

    public String toStringWithStatistics(String newLine) {
        StringBuilder sb = new StringBuilder(this.toString(newLine));
        GeoGridStatistics stats = this.getStatistics();
        sb.append(newLine);
        sb.append(stats.toString(newLine));
        return sb.toString();
    }
}
