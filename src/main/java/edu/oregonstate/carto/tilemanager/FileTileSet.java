package edu.oregonstate.carto.tilemanager;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Nicholas Hallahan nick@theoutpost.io
 */
public class FileTileSet extends TileSet {

    private URL rootDirectory = null;

    /**
     * Pass in the root directory that your tile set lives in on your local
     * file system. Example:
     *      Example:
     *      "C:\\Users\\nick\\Documents\\TMS_tiles_MountHood\\buildingMask"
     * 
     *      or
     * 
     *      "C:/Users/nick/Documents/TMS_tiles_MountHood/buildingMask"
     * 
     *      or
     * 
     *      "/some/unix/directory/path"
     * 
     * @param rootDir
     * @throws MalformedURLException 
     */
    public FileTileSet(String rootDir) throws MalformedURLException {
        super();
        this.rootDirectory = new File(rootDir).toURI().toURL();
    }
    
    public FileTileSet(String rootDir, boolean sourceSchemaOpposite) throws MalformedURLException {
        super(sourceSchemaOpposite);
        this.rootDirectory = new File(rootDir).toURI().toURL();
    }
    
    /**
     * You can specify a TMS tile schema by passing in a TMSTileSchema object.
     * For example:
     *
     * new FileTileSet(formatString, new TMSTileSchema());
     *
     * The tile type is image by default.
     *
     * @param rootDir 
     * @param schema
     */
    public FileTileSet(String rootDir, TileSchema schema) throws MalformedURLException {
        super(schema);
        this.rootDirectory = new File(rootDir).toURI().toURL();
    }
    
    public FileTileSet(String rootDir, TileSchema schema, boolean sourceSchemaOpposite) throws MalformedURLException {
        super(schema, sourceSchemaOpposite);
        this.rootDirectory = new File(rootDir).toURI().toURL();
    }

    /**
     * You can specify the type of tile: IMAGE or GRID. This constructor sets
     * the schema to GoogleTile by default.
     *
     * @param rootDir
     * @param type
     */
    public FileTileSet(String rootDir, TileType type) throws MalformedURLException {
        super(type);
        this.rootDirectory = new File(rootDir).toURI().toURL();
    }
    
    public FileTileSet(String rootDir, TileType type, boolean sourceSchemaOpposite) throws MalformedURLException {
        super(type, sourceSchemaOpposite);
        this.rootDirectory = new File(rootDir).toURI().toURL();
    }

    /**
     * This constructor explicitly sets both the type and schema.
     *
     * @param rootDir 
     * @param schema
     * @param type
     */
    public FileTileSet(String rootDir, TileSchema schema, TileType type) throws MalformedURLException {
        super(schema, type);
        this.rootDirectory = new File(rootDir).toURI().toURL();
    }
    
    public FileTileSet(String rootDir, TileSchema schema, TileType type, boolean sourceSchemaOpposite) throws MalformedURLException {
        super(schema, type, sourceSchemaOpposite);
        this.rootDirectory = new File(rootDir).toURI().toURL();
    }

    @Override
    public URL urlForZXY(int z, int x, int y) {
        if (sourceSchemaOpposite) {
            y = flipY(z, y);
        }
        
        try {
            return new URL(rootDirectory, z + "/" + x + "/" + y + ".png");
        } catch (MalformedURLException ex) { // FIXME
            Logger.getLogger(HTTPTileSet.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public URL getRootDirectory() {
        return rootDirectory;
    }
    
}
