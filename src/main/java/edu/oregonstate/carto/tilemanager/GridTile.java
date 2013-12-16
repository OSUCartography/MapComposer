package edu.oregonstate.carto.tilemanager;

import edu.oregonstate.carto.importer.BinaryGridReader;
import edu.oregonstate.carto.tilemanager.util.Grid;
import java.io.IOException;
import java.net.URL;

/**
 *
 * @author Nicholas Hallahan nick@theoutpost.io
 */
public class GridTile extends Tile {

    /**
     * This is the DEM grid data of this tile. This in-memory field is populated
     * by fetch.
     */
    private Grid grid;

    public GridTile(TileSet tileSet, int z, int x, int y) {
        super(tileSet, z, x, y);
    }

    public GridTile(TileSet tileSet, TileCoord coord) {
        super(tileSet, coord);
    }

    @Override
    public synchronized Grid fetch() throws IOException {
        if (grid == null) {
            URL url = tileSet.urlForTile(this);
            grid = BinaryGridReader.read(url);
        }
        return grid;
    }
}
