var marker;
var circle;
var map = new L.Map('leaflet', {
    dragging: true
});
var cloudmadeUrl = 'http://{s}.tile.cloudmade.com/396d41ac26d14f84a7ef6176c59f4720/997/256/{z}/{x}/{y}.png',
    cloudmadeAttrib = '',
    cloudmade = new L.TileLayer(cloudmadeUrl, {
        maxZoom: 18,
        attribution: cloudmadeAttrib
    });

map.addLayer(cloudmade);
map.locateAndSetView(16);
map.touchZoom.disable();
map.shiftDragZoom.disable();

// You have to use 'dragstart' & 'dragend'
// movestart fires on initialization...
map.on('dragstart', dragstart);
map.on('dragend', dragend);
map.on('locationfound', onLocationFound);


var markerLocation = new L.LatLng(51.962805, 7.625277);
marker = new L.Marker(markerLocation, {
    draggable: true
});
marker.on('drag', onDragging)

function onLocationFound(e) {
    var radius = e.accuracy / 2;

    map.addLayer(marker);
    marker.setLatLng(e.latlng);

    circle = new L.Circle(e.latlng, radius);
    map.addLayer(circle);
}

/*Updating the Circle position while the marker is
*beeing dragged. Works on Firefox 7.0.1 & Chrome
*but not Android 2.2.1 & 2.3.2
*the circle is beeing updated but there are artifacts 
*the map doesn't get refreshed properly. 
*map.invalidateSize() does help a bit but
*drains performance 
*/

function onDragging(e) {
    //map.invalidateSize()
    circle.setLatLng(e.target.getLatLng());

}

map.on('locationerror', onLocationError);

function onLocationError(e) {
    alert(e.message);
}

/*disabling/enabling marker draggability when the map
*pans since the marker is dragged as well when the map
*is "touchdragged" on Android 2.3.2 & 2.2.1 
*this is likley to happen because both map & marker react to 
*"touchdragging" events.
*I would propose to disable marker dragability every time the map is dragged
* automatically
*/

function dragstart(e) {
    marker.dragging.disable();
}

function dragend(e) {
    marker.dragging.enable();
}