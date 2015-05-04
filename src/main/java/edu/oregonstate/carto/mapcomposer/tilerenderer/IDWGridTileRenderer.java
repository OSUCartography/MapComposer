package edu.oregonstate.carto.mapcomposer.tilerenderer;

import edu.oregonstate.carto.tilemanager.GridTile;
import edu.oregonstate.carto.tilemanager.Tile;
import edu.oregonstate.carto.tilemanager.TileRenderer;
import edu.oregonstate.carto.tilemanager.util.Grid;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.util.ArrayList;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author Jane Darbyshire and Bernie Jenny, Oregon State University
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class IDWGridTileRenderer implements TileRenderer {

    private static final int LUT_SIZE = 256;

    private ArrayList<IDWPoint> points = new ArrayList<>();
    private double exponentP = 1.3;
    private boolean useIDW = false;

    @XmlTransient
    private int[][] lut;

    {
        updateLUT();
    }

    public IDWGridTileRenderer() {
        initPoints();
    }

    /**
     * Updates the color look-up table. Needs to be called after any point or
     * the exponent changes.
     */
    public void updateLUT() {
        lut = new int[LUT_SIZE][LUT_SIZE];
        for (int r = 0; r < LUT_SIZE; r++) {
            double y = r / (LUT_SIZE - 1d);
            for (int c = 0; c < LUT_SIZE; c++) {
                double x = c / (LUT_SIZE - 1d);
                lut[r][c] = interpolateValue(x, y);
            }
        }
    }

    public void colorPointsChanged() {
        updateLUT();
    }

    /**
     * @return the exponentP
     */
    public double getExponentP() {
        return exponentP;
    }

    /**
     * @param exponentP the exponentP to set
     */
    public void setExponentP(double exponentP) {
        this.exponentP = exponentP;
        updateLUT();
    }

    /**
     * @return the points
     */
    public ArrayList<IDWPoint> getPoints() {
        return points;
    }

    public void renderImage(BufferedImage img, Grid attribute1Grid, Grid attribute2Grid) {
        int cols = img.getWidth();
        int rows = img.getHeight();
        int[] imageBuffer = ((DataBufferInt) (img.getRaster().getDataBuffer())).getData();

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                double attr1AtPixel = attribute1Grid.getValue(col, row);
                double attr2AtPixel = attribute2Grid.getValue(col, row);
                //int color = interpolateValue(attr1AtPixel, attr2AtPixel);
                int lutCol = (int) Math.round(attr1AtPixel * (LUT_SIZE - 1));
                int lutRow = (int) Math.round(attr2AtPixel * (LUT_SIZE - 1));
                imageBuffer[row * cols + col] = lut[lutRow][lutCol];
            }
        }
    }

    private double gaussianWeight(double d) {
        double K = exponentP / 10000 /*0.0002*/ * 255 * 255 / 3;
        return Math.exp(-K * d * d);
    }

    private double inverseDistanceWeight(double d) {
        return 1. / Math.pow(d, exponentP);
    }

    public int interpolateValue(double attr1AtPixel, double attr2AtPixel) {

        double wTot = 0;
        double weightedSumR = 0;
        double weightedSumG = 0;
        double weightedSumB = 0;

        /* loop over all points. For each point, compute distance */
        for (IDWPoint point : points) {
            double attr1Point = point.getAttribute1();
            double attr2Point = point.getAttribute2();

            double d1 = attr1Point - attr1AtPixel;
            double d2 = attr2Point - attr2AtPixel;
            double distance = Math.sqrt(d1 * d1 + d2 * d2);

            double w = useIDW ? inverseDistanceWeight(distance) : gaussianWeight(distance);
            weightedSumR += point.getR() * w;
            weightedSumG += point.getG() * w;
            weightedSumB += point.getB() * w;

            wTot += w;
        }

        weightedSumR = Math.min(255, Math.max(0, weightedSumR / wTot));
        weightedSumG = Math.min(255, Math.max(0, weightedSumG / wTot));
        weightedSumB = Math.min(255, Math.max(0, weightedSumB / wTot));

        //Encode r, g, & b values into a single int value using shifting
        return ((int) weightedSumB) | (((int) weightedSumG) << 8) | (((int) weightedSumR) << 16) | (255 << 24);
    }

    public void setColorPoints(ArrayList<IDWPoint> newPoints) {
        this.points = newPoints;
        updateLUT();
    }

    public String getColorPointsString() {
        StringBuilder sb = new StringBuilder();
        for (IDWPoint point : points) {
            if (point.isLonLatDefined()) {
                sb.append(point.getLon());
                sb.append(", ");
                sb.append(point.getLat());
                sb.append(", 0x");
                sb.append(Integer.toHexString(point.getColor().getRGB()));
                sb.append(", ");
            }
        }
        // remove last coma and trailing empty space
        String str = sb.toString();
        if (str.length() >= 2) {
            str = str.substring(0, str.length() - 2);
        }
        return str;
    }

    // FIXME hard coded color points for the moment
    private void initPoints() {
        //Assign point x, y values
        //Set their r, g, b values (0-255)
        //Set grid values (normalized 0-1)

        //Point 1: 0 elevation and .01 precip = brown
        IDWPoint point1 = new IDWPoint();
        point1.setR(131);
        point1.setG(116);
        point1.setB(96);
        //Set precipitation grid value
        point1.setAttribute1(0.0);
        //Set elevation grid value
        point1.setAttribute2(0.0);

        //Point 2: 0.0 elevation and 1.0 precip = green
        IDWPoint point2 = new IDWPoint();
        point2.setR(0);
        point2.setG(100);
        point2.setB(0);
        //Set precipitation grid value
        point2.setAttribute1(1.0);
        //Set elevation grid value
        point2.setAttribute2(0.0);

        //Point 3: 1 elevation and 1 precip = white
        IDWPoint point3 = new IDWPoint();
        point3.setR(255);
        point3.setG(255);
        point3.setB(255);
        //Set precipitation grid value
        point3.setAttribute1(1.0);
        //Set elevation grid value
        point3.setAttribute2(1.0);

        //Point 4: 1 elevation and 0 precip = best color?
        IDWPoint point4 = new IDWPoint();
        point4.setR(0);
        point4.setG(0);
        point4.setB(255);
        //Set precipitation grid value
        point4.setAttribute1(0);
        //Set elevation grid value
        point4.setAttribute2(1);

        points.add(point1);
        points.add(point2);
        points.add(point3);
        points.add(point4);
    }

    public BufferedImage render(Tile tile1, Tile tile2) {
        BufferedImage img = new BufferedImage(Tile.TILE_SIZE * 3,
                Tile.TILE_SIZE * 3, BufferedImage.TYPE_INT_ARGB);
        try {
            Grid attribute1Grid = ((GridTile) tile1).createMegaTile();
            Grid attribute2Grid = ((GridTile) tile2).createMegaTile();
            renderImage(img, attribute1Grid, attribute2Grid);
        } catch (IOException ex) {
        }
        return img;
    }

    @Override
    public BufferedImage render(Tile tile) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Renders an image with all possible colors.
     *
     * @param width Width of the image
     * @param height Height of the image
     * @return The new image.
     */
    public BufferedImage getDiagramImage(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] imageBuffer = ((DataBufferInt) (img.getRaster().getDataBuffer())).getData();
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                double x = c / (width - 1d);
                double y = 1d - r / (height - 1d);
                int lutCol = (int) Math.round(x * (LUT_SIZE - 1));
                int lutRow = (int) Math.round(y * (LUT_SIZE - 1));
                imageBuffer[r * width + c] = lut[lutRow][lutCol];
            }
        }
        return img;
    }

    /**
     * @return the useIDW
     */
    public boolean isUseIDW() {
        return useIDW;
    }

    /**
     * @param useIDW the useIDW to set
     */
    public void setUseIDW(boolean useIDW) {
        this.useIDW = useIDW;
        updateLUT();
    }

}
