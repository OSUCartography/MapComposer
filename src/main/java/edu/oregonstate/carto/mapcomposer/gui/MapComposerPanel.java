/*
 * MapComposerPanel.java
 *
 * Created on July 31, 2007, 8:27 AM
 */
package edu.oregonstate.carto.mapcomposer.gui;

import edu.oregonstate.carto.mapcomposer.Emboss;
import edu.oregonstate.carto.mapcomposer.Layer;
import edu.oregonstate.carto.mapcomposer.Map;
import edu.oregonstate.carto.mapcomposer.Shadow;
import edu.oregonstate.carto.mapcomposer.Tint;
import edu.oregonstate.carto.tilemanager.Tile;
import edu.oregonstate.carto.tilemanager.TileGenerator;
import edu.oregonstate.carto.tilemanager.TileSet;
import edu.oregonstate.carto.utils.FileUtils;
import edu.oregonstate.carto.utils.GUIUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.xml.bind.JAXBException;

public class MapComposerPanel extends javax.swing.JPanel {

    private static final String PREFS_PREVIEW_WEST = "preview_extent_west";
    private static final String PREFS_PREVIEW_EAST = "preview_extent_east";
    private static final String PREFS_PREVIEW_SOUTH = "preview_extent_south";
    private static final String PREFS_PREVIEW_NORTH = "preview_extent_north";
    private static final String PREFS_PREVIEW_MIN_ZOOM = "preview_min_zoom";
    private static final String PREFS_PREVIEW_MAX_ZOOM = "preview_max_zoom";

    /**
     * This map is the model.
     */
    private Map map = new Map();

    /**
     * updating is true while any code in this class is modifying the GUI. While
     * this flag is true, event handlers should not modify the GUI to avoid
     * recursive calls to event handlers.
     */
    private boolean updating = false;

    /**
     * Extent of the preview in lat/lon coordinates in degrees
     */
    private Rectangle2D.Double previewExtent = new Rectangle2D.Double(-180, -Tile.MAX_LAT, 360, 2 * Tile.MAX_LAT);

    /**
     * minimum zoom level for preview
     */
    private int previewMinZoom = 0;

    /**
     * * maximum zoom level for preview
     */
    private int previewMaxZoom = 2;

    /**
     * counts the number of layers created
     */
    private int layerCounter = 0;

    /**
     * A JavaFX web renderer for the preview map. Must only be accessed from
     * within the JavaFX thread, not the Swing event dispatching thread.
     */
    private WebView webView;

    /**
     * Undo/redo manager.
     */
    private final Undo undo;

    private abstract class DocumentListenerAdaptor implements DocumentListener {

        protected abstract void textChanged(Layer layer, DocumentEvent e);

        private void upate(DocumentEvent e) {
            Layer layer = getSelectedMapLayer();
            if (layer != null) {
                boolean initialUpdating = updating;
                try {
                    updating = true;
                    textChanged(layer, e);
                } finally {
                    updating = initialUpdating;
                }
            }
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            // text was inserted
            upate(e);
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            // text was deleted
            upate(e);
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            // text was changed
            upate(e);
        }
    }

    private class LayerEditListAction extends EditListAction {

        @Override
        protected void applyValueToModel(String value, ListModel model, int row) {
            DnDListModel m = (DnDListModel) model;
            Layer layer = (Layer) m.get(row);
            layer.setName(value);
        }
    }

    /**
     * Creates new form MapComposerPanel
     */
    public MapComposerPanel() {
        readExtentPreferences();
        initComponents();
        try {
            this.undo = new Undo(map.marshal());
        } catch (JAXBException ex) {
            throw new IllegalStateException(ex);
        }

        LayerEditListAction edit = new LayerEditListAction();
        ListAction listAction = new ListAction(layerList, edit);

        initMapPreview();

        // add document listener to URL text field
        urlTextField.getDocument().addDocumentListener(new DocumentListenerAdaptor() {
            @Override
            protected void textChanged(Layer layer, DocumentEvent e) {
                // change color to red if URL is not valid
                String tileSetURL = urlTextField.getText();
                boolean urlTemplateIsValid = TileSet.isURLTemplateValid(tileSetURL);
                Color okColor = UIManager.getDefaults().getColor("TextField.foreground");
                urlTextField.setForeground(urlTemplateIsValid ? okColor : Color.RED);

                layer.setTileSetURLTemplate(tileSetURL);
                // re-render map preview
                reloadHTMLPreviewMap();
            }
        });

        // add document listener to maskURL text field
        maskUrlTextField.getDocument().addDocumentListener(new DocumentListenerAdaptor() {
            @Override
            protected void textChanged(Layer layer, DocumentEvent e) {
                // change color to red if URL is not valid
                String tileSetURL = maskUrlTextField.getText();
                boolean urlTemplateIsValid = TileSet.isURLTemplateValid(tileSetURL);
                Color okColor = UIManager.getDefaults().getColor("TextField.foreground");
                maskUrlTextField.setForeground(urlTemplateIsValid ? okColor : Color.RED);

                layer.setMaskTileSetURLTemplate(tileSetURL);
                // re-render map preview
                reloadHTMLPreviewMap();
            }
        });

        writeGUI();
    }

    /**
     * Creates the preview map.
     */
    private void initMapPreview() {
        final JFXPanel fxPanel = new JFXPanel();
        centralPanel.add(fxPanel, BorderLayout.CENTER);
        final String html = loadHTMLPreviewMap(0, 0, 0);

        // run in the JavaFX thread
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                Group group = new Group();
                Scene scene = new Scene(group);
                fxPanel.setScene(scene);
                webView = new WebView();
                group.getChildren().add(webView);
                webView.getEngine().loadContent(html);

                final Worker<Void> worker = webView.getEngine().getLoadWorker();
                worker.exceptionProperty().addListener(new ChangeListener<Throwable>() {
                    @Override
                    public void changed(ObservableValue<? extends Throwable> observable, Throwable oldValue, Throwable newValue) {
                        System.err.println("WebView exception: " + newValue.getMessage());
                    }
                });
            }
        });

        // add resize event handler to the JavaFX panel to
        // adjust the size of the HTML page to the FXPanel size.
        fxPanel.addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent e) {
                final int w = fxPanel.getWidth();
                final int h = fxPanel.getHeight();
                // run in the JavaFX thread
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        webView.setPrefSize(w, h);
                    }
                });
            }

            @Override
            public void componentMoved(ComponentEvent e) {
            }

            @Override
            public void componentShown(ComponentEvent e) {
            }

            @Override
            public void componentHidden(ComponentEvent e) {
            }
        });

    }

    /**
     * Loads HTML map file and fills placeholders for map center and zoom
     *
     * @param zoom Zoom level of map.
     * @param centerLon Central longitude of map.
     * @param centerLat Central latitude of map.
     * @return HTML document wit map preview.
     */
    private String loadHTMLPreviewMap(Number zoom, Number centerLon, Number centerLat) {
        try {
            URL inputUrl = getClass().getResource("/index_with_variables.html");
            File file = new File(inputUrl.toURI());
            String html = org.apache.commons.io.FileUtils.readFileToString(file);
            // replace placeholders with zoom and central location of map
            html = html.replace("$$viewZoomlevel$$", zoom.toString());
            html = html.replace("$$viewCenterLatitude$$", centerLat.toString());
            html = html.replace("$$viewCenterLongitude$$", centerLon.toString());
            return html;
        } catch (URISyntaxException | IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Reloads the tiles of the preview map. Call this method after changing map
     * settings. To be called from the Swing thread.
     */
    public void reloadHTMLPreviewMap() {
        // run in JavaFX thread
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                WebEngine webEngine = webView.getEngine();
                // run scripts to retreive current map center and zoom
                Number centerLat = (Number) webEngine.executeScript("map.getCenter().lat");
                Number centerLon = (Number) webEngine.executeScript("map.getCenter().lng");
                Number zoom = (Number) webEngine.executeScript("map.getZoom()");

                // create and load new HTML page with same map center and zoom
                String html = loadHTMLPreviewMap(zoom, centerLon, centerLat);
                webEngine.loadContent(html);
            }
        });
    }

    /**
     * Returns the MapLayer currently selected by the user.
     *
     * @return The selected map layer.
     */
    private Layer getSelectedMapLayer() {
        int index = layerList.getSelectedIndex();
        return index == -1 ? null : map.getLayer(index);
    }

    private void byteArrayToMap(byte[] buf) {
        if (buf == null) {
            return;
        }
        try {
            setMap(Map.unmarshal(buf));
            reloadHTMLPreviewMap();
        } catch (JAXBException ex) {
            Logger.getLogger(MapComposerPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    protected void registerUndoMenuItems(JMenuItem undoMenuItem, JMenuItem redoMenuItem) {
        undo.registerUndoMenuItems(undoMenuItem, redoMenuItem);
    }

    protected void undo() {
        Object undoData = undo.getUndo();
        if (undoData != null) {
            byteArrayToMap((byte[]) undoData);
        }
    }

    protected void redo() {
        Object undoData = undo.getRedo();
        if (undoData != null) {
            byteArrayToMap((byte[]) undoData);
        }
    }

    private void addUndo(String message) {
        try {
            undo.add(message, map.marshal());
        } catch (JAXBException ex) {
            Logger.getLogger(MapComposerPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Uses the optional component name to construct an undo string to display
     * in the Undo menu and adds an undo state.
     *
     * @param component
     */
    private void addUndoFromNamedComponent(JComponent component) {
        String name = component.getName();
        if (name != null && name.length() > 0) {
            addUndo(name);
        } else {
            addUndo("");
        }
    }

    /**
     * Reads extent and zoom levels for preview from user preferences.
     */
    private void readExtentPreferences() {
        Preferences prefs = Preferences.userNodeForPackage(MapComposerPanel.class);
        double w = prefs.getDouble(PREFS_PREVIEW_WEST, previewExtent.getMinX());
        double e = prefs.getDouble(PREFS_PREVIEW_EAST, previewExtent.getMaxX());
        double s = prefs.getDouble(PREFS_PREVIEW_SOUTH, previewExtent.getMinY());
        double n = prefs.getDouble(PREFS_PREVIEW_NORTH, previewExtent.getMaxY());
        previewExtent.setFrame(w, s, e - w, n - s);

        previewMinZoom = prefs.getInt(PREFS_PREVIEW_MIN_ZOOM, previewMinZoom);
        previewMaxZoom = prefs.getInt(PREFS_PREVIEW_MAX_ZOOM, previewMaxZoom);
    }

    /**
     * Writes extent and zoom levels for preview to user preferences.
     */
    private void writeExtentPreferences() {
        Preferences prefs = Preferences.userNodeForPackage(MapComposerPanel.class);
        prefs.putDouble(PREFS_PREVIEW_WEST, previewExtent.getMinX());
        prefs.putDouble(PREFS_PREVIEW_EAST, previewExtent.getMaxX());
        prefs.putDouble(PREFS_PREVIEW_SOUTH, previewExtent.getMinY());
        prefs.putDouble(PREFS_PREVIEW_NORTH, previewExtent.getMaxY());

        prefs.putInt(PREFS_PREVIEW_MIN_ZOOM, previewMinZoom);
        prefs.putInt(PREFS_PREVIEW_MAX_ZOOM, previewMaxZoom);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        blendingButtonGroup = new javax.swing.ButtonGroup();
        extentPanel = new javax.swing.JPanel();
        westField = new javax.swing.JFormattedTextField();
        eastField = new javax.swing.JFormattedTextField();
        southField = new javax.swing.JFormattedTextField();
        northField = new javax.swing.JFormattedTextField();
        jPanel1 = new javax.swing.JPanel();
        minZoomSpinner = new javax.swing.JSpinner();
        maxZoomSpinner = new javax.swing.JSpinner();
        javax.swing.JLabel jLabel1 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel2 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel3 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel4 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel5 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel6 = new javax.swing.JLabel();
        jFormattedTextField3 = new javax.swing.JFormattedTextField();
        layersPanel = new javax.swing.JPanel();
        layerListScrollPane = new javax.swing.JScrollPane();
        layerList = new edu.oregonstate.carto.mapcomposer.gui.DraggableList();
        layerListToolBar = new javax.swing.JToolBar();
        addLayerButton = new javax.swing.JButton();
        removeLayerButton = new javax.swing.JButton();
        moveUpLayerButton = new javax.swing.JButton();
        moveDownLayerButton = new javax.swing.JButton();
        visibleCheckBox = new javax.swing.JCheckBox();
        lockedCheckBox = new javax.swing.JCheckBox();
        javax.swing.JPanel bottomPanel = new TransparentMacPanel();
        javax.swing.JTabbedPane settingsTabbedPane = new javax.swing.JTabbedPane();
        colorPanel = new TransparentMacPanel();
        javax.swing.JLabel blendingModeLabel = new javax.swing.JLabel();
        normalBlendingRadioButton = new javax.swing.JRadioButton();
        multiplyBlendingRadioButton = new javax.swing.JRadioButton();
        javax.swing.JLabel opacityLabel = new javax.swing.JLabel();
        opacitySlider = new javax.swing.JSlider();
        opacityValueLabel = new javax.swing.JLabel();
        javax.swing.JLabel gradationCurveLabel = new javax.swing.JLabel();
        loadCurveFileButton = new javax.swing.JButton();
        removeCurveFileButton = new javax.swing.JButton();
        javax.swing.JLabel tintLabel = new javax.swing.JLabel();
        tintCheckBox = new javax.swing.JCheckBox();
        tintColorButton = new edu.oregonstate.carto.mapcomposer.gui.ColorButton();
        curveTextArea = new javax.swing.JTextArea();
        interpolColorLabel = new javax.swing.JLabel();
        gridTextArea1 = new javax.swing.JTextArea();
        gridTextArea2 = new javax.swing.JTextArea();
        interpolColorButton = new javax.swing.JButton();
        tilesPanel = new TransparentMacPanel();
        javax.swing.JLabel urlLabel = new javax.swing.JLabel();
        urlTextField = new javax.swing.JTextField();
        tmsCheckBox = new javax.swing.JCheckBox();
        javax.swing.JTextArea urlHintTextArea = new javax.swing.JTextArea();
        loadDirectoryPathButton = new javax.swing.JButton();
        javax.swing.JPanel maskPanel = new TransparentMacPanel();
        maskInvertCheckBox = new javax.swing.JCheckBox();
        javax.swing.JLabel maskBlurLabel = new javax.swing.JLabel();
        maskBlurSlider = new javax.swing.JSlider();
        maskUrlTextField = new javax.swing.JTextField();
        javax.swing.JTextArea urlHintTextArea1 = new javax.swing.JTextArea();
        maskLoadDirectoryPathButton = new javax.swing.JButton();
        maskTMSCheckBox = new javax.swing.JCheckBox();
        javax.swing.JLabel urlLabel1 = new javax.swing.JLabel();
        effectsTabbedPane = new javax.swing.JTabbedPane();
        effectsPanel = new TransparentMacPanel();
        shadowCheckBox = new javax.swing.JCheckBox();
        shadowOffsetLabel = new javax.swing.JLabel();
        shadowOffsetSlider = new javax.swing.JSlider();
        shadowColorButton = new edu.oregonstate.carto.mapcomposer.gui.ColorButton();
        javax.swing.JLabel DropShadowFuzinessLabel = new javax.swing.JLabel();
        shadowFuziSlider = new javax.swing.JSlider();
        embossPanel = new TransparentMacPanel();
        embossCheckBox = new javax.swing.JCheckBox();
        javax.swing.JLabel embossAzimuthLabel = new javax.swing.JLabel();
        javax.swing.JLabel embossElevationLabel = new javax.swing.JLabel();
        javax.swing.JLabel embossHeightLabel = new javax.swing.JLabel();
        javax.swing.JLabel embossSoftnessLabel = new javax.swing.JLabel();
        embossSoftnessSlider = new javax.swing.JSlider();
        embossHeightSlider = new javax.swing.JSlider();
        embossElevationSlider = new javax.swing.JSlider();
        embossAzimuthSlider = new javax.swing.JSlider();
        embossAzimuthFormattedTextField = new javax.swing.JFormattedTextField();
        embossElevationFormattedTextField = new javax.swing.JFormattedTextField();
        embossHeightFormattedTextField = new javax.swing.JFormattedTextField();
        embossSoftnessFormattedTextField = new javax.swing.JFormattedTextField();
        blurPanel = new TransparentMacPanel();
        gaussBlurLabel = new javax.swing.JLabel();
        gaussBlurSlider = new javax.swing.JSlider();
        javax.swing.JPanel texturePanel = new TransparentMacPanel();
        textureSelectionButton = new javax.swing.JButton();
        javax.swing.JLabel textureScaleLabel = new javax.swing.JLabel();
        textureScaleSlider = new javax.swing.JSlider();
        textureURLLabel = new javax.swing.JLabel();
        textureClearButton = new javax.swing.JButton();
        texturePreviewLabel = new javax.swing.JLabel();
        textureScaleFormattedTextField = new javax.swing.JFormattedTextField();
        centralPanel = new javax.swing.JPanel();

        extentPanel.setLayout(new java.awt.GridBagLayout());

        westField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#,##0.#####"))));
        westField.setPreferredSize(new java.awt.Dimension(65, 28));
        westField.setValue(new Double(-180));
        javax.swing.text.NumberFormatter nfWest = new javax.swing.text.NumberFormatter();
        nfWest.setMinimum(-180.0);
        nfWest.setMaximum(180.0);
        westField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(nfWest));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        extentPanel.add(westField, gridBagConstraints);

        eastField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#,##0.#####"))));
        eastField.setPreferredSize(new java.awt.Dimension(65, 28));
        eastField.setValue(new Double(180));
        javax.swing.text.NumberFormatter nfEast = new javax.swing.text.NumberFormatter();
        nfEast.setMinimum(-180.0);
        nfEast.setMaximum(180.0);
        eastField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(nfEast));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 7;
        extentPanel.add(eastField, gridBagConstraints);

        southField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#,##0.#####"))));
        southField.setPreferredSize(new java.awt.Dimension(65, 28));
        southField.setValue(new Double(-90));
        javax.swing.text.NumberFormatter nfSouth = new javax.swing.text.NumberFormatter();
        nfSouth.setMinimum(-85.05113);
        nfSouth.setMaximum(85.05113);
        southField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(nfSouth));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 20, 0);
        extentPanel.add(southField, gridBagConstraints);

        northField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#,##0.#####"))));
        northField.setPreferredSize(new java.awt.Dimension(65, 28));
        northField.setValue(new Double(90));
        javax.swing.text.NumberFormatter nfNorth = new javax.swing.text.NumberFormatter();
        nfNorth.setMinimum(-85.05113);
        nfNorth.setMaximum(85.05113);
        northField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(nfNorth));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        extentPanel.add(northField, gridBagConstraints);

        jPanel1.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        extentPanel.add(jPanel1, gridBagConstraints);

        minZoomSpinner.setModel(new javax.swing.SpinnerNumberModel(0, 0, 15, 1));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 11;
        extentPanel.add(minZoomSpinner, gridBagConstraints);

        maxZoomSpinner.setModel(new javax.swing.SpinnerNumberModel(4, 0, 15, 1));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 12;
        extentPanel.add(maxZoomSpinner, gridBagConstraints);

        jLabel1.setText("West");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        extentPanel.add(jLabel1, gridBagConstraints);

        jLabel2.setText("East");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 6;
        extentPanel.add(jLabel2, gridBagConstraints);

        jLabel3.setText("North");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        extentPanel.add(jLabel3, gridBagConstraints);

        jLabel4.setText("South");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        extentPanel.add(jLabel4, gridBagConstraints);

        jLabel5.setText("Minimum Zoom");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        extentPanel.add(jLabel5, gridBagConstraints);

        jLabel6.setText("Maximimum Zoom");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        extentPanel.add(jLabel6, gridBagConstraints);

        jFormattedTextField3.setText("jFormattedTextField3");

        setLayout(new java.awt.BorderLayout());

        layersPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        layersPanel.setLayout(new java.awt.BorderLayout(0, 5));

        layerListScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        layerListScrollPane.setPreferredSize(new java.awt.Dimension(200, 132));

        layerList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        layerList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                layerListValueChanged(evt);
            }
        });
        layerListScrollPane.setViewportView(layerList);

        layersPanel.add(layerListScrollPane, java.awt.BorderLayout.CENTER);

        layerListToolBar.setFloatable(false);

        addLayerButton.setText("+");
        addLayerButton.setMaximumSize(new java.awt.Dimension(22, 22));
        addLayerButton.setMinimumSize(new java.awt.Dimension(22, 22));
        addLayerButton.setPreferredSize(new java.awt.Dimension(22, 22));
        addLayerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addLayerButtonActionPerformed(evt);
            }
        });
        layerListToolBar.add(addLayerButton);

        removeLayerButton.setText("-");
        removeLayerButton.setMaximumSize(new java.awt.Dimension(22, 22));
        removeLayerButton.setMinimumSize(new java.awt.Dimension(22, 22));
        removeLayerButton.setPreferredSize(new java.awt.Dimension(22, 22));
        removeLayerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeLayerButtonActionPerformed(evt);
            }
        });
        layerListToolBar.add(removeLayerButton);

        moveUpLayerButton.setText("Up");
        moveUpLayerButton.setMaximumSize(new java.awt.Dimension(45, 22));
        moveUpLayerButton.setMinimumSize(new java.awt.Dimension(45, 22));
        moveUpLayerButton.setPreferredSize(new java.awt.Dimension(45, 22));
        moveUpLayerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveUpLayerButtonActionPerformed(evt);
            }
        });
        layerListToolBar.add(moveUpLayerButton);

        moveDownLayerButton.setText("Down");
        moveDownLayerButton.setMaximumSize(new java.awt.Dimension(45, 22));
        moveDownLayerButton.setMinimumSize(new java.awt.Dimension(45, 22));
        moveDownLayerButton.setPreferredSize(new java.awt.Dimension(45, 22));
        moveDownLayerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveDownLayerButtonActionPerformed(evt);
            }
        });
        layerListToolBar.add(moveDownLayerButton);

        visibleCheckBox.setText("Visible");
        visibleCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
        visibleCheckBox.setMargin(new java.awt.Insets(0, 0, 0, 0));
        visibleCheckBox.setName("Visibility"); // NOI18N
        visibleCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MapComposerPanel.this.actionPerformed(evt);
            }
        });
        layerListToolBar.add(visibleCheckBox);

        lockedCheckBox.setText("Locked");
        lockedCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 5, 0, 0));
        lockedCheckBox.setName("Locked"); // NOI18N
        lockedCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lockedCheckBoxActionPerformed(evt);
            }
        });
        layerListToolBar.add(lockedCheckBox);

        layersPanel.add(layerListToolBar, java.awt.BorderLayout.NORTH);

        bottomPanel.setLayout(new java.awt.GridBagLayout());

        colorPanel.setLayout(new java.awt.GridBagLayout());

        blendingModeLabel.setText("Blending:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        colorPanel.add(blendingModeLabel, gridBagConstraints);

        blendingButtonGroup.add(normalBlendingRadioButton);
        normalBlendingRadioButton.setText("Normal");
        normalBlendingRadioButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        normalBlendingRadioButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        normalBlendingRadioButton.setName("Blending Normal"); // NOI18N
        normalBlendingRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MapComposerPanel.this.actionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        colorPanel.add(normalBlendingRadioButton, gridBagConstraints);

        blendingButtonGroup.add(multiplyBlendingRadioButton);
        multiplyBlendingRadioButton.setText("Multiply");
        multiplyBlendingRadioButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        multiplyBlendingRadioButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        multiplyBlendingRadioButton.setName("Blending Multiply"); // NOI18N
        multiplyBlendingRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MapComposerPanel.this.actionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        colorPanel.add(multiplyBlendingRadioButton, gridBagConstraints);

        opacityLabel.setText("Opacity:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        colorPanel.add(opacityLabel, gridBagConstraints);

        opacitySlider.setMajorTickSpacing(20);
        opacitySlider.setMinorTickSpacing(5);
        opacitySlider.setPaintLabels(true);
        opacitySlider.setPaintTicks(true);
        opacitySlider.setValue(100);
        opacitySlider.setName("Opacity"); // NOI18N
        opacitySlider.setPreferredSize(new java.awt.Dimension(150, 52));
        opacitySlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        colorPanel.add(opacitySlider, gridBagConstraints);

        opacityValueLabel.setFont(opacityValueLabel.getFont().deriveFont(opacityValueLabel.getFont().getSize()-2f));
        opacityValueLabel.setText("100");
        opacityValueLabel.setPreferredSize(new java.awt.Dimension(30, 14));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        colorPanel.add(opacityValueLabel, gridBagConstraints);

        gradationCurveLabel.setText("Curve:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        colorPanel.add(gradationCurveLabel, gridBagConstraints);

        loadCurveFileButton.setText("File");
        loadCurveFileButton.setToolTipText("Load an ACV Photoshop curve file");
        loadCurveFileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadCurveFileButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        colorPanel.add(loadCurveFileButton, gridBagConstraints);

        removeCurveFileButton.setText("Remove");
        removeCurveFileButton.setToolTipText("Remove the curve");
        removeCurveFileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeCurveFileButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        colorPanel.add(removeCurveFileButton, gridBagConstraints);

        tintLabel.setText("Tint:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        colorPanel.add(tintLabel, gridBagConstraints);

        tintCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        tintCheckBox.setMargin(new java.awt.Insets(0, 0, 0, 0));
        tintCheckBox.setName("Apply Tint"); // NOI18N
        tintCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MapComposerPanel.this.actionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        colorPanel.add(tintCheckBox, gridBagConstraints);

        tintColorButton.setName("Tint"); // NOI18N
        tintColorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MapComposerPanel.this.actionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        colorPanel.add(tintColorButton, gridBagConstraints);

        curveTextArea.setEditable(false);
        curveTextArea.setColumns(20);
        curveTextArea.setFont(curveTextArea.getFont().deriveFont(curveTextArea.getFont().getSize()-2f));
        curveTextArea.setRows(1);
        curveTextArea.setText("file:///../curve.acv");
        curveTextArea.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 15, 0);
        colorPanel.add(curveTextArea, gridBagConstraints);

        interpolColorLabel.setText("Interpolated Color:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        colorPanel.add(interpolColorLabel, gridBagConstraints);

        gridTextArea1.setEditable(false);
        gridTextArea1.setColumns(20);
        gridTextArea1.setFont(new java.awt.Font("Monospaced", 0, 11)); // NOI18N
        gridTextArea1.setRows(5);
        gridTextArea1.setText("file:///.../grid1");
        gridTextArea1.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        colorPanel.add(gridTextArea1, gridBagConstraints);

        gridTextArea2.setEditable(false);
        gridTextArea2.setColumns(20);
        gridTextArea2.setFont(new java.awt.Font("Monospaced", 0, 11)); // NOI18N
        gridTextArea2.setRows(5);
        gridTextArea2.setText("file:///.../grid2");
        gridTextArea2.setToolTipText("");
        gridTextArea2.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 17;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 2;
        colorPanel.add(gridTextArea2, gridBagConstraints);

        interpolColorButton.setText("Select Grids");
        interpolColorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                interpolColorButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        colorPanel.add(interpolColorButton, gridBagConstraints);

        settingsTabbedPane.addTab("Color", colorPanel);

        tilesPanel.setLayout(new java.awt.GridBagLayout());

        urlLabel.setText("URL");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        tilesPanel.add(urlLabel, gridBagConstraints);

        urlTextField.setFont(urlTextField.getFont().deriveFont(urlTextField.getFont().getSize()-1f));
        urlTextField.setText("http://tile.stamen.com/watercolor/{z}/{x}/{y}.png");
        urlTextField.setPreferredSize(new java.awt.Dimension(200, 28));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        tilesPanel.add(urlTextField, gridBagConstraints);

        tmsCheckBox.setText("TMS");
        tmsCheckBox.setName("Tiles TMS"); // NOI18N
        tmsCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MapComposerPanel.this.actionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        tilesPanel.add(tmsCheckBox, gridBagConstraints);

        urlHintTextArea.setEditable(false);
        urlHintTextArea.setColumns(20);
        urlHintTextArea.setFont(urlHintTextArea.getFont().deriveFont(urlHintTextArea.getFont().getSize()-2f));
        urlHintTextArea.setLineWrap(true);
        urlHintTextArea.setRows(4);
        urlHintTextArea.setText("Examples:\nhttp://tile.openstreetmap.org/{z}/{x}/{y}.png\nhttp://tile.stamen.com/watercolor/{z}/{x}/{y}.png\nhttp://services.arcgisonline.com/ArcGIS/rest/services/NatGeo_World_Map/MapServer/tile/{z}/{y}/{x}.png");
        urlHintTextArea.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        tilesPanel.add(urlHintTextArea, gridBagConstraints);

        Icon folderIcon = UIManager.getDefaults().getIcon("FileView.directoryIcon");
        loadDirectoryPathButton.setIcon(folderIcon);
        int iconH = folderIcon.getIconHeight();
        int iconW = folderIcon.getIconWidth();
        loadDirectoryPathButton.setPreferredSize(new Dimension(iconW + 18, iconH + 18));
        loadDirectoryPathButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadDirectoryPathButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        tilesPanel.add(loadDirectoryPathButton, gridBagConstraints);

        settingsTabbedPane.addTab("Tiles", tilesPanel);

        maskPanel.setLayout(new java.awt.GridBagLayout());

        maskInvertCheckBox.setText("Invert Mask");
        maskInvertCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        maskInvertCheckBox.setMargin(new java.awt.Insets(0, 0, 0, 0));
        maskInvertCheckBox.setName("Invert Mask"); // NOI18N
        maskInvertCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MapComposerPanel.this.actionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        maskPanel.add(maskInvertCheckBox, gridBagConstraints);

        maskBlurLabel.setText("Blur Mask:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        maskPanel.add(maskBlurLabel, gridBagConstraints);

        maskBlurSlider.setMinorTickSpacing(10);
        maskBlurSlider.setPaintTicks(true);
        maskBlurSlider.setValue(0);
        maskBlurSlider.setName("Blur Mask"); // NOI18N
        maskBlurSlider.setPreferredSize(new java.awt.Dimension(120, 38));
        maskBlurSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        maskPanel.add(maskBlurSlider, gridBagConstraints);

        maskUrlTextField.setFont(maskUrlTextField.getFont().deriveFont(maskUrlTextField.getFont().getSize()-1f));
        maskUrlTextField.setText("http://tile.stamen.com/watercolor/{z}/{x}/{y}.png");
        maskUrlTextField.setPreferredSize(new java.awt.Dimension(200, 28));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        maskPanel.add(maskUrlTextField, gridBagConstraints);

        urlHintTextArea1.setEditable(false);
        urlHintTextArea1.setColumns(20);
        urlHintTextArea1.setFont(urlHintTextArea1.getFont().deriveFont(urlHintTextArea1.getFont().getSize()-2f));
        urlHintTextArea1.setRows(2);
        urlHintTextArea1.setText("Example:\nhttp://tile.stamen.com/toner/{z}/{x}/{y}.png");
        urlHintTextArea1.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        maskPanel.add(urlHintTextArea1, gridBagConstraints);

        maskLoadDirectoryPathButton.setIcon(folderIcon);
        maskLoadDirectoryPathButton.setPreferredSize(new Dimension(iconW + 18, iconH + 18));

        maskLoadDirectoryPathButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                maskLoadDirectoryPathButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        maskPanel.add(maskLoadDirectoryPathButton, gridBagConstraints);

        maskTMSCheckBox.setText("TMS");
        maskTMSCheckBox.setName("Mask TMS"); // NOI18N
        maskTMSCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MapComposerPanel.this.actionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        maskPanel.add(maskTMSCheckBox, gridBagConstraints);

        urlLabel1.setText("URL");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        maskPanel.add(urlLabel1, gridBagConstraints);

        settingsTabbedPane.addTab("Mask", maskPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        bottomPanel.add(settingsTabbedPane, gridBagConstraints);

        effectsPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 5, 0, 5));
        effectsPanel.setLayout(new java.awt.GridBagLayout());

        shadowCheckBox.setText("Drop Shadow");
        shadowCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        shadowCheckBox.setMargin(new java.awt.Insets(0, 0, 0, 0));
        shadowCheckBox.setName("Apply Drop Shadow"); // NOI18N
        shadowCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MapComposerPanel.this.actionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        effectsPanel.add(shadowCheckBox, gridBagConstraints);

        shadowOffsetLabel.setText("Offset:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        effectsPanel.add(shadowOffsetLabel, gridBagConstraints);

        shadowOffsetSlider.setMaximum(20);
        shadowOffsetSlider.setValue(1);
        shadowOffsetSlider.setName("Shadow Offset"); // NOI18N
        shadowOffsetSlider.setPreferredSize(new java.awt.Dimension(120, 29));
        shadowOffsetSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        effectsPanel.add(shadowOffsetSlider, gridBagConstraints);

        shadowColorButton.setName("Shadow Color"); // NOI18N
        shadowColorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MapComposerPanel.this.actionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        effectsPanel.add(shadowColorButton, gridBagConstraints);

        DropShadowFuzinessLabel.setText("Fuzziness:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        effectsPanel.add(DropShadowFuzinessLabel, gridBagConstraints);

        shadowFuziSlider.setMaximum(20);
        shadowFuziSlider.setValue(10);
        shadowFuziSlider.setName("Shadow Fuzziness"); // NOI18N
        shadowFuziSlider.setPreferredSize(new java.awt.Dimension(120, 29));
        shadowFuziSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        effectsPanel.add(shadowFuziSlider, gridBagConstraints);

        effectsTabbedPane.addTab("Shadow", effectsPanel);

        embossPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 5, 0, 5));
        embossPanel.setLayout(new java.awt.GridBagLayout());

        embossCheckBox.setText("Emboss");
        embossCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        embossCheckBox.setMargin(new java.awt.Insets(0, 0, 0, 0));
        embossCheckBox.setName("Apply Emboss"); // NOI18N
        embossCheckBox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                embossCheckBoxStateChanged(evt);
            }
        });
        embossCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MapComposerPanel.this.actionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        embossPanel.add(embossCheckBox, gridBagConstraints);

        embossAzimuthLabel.setText("Azimuth:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        embossPanel.add(embossAzimuthLabel, gridBagConstraints);

        embossElevationLabel.setText("Elevation:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        embossPanel.add(embossElevationLabel, gridBagConstraints);

        embossHeightLabel.setText("Height:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        embossPanel.add(embossHeightLabel, gridBagConstraints);

        embossSoftnessLabel.setText("Softness:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        embossPanel.add(embossSoftnessLabel, gridBagConstraints);

        embossSoftnessSlider.setMaximum(50);
        embossSoftnessSlider.setValue(10);
        embossSoftnessSlider.setName("Emboss Softness"); // NOI18N
        embossSoftnessSlider.setPreferredSize(new java.awt.Dimension(120, 29));
        embossSoftnessSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        embossPanel.add(embossSoftnessSlider, gridBagConstraints);

        embossHeightSlider.setName("Emboss Height"); // NOI18N
        embossHeightSlider.setPreferredSize(new java.awt.Dimension(120, 29));
        embossHeightSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        embossPanel.add(embossHeightSlider, gridBagConstraints);

        embossElevationSlider.setMaximum(90);
        embossElevationSlider.setValue(45);
        embossElevationSlider.setName("Emboss Elevation"); // NOI18N
        embossElevationSlider.setPreferredSize(new java.awt.Dimension(120, 29));
        embossElevationSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        embossPanel.add(embossElevationSlider, gridBagConstraints);

        embossAzimuthSlider.setMaximum(360);
        embossAzimuthSlider.setValue(315);
        embossAzimuthSlider.setName("Emboss Azimuth"); // NOI18N
        embossAzimuthSlider.setPreferredSize(new java.awt.Dimension(120, 29));
        embossAzimuthSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        embossPanel.add(embossAzimuthSlider, gridBagConstraints);

        javax.swing.text.NumberFormatter nfEmbossAziuth = new javax.swing.text.NumberFormatter();
        nfEmbossAziuth.setMinimum(0);
        nfEmbossAziuth.setMaximum(360);
        embossAzimuthFormattedTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(nfEmbossAziuth));
        embossAzimuthFormattedTextField.setText("0");
        embossAzimuthFormattedTextField.setName("Emboss Azimuth"); // NOI18N
        embossAzimuthFormattedTextField.setPreferredSize(new java.awt.Dimension(40, 28));
        embossAzimuthFormattedTextField.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                numberFieldChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        embossPanel.add(embossAzimuthFormattedTextField, gridBagConstraints);

        embossElevationFormattedTextField.setText("0");
        embossElevationFormattedTextField.setName("Emboss Elevation"); // NOI18N
        embossElevationFormattedTextField.setPreferredSize(new java.awt.Dimension(40, 28));
        javax.swing.text.NumberFormatter nfEmbossElevation = new javax.swing.text.NumberFormatter();
        nfEmbossElevation.setMinimum(0);
        nfEmbossElevation.setMaximum(90);
        embossElevationFormattedTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(nfEmbossElevation));
        embossElevationFormattedTextField.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                numberFieldChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        embossPanel.add(embossElevationFormattedTextField, gridBagConstraints);

        embossHeightFormattedTextField.setText("0");
        embossHeightFormattedTextField.setName("Emboss Height"); // NOI18N
        embossHeightFormattedTextField.setPreferredSize(new java.awt.Dimension(40, 28));
        javax.swing.text.NumberFormatter nfEmbossHeight = new javax.swing.text.NumberFormatter();
        nfEmbossHeight.setMinimum(0);
        nfEmbossHeight.setMaximum(100);
        embossHeightFormattedTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(nfEmbossHeight));
        embossHeightFormattedTextField.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                numberFieldChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        embossPanel.add(embossHeightFormattedTextField, gridBagConstraints);

        embossSoftnessFormattedTextField.setText("0");
        embossSoftnessFormattedTextField.setName("Emboss Softness"); // NOI18N
        embossSoftnessFormattedTextField.setPreferredSize(new java.awt.Dimension(40, 28));
        javax.swing.text.NumberFormatter nfEmbossSoftness = new javax.swing.text.NumberFormatter();
        nfEmbossSoftness.setMinimum(0);
        nfEmbossSoftness.setMaximum(50);
        embossSoftnessFormattedTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(nfEmbossSoftness));
        embossSoftnessFormattedTextField.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                numberFieldChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        embossPanel.add(embossSoftnessFormattedTextField, gridBagConstraints);

        effectsTabbedPane.addTab("Emboss", embossPanel);

        blurPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 5, 0, 5));
        blurPanel.setLayout(new java.awt.GridBagLayout());

        gaussBlurLabel.setText("Blur");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        blurPanel.add(gaussBlurLabel, gridBagConstraints);

        gaussBlurSlider.setMajorTickSpacing(25);
        gaussBlurSlider.setMinorTickSpacing(5);
        gaussBlurSlider.setValue(0);
        gaussBlurSlider.setName("Blur"); // NOI18N
        gaussBlurSlider.setPreferredSize(new java.awt.Dimension(120, 29));
        gaussBlurSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        blurPanel.add(gaussBlurSlider, gridBagConstraints);

        effectsTabbedPane.addTab("Blur", blurPanel);

        texturePanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 5, 0, 5));
        texturePanel.setLayout(new java.awt.GridBagLayout());

        textureSelectionButton.setText("Select");
        textureSelectionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                textureSelectionButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        texturePanel.add(textureSelectionButton, gridBagConstraints);

        textureScaleLabel.setText("Texture Scale:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        texturePanel.add(textureScaleLabel, gridBagConstraints);

        textureScaleSlider.setMajorTickSpacing(50);
        textureScaleSlider.setMinimum(-100);
        textureScaleSlider.setMinorTickSpacing(10);
        textureScaleSlider.setPaintTicks(true);
        textureScaleSlider.setValue(0);
        textureScaleSlider.setName("Texture Scale"); // NOI18N
        textureScaleSlider.setPreferredSize(new java.awt.Dimension(120, 38));
        textureScaleSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        texturePanel.add(textureScaleSlider, gridBagConstraints);

        textureURLLabel.setFont(textureURLLabel.getFont().deriveFont(textureURLLabel.getFont().getSize()-3f));
        textureURLLabel.setText("Texture Path");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        texturePanel.add(textureURLLabel, gridBagConstraints);

        textureClearButton.setText("Clear");
        textureClearButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                textureClearButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        texturePanel.add(textureClearButton, gridBagConstraints);

        texturePreviewLabel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        texturePreviewLabel.setPreferredSize(new java.awt.Dimension(4, 64));
        texturePreviewLabel.setRequestFocusEnabled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        texturePanel.add(texturePreviewLabel, gridBagConstraints);

        textureScaleFormattedTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#,##0.#"))));
        textureScaleFormattedTextField.setName("Texture Scale"); // NOI18N
        textureScaleFormattedTextField.setPreferredSize(new java.awt.Dimension(50, 28));
        textureScaleFormattedTextField.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                numberFieldChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        texturePanel.add(textureScaleFormattedTextField, gridBagConstraints);

        effectsTabbedPane.addTab("Texture", texturePanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        bottomPanel.add(effectsTabbedPane, gridBagConstraints);

        layersPanel.add(bottomPanel, java.awt.BorderLayout.SOUTH);

        add(layersPanel, java.awt.BorderLayout.WEST);

        centralPanel.setLayout(new java.awt.BorderLayout());
        add(centralPanel, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Add a listener for changes to text fields. The listener calls readGUI()
     * whenever the text changes.
     *
     * @param textField
     */
    private void addDocumentListener(JTextField textField) {
        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                // text was changed
                documentChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                // text was deleted
                documentChanged();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                // text was inserted
                documentChanged();
            }

            private void documentChanged() {
                readGUI();
                try {
                    updating = true;
                    updateLayerList();
                } finally {
                    updating = false;
                }
            }
        });
    }

    /**
     * Event handler for moving a layer downwards in the layers hierarchy.
     *
     * @param evt
     */
    private void moveDownLayerButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveDownLayerButtonActionPerformed
        int selectedLayerID = layerList.getSelectedIndex();
        if (selectedLayerID < 0 || selectedLayerID >= map.getLayerCount() - 1) {
            return;
        }
        Layer layer = map.removeLayer(selectedLayerID);
        map.addLayer(++selectedLayerID, layer);
        updateLayerList();
        reloadHTMLPreviewMap();
        layerList.setSelectedIndex(selectedLayerID);
        writeGUI();
        addUndo("Move Layer Down");
    }//GEN-LAST:event_moveDownLayerButtonActionPerformed

    /**
     * Event handler for moving a layer upwards in the layers hierarchy.
     *
     * @param evt
     */
    private void moveUpLayerButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveUpLayerButtonActionPerformed
        int selectedLayerID = layerList.getSelectedIndex();
        if (selectedLayerID <= 0) {
            return;
        }
        Layer layer = map.removeLayer(selectedLayerID);
        map.addLayer(--selectedLayerID, layer);
        updateLayerList();
        reloadHTMLPreviewMap();
        this.layerList.setSelectedIndex(selectedLayerID);
        this.writeGUI();
        addUndo("Move Layer Up");
    }//GEN-LAST:event_moveUpLayerButtonActionPerformed

    /**
     * Event handler for removing a layer.
     */
    private void removeLayerButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeLayerButtonActionPerformed
        removeLayer();
    }//GEN-LAST:event_removeLayerButtonActionPerformed

    /**
     * Writes a HTML file for viewing a tile set.
     *
     * @param directory Directory to contain the HTML file.
     * @param zoom Default zoom level of the HTML map.
     * @param centerLat Central latitude of the HTML map.
     * @param centerLon Central longitude of the HTML map.
     * @return URL to the created file.
     * @throws IOException
     * @throws URISyntaxException
     */
    private URL generateHTMLMapViewer(File directory,
            int zoom, double centerLat, double centerLon)
            throws IOException, URISyntaxException {
        String html = loadHTMLPreviewMap(zoom, centerLon, centerLat);
        String path = directory.getAbsolutePath();
        File dest = new File(path + File.separator + "index.html");
        org.apache.commons.io.FileUtils.writeStringToFile(dest, html);
        return dest.toURI().toURL();
    }

    /**
     * Renders map tiles to a directory.
     *
     * @param directory
     */
    protected void renderTilesWithProgressDialog(final File directory) {
        final TileGenerator tileGenerator = new TileGenerator(directory);
        String dialogTitle = "Rendering Tiles";
        Frame frame = GUIUtil.getOwnerFrame(this);
        SwingWorkerWithProgressIndicator worker;
        worker = new SwingWorkerWithProgressIndicator<Void>(frame, dialogTitle, "", true) {

            @Override
            public void done() {
                try {
                    // close progress dialog
                    completeProgress();

                    // a call to get() will throw an ExecutionException if an 
                    // exception occured in doInBackground
                    get();

                } catch (ExecutionException | InterruptedException | CancellationException ex) {
                    completeProgress();
                    if (!isAborted()) {
                        ErrorDialog.showErrorDialog("Could not open a preview browser window.", ex);
                        Logger.getLogger(MapComposerPanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } finally {
                    // hide the progress dialog
                    completeProgress();

                    try {
                        // copy html file to directory with tiles
                        URL htmlMapViewerURL = generateHTMLMapViewer(
                                tileGenerator.getDirectory(),
                                previewMinZoom,
                                previewExtent.getCenterY(), previewExtent.getCenterX());

                        // open web browser
                        if (htmlMapViewerURL != null) {
                            Desktop.getDesktop().browse(htmlMapViewerURL.toURI());
                        }
                    } catch (IOException | URISyntaxException ex) {
                        Logger.getLogger(MapComposerPanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }

            @Override
            protected Void doInBackground() throws Exception {
                // start progress dialog
                start();

                // create tiles
                tileGenerator.generateTiles(map, this);

                return null;
            }
        };

        tileGenerator.setExtent(previewExtent.getMinX(),
                previewExtent.getMaxX(),
                previewExtent.getMinY(),
                previewExtent.getMaxY());
        tileGenerator.setZoomRange(previewMinZoom, previewMaxZoom);
        worker.setMaxTimeWithoutDialogMilliseconds(0);
        worker.setIndeterminate(true);
        worker.setMessage("");
        worker.execute();
    }

    /**
     * A general event handler for when a slider value changes. Used by various
     * sliders.
     *
     * @param evt
     */
    private void sliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sliderStateChanged
        if (updating) {
            return;
        }

        updating = true;
        try {
            opacityValueLabel.setText(Integer.toString(opacitySlider.getValue()));
            textureScaleFormattedTextField.setValue(readTextureScale());
            embossAzimuthFormattedTextField.setValue(embossAzimuthSlider.getValue());
            embossElevationFormattedTextField.setValue(embossElevationSlider.getValue());
            embossHeightFormattedTextField.setValue(embossHeightSlider.getValue());
            embossSoftnessFormattedTextField.setValue(embossSoftnessSlider.getValue());
        } finally {
            updating = false;
        }

        if (((JSlider) evt.getSource()).getValueIsAdjusting() == false) {
            readGUI();
            addUndoFromNamedComponent((JComponent) (evt.getSource()));
        }
    }//GEN-LAST:event_sliderStateChanged

    /**
     * A generic event handler for different kinds of action events. Used by
     * various components.
     *
     * @param evt
     */
    private void actionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_actionPerformed
        readGUI();
        addUndoFromNamedComponent((JComponent) (evt.getSource()));
    }//GEN-LAST:event_actionPerformed

    /**
     * Event handler called when the selection in the layers list changes.
     *
     * @param evt
     */
    private void layerListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_layerListValueChanged
        if (!evt.getValueIsAdjusting()) {
            writeGUI();
        }
    }//GEN-LAST:event_layerListValueChanged

    /**
     * Add a layer.
     *
     * @param evt
     */
    private void addLayerButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addLayerButtonActionPerformed
        addLayer(false);
    }//GEN-LAST:event_addLayerButtonActionPerformed

    private void embossCheckBoxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_embossCheckBoxStateChanged
        if (updating) {
            return;
        }
        if (this.embossCheckBox.isSelected()) {
            this.embossHeightSlider.setEnabled(true);
            this.embossHeightFormattedTextField.setEnabled(true);
            this.embossSoftnessSlider.setEnabled(true);
            this.embossSoftnessFormattedTextField.setEnabled(true);
            this.embossAzimuthSlider.setEnabled(true);
            this.embossAzimuthFormattedTextField.setEnabled(true);
            this.embossElevationSlider.setEnabled(true);
            this.embossElevationFormattedTextField.setEnabled(true);
        } else {
            this.embossHeightSlider.setEnabled(false);
            this.embossHeightFormattedTextField.setEnabled(false);
            this.embossSoftnessSlider.setEnabled(false);
            this.embossSoftnessFormattedTextField.setEnabled(false);
            this.embossAzimuthSlider.setEnabled(false);
            this.embossAzimuthFormattedTextField.setEnabled(false);
            this.embossElevationSlider.setEnabled(false);
            this.embossElevationFormattedTextField.setEnabled(false);
        }
    }//GEN-LAST:event_embossCheckBoxStateChanged

    /**
     * Event handler to add a texture image file or a texture tile set to the
     * selected layer.
     *
     * @param evt
     */
    private void textureSelectionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_textureSelectionButtonActionPerformed
        try {
            String msg = "Select a Texture Image Tile";
            String path = FileUtils.askFile(GUIUtil.getOwnerFrame(this), msg, true);
            if (path != null) {
                Layer layer = getSelectedMapLayer();
                if (layer != null) {
                    layer.setTextureTileFilePath(path);
                }
                writeGUI();
                addUndo("Select Texture");
            }
        } catch (Exception ex) {
            String msg = "Error";
            String title = "The texture could not be loaded.";
            ErrorDialog.showErrorDialog(msg, title, ex, this);
        }
    }//GEN-LAST:event_textureSelectionButtonActionPerformed

    /**
     * Event handler to remove the current texture from the selected layer.
     *
     * @param evt
     */
    private void textureClearButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_textureClearButtonActionPerformed
        Layer layer = getSelectedMapLayer();
        if (layer != null) {
            layer.setTextureTileFilePath(null);
        }
        writeGUI();
        addUndo("Clear Texture");
    }//GEN-LAST:event_textureClearButtonActionPerformed

    private void numberFieldChanged(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_numberFieldChanged
        if ("value".equals(evt.getPropertyName()) == false || updating) {
            return;
        }

        this.updating = true;
        try {
            if (textureScaleFormattedTextField.getValue() != null) {
                float textureScale = ((Number) textureScaleFormattedTextField.getValue()).floatValue();
                this.writeTextureScale(textureScale);
            }
            if (embossAzimuthFormattedTextField.getValue() != null) {
                int embossAzimuth = ((Number) embossAzimuthFormattedTextField.getValue()).intValue();
                this.embossAzimuthSlider.setValue(embossAzimuth);
            }
            if (embossElevationFormattedTextField.getValue() != null) {
                int embossElevation = ((Number) embossElevationFormattedTextField.getValue()).intValue();
                this.embossElevationSlider.setValue(embossElevation);
            }

            if (embossHeightFormattedTextField.getValue() != null) {
                int embossHeight = ((Number) embossHeightFormattedTextField.getValue()).intValue();
                this.embossHeightSlider.setValue(embossHeight);
            }
            if (embossSoftnessFormattedTextField.getValue() != null) {
                int embossSoftness = ((Number) embossSoftnessFormattedTextField.getValue()).intValue();
                this.embossSoftnessSlider.setValue(embossSoftness);
            }
            addUndoFromNamedComponent((JComponent) (evt.getSource()));
        } finally {
            this.updating = false;
        }
    }//GEN-LAST:event_numberFieldChanged

    /**
     * Event handler for selecting a Photoshop ACV curve file.
     *
     * @param evt
     */
    private void loadCurveFileButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadCurveFileButtonActionPerformed
        // read curves file
        String msg = "Select an Adobe Photoshop ACV File";
        String filePath = FileUtils.askFile(GUIUtil.getOwnerFrame(this), msg, true);
        if (filePath == null) {
            return;
        }
        curveTextArea.setText("file:///" + filePath);
        readGUI();

        // update enable state of remove button
        writeGUI();
        addUndo("Load Curve");
    }//GEN-LAST:event_loadCurveFileButtonActionPerformed

    protected void askMapExtent() {
        String title = "Tiles Extent";

        // write current values to dialog
        westField.setValue(previewExtent.getMinX());
        southField.setValue(previewExtent.getMinY());
        eastField.setValue(previewExtent.getMaxX());
        northField.setValue(previewExtent.getMaxY());
        minZoomSpinner.setValue(previewMinZoom);
        maxZoomSpinner.setValue(previewMaxZoom);

        // show modal dialog
        int res = JOptionPane.showOptionDialog(this, extentPanel, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);

        // read new values from dialog
        if (res == JOptionPane.OK_OPTION) {
            double west = ((Number) westField.getValue()).doubleValue();
            double east = ((Number) eastField.getValue()).doubleValue();
            double south = ((Number) southField.getValue()).doubleValue();
            double north = ((Number) northField.getValue()).doubleValue();
            this.previewExtent = new Rectangle2D.Double(west, south, east - west, north - south);
            this.previewMinZoom = ((Number) minZoomSpinner.getValue()).intValue();
            this.previewMaxZoom = ((Number) maxZoomSpinner.getValue()).intValue();

            writeExtentPreferences();
        }
    }

    private void removeCurveFileButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeCurveFileButtonActionPerformed
        getSelectedMapLayer().setCurveURL(null);
        // update enable state of remove button and curve text area
        writeGUI();
        addUndo("Remove Curve");
    }//GEN-LAST:event_removeCurveFileButtonActionPerformed

    private String askTilesDirectory(String msg) {
        String directoryPath;
        try {
            directoryPath = FileUtils.askDirectory(GUIUtil.getOwnerFrame(this), msg, true, null);
        } catch (IOException ex) {
            Logger.getLogger(MapComposerPanel.class.getName()).log(Level.SEVERE, null, ex);
            directoryPath = null;
        }
        return directoryPath;
    }
    private void loadDirectoryPathButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadDirectoryPathButtonActionPerformed
        String directoryPath = askTilesDirectory("Select Tiles Directory");
        if (directoryPath != null) {
            String msg = "Is this an image or grid tile set?";
            String title = "Tile Set Type";
            Object[] selectionValues = {"Image", "Grid"};
            String typeStr = (String)JOptionPane.showInputDialog(this, msg, title, 
                    JOptionPane.QUESTION_MESSAGE, null, selectionValues, selectionValues[0]);
            String extension = "bil";
            if (typeStr.equals(selectionValues[0])) {
                extension = "png";
            }
            urlTextField.setText("file:///" + directoryPath + "/{z}/{x}/{y}." + extension);
            readGUI();
            addUndo("Load Tiles Directory");
        }
    }//GEN-LAST:event_loadDirectoryPathButtonActionPerformed

    private void maskLoadDirectoryPathButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_maskLoadDirectoryPathButtonActionPerformed
        String directoryPath = askTilesDirectory("Select Mask Directory with Tiles");
        if (directoryPath != null) {
            maskUrlTextField.setText("file:///" + directoryPath + "/{z}/{x}/{y}.png");
            this.readGUI();
            addUndo("Load Mask Tiles Directory");
        }
    }//GEN-LAST:event_maskLoadDirectoryPathButtonActionPerformed

    private void lockedCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lockedCheckBoxActionPerformed
        boolean locked = lockedCheckBox.isSelected();
        Layer layer = getSelectedMapLayer();
        if (layer != null) {
            this.updating = true;
            try {
                layer.setLocked(locked);
                writeGUI();
                addUndoFromNamedComponent((JComponent) (evt.getSource()));
            } finally {
                this.updating = false;
            }
        }
    }//GEN-LAST:event_lockedCheckBoxActionPerformed

    private void interpolColorButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_interpolColorButtonActionPerformed
        // TODO add your handling code here:
        // Set grids to use for interpolated color
        
        String directoryPath = askTilesDirectory("Select Grid Tile Set 1 of 2");
        if (directoryPath != null) {
            gridTextArea1.setText("file:///" + directoryPath + "/{z}/{x}/{y}.bil");
            this.readGUI();
            addUndo("Set Grid 1");
        }
        
        String directoryPath2 = askTilesDirectory("Select Grid Tile Set 2 of 2");
        if (directoryPath != null) {
            gridTextArea2.setText("file:///" + directoryPath2 + "/{z}/{x}/{y}.bil");
            this.readGUI();
            addUndo("Set Grid 2");
        }
        
        readGUI();

        writeGUI();
        addUndo("Interpolated Color");
    }//GEN-LAST:event_interpolColorButtonActionPerformed

    /**
     * Updates the value of the texture scale slider
     *
     * @param textureScale
     */
    private void writeTextureScale(float textureScale) {
        if (textureScale > 1) {
            textureScale = -100f + textureScale * 100f;
        } else {
            textureScale = -200 + textureScale * 200f;
        }
        this.textureScaleSlider.setValue((int) (textureScale));
    }

    /**
     * Reads the value of the texture scale slider.
     *
     * @return
     */
    private float readTextureScale() {
        // scale from [-100, +100] to [0.5, 2]
        float textureScale = this.textureScaleSlider.getValue();
        if (textureScale > 0) {
            textureScale = 1f + textureScale / 100f;
        } else {
            textureScale = 1f + textureScale * 0.5f / 100f;
        }
        return textureScale;
    }

    private void updateLayerList() {
        int selectedID = layerList.getSelectedIndex();
        layerList.setListData(map.getLayers());
        layerList.setSelectedIndex(selectedID);
    }

    /**
     * Writes settings of the currently selected layer to the GUI.
     */
    private void writeGUI() {
        int selectedLayerID = layerList.getSelectedIndex();
        Layer selectedLayer = getSelectedMapLayer();

        // enable or disable user interface elements depending on whether
        // a layer is currently selected
        final boolean on = selectedLayer != null && !selectedLayer.isLocked();
        boolean hasCurveURL = selectedLayer != null && selectedLayer.getCurveURL() != null;
        this.visibleCheckBox.setEnabled(on);
        this.urlTextField.setEnabled(on);
        this.loadDirectoryPathButton.setEnabled(on);
        this.tmsCheckBox.setEnabled(on);
        this.normalBlendingRadioButton.setEnabled(on);
        this.multiplyBlendingRadioButton.setEnabled(on);
        this.opacitySlider.setEnabled(on);
        this.curveTextArea.setEnabled(on);
        this.loadCurveFileButton.setEnabled(on);
        this.removeCurveFileButton.setEnabled(on && hasCurveURL);
        this.tintCheckBox.setEnabled(on);
        this.tintColorButton.setEnabled(on);
        this.textureSelectionButton.setEnabled(on);
        this.textureClearButton.setEnabled(on);
        this.textureURLLabel.setEnabled(on);
        this.textureScaleSlider.setEnabled(on);
        this.maskUrlTextField.setEnabled(on);
        this.maskTMSCheckBox.setEnabled(on);
        this.maskBlurSlider.setEnabled(on);
        this.maskLoadDirectoryPathButton.setEnabled(on);
        this.maskInvertCheckBox.setEnabled(on);
        this.shadowCheckBox.setEnabled(on);
        this.shadowOffsetSlider.setEnabled(on);
        this.shadowColorButton.setEnabled(on);
        this.shadowFuziSlider.setEnabled(on);
        this.embossCheckBox.setEnabled(on);
        this.embossHeightSlider.setEnabled(on);
        this.embossHeightFormattedTextField.setEnabled(on);
        this.embossSoftnessSlider.setEnabled(on);
        this.embossSoftnessFormattedTextField.setEnabled(on);
        this.embossAzimuthSlider.setEnabled(on);
        this.embossAzimuthFormattedTextField.setEnabled(on);
        this.embossElevationSlider.setEnabled(on);
        this.embossElevationFormattedTextField.setEnabled(on);
        this.gaussBlurSlider.setEnabled(on);
        this.removeLayerButton.setEnabled(on);
        this.moveUpLayerButton.setEnabled(on && selectedLayerID != 0);
        this.moveDownLayerButton.setEnabled(on && selectedLayerID != map.getLayerCount() - 1);

        if (selectedLayer == null) {
            this.urlTextField.setText(null);
            this.curveTextArea.setText(null);
            this.maskUrlTextField.setText(null);
            return;
        }
        
        if (this.updating) {
            return;
        }

        try {
            this.updating = true;

            this.visibleCheckBox.setSelected(selectedLayer.isVisible());
            this.lockedCheckBox.setSelected(selectedLayer.isLocked());

            TileSet tileSet = selectedLayer.getTileSet();
            if (tileSet == null) {
                this.urlTextField.setText(null);
            } else {
                this.urlTextField.setText(tileSet.getUrlTemplate());
            }

            // TMS schema
            this.tmsCheckBox.setSelected(tileSet != null ? tileSet.isTMSSchema() : false);

            // blending
            if (selectedLayer.isBlendingNormal()) {
                this.normalBlendingRadioButton.setSelected(true);
            } else {
                multiplyBlendingRadioButton.setSelected(true);
            }

            // opacity
            this.opacitySlider.setValue((int) (selectedLayer.getOpacity() * 100));
            opacityValueLabel.setText(Integer.toString(opacitySlider.getValue()));

            // curve
            this.curveTextArea.setText(selectedLayer.getCurveURL());

            // tinting
            if (selectedLayer.getTint() != null) {
                this.tintCheckBox.setSelected(true);
                this.tintColorButton.setColor(selectedLayer.getTint().getTintColor());
            } else {
                this.tintCheckBox.setSelected(false);
                this.tintColorButton.setColor(Color.BLACK);
            }

            // texture
            textureURLLabel.setText(selectedLayer.getTextureTileFilePath());
            this.writeTextureScale(selectedLayer.getTextureScale());
            this.textureScaleFormattedTextField.setValue(selectedLayer.getTextureScale());

            // mask
            TileSet maskTileSet = selectedLayer.getMaskTileSet();
            if (maskTileSet == null) {
                this.maskUrlTextField.setText(null);
            } else {
                this.maskUrlTextField.setText(maskTileSet.getUrlTemplate());
            }
            this.maskInvertCheckBox.setSelected(selectedLayer.isInvertMask());
            this.maskTMSCheckBox.setSelected(maskTileSet != null ? maskTileSet.isTMSSchema() : false);
            this.maskBlurSlider.setValue((int) (selectedLayer.getMaskBlur() * 10f));

            // drop shadow
            if (selectedLayer.getShadow() != null) {
                this.shadowCheckBox.setSelected(true);
                this.shadowOffsetSlider.setValue(selectedLayer.getShadow().getShadowOffset());
                this.shadowFuziSlider.setValue(selectedLayer.getShadow().getShadowFuziness());
                this.shadowColorButton.setColor(selectedLayer.getShadow().getShadowColor());
            } else {
                this.shadowCheckBox.setSelected(false);
            }

            // embossing
            if (selectedLayer.getEmboss() != null) {
                this.embossCheckBox.setSelected(true);
                this.embossHeightSlider.setEnabled(true);
                this.embossSoftnessSlider.setEnabled(true);
                this.embossAzimuthSlider.setEnabled(true);
                this.embossElevationSlider.setEnabled(true);

                this.embossHeightSlider.setValue((int) (selectedLayer.getEmboss().getEmbossHeight() * 100f));
                this.embossHeightFormattedTextField.setValue(selectedLayer.getEmboss().getEmbossHeight() * 100);
                this.embossSoftnessSlider.setValue((int) selectedLayer.getEmboss().getEmbossSoftness());
                this.embossSoftnessFormattedTextField.setValue(selectedLayer.getEmboss().getEmbossSoftness());
                this.embossAzimuthSlider.setValue((int) selectedLayer.getEmboss().getEmbossAzimuth());
                this.embossAzimuthFormattedTextField.setValue(selectedLayer.getEmboss().getEmbossAzimuth());
                this.embossElevationSlider.setValue((int) selectedLayer.getEmboss().getEmbossElevation());
                this.embossElevationFormattedTextField.setValue(selectedLayer.getEmboss().getEmbossElevation());
            } else {
                this.embossCheckBox.setSelected(false);
            }

            // gaussian blur
            this.gaussBlurSlider.setValue((int) (selectedLayer.getGaussBlur()));

        } finally {
            this.updating = false;
        }
    }

    /**
     * Reads user settings from the GUI and passes the settings to the currently
     * selected layer.
     */
    protected void readGUI() {
        if (this.updating) {
            return;
        }
        Layer layer = getSelectedMapLayer();
        if (layer == null) {
            return;
        }

        layer.setVisible(visibleCheckBox.isSelected());
        layer.setLocked(lockedCheckBox.isSelected());
        layer.setBlending(normalBlendingRadioButton.isSelected()
                ? Layer.BlendType.NORMAL : Layer.BlendType.MULTIPLY);
        layer.setOpacity(opacitySlider.getValue() / 100.f);

        // curve
        layer.setCurveURL(curveTextArea.getText());

        // URL
        layer.setTileSetURLTemplate(urlTextField.getText());

        // TMS
        layer.setTileSetTMSSchema(tmsCheckBox.isSelected());

        // mask
        layer.setMaskTileSetURLTemplate(maskUrlTextField.getText());
        layer.setInvertMask(this.maskInvertCheckBox.isSelected());
        layer.setMaskTileSetTMSSchema(maskTMSCheckBox.isSelected());
        layer.setMaskBlur(this.maskBlurSlider.getValue() / 10f);

        // tint
        if (this.tintCheckBox.isSelected()) {
            Tint tint = new Tint();
            tint.setTintColor(this.tintColorButton.getColor());
            layer.setTint(tint);
        } else {
            layer.setTint(null);
        }

        // texture
        layer.setTextureScale(this.readTextureScale());

        // shadow
        if (this.shadowCheckBox.isSelected()) {
            Shadow shadow = new Shadow();
            shadow.setShadowOffset(this.shadowOffsetSlider.getValue());
            shadow.setShadowColor(this.shadowColorButton.getColor());
            shadow.setShadowFuziness(this.shadowFuziSlider.getValue());
            layer.setShadow(shadow);
        } else {
            layer.setShadow(null);
        }

        // emboss
        if (this.embossCheckBox.isSelected()) {
            Emboss emboss = new Emboss();
            emboss.setEmbossHeight(this.embossHeightSlider.getValue() / 100f);
            emboss.setEmbossSoftness(this.embossSoftnessSlider.getValue());
            emboss.setEmbossAzimuth(this.embossAzimuthSlider.getValue());
            emboss.setEmbossElevation(this.embossElevationSlider.getValue());
            layer.setEmboss(emboss);
        } else {
            layer.setEmboss(null);
        }

        //gaussian blur
        layer.setGaussBlur(this.gaussBlurSlider.getValue());

        // re-render map preview
        reloadHTMLPreviewMap();
    }

    public Map getMap() {
        return map;
    }

    public void setMap(Map map) {
        this.map = map;
        updateLayerList();
        layerList.setSelectedIndex(layerList.getFirstVisibleIndex());
        this.writeGUI();
    }

    void addLayer(boolean focusList) {
        int layerID = this.layerList.getSelectedIndex() + 1;
        String name = "Layer " + (++layerCounter);
        map.addLayer(layerID, new Layer(name));
        updateLayerList();
        writeGUI();
        layerList.setSelectedIndex(layerID);
        if (focusList) {
            layerList.requestFocus();
        }
        addUndo("Add Layer");
    }

    void removeLayer() {
        int selectedLayerID = layerList.getSelectedIndex();
        if (selectedLayerID < 0) {
            return;
        }
        map.removeLayer(selectedLayerID);
        updateLayerList();
        reloadHTMLPreviewMap();
        writeGUI();
        layerList.setSelectedIndex(--selectedLayerID);
        addUndo("Remove Layer");
    }

    void removeAllLayers() {
        map.removeAllLayers();
        updateLayerList();
        reloadHTMLPreviewMap();
        writeGUI();
        addUndo("Remove All Layer");
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addLayerButton;
    private javax.swing.ButtonGroup blendingButtonGroup;
    private javax.swing.JPanel blurPanel;
    private javax.swing.JPanel centralPanel;
    private javax.swing.JPanel colorPanel;
    private javax.swing.JTextArea curveTextArea;
    private javax.swing.JFormattedTextField eastField;
    private javax.swing.JPanel effectsPanel;
    private javax.swing.JTabbedPane effectsTabbedPane;
    private javax.swing.JFormattedTextField embossAzimuthFormattedTextField;
    private javax.swing.JSlider embossAzimuthSlider;
    private javax.swing.JCheckBox embossCheckBox;
    private javax.swing.JFormattedTextField embossElevationFormattedTextField;
    private javax.swing.JSlider embossElevationSlider;
    private javax.swing.JFormattedTextField embossHeightFormattedTextField;
    private javax.swing.JSlider embossHeightSlider;
    private javax.swing.JPanel embossPanel;
    private javax.swing.JFormattedTextField embossSoftnessFormattedTextField;
    private javax.swing.JSlider embossSoftnessSlider;
    private javax.swing.JPanel extentPanel;
    private javax.swing.JLabel gaussBlurLabel;
    private javax.swing.JSlider gaussBlurSlider;
    private javax.swing.JTextArea gridTextArea1;
    private javax.swing.JTextArea gridTextArea2;
    private javax.swing.JButton interpolColorButton;
    private javax.swing.JLabel interpolColorLabel;
    private javax.swing.JFormattedTextField jFormattedTextField3;
    private javax.swing.JPanel jPanel1;
    private edu.oregonstate.carto.mapcomposer.gui.DraggableList layerList;
    private javax.swing.JScrollPane layerListScrollPane;
    private javax.swing.JToolBar layerListToolBar;
    private javax.swing.JPanel layersPanel;
    private javax.swing.JButton loadCurveFileButton;
    private javax.swing.JButton loadDirectoryPathButton;
    private javax.swing.JCheckBox lockedCheckBox;
    private javax.swing.JSlider maskBlurSlider;
    private javax.swing.JCheckBox maskInvertCheckBox;
    private javax.swing.JButton maskLoadDirectoryPathButton;
    private javax.swing.JCheckBox maskTMSCheckBox;
    private javax.swing.JTextField maskUrlTextField;
    private javax.swing.JSpinner maxZoomSpinner;
    private javax.swing.JSpinner minZoomSpinner;
    private javax.swing.JButton moveDownLayerButton;
    private javax.swing.JButton moveUpLayerButton;
    private javax.swing.JRadioButton multiplyBlendingRadioButton;
    private javax.swing.JRadioButton normalBlendingRadioButton;
    private javax.swing.JFormattedTextField northField;
    private javax.swing.JSlider opacitySlider;
    private javax.swing.JLabel opacityValueLabel;
    private javax.swing.JButton removeCurveFileButton;
    private javax.swing.JButton removeLayerButton;
    private javax.swing.JCheckBox shadowCheckBox;
    private edu.oregonstate.carto.mapcomposer.gui.ColorButton shadowColorButton;
    private javax.swing.JSlider shadowFuziSlider;
    private javax.swing.JLabel shadowOffsetLabel;
    private javax.swing.JSlider shadowOffsetSlider;
    private javax.swing.JFormattedTextField southField;
    private javax.swing.JButton textureClearButton;
    private javax.swing.JLabel texturePreviewLabel;
    private javax.swing.JFormattedTextField textureScaleFormattedTextField;
    private javax.swing.JSlider textureScaleSlider;
    private javax.swing.JButton textureSelectionButton;
    private javax.swing.JLabel textureURLLabel;
    private javax.swing.JPanel tilesPanel;
    private javax.swing.JCheckBox tintCheckBox;
    private edu.oregonstate.carto.mapcomposer.gui.ColorButton tintColorButton;
    private javax.swing.JCheckBox tmsCheckBox;
    private javax.swing.JTextField urlTextField;
    private javax.swing.JCheckBox visibleCheckBox;
    private javax.swing.JFormattedTextField westField;
    // End of variables declaration//GEN-END:variables

}
