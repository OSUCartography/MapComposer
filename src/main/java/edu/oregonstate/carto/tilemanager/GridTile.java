package edu.oregonstate.carto.tilemanager;

import edu.oregonstate.carto.importer.BinaryGridReader;
import edu.oregonstate.carto.tilemanager.util.Grid;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;

/**
 *
 * @author Nicholas Hallahan nick@theoutpost.io
 * @author Bernie Jenny, Oregon State University
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
        for (float[] row : g) {
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

            // cell size at equator for given zoom level
            double cellSize = (2 * Math.PI * 6378137) / (TILE_SIZE * Math.pow(2, getZ()));
            grid.setCellSize(cellSize);

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
        for (float[] row : g) {
            for (float v : row) {
                out.writeFloat(v);
            }
        }

        out.flush();
    }

    public Grid createMegaTile() throws IOException {

        int tileRows = TILE_SIZE;
        int tileCols = TILE_SIZE;
        int megaTileSize = TILE_SIZE * 3;
        float[][] mergedArray = new float[megaTileSize][megaTileSize];
        try {
            Grid topLeftGrid = ((GridTile) getTopLeftTile()).fetch();
            for (int r = 0; r < tileRows; r++) {
                System.arraycopy(topLeftGrid.getGrid()[r], 0, mergedArray[r], 0, tileCols);
            }
        } catch (IOException ex) {
        }

        try {
            Grid topGrid = ((GridTile) getTopTile()).fetch();
            for (int r = 0; r < tileRows; r++) {
                System.arraycopy(topGrid.getGrid()[r], 0, mergedArray[r], tileCols, tileCols);
            }
        } catch (IOException ex) {
        }

        try {
            Grid topRightGrid = ((GridTile) getTopRightTile()).fetch();
            for (int r = 0; r < tileRows; r++) {
                System.arraycopy(topRightGrid.getGrid()[r], 0, mergedArray[r], tileCols * 2, tileCols);
            }
        } catch (IOException ex) {
        }

        try {
            Grid leftGrid = ((GridTile) getLeftTile()).fetch();
            for (int r = 0; r < tileRows; r++) {
                System.arraycopy(leftGrid.getGrid()[r], 0, mergedArray[r + tileRows], 0, tileCols);
            }
        } catch (IOException ex) {
        }

        try {
            Grid centerGrid = fetch();
            for (int r = 0; r < tileRows; r++) {
                System.arraycopy(centerGrid.getGrid()[r], 0, mergedArray[r + tileRows], tileCols, tileCols);
            }
        } catch (IOException ex) {
        }

        try {
            Grid rightGrid = ((GridTile) getRightTile()).fetch();
            for (int r = 0; r < tileRows; r++) {
                System.arraycopy(rightGrid.getGrid()[r], 0, mergedArray[r + tileRows], tileCols * 2, tileCols);
            }
        } catch (IOException ex) {
        }

        try {
            Grid bottomLeftGrid = ((GridTile) getBottomLeftTile()).fetch();
            for (int r = 0; r < tileRows; r++) {
                System.arraycopy(bottomLeftGrid.getGrid()[r], 0, mergedArray[r + tileRows * 2], 0, tileCols);
            }
        } catch (IOException ex) {
        }

        try {
            Grid bottomGrid = ((GridTile) getBottomTile()).fetch();
            for (int r = 0; r < tileRows; r++) {
                System.arraycopy(bottomGrid.getGrid()[r], 0, mergedArray[r + tileRows * 2], tileCols, tileCols);
            }
        } catch (IOException ex) {
        }

        try {
            Grid bottomRightGrid = ((GridTile) getBottomRightTile()).fetch();
            for (int r = 0; r < tileRows; r++) {
                System.arraycopy(bottomRightGrid.getGrid()[r], 0, mergedArray[r + tileRows * 2], tileCols * 2, tileCols);
            }
        } catch (IOException ex) {
        }
        
        Grid mergedGrid = new Grid(mergedArray, fetch().getCellSize());
        mergedGrid.setWest(fetch().getWest());
        mergedGrid.setNorth(fetch().getNorth());

        return mergedGrid;
    }
}
