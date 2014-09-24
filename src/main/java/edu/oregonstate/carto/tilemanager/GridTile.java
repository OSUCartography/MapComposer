package edu.oregonstate.carto.tilemanager;

import edu.oregonstate.carto.importer.BinaryGridReader;
import edu.oregonstate.carto.tilemanager.util.Grid;
import edu.oregonstate.carto.grid.operators.ColorizerOperator;
import edu.oregonstate.carto.grid.operators.ColorizerOperator.ColorVisualization;
import edu.oregonstate.carto.grid.operators.ShaderOperator;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;

/**
 *
 * @author Nicholas Hallahan nick@theoutpost.io
 */
public class GridTile extends Tile<Grid> {

    /**
     * This is the grid data of this tile. This is populated by a call to
     * fetch().
     */
    private Grid grid;

    /**
     * Creates a new instance of GridTile.
     *
     * @param tileSet The tile set for the new tile
     * @param z The zoom level.
     * @param x The horizontal coordinate.
     * @param y The vertical coordinate.
     */
    public GridTile(TileSet tileSet, int z, int x, int y) {
        super(tileSet, z, x, y);
    }

    /**
     * Creates a new instance of GridTile.
     *
     * @param tileSet The tile set for the new tile.
     * @param coord The zoom level and coordinates.
     */
    public GridTile(TileSet tileSet, TileCoord coord) {
        super(tileSet, coord);
    }

    /**
     * Reads a tile from a binary stream. A tile can be written to a stream with
     * toBinary().
     *
     * @param tileSet
     * @param inStream
     * @throws IOException
     */
    public GridTile(TileSet tileSet, DataInputStream inStream) throws IOException {
        super(tileSet, inStream.readInt(), inStream.readInt(), inStream.readInt());
        int cols = inStream.readInt();
        int rows = inStream.readInt();
        double cellSize = inStream.readDouble();
        double west = inStream.readDouble();
        double north = inStream.readDouble();
        float[][] g = new float[rows][cols];
        for (float [] row : g) {
            for (int col = 0; col < cols; col++) {
                row[col] = inStream.readFloat();
            }
        }
        grid = new Grid(g, cellSize);
        grid.setWest(west);
        grid.setNorth(north);
    }
    
    /**
     * Fetches the tile's grid data from memory, http, or file. This may block
     * the calling thread until the grid is loaded.
     *
     * @return The grid for this tile.
     * @throws IOException
     */
    @Override
    public synchronized Grid fetch() throws IOException {
        if (grid == null) {
            TileSet tileSet = getTileSet();
            URL url = tileSet.urlForTile(this);
            grid = BinaryGridReader.read(url);
            tileSet.tileChanged(this);
        }
        return grid;
    }

    /**
     * Serialize this tile to a binary stream.
     *
     * @param out The stream to write to.
     * @throws IOException
     */
    @Override
    protected void toBinary(DataOutputStream out) throws IOException {
        super.toBinary(out);
        
        out.writeInt(grid.getCols());
        out.writeInt(grid.getRows());
        out.writeDouble(grid.getCellSize());
        out.writeDouble(grid.getWest());
        out.writeDouble(grid.getNorth());
                
        float[][] g = grid.getGrid();
        for (float [] row : g) {
            for (float v : row) {
                out.writeFloat(v);
            }
        }
        
        out.flush();
    }

    @Override
    public BufferedImage createMegaTile() throws IOException {
        
        Grid topLeftGrid = ((GridTile)getTopLeftTile()).fetch();
        Grid topGrid = ((GridTile)getTopTile()).fetch();
        Grid topRightGrid = ((GridTile)getTopRightTile()).fetch();
        Grid rightGrid = ((GridTile)getRightTile()).fetch();
        Grid bottomRightGrid = ((GridTile)getBottomRightTile()).fetch();
        Grid bottomGrid = ((GridTile)getBottomTile()).fetch();
        Grid bottomLeftGrid = ((GridTile)getBottomLeftTile()).fetch();
        Grid leftGrid = ((GridTile)getLeftTile()).fetch();
        Grid centerGrid = fetch();

        int tileRows = grid.getRows();
        int tileCols = grid.getCols();
        int megaTileSize = tileRows * 3;
        BufferedImage img = new BufferedImage(megaTileSize, megaTileSize, BufferedImage.TYPE_INT_ARGB);
        float[][] mergedArray = new float[megaTileSize][megaTileSize];
        for (int r = 0; r < tileRows; r++) {
            System.arraycopy(topLeftGrid.getGrid()[r], 0, mergedArray[r], 0, tileCols);
            System.arraycopy(topGrid.getGrid()[r], 0, mergedArray[r], tileCols, tileCols);
            System.arraycopy(topRightGrid.getGrid()[r], 0, mergedArray[r], tileCols * 2, tileCols);
        }
        
        for (int r = 0; r < tileRows; r++) {
            System.arraycopy(leftGrid.getGrid()[r], 0, mergedArray[r + tileRows], 0, tileCols);
            System.arraycopy(centerGrid.getGrid()[r], 0, mergedArray[r + tileRows], tileCols, tileCols);
            System.arraycopy(rightGrid.getGrid()[r], 0, mergedArray[r + tileRows], tileCols * 2, tileCols);
        }
        
        for (int r = 0; r < tileRows; r++) {
            System.arraycopy(bottomLeftGrid.getGrid()[r], 0, mergedArray[r + tileRows * 2], 0, tileCols);
            System.arraycopy(bottomGrid.getGrid()[r], 0, mergedArray[r + tileRows * 2], tileCols, tileCols);
            System.arraycopy(bottomRightGrid.getGrid()[r], 0, mergedArray[r + tileRows * 2], tileCols * 2, tileCols);
        }

        Grid mergedGrid = new Grid(mergedArray, topGrid.getCellSize());
        mergedGrid.setWest(topLeftGrid.getWest());
        mergedGrid.setNorth(topLeftGrid.getNorth());
        
        ShaderOperator shader = new ShaderOperator();
        Grid shading = shader.operate(mergedGrid);
        ColorizerOperator op = new ColorizerOperator(ColorVisualization.GRAY_SHADING);
        return op.operate(shading, mergedGrid, img, 0, 0);
    }
}
