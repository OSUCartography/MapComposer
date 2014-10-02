package edu.oregonstate.carto.mapcomposer.tilerenderer;

import edu.oregonstate.carto.grid.operators.ColorizerOperator;
import edu.oregonstate.carto.grid.operators.GridOperator;
import edu.oregonstate.carto.grid.operators.GridSlopeOperator;
import edu.oregonstate.carto.grid.operators.IlluminatedContoursOperator;
import edu.oregonstate.carto.grid.operators.ShaderOperator;
import edu.oregonstate.carto.tilemanager.GridTile;
import edu.oregonstate.carto.tilemanager.Tile;
import edu.oregonstate.carto.tilemanager.TileRenderer;
import edu.oregonstate.carto.tilemanager.util.Grid;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 *
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State
 * University
 */
public class ShadingGridTileRenderer implements TileRenderer {

    @Override
    public BufferedImage render(Tile tile) {
        BufferedImage img = new BufferedImage(Tile.TILE_SIZE * 3,
                Tile.TILE_SIZE * 3, BufferedImage.TYPE_INT_ARGB);
        try {
            Grid mergedGrid = ((GridTile) tile).createMegaTile();
            
            ShaderOperator shader = new ShaderOperator();
            Grid shading = shader.operate(mergedGrid);
            ColorizerOperator op = new ColorizerOperator(ColorizerOperator.ColorVisualization.GRAY_SHADING);
            op.operate(shading, mergedGrid, img, 0, 0);
            
            /*
            // uncomment for experimental illuminated contours
            Grid slopeGrid = new GridSlopeOperator().operate(mergedGrid);
            IlluminatedContoursOperator op = new IlluminatedContoursOperator(true, 1, 1, 0.2, true, 315, 500, 0, 255, 2., 90);
            op.renderToImage(img, mergedGrid, slopeGrid, null);
            */
            
        } catch (IOException ex) {
        }
        return img;
    }

}
