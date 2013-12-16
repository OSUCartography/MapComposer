package edu.oregonstate.carto.tilemanager;

/**
 *
 * @author Nicholas Hallahan nick@theoutpost.io
 */
public class GoogleTileSchema extends TileSchema {
    
    @Override
    public TileCoord getTileCoordsForLatLngZoom(double lat, double lng, int zoom) {
        TileCoord tmsCoord = super.getTileCoordsForLatLngZoom(lat, lng, zoom);
        
        // python reference code has automatic casting, so it looks kind of messy in java
        int y = (int) ( (Math.pow(2, zoom) - 1) - (double)tmsCoord.Y );
        
        return new TileCoord(tmsCoord.Z, tmsCoord.X, y);
    }
    
    @Override
    public TileCoord getTopLeftTile(Tile tile) {
        int x = tile.getX() - 1;
        int y = tile.getY() - 1;
        int z = tile.getZ();
        
        return new TileCoord(z, x, y);
    }

    @Override
    public TileCoord getTopTile(Tile tile) {
        int x = tile.getX();
        int y = tile.getY() - 1;
        int z = tile.getZ();
        
        return new TileCoord(z, x, y);
    }

    @Override
    public TileCoord getTopRightTile(Tile tile) {
        int x = tile.getX() + 1;
        int y = tile.getY() - 1;
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
        int y = tile.getY() + 1;
        int z = tile.getZ();
        
        return new TileCoord(z, x, y);
    }

    @Override
    public TileCoord getBottomTile(Tile tile) {
        int x = tile.getX();
        int y = tile.getY() + 1;
        int z = tile.getZ();
        
        return new TileCoord(z, x, y);
    }

    @Override
    public TileCoord getBottomRightTile(Tile tile) {
        int x = tile.getX() + 1;
        int y = tile.getY() + 1;
        int z = tile.getZ();
        
        return new TileCoord(z, x, y);
    }
}
