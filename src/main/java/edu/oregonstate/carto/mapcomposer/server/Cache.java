package edu.oregonstate.carto.mapcomposer.server;

import edu.oregonstate.carto.mapcomposer.Layer;
import edu.oregonstate.carto.mapcomposer.Map;
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
@Path("cache/{z}/{x}/{y}.png")
public class Cache {
    
    @Context
    private UriInfo context;

    public Cache() {

    }

    @GET
    @Produces("image/png")
    public Response generateTile(
            @PathParam("z") int z,
            @PathParam("x") int x,
            @PathParam("y") int y,
            @QueryParam("source") String source) { //NH Why does this work? Numbers are put in the format.

        Map map = new Map();
        Layer layer = new Layer(null, source);
        map.addLayer(layer);
        BufferedImage img = map.generateTile(z, x, y);

        return Response.ok(img).build();
    }
}
