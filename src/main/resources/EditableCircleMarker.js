/*
 L.EditableCircleMarker is a marker with a radius
 The marker can be moved and the radius can be changed
*/
 
L.EditableCircleMarker = L.Class.extend({
    includes: L.Mixin.Events,
 
    options: {
        weight: 1,
        clickable: false
    },
 
    initialize: function (latlng, radius, options) {
        L.Util.setOptions(this, options);
 
        this._latlng = L.latLng(latlng);
        this._radius = radius;
 
        this._marker = new L.Marker(latlng, {
            icon: new L.Icon({
                iconUrl: this.options.pinUrl,
                className: this.options.className
            }),
            draggable: true
        });
 
        this._circle = new L.Circle(latlng, radius, this.options);
 
        // move circle when marker is dragged
        var me = this;
        this._marker.on('movestart', function() {
            me.fire('movestart');
        });
        this._marker.on('move', function() {
            me._latlng = this._latlng;
            me._marker.setLatLng(this._latlng);
            me._circle.setLatLng(this._latlng);
            me.fire('move');
        });
        this._marker.on('moveend', function() {
            me.fire('moveend');
        });
    },
 
    onAdd: function (map) {
        this._map = map;
        this._marker.onAdd(map);
        this._circle.onAdd(map);
        this.fire('loaded');
    },
 
    onRemove: function (map) {
        this._marker.onRemove(map);
        this._circle.onRemove(map);
        this.fire('unloaded');
    },
 
    getBounds: function() {
        return this._circle.getBounds();
    },
 
    getLatLng: function () {
        return this._latlng;
    },
 
    setLatLng: function (latlng) {
        this._marker.fire('movestart');
        this._latlng = L.latLng(latlng);
        this._marker.setLatLng(this._latlng);
        this._circle.setLatLng(this._latlng);
        this._marker.fire('moveend');
    },
 
    getRadius: function () {
        return this._radius;
    },
 
    setRadius: function (meters) {
        this._marker.fire('movestart');
        this._radius = meters;
        this._circle.setRadius(meters);
        this._marker.fire('moveend');
    },
 
    getCircleOptions: function () {
        return this._circle.options;
    },
 
    setCircleStyle: function (style) {
        this._circle.setStyle(style);
    },
 
});
 
L.editableCircleMarker = function (latlng, radius, options) {
    return new L.EditableCircleMarker(latlng, radius, options);
};