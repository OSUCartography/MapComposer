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
        this.rootDirectory = new File(rootDir).toURI().toURL();
    }
    
    public FileTileSet(String rootDir, boolean sourceSchemaOpposite) throws MalformedURLException {
        super(sourceSchemaOpposite);
        this.rootDirectory = new File(rootDir).toURI().toURL();
    }
    
    public FileTileSet(String rootDir, 
            Cache cache,
            TileType type, 
            boolean sourceSchemaOpposite) throws MalformedURLException {
        
        super(cache, type, sourceSchemaOpposite);
        this.rootDirectory = new File(rootDir).toURI().toURL();
    }

    @Override
    public URL urlForZXY(int z, int x, int y) {
        if (tmsSchema) {
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
