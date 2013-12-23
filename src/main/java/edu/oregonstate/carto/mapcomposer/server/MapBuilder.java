package edu.oregonstate.carto.mapcomposer.server;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Path;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * REST Web Service
 *
 * @author Nicholas Hallahan nick@theoutpost.io
 */
@Path("build-map")
public class MapBuilder {
    
    @Context
    private UriInfo context;
    

    @PUT
    @Path("{mapName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("test/plain")
    public String buildMap(){

        return "hello post";
    }
}
