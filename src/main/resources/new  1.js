		//FIX ME: I'm making this too difficult!
		function setColors(colorMarkers){
					
			colorMarkers.getLayers()[1].options.color = "pink" // this sets the box in the color picker of the first circle placed to pink, but it does not make the circle itself pink. 
			//this is b/c it doesn't go back and check its options after being initially created i'm guessing. 
			//however, if you place the circle (click map) BEFORE checking the layer group box, you get a pink circle! sooo hmm. 
				//Could you remove the layer group from the map, then re-add it after resetting the locations and colors as part of setColors...?
			console.log(colorMarkers.getLayers()[1].options.color);
			
			//You have to reset the marker location with the circle marker location at the same time
			//puts the circle in the clicked spot (not the reset spot) if you have the circleMarker layer group checked before clicking
			colorMarkers.getLayers()[0]._latlng = L.latLng(46,-145);
			colorMarkers.getLayers()[1]._latlng = L.latLng(46,-145);
			console.log(colorMarkers.getLayers()[0]._latlng);
			
			//This makes me think it would be better just to use getColors and setColors with the original colorMarkers.getLayers() array. 
			//Could I still use circleArray as a way to pick out the circles in colorMarkersArray? Ex if it's in circleArray...match up the leaflet id's and change the color? Argh!
			//what about matching leaflet ID to leaflet ID of circles that got their colors changed, then setting the color using the dot operator stuff? 
					//But how do you get the circle color to actually change?
			//Then you should be able to work out how to reset the latlng for markers & circleMarkers, and the options.color for circles. 
			//The problem is keeping everything associated together. When a marker/circle pair is created, the circle should always move when that marker does...
			//AHA does that mean we only need to set the latlng of the clear MARKER and the circle will follow...? << - Nope. this part is confusing me. 
		}
		
		//FIX ME:
		function setColors(colorMarkers){
			//remove colorMarkers Layer Group from map
			map.removeLayer(colorMarkers); //not sure if this works with layer groups
			
			//get Leaflet IDs that had colors and locations changed by the Java interface
			
			//figure out which positions those IDs have in the colorMarkers.getLayers() array
			//change/set the marker & circleMarker latlng locations using dot property notation
			//change/set the circleMarker colors
			//if position has desired ._leaflet_id(s), change properties
			
			//Place holder values (31 and 36 are always the first marker and circleMarker, the way the code is currently written)
			var changedMarker = 31;
			var changedCircleMarker = 36;
			var newColor = "red";
			
			for (i = 0; i < colorMarkers.getLayers().length; i++) {
				if (colorMarkersArray[i]._leaflet_id = changedMarker || changedCircleMarker){
					colorMarkers.getLayers()[i]._latlng = L.latLng(46,-145);
				}
			}
			
			for (i = 0; i < colorMarkers.getLayers().length; i++) {
				if (colorMarkersArray[i]._leaflet_id = changedCircleMarker){
					colorMarkers.getLayers()[i].options.color = newColor;
				}
			}
			
			//Add colorMarkers Layer Group back to map
			map.addLayer(colorMarkers); //not sure if this works with layer groups
		}