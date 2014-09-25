package edu.oregonstate.carto.mapcomposer.tilerenderer;

import edu.oregonstate.carto.grid.operators.ColorizerOperator;
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
        } catch (IOException ex) {
        }
        return img;
    }

}
