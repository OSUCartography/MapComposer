window.onload = init;

var map;
// a global variable

function init() {

	//creates a new map
	map = new L.Map('map');

	var basemap = 'http://tile.openstreetmap.org/{z}/{x}/{y}.png';
	var oceanFloorLayer = new L.TileLayer(basemap, {
		maxZoom : 19,
		attribution : 'Basemap Tiles: OpenStreetMap'
	});

	//centers map and default zoom level
	map.setView([45.371443,-121.698647], 12);

	//adds the background layer to the map
	map.addLayer(oceanFloorLayer);

	var renderedTilesURL = "../../cache/{z}/{x}/{y}.png?source=http://services.arcgisonline.com/ArcGIS/rest/services/USA_Topo_Maps/MapServer/tile/{z}/{y}/{x}";
	var renderedTiles = new L.TileLayer(renderedTilesURL);
	map.addLayer(renderedTiles);

	var overlayMaps = {
	    "Rendered Tiles": renderedTiles
	};

	L.control.layers(null, overlayMaps).addTo(map);

	$('#zoom-num').html(map.getZoom());

	map.on('zoomend', function(e) {
		$('#zoom-num').html(map.getZoom());
	});
}