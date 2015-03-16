package edu.oregonstate.carto.mapcomposer.gui;

import edu.oregonstate.carto.mapcomposer.tilerenderer.IDWPoint;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

/**
 * A wrapper around a JavaFX WebView with a HTML map. All non-static calls must
 * be made from the JavaFX thread.
 *
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State
 * University
 */
public class JavaFXMap {

    /**
     * HTML template for map
     */
    private static final String HTML_MAP_TEMPLATE = loadHtmlMapTemplate();

    private static String loadHtmlMapTemplate() {
        URL inputUrl = JavaFXMap.class.getResource("/index_with_variables.html");
        try {
            File file = new File(inputUrl.toURI());
            return org.apache.commons.io.FileUtils.readFileToString(file);
        } catch (URISyntaxException | IOException ex) {
            Logger.getLogger(MapComposerPanel.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    /**
     * A JavaFX web renderer for the preview map. Must only be accessed from the
     * JavaFX thread, not the Swing event dispatch thread. To be initialized in
     * the JavaFX thread.
     */
    private WebView webView;

    /**
     * Initialize the map and add it to the passed JFXPanel. Must be called from
     * JavaFX thread.
     *
     * @param fxPanel The panel to add the map to.
     * @param html HTML content to load
     */
    public void init(JFXPanel fxPanel, String html) {
        assert Platform.isFxApplicationThread();

        webView = new WebView();
        Group group = new Group();
        group.getChildren().add(webView);
        Scene scene = new Scene(group);
        // setScene can be called on the JavaFX thread or the event dispatch thread 
        fxPanel.setScene(scene);

        // pipe JavaScript alert() calls to the standard output
        webView.getEngine().setOnAlert(new EventHandler<WebEvent<String>>() {
            @Override
            public void handle(WebEvent<String> t) {
                System.out.println(t);
            }
        });

        final Worker<Void> worker = webView.getEngine().getLoadWorker();
        worker.exceptionProperty().addListener(new ChangeListener<Throwable>() {
            @Override
            public void changed(ObservableValue<? extends Throwable> observable, Throwable oldValue, Throwable newValue) {
                System.err.println("WebView exception: " + newValue.getMessage());
            }
        });

        // load the HTML page
        webView.getEngine().loadContent(html);
    }

    /**
     * Set preferred width of WebView. Must be called from JavaFX thread.
     *
     * @param w Width
     * @param h Height
     */
    public void setPreferredSize(int w, int h) {
        assert Platform.isFxApplicationThread();
        webView.setPrefSize(w, h);
    }

    /**
     * Fill placeholders in HTML map template. Can be called from any thread.
     *
     * @param canAddColorPoints Whether the user can add color points.
     * @param zoom Zoom level of map.
     * @param centerLon Central longitude of map.
     * @param centerLat Central latitude of map.
     * @return HTML document wit map preview.
     */
    public static String fillHTMLMapTemplate(boolean canAddColorPoints,
            Number zoom,
            Number centerLon,
            Number centerLat) {
        if (zoom == null) {
            zoom = 2;
        }
        if (centerLon == null) {
            centerLon = 0;
        }
        if (centerLat == null) {
            centerLat = 0;
        }
        // replace placeholders with zoom and central location of map
        String html = HTML_MAP_TEMPLATE.replace("$$viewZoomlevel$$", zoom.toString());
        html = html.replace("$$viewCenterLatitude$$", centerLat.toString());
        html = html.replace("$$viewCenterLongitude$$", centerLon.toString());
        return html.replace("$$canAddColorPoints$$", Boolean.toString(canAddColorPoints));
    }

    /**
     * Reloads all map tiles, but not the entire HTML page.
     * @param colorPointsStr Color points to add to the map.
     */
    public void reloadTiles(String colorPointsStr) {
        WebEngine webEngine = webView.getEngine();
        webEngine.executeScript("reloadTiles()");
        setColorPoints(colorPointsStr);
    }

    /**
     * Loads the map and optionally applies previous zoom, panning and color
     * points to the map. Must be called from JavaFX thread.
     *
     * @param colorPointsStr A string with encoded color points. Can be null, in
     * which case the user is not allowed to add color points to the map.
     * @param centerLon Longitude of the center of the map. If null, the current
     * value is used.
     * @param centerLat Latitude of the center of the map. If null, the current
     * value is used.
     * @param zoom Zoom level of the map. If null, the current value is used.
     * @param javaScriptToJavaBridge An object to be registered with JS Window object.
     */
    public void loadHTMLMap(final String colorPointsStr,
            Number centerLon,
            Number centerLat,
            Number zoom,
            final Object javaScriptToJavaBridge) {
        assert Platform.isFxApplicationThread();

        final WebEngine webEngine = webView.getEngine();
        // run scripts to retreive current map center and zoom
        if (centerLat == null) {
            centerLat = (Number) webEngine.executeScript("map.getCenter().lat");
        }
        if (centerLon == null) {
            centerLon = (Number) webEngine.executeScript("map.getCenter().lng");
        }
        if (zoom == null) {
            zoom = (Number) webEngine.executeScript("map.getZoom()");
        }
        // create and load new HTML page with same map center and zoom
        String html = fillHTMLMapTemplate(colorPointsStr != null, zoom, centerLon, centerLat);

        ChangeListener listener = new ChangeListener<Worker.State>() {
            @Override
            public void changed(ObservableValue ov, Worker.State oldState, Worker.State newState) {

                if (newState == Worker.State.SUCCEEDED) {
                    // register JavaScript-to-Java bridge that will receive calls from JavaScript
                    JSObject w = (JSObject) webEngine.executeScript("window");
                    w.setMember("java", javaScriptToJavaBridge);
                    setColorPoints(colorPointsStr);
                    // FIXME not called when the page loads initially
                    System.out.println(w.getMember("registered JavaScript-to-Java bridge"));
                }
            }
        };
        if (colorPointsStr != null) {
            webEngine.getLoadWorker().stateProperty().addListener(listener);
        }

        webEngine.loadContent(html);
    }

    /**
     * Get color points currently on the map. Values for IDW interpolation are
     * NaN.
     *
     * @return An array with all color points.
     * @throws IOException
     */
    public ArrayList<IDWPoint> getColorPoints() throws IOException {
        assert Platform.isFxApplicationThread();

        ArrayList<IDWPoint> points = new ArrayList<>();

        WebEngine webEngine = webView.getEngine();
        // run scripts to retreive color points from map
        JSObject ret = (JSObject) webEngine.executeScript("getColors()");

        // convert the JavaScript result to IDWPoint objects
        Object len = ret.getMember("length");
        int n = ((Number) len).intValue();

        for (int i = 0; i < n; i += 3) {
            double lon = ((Number) ret.getSlot(i)).doubleValue();
            double lat = ((Number) ret.getSlot(i + 1)).doubleValue();

            // make sure longitude values are within -180..+180
            while (lon > 180) {
                lon -= 360;
            }
            while (lon < -180) {
                lon += 360;
            }

            IDWPoint p = new IDWPoint();
            p.setLonLat(lon, lat);
            //Hex to RGB Conversion, convert the hex value from the color picker
            p.setColor(Color.decode((String) ret.getSlot(i + 2)));
            points.add(p);
        }
        return points;
    }

    private void setColorPoints(String colorPointsStr) {
        WebEngine webEngine = webView.getEngine();
        if (colorPointsStr == null) {
            webEngine.executeScript("clearColorPoints()");
        } else {
            webEngine.executeScript("setColors('" + colorPointsStr + "')");
        }
    }

}
