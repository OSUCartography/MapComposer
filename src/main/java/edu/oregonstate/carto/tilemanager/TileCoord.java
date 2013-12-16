package edu.oregonstate.carto.tilemanager;

/**
 *
 * @author Nicholas Hallahan nick@theoutpost.io
 */
public class TileCoord {

    public final int Z, X, Y;

    public TileCoord(int z, int x, int y) {
        this.Z = z;
        this.X = x;
        this.Y = y;
    }
}
