# Geo599 - Algorithms for Geovisualization Project Specification

Nicholas Hallahan, Fall 2013

## Purpose

The purpose of my project is to refine and extend MapComposer so that it can
function as a stand-alone web application allowing the user to create pseudo-
natural maps of any location on Earth from a web interface.

## Milestones

I would like to split the project into three phases:

### Phase 1

In addition to consuming imagery and image masks as an individual local file
or a local directory with tiles, I will be adding the functionality to consume
any remote image or tile set via http. This means that MapComposer will be able
to use masks and composite imagery from any http source.

To make the tool useful, I have set up a TileMill server that provides tiles
to function as binary masks for producing a given layer in MapComposer. This
server can also produce tiles of orthoimagery as well as an arbitrary tiled
basemap than can be used as a layer in MapComposer. An installer script will be
provided that does all of the necessary work of importing OpenStreetMap data
into PostGIS and properly connecting PostGIS to TileMill.

### Phase 2

Rather than simply batch rendering the pseudo-natural map to PNG tiles locally, 
I will be creating a Java servlet that provides a REST API 
that generates a given tile with given parameters by an HTTP request.

This will provide the groundwork for creating an HTML5 application that allows
the user to define and view a pseudo-natural tile set dynamically within the
browser.

The REST API that define the tiles to be generated will be inspired from the
general architecture of [MapStack created by Stamen Design](http://mapstack.stamen.com/). 

Here is an example of an URL that gets a dynamically generated tile with parameters
from Mapstack:

```
http://c.sm.mapstack.stamen.com/((toner,$00d5ff[hsl-color])[multiply],(watercolor,parks[destination-in]),(naip,mapbox-water[destination-in]))/11/327/791.png
```

### Phase 3

I will create an HTML5 application that provides similar functionality seen in
the Java SWING user interface. The pseudo-natural map will be rendered on the
fly as the user changes parameters. These generated tiles will be seen in a 
Leaflet Map.

## Schedule

Phase 1: Wed Dec 4 2013

Phase 2: Wed Dec 18 2013

Phase 3: Wed Jan 1 2014

## Input / Output Data Types and Formats

### Input

**Binary masks** will continue to be used to define the pixels that a corresponding
layer will be rendered to. The pixels in the mask PNGs will function as a boolean
flag. Black is on, white is off. 

Layers will also consume **imagery** from any http WMTS (Web Map Tile Service). This
will adhere to the standard http schema of:

```
http://someurl.com/params/{z}/{x}/{y}.png
```

### Output

The user can continue to render tiles to the **file system** in a batch mode. The new Java
Servlet functionality will render a given tile on the fly based on the paremeters of
the **RESTful URL**.

Also, the rendered product can be a single PNG image for static usage.

## Code Structure

todo

## UML

todo

## Potential Difficulties

### Phase 1

Because tiles will be received via http, it may take some time to receive the tile
being requested. To address this issue, the rendering for a given tile should be
coupled to its own thread. Currently, the each tile will block the requests for all
of the rest, and this may produce an extremely slow application without doing this
in a properly multi-threaded design.

Setting up TileMill with your own PostGIS database using OpenStreetMap data is not
a trivial task, so I will provide Vagrant provisioning script that will set up a
virtual machine that has all of this set up. To avoid coupling MapComposer to this
complexity, TileMill will read tiles via http, and the application will be set to
read from the proper url to get tiles from the given TileMill server. Also, this
de-coupled architecture will allow the user to install MapComposer without TileMill
and instead receive masks from a remote http server.

### Phase 2

This will be fairly straight-forward. The main challenge will be learning how to
use Java Servlets.

### Phase 3

This part will be a completely client-side development effort. Because the tiles
with the applicable parameters will be through a REST API, the server and the HTML5
application are not coupled together. The challenges will be less of a technical
nature and more of a design and HCI nature. The main challenge will be to construct
an intuitive interface for a user without knowledge of the architecture of the
project.
