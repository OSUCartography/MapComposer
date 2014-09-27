package edu.oregonstate.carto.mapcomposer.tilerenderer;

import edu.oregonstate.carto.grid.operators.GridScaleOperator;
import edu.oregonstate.carto.grid.operators.GridScaleToRangeOperator;
import edu.oregonstate.carto.tilemanager.GridTile;
import edu.oregonstate.carto.tilemanager.Tile;
import edu.oregonstate.carto.tilemanager.TileRenderer;
import edu.oregonstate.carto.tilemanager.util.Grid;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author darbyshj
 */
public class IDWGridTileRenderer implements TileRenderer {

    private ArrayList<Point> points = new ArrayList<>();
    private double exponentP = 0.2;

    public IDWGridTileRenderer() {
        initPoints();
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
    }

    public void renderImage(BufferedImage colorizedImage, Grid attribute1Grid, Grid attribute2Grid) {
        int cols = colorizedImage.getWidth();
        int rows = colorizedImage.getHeight();

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int color = interpolateValue(col, row, attribute1Grid, attribute2Grid);
                colorizedImage.setRGB(col, row, color);
            }
        }
    }

    //Computes an interpolated value. Parameters are the x,y coords of a pixel
    public int interpolateValue(int col, int row, Grid attribute1Grid, Grid attribute2Grid) {

        double wTot = 0;
        double weightedSumR = 0;
        double weightedSumG = 0;
        double weightedSumB = 0;

        double attr1AtPixel = attribute1Grid.getValue(col, row);
        double attr2AtPixel = attribute2Grid.getValue(col, row);

        /* loop over all points. For each point, compute distance */
        for (int i = 0; i <= points.size() - 1; i++) {
            Point point = points.get(i);
            double attr1Point = point.getAttribute1();
            double attr2Point = point.getAttribute2();

            double d1 = attr1Point - attr1AtPixel;
            double d2 = attr2Point - attr2AtPixel;
            double distance = Math.sqrt(d1 * d1 + d2 * d2);

            double w = 1 / Math.pow(distance, exponentP);
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

    public void setColorPoints(ArrayList<Point> newPoints) {
        this.points = newPoints;
    }
    
    
    public String getColorPointsString() {
        StringBuilder sb = new StringBuilder();
        for (Point point : points) {
            sb.append(point.getAttribute1());
            sb.append(" ");
            sb.append(point.getAttribute2());
            sb.append(" ");
            sb.append(point.getR());
            sb.append(" ");
            sb.append(point.getG());
            sb.append(" ");
            sb.append(point.getB());
            sb.append("\n");
        }
        return sb.toString();
    }
    
    // FIXME hard coded color points for the moment
    private void initPoints() {
        //Assign point x, y values
        //Set their r, g, b values (0-255)
        //Set grid values (normalized 0-1)

        //Point 1: 0 elevation and .01 precip = brown
        Point point1 = new Point(0, 0);
        point1.setR(131);
        point1.setG(116);
        point1.setB(96);
        //Set precipitation grid value
        point1.setAttribute1(0.0);
        //Set elevation grid value
        point1.setAttribute2(0.0);

        //Point 2: 0.0 elevation and 1.0 precip = green
        Point point2 = new Point(200, 200);
        point2.setR(0);
        point2.setG(100);
        point2.setB(0);
        //Set precipitation grid value
        point2.setAttribute1(1.0);
        //Set elevation grid value
        point2.setAttribute2(0.0);

        //Point 3: 1 elevation and 1 precip = white
        Point point3 = new Point(50, 180);
        point3.setR(255);
        point3.setG(255);
        point3.setB(255);
        //Set precipitation grid value
        point3.setAttribute1(1.0);
        //Set elevation grid value
        point3.setAttribute2(1.0);

        //Point 4: 1 elevation and 0 precip = best color?
        Point point4 = new Point(80, 200);
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

            // FIXME
//            attribute1Grid = new GridScaleToRangeOperator(0, 1).operate(attribute1Grid);
//            attribute2Grid = new GridScaleToRangeOperator(0, 1).operate(attribute2Grid);
            renderImage(img, attribute1Grid, attribute2Grid);

        } catch (IOException ex) {
        }
        return img;
    }

    @Override
    public BufferedImage render(Tile tile) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
