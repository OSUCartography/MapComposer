package edu.oregonstate.carto.mapcomposer.server;

import edu.oregonstate.carto.mapcomposer.Layer;
import edu.oregonstate.carto.mapcomposer.Map;
import edu.oregonstate.carto.tilemanager.TileSet;
import java.awt.image.BufferedImage;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.PathParam;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * REST Web Service
 *
 * @author Nicholas Hallahan nick@theoutpost.io
 */
@Path("example1/{z}/{x}/{y}.png")
public class RestAPIExample1 {
    
    @Context
    private UriInfo context;
    
    private Map map;
    private TileSet esriSatelliteSet, watercolorSet;
    private Layer layer1, layer2;

    public RestAPIExample1() {
        map = new Map();
        
        esriSatelliteSet = new TileSet("http://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}");
//        esriSatelliteSet = new HTTPTileSet(
//                "http://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}",
//                SQLiteCache.getInstance(),
//                TileSet.TileType.IMAGE,
//                false);
        
        watercolorSet = new TileSet("http://tile.stamen.com/watercolor/{z}/{x}/{y}.png");
//        watercolorSet = new HTTPTileSet(
//                "http://tile.stamen.com/watercolor/{z}/{x}/{y}.png",
//                SQLiteCache.getInstance(),
//                TileSet.TileType.IMAGE,
//                false);
        //glacierMask = new FileTileSet("data/TMS_tiles_MountHood/glacierMask", true);
        
        layer1 = new Layer();
        layer2 = new Layer();
        layer1.setImageTileSet(esriSatelliteSet);
        layer2.setImageTileSet(watercolorSet);
        //layer2.setMaskTileSet(glacierMask);
        map.addLayer(layer1);
        map.addLayer(layer2);
    }

    @GET
    @Produces("image/png")
    public Response generateTile(
            @PathParam("z") int z,
            @PathParam("x") int x,
            @PathParam("y") int y,
            @QueryParam("first") String first,
            @QueryParam("second") String second,
            @QueryParam("third") String third) {

        BufferedImage img = map.generateTile(z, x, y);

        return Response.ok(img).build();
    }
}
