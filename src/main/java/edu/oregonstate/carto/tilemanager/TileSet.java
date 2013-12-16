package edu.oregonstate.carto.tilemanager;

import java.net.URL;

/**
 *
 * @author Nicholas Hallahan nick@theoutpost.io
 */
public abstract class TileSet {

    /**
     * Tiles in the tile set can only be one type of tile. When constructing
     * tiles, we need to know which type of tile to construct.
     */
    public enum TileType {
        IMAGE, GRID
    }
    /**
     * The constructor sets the type of tiles we will have in the set. This
     * helps us decide what type of tile to construct.
     */
    private final TileType type;
    /**
     * The constructor also sets the schema. once this is set, the TileSchema
     * methods will compute the correct tile relative to a given input tile.
     */
    private TileSchema schema;
    /**
     * The cache is a content addressable object that will return a given tile
     * if it already has been created.
     */
    private MemCache cache = MemCache.getInstance();
    
    /**
     * It should be set in the constructor if we have a source schema that is
     * different from our internal and served tile schema. In this case, we
     * need to transform the y coordinate when we fetch the tile from file 
     * or HTTP but maintain the intended y coordinate internally.
     */
    protected boolean sourceSchemaOpposite = false;

    /**
     * Almost all tile sets on the internet use Google Tile schema, including
     * OpenStreetMap, MapQuest, MapBox, and Esri. We also have the tile type be
     * an image by default.
     */
    public TileSet() {
        schema = new GoogleTileSchema();
        type = TileType.IMAGE;
    }
    
    
    public TileSet(boolean sourceSchemaOpposite) {
        this();
        this.sourceSchemaOpposite = sourceSchemaOpposite;
    }

    
    /**
     * You can specify a TMS tile schema by passing in a TMSTileSchema object.
     * For example:
     *
     * new TileSet(new TMSTileSchema());
     *
     * The tile type is image by default.
     *
     * @param schema
     */
    public TileSet(TileSchema schema) {
        this.schema = schema;
        type = TileType.IMAGE;
    }
    
    public TileSet(TileSchema schema, boolean sourceSchemaOpposite) {
        this(schema);
        this.sourceSchemaOpposite = sourceSchemaOpposite;
    }

    /**
     * You can specify the type of tile: IMAGE or GRID. This constructor sets
     * the schema to GoogleTile by default.
     *
     * @param type
     */
    public TileSet(TileType type) {
        schema = new GoogleTileSchema();
        this.type = type;
    }
    
    public TileSet(TileType type, boolean sourceSchemaOpposite) {
        this(type);
        this.sourceSchemaOpposite = sourceSchemaOpposite;
    }

    /**
     * This constructor explicitly sets both the type and schema.
     *
     * @param schema
     * @param type
     */
    public TileSet(TileSchema schema, TileType type) {
        this.schema = schema;
        this.type = type;
    }
    
    public TileSet(TileSchema schema, TileType type, boolean sourceSchemaOpposite) {
        this(schema, type);
        this.sourceSchemaOpposite = sourceSchemaOpposite;
    }

    /**
     * Constructs the URL corresponding to a given tile.
     *
     * @param tile
     * @return URL of a tile.
     */
    public URL urlForTile(Tile tile) {
        int z = tile.getZ();
        int x = tile.getX();
        int y = tile.getY();

        return urlForZXY(z, x, y);
    }

    public URL urlForTileCoord(TileCoord coord) {
        int z = coord.Z;
        int x = coord.X;
        int y = coord.Y;
        
        return urlForZXY(z, x, y);
    }
    
    /**
     * Returns a properly formatted URL for tile coordinates (z, x, y).
     *
     * @param z
     * @param x
     * @param y
     * @return URL
     */
    public abstract URL urlForZXY(int z, int x, int y);

    public TileSchema getTileSchema() {
        return schema;
    }

    /**
     * This creates a new tile and puts it in the cache.
     *
     * @param z coordinate
     * @param x coordinate
     * @param y coordinate
     * @return the new tile
     */
    private Tile createTile(int z, int x, int y) {
        if (type == TileType.GRID) {
            return new GridTile(this, z, x, y);
        }
        return new ImageTile(this, z, x, y);
    }

    /**
     * Sometimes the source tile is of the opposite schema than what we want to
     * have represented internal and serve. In this situation, we should fetch
     * a tile with a y coordinate with the alternate schema but internally
     * represent it with our desired schema.
     * 
     * @param y
     * @return 
     */
    protected int flipY(int z, int y) {
        return (int) ( (Math.pow(2, z) - 1) - (double)y );
    }
    
    /**
     * Gets the tile with the corresponding coordinates from the cache. If not,
     * a new tile is created.
     *
     * @param coord
     * @return the tile we are looking for
     */
    public Tile getTile(TileCoord coord) {
        return getTile(coord.Z, coord.X, coord.Y);
    }
    
    /**
     * Gets the tile with the corresponding coordinates from the cache. If not,
     * a new tile is created.
     *
     * @param coord
     * @return the tile we are looking for
     */
    public synchronized Tile getTile(int z, int x, int y) {
        URL url = urlForZXY(z, x, y);
        Tile t = cache.get(url);
        if (t == null) {
            t = createTile(z, x, y);
            cache.put(url, t);
        }
        return t;
    }
    
    public Tile[] getTilesForBBoxZoomRange(double minLat, double minLng, double maxLat, double maxLng, int minZoom, int maxZoom) {
        TileCoord[] tileCoords = schema.getTileCoordsForBBoxZoomRange(minLat, minLng, maxLat, maxLng, minZoom, maxZoom);
        Tile[] tiles = new Tile[tileCoords.length];
        for (int i = 0; i < tiles.length; ++i) {
            TileCoord coord = tileCoords[i];
            Tile t = getTile(coord);
            tiles[i] = t;
        }
        return tiles;
    }

    public Tile getTopLeftTile(Tile tile) {
        TileCoord coord = schema.getTopLeftTile(tile);
        return getTile(coord);
    }

    public Tile getTopTile(Tile tile) {
        TileCoord coord = schema.getTopTile(tile);
        return getTile(coord);
    }

    public Tile getTopRightTile(Tile tile) {
        TileCoord coord = schema.getTopRightTile(tile);
        return getTile(coord);
    }

    public Tile getLeftTile(Tile tile) {
        TileCoord coord = schema.getLeftTile(tile);
        return getTile(coord);
    }

    public Tile getRightTile(Tile tile) {
        TileCoord coord = schema.getRightTile(tile);
        return getTile(coord);
    }

    public Tile getBottomLeftTile(Tile tile) {
        TileCoord coord = schema.getBottomLeftTile(tile);
        return getTile(coord);
    }

    public Tile getBottomTile(Tile tile) {
        TileCoord coord = schema.getBottomTile(tile);
        return getTile(coord);
    }

    public Tile getBottomRightTile(Tile tile) {
        TileCoord coord = schema.getBottomRightTile(tile);
        return getTile(coord);
    }
}
