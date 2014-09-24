package edu.oregonstate.carto.grid.operators;

import edu.oregonstate.carto.tilemanager.util.Grid;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

/**
 * This operator colorizes a grid.
 *
 * @author Charles Preppernau and Bernie Jenny, Oregon State University
 */
public class ColorizerOperator extends ThreadedGridOperator {

    /**
     * transparent white for void (NaN) values.
     */
    private static final int VOID_COLOR = 0x00000000;

    /**
     * The type of colored visualization this operator can create.
     */
    public enum ColorVisualization {

        GRAY_SHADING("Gray Shading"),
        EXPOSITION("Exposition Color"),
        HYPSOMETRIC_SHADING("Hypsometric Color with Shading"),
        HYPSOMETRIC("Hypsometric Color"),
        LOCAL_HYPSOMETRIC_SHADING("Local Hypsometric Color with Shading"),
        LOCAL_HYPSOMETRIC("Local Hypsometric Color"),
        CONTINUOUS("Continuous Tone (for Illuminated Contours)");

        private final String description;

        private ColorVisualization(String s) {
            description = s;
        }

        public boolean isLocal() {
            return this == LOCAL_HYPSOMETRIC 
                    || this == LOCAL_HYPSOMETRIC_SHADING;
        }

        public boolean isShading() {
            return this == GRAY_SHADING
                    || this == EXPOSITION
                    || this == HYPSOMETRIC_SHADING
                    || this == LOCAL_HYPSOMETRIC_SHADING;
        }
        
        public boolean isColored() {
            return this == EXPOSITION
                    || this == HYPSOMETRIC
                    || this == HYPSOMETRIC_SHADING
                    || this == LOCAL_HYPSOMETRIC
                    || this == LOCAL_HYPSOMETRIC_SHADING;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    // minimum and maximum value in the elevation model
    private float minElev, maxElev;

    // the position of each color on a relative scale between 0 and 1
    // the lowest elevation is at index 0, the highest elevation at index
    // elevations.length - 1
    private float[] colorPositions;

    // a set of colors for the hypsometric tints
    // each elevation in the elevations array has a corresponding color in the 
    // colors array.
    // store the three color components in separate arrays. This avoid repeated
    // slow calls to color.getRed(), color.getGreen() and color.getBlue()
    private int[] reds;
    private int[] greens;
    private int[] blues;

    // colored image output
    private BufferedImage dstImage;

    // the type of visualization created
    private ColorVisualization colorVisualization = ColorVisualization.GRAY_SHADING;

    /**
     * Creates a new instance
     *
     * @param colorVisualization the type of color that is created by this
     * operator
     */
    public ColorizerOperator(ColorVisualization colorVisualization) {
        this.colorVisualization = colorVisualization;
    }

    /**
     * Set the color ramp to use for coloring the grid.
     *
     * @param colors The color definitions
     * @param colorPositions The relative locations of the colors between 0 and
     * 1
     */
    public void setColors(Color[] colors, float[] colorPositions) {
        assert (colors.length == colorPositions.length);

        reds = new int[colors.length];
        greens = new int[colors.length];
        blues = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            reds[i] = colors[i].getRed();
            greens[i] = colors[i].getGreen();
            blues[i] = colors[i].getBlue();
        }
        this.colorPositions = Arrays.copyOf(colorPositions, colorPositions.length);
    }

    /**
     * Computes the linearly interpolated color between the knots on the color
     * gradient
     *
     * @param gridValue the elevation or gray scale value of the current cell
     * @param minVal the lowest possible value of gridValue
     * @param maxVal the highest possible value of gridValue
     * @param mult a multiplication factor applied to the interpolated color
     * @return the interpolated color value
     */
    private int getLinearRGB(float gridVal, float minVal, float maxVal, float mult) {

        //normalize the cell elevation
        float nElev = (gridVal - minVal) / (maxVal - minVal);

        int highestID = colorPositions.length - 1;

        //Loop through the elevation values of the gradient knots, starting at 
        //the second from the top.  Stop after we get to the lowest.
        for (int i = colorPositions.length - 2; i >= 0; i -= 1) {

            //Check to see if nElev is above the highest knot
            if (nElev >= colorPositions[highestID]) {

                //If so, use the color of the highest knot and break out of the loop
                return (int) (mult * blues[highestID])
                        | ((int) (mult * greens[highestID])) << 8
                        | ((int) (mult * reds[highestID])) << 16
                        | 0xFF000000;
            }

            //Check to see if nElev is higher than the current knot
            if (nElev >= colorPositions[i]) {

                // if so, get the distance to the knot and normalize it by the 
                // distance between the two surrounding knots.
                float tu = (nElev - colorPositions[i]) / (colorPositions[i + 1] - colorPositions[i]);
                float tl = 1f - tu;
                //Get the rgb values of the upper knot
                float ur = reds[i + 1];
                float ug = greens[i + 1];
                float ub = blues[i + 1];

                //Get the rgb values of the lower knot
                float lr = reds[i];
                float lg = greens[i];
                float lb = blues[i];

                // interpolate between the colors using the normalized distance
                // to the knots as weights
                float r = mult * (tl * lr + tu * ur);
                float g = mult * (tl * lg + tu * ug);
                float b = mult * (tl * lb + tu * ub);
                return (int) b | ((int) g << 8) | ((int) r << 16) | 0xFF000000;
            }
        }

        //There's one case left to deal with; when nElev is below the lowest knot.                
        //Use the color of the lowest knot.
        return (int) (mult * blues[0])
                | ((int) (mult * greens[0]) << 8)
                | ((int) (mult * reds[0]) << 16)
                | 0xFF000000;
    }

    /**
     * Do not call this method. It will throw an UnsupportedOperationException.
     *
     * @param src
     * @param dst
     * @return
     */
    @Override
    public Grid operate(Grid src, Grid dst) {
        throw new UnsupportedOperationException();
    }

    /**
     * Compute the color image.
     *
     * @param grayShadingGrid Grid with shading values between 0 and 255
     * @param elevationGrid Grid with elevation values.
     * @param image Image to write pixels to. Can be null.
     * @param minElev Lowest elevation in elevationGrid
     * @param maxElev Highest elevation in elevationGrid
     * @return An image with new pixels. This can be identical to the passed
     * image.
     */
    public BufferedImage operate(Grid grayShadingGrid, Grid elevationGrid,
            BufferedImage image, float minElev, float maxElev) {
        dstImage = image;
        this.minElev = minElev;
        this.maxElev = maxElev;
        super.operate(grayShadingGrid, elevationGrid);
        return dstImage;
    }

    /**
     * Compute a colored chunk of this image.
     *
     * @param grayShadingGrid Grid with shaded values between 0 and 255
     * @param elevationGrid Grid with elevation values.
     * @param startRow First row to compute.
     * @param endRow First row of next chunk.
     */
    @Override
    protected void operate(Grid grayShadingGrid, Grid elevationGrid, int startRow, int endRow) {
        int argb = 0;
        float elev;
        int nCols = dstImage.getWidth();
        int[] imageBuffer = ((DataBufferInt) (dstImage.getRaster().getDataBuffer())).getData();
        for (int row = startRow; row < endRow; ++row) {
            for (int col = 0; col < nCols; ++col) {
                //Get the height or gray shading value of the current cell
                float gray = grayShadingGrid.getValue(col, row);
                if (Float.isNaN(gray)) {
                    imageBuffer[row * nCols + col] = VOID_COLOR;
                    continue;
                }
                switch (colorVisualization) {
                    case GRAY_SHADING:
                        // convert the shaded gray value to an ARGB pixel value
                        int g = (int) gray;
                        argb = g | (g << 8) | (g << 16) | 0xFF000000;
                        break;
                    case EXPOSITION:
                        // apply a color ramp to the shaded gray value 
                        argb = getLinearRGB(gray, 0, 255, 1f);
                        break;
                    case HYPSOMETRIC_SHADING:
                    case LOCAL_HYPSOMETRIC_SHADING:
                        // apply a color ramp to the elevation value
                        elev = elevationGrid.getValue(col, row);
                        // multiply the elevation color with the gray value of the shading
                        argb = getLinearRGB(elev, minElev, maxElev, gray / 255f);
                        break;
                    case HYPSOMETRIC:
                    case LOCAL_HYPSOMETRIC:
                        // apply a color ramp to the elevation value
                        elev = elevationGrid.getValue(col, row);
                        argb = getLinearRGB(elev, minElev, maxElev, 1);
                        break;
                }
                imageBuffer[row * nCols + col] = argb;
            }
        }
    }

    @Override
    public String getName() {
        return "Colorizer";
    }

}
