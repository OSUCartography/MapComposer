package edu.oregonstate.carto.tilemanager;

import static edu.oregonstate.carto.tilemanager.TileSchema.TILE_SIZE;
import java.net.URL;

/**
 *
 * @author Nicholas Hallahan nick@theoutpost.io
 */
public class TMSTileSchema extends TileSchema {

    @Override
    public TileCoord getTopLeftTile(Tile tile) {
        int x = tile.getX() - 1;
        int y = tile.getY() + 1;
        int z = tile.getZ();

        return new TileCoord(z, x, y);
    }

    @Override
    public TileCoord getTopTile(Tile tile) {
        int x = tile.getX();
        int y = tile.getY() + 1;
        int z = tile.getZ();

        return new TileCoord(z, x, y);
    }

    @Override
    public TileCoord getTopRightTile(Tile tile) {
        int x = tile.getX() + 1;
        int y = tile.getY() + 1;
        int z = tile.getZ();

        return new TileCoord(z, x, y);
    }

    @Override
    public TileCoord getLeftTile(Tile tile) {
        int x = tile.getX() - 1;
        int y = tile.getY();
        int z = tile.getZ();

        return new TileCoord(z, x, y);
    }

    @Override
    public TileCoord getRightTile(Tile tile) {
        int x = tile.getX() + 1;
        int y = tile.getY();
        int z = tile.getZ();

        return new TileCoord(z, x, y);
    }

    @Override
    public TileCoord getBottomLeftTile(Tile tile) {
        int x = tile.getX() - 1;
        int y = tile.getY() - 1;
        int z = tile.getZ();

        return new TileCoord(z, x, y);
    }

    @Override
    public TileCoord getBottomTile(Tile tile) {
        int x = tile.getX();
        int y = tile.getY() - 1;
        int z = tile.getZ();

        return new TileCoord(z, x, y);
    }

    @Override
    public TileCoord getBottomRightTile(Tile tile) {
        int x = tile.getX() + 1;
        int y = tile.getY() - 1;
        int z = tile.getZ();

        return new TileCoord(z, x, y);
    }
}
