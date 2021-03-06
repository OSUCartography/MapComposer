/*
 * MapComposerPanel.java
 *
 * Created on July 31, 2007, 8:27 AM
 */
package edu.oregonstate.carto.mapcomposer.gui;

import edu.oregonstate.carto.mapcomposer.Curve;
import edu.oregonstate.carto.mapcomposer.Emboss;
import edu.oregonstate.carto.mapcomposer.Layer;
import edu.oregonstate.carto.mapcomposer.Layer.ColorType;
import edu.oregonstate.carto.mapcomposer.Map;
import edu.oregonstate.carto.mapcomposer.Shadow;
import edu.oregonstate.carto.mapcomposer.tilerenderer.IDWGridTileRenderer;
import edu.oregonstate.carto.mapcomposer.tilerenderer.IDWPoint;
import edu.oregonstate.carto.tilemanager.Tile;
import edu.oregonstate.carto.tilemanager.TileGenerator;
import edu.oregonstate.carto.tilemanager.TileSet;
import edu.oregonstate.carto.tilemanager.util.Grid;
import edu.oregonstate.carto.utils.FileUtils;
import edu.oregonstate.carto.utils.GUIUtil;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
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

    private JavaFXMap javaFXMap = new JavaFXMap();

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
                if (!initialUpdating) {
                    reloadMapTiles();
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
            // TODO not complete
        }
    }

    /**
     * JavaScript-to-Java communication FIXME call to event dispatch thread
     * should not be in this class
     */
    public class JavaScriptBridge {

        // this is supposedly run in the JavaFX thread
        public void colorPointChanged() {
            System.out.println("JavaScript changed color points");
            // run in Swing event dispatching thread
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    readIDWPoints(true);
                }
            });
        }
    }

    /**
     * Change text color to red if URL in a text field is not valid
     *
     * @param textField
     */
    private static void adjustURLTextFieldColor(JTextField textField) {
        String tileSetURL = textField.getText();
        boolean urlTemplateIsValid = TileSet.isURLTemplateValid(tileSetURL);
        Color okColor = UIManager.getDefaults().getColor("TextField.foreground");
        textField.setForeground(urlTemplateIsValid ? okColor : Color.RED);
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
                adjustURLTextFieldColor(urlTextField);
                layer.setTileSetURLTemplate(urlTextField.getText());
            }
        });

        // add document listener to maskURL text field
        maskUrlTextField.getDocument().addDocumentListener(new DocumentListenerAdaptor() {
            @Override
            protected void textChanged(Layer layer, DocumentEvent e) {
                adjustURLTextFieldColor(maskUrlTextField);
                layer.setMaskTileSetURLTemplate(maskUrlTextField.getText());
            }
        });

        // add document listener to mask values text field
        maskValuesTextField.getDocument().addDocumentListener(new DocumentListenerAdaptor() {
            @Override
            protected void textChanged(Layer layer, DocumentEvent e) {
                layer.setMaskValues(maskValuesTextField.getText().trim());
            }
        });

        final JPanel panel = this;
        idwPreview.addMouseListener(new MouseAdapter() {

            private boolean hasValidIDWTileSets(Layer layer) {
                TileSet tileSet1 = layer.getGrid1TileSet();
                TileSet tileSet2 = layer.getGrid2TileSet();
                return tileSet1.isURLTemplateValid() && tileSet2.isURLTemplateValid();
            }

            @Override
            public void mouseClicked(MouseEvent e) {

                Layer layer = getSelectedMapLayer();
                String horLabel = layer.getGrid1TileSet().getUrlTemplate();
                if (horLabel != null) {
                    String[] tokens = horLabel.split("/");
                    if (tokens.length >= 5) {
                        horLabel = tokens[tokens.length - 4];
                    }
                }
                idwHorizontalLabel.setText(horLabel);

                String verLabel = layer.getGrid2TileSet().getUrlTemplate();
                if (verLabel != null) {
                    String[] tokens = verLabel.split("/");
                    if (tokens.length >= 4) {
                        verLabel = tokens[tokens.length - 4];
                    }
                }
                idwVerticalLabel.setText(verLabel);
                idwPanel.setIdw(layer.getIdwTileRenderer());

                // make sure the tile sets for IDW interpolation are valid
                if (!hasValidIDWTileSets(layer)) {
                    selectIDWTileSets();
                }
                if (!hasValidIDWTileSets(layer)) {
                    return;
                }

                // read current color points from map
                readIDWPoints(false);

                // open dialog to adjust color points
                String title = "IDW Color Interpolation";
                Object[] options = {"OK"};
                JOptionPane.showOptionDialog(panel, idwColorPanel, title,
                        JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

                // re-render map preview
                reloadMapTiles();
                addUndo("IDW Color Points");
            }
        });

        idwColorChooser.getSelectionModel().addChangeListener(new javax.swing.event.ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                idwPanel.setSelectedColor(idwColorChooser.getColor());
            }
        });

        writeGUI(false);
    }

    /**
     * Creates the preview map.
     */
    private void initMapPreview() {
        final JFXPanel fxPanel = new JFXPanel();
        mapPanel.add(fxPanel, BorderLayout.CENTER);
        final String html = JavaFXMap.fillHTMLMapTemplate(canAddColorPoints(), 2, 0, 0);

        // run in the JavaFX thread
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                javaFXMap.init(fxPanel, html);

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        loadHTMLMap();
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
                        javaFXMap.setPreferredSize(w, h);
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
     * Loads the HTML map. To be called from the Swing thread.
     */
    // FIXME remove
    int loadHTMLCounter = 0;

    public void loadHTMLMap() {
        assert SwingUtilities.isEventDispatchThread();
        System.out.println("Map load " + ++loadHTMLCounter);
        final String colorPointsStr = canAddColorPoints() ? getColorPointsOfSelectedLayer() : null;

        // run in JavaFX thread
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                javaFXMap.loadHTMLMap(colorPointsStr, null, null, null, new JavaScriptBridge());
            }
        });
    }

    // FIXME remove
    int reloadTilesCounter = 0;

    public void reloadMapTiles() {
        assert SwingUtilities.isEventDispatchThread();

        final String colorPointsStr = canAddColorPoints() ? getColorPointsOfSelectedLayer() : null;
        System.out.println("Tiles reload " + ++reloadTilesCounter + " " + colorPointsStr != null ? colorPointsStr : "");

        // run in JavaFX thread
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                javaFXMap.reloadTiles(colorPointsStr);
            }
        });
    }

    /**
     * Set zoom level and central point of map.
     *
     * @param zoom Map zoom level.
     * @param lon Longitude of central point.
     * @param lat Latitudes of central point.
     */
    public void panAndZoom(final int zoom, final double lon, final double lat) {
        assert SwingUtilities.isEventDispatchThread();

        final String colorPointsStr = canAddColorPoints() ? getColorPointsOfSelectedLayer() : null;

        // run in JavaFX thread
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                javaFXMap.loadHTMLMap(colorPointsStr, lon, lat, zoom, new JavaScriptBridge());
            }
        });
    }

    /**
     * Returns the MapLayer currently selected by the user.
     *
     * @return The selected map layer or null if none is selected.
     */
    private Layer getSelectedMapLayer() {
        assert SwingUtilities.isEventDispatchThread();
        int index = layerList.getSelectedIndex();
        return index == -1 ? null : map.getLayer(index);
    }

    /**
     * Returns the color points of the selected layer.
     *
     * @return String with encoded color points or null, if no points exist.
     */
    private String getColorPointsOfSelectedLayer() {
        Layer selectedLayer = getSelectedMapLayer();
        if (selectedLayer != null) {
            return selectedLayer.getIdwTileRenderer().getColorPointsString();
        }
        return null;
    }

    /**
     * Returns true if the user is allowed to add color points to the map.
     *
     * @return
     */
    private boolean canAddColorPoints() {
        assert SwingUtilities.isEventDispatchThread();
        Layer selectedLayer = getSelectedMapLayer();
        return selectedLayer != null && selectedLayer.getColorType() == ColorType.INTERPOLATE;
    }

    private void byteArrayToMap(byte[] buf) {
        if (buf == null) {
            return;
        }
        try {
            setMap(Map.unmarshal(buf));
            loadHTMLMap();
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
        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        textScrollPane = new javax.swing.JScrollPane();
        colorPointsTextArea = new javax.swing.JTextArea();
        idwColorPanel = new javax.swing.JPanel();
        idwPanel = new edu.oregonstate.carto.mapcomposer.gui.IDWPanel();
        idwExponentSlider = new javax.swing.JSlider();
        javax.swing.JLabel idwExponentSliderLabel = new javax.swing.JLabel();
        idwExponentValueLabel = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        idwColorChooser = new javax.swing.JColorChooser();
        idwHorizontalLabel = new javax.swing.JLabel();
        idwVerticalLabel = new edu.oregonstate.carto.mapcomposer.gui.RotatedLabel();
        javax.swing.JPanel idwButtonPanel = new javax.swing.JPanel();
        idwTileSetsButton = new javax.swing.JButton();
        idwApplyButton = new javax.swing.JButton();
        idwRadioButton = new javax.swing.JRadioButton();
        gaussRadioButton = new javax.swing.JRadioButton();
        idwTileSetsPanel = new javax.swing.JPanel();
        javax.swing.JLabel grid1URLLabel = new javax.swing.JLabel();
        grid1URLTextField = new javax.swing.JTextField();
        grid1TMSCheckBox = new javax.swing.JCheckBox();
        grid1LoadDirectoryPathButton = new javax.swing.JButton();
        javax.swing.JLabel grid2URLLabel = new javax.swing.JLabel();
        grid2URLTextField = new javax.swing.JTextField();
        grid2TMSCheckBox = new javax.swing.JCheckBox();
        grid2LoadDirectoryPathButton = new javax.swing.JButton();
        curvesPanel = new javax.swing.JPanel();
        gradationGraph = new edu.oregonstate.carto.mapcomposer.gui.GradationGraph();
        loadCurveFileButton = new javax.swing.JButton();
        resetCurveFileButton = new javax.swing.JButton();
        interpolationMethodButtonGroup = new javax.swing.ButtonGroup();
        layersPanel = new javax.swing.JPanel();
        Icon folderIcon = UIManager.getDefaults().getIcon("FileView.directoryIcon");
        int iconH = folderIcon.getIconHeight();
        int iconW = folderIcon.getIconWidth();
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
        colorSelectionComboBox = new javax.swing.JComboBox();
        colorMethodPanel = new TransparentMacPanel();
        javax.swing.JPanel emptyPanel = new TransparentMacPanel();
        javax.swing.JPanel solidColorPanel = new TransparentMacPanel();
        tintColorButton = new edu.oregonstate.carto.mapcomposer.gui.ColorButton();
        javax.swing.JPanel interpolatedColorPanel = new TransparentMacPanel();
        idwPreview = new edu.oregonstate.carto.mapcomposer.gui.IDWPreview();
        colorLabel = new javax.swing.JLabel();
        curvesButton = new javax.swing.JButton();
        tilesPanel = new TransparentMacPanel();
        javax.swing.JLabel urlLabel = new javax.swing.JLabel();
        urlTextField = new javax.swing.JTextField();
        tmsCheckBox = new javax.swing.JCheckBox();
        loadDirectoryPathButton = new javax.swing.JButton();
        javax.swing.JTextArea urlHintTextArea = new javax.swing.JTextArea();
        javax.swing.JPanel maskPanel = new TransparentMacPanel();
        maskInvertCheckBox = new javax.swing.JCheckBox();
        javax.swing.JLabel maskBlurLabel = new javax.swing.JLabel();
        maskBlurSlider = new javax.swing.JSlider();
        maskUrlTextField = new javax.swing.JTextField();
        javax.swing.JTextArea urlHintTextArea1 = new javax.swing.JTextArea();
        maskLoadDirectoryPathButton = new javax.swing.JButton();
        maskTMSCheckBox = new javax.swing.JCheckBox();
        javax.swing.JLabel urlLabel1 = new javax.swing.JLabel();
        maskValuesTextField = new javax.swing.JTextField();
        javax.swing.JLabel jLabel7 = new javax.swing.JLabel();
        effectsTabbedPane = new javax.swing.JTabbedPane();
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
        effectsPanel = new TransparentMacPanel();
        shadowCheckBox = new javax.swing.JCheckBox();
        shadowOffsetLabel = new javax.swing.JLabel();
        shadowOffsetSlider = new javax.swing.JSlider();
        shadowColorButton = new edu.oregonstate.carto.mapcomposer.gui.ColorButton();
        javax.swing.JLabel DropShadowFuzinessLabel = new javax.swing.JLabel();
        shadowFuziSlider = new javax.swing.JSlider();
        mapPanel = new javax.swing.JPanel();

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

        colorPointsTextArea.setColumns(20);
        colorPointsTextArea.setRows(5);
        textScrollPane.setViewportView(colorPointsTextArea);

        idwColorPanel.setLayout(new java.awt.GridBagLayout());

        idwPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        idwPanel.setPreferredSize(new java.awt.Dimension(250, 250));
        idwPanel.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                idwPanelPropertyChange(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        idwColorPanel.add(idwPanel, gridBagConstraints);

        idwExponentSlider.setMajorTickSpacing(10);
        idwExponentSlider.setMaximum(50);
        idwExponentSlider.setMinorTickSpacing(5);
        idwExponentSlider.setPaintTicks(true);
        idwExponentSlider.setPreferredSize(new java.awt.Dimension(130, 38));
        idwExponentSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                idwExponentSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        idwColorPanel.add(idwExponentSlider, gridBagConstraints);

        idwExponentSliderLabel.setText("Exponent");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        idwColorPanel.add(idwExponentSliderLabel, gridBagConstraints);

        idwExponentValueLabel.setFont(idwExponentValueLabel.getFont().deriveFont(idwExponentValueLabel.getFont().getSize()-2f));
        idwExponentValueLabel.setText("1.3");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        idwColorPanel.add(idwExponentValueLabel, gridBagConstraints);

        jLabel9.setFont(jLabel9.getFont().deriveFont(jLabel9.getFont().getSize()-2f));
        jLabel9.setText("Click to add points. Click and drag to move points.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        idwColorPanel.add(jLabel9, gridBagConstraints);

        jLabel10.setFont(jLabel10.getFont().deriveFont(jLabel10.getFont().getSize()-2f));
        jLabel10.setText("Hit the delete key to remove the selected point.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        idwColorPanel.add(jLabel10, gridBagConstraints);

        idwColorChooser.setPreviewPanel(new JPanel());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        idwColorPanel.add(idwColorChooser, gridBagConstraints);

        idwHorizontalLabel.setFont(idwHorizontalLabel.getFont().deriveFont(idwHorizontalLabel.getFont().getSize()-2f));
        idwHorizontalLabel.setText("horLabel");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 8, 0);
        idwColorPanel.add(idwHorizontalLabel, gridBagConstraints);

        idwVerticalLabel.setText("vertLabel");
        idwVerticalLabel.setFont(idwVerticalLabel.getFont().deriveFont(idwVerticalLabel.getFont().getSize()-2f));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        idwColorPanel.add(idwVerticalLabel, gridBagConstraints);

        idwButtonPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 10, 5));

        idwTileSetsButton.setText("Tile Sets…");
        idwTileSetsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                idwTileSetsButtonActionPerformed(evt);
            }
        });
        idwButtonPanel.add(idwTileSetsButton);

        idwApplyButton.setText("Apply");
        idwApplyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                idwApplyButtonActionPerformed(evt);
            }
        });
        idwButtonPanel.add(idwApplyButton);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 4;
        idwColorPanel.add(idwButtonPanel, gridBagConstraints);

        interpolationMethodButtonGroup.add(idwRadioButton);
        idwRadioButton.setText("Inverse Distance");
        idwRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                idwRadioButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        idwColorPanel.add(idwRadioButton, gridBagConstraints);

        interpolationMethodButtonGroup.add(gaussRadioButton);
        gaussRadioButton.setSelected(true);
        gaussRadioButton.setText("Gaussian Weight");
        gaussRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                idwRadioButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        idwColorPanel.add(gaussRadioButton, gridBagConstraints);

        idwTileSetsPanel.setLayout(new java.awt.GridBagLayout());

        grid1URLLabel.setText("Grid 1 URL: Horizontal Axis");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        idwTileSetsPanel.add(grid1URLLabel, gridBagConstraints);

        grid1URLTextField.setFont(grid1URLTextField.getFont().deriveFont(grid1URLTextField.getFont().getSize()-2f));
        grid1URLTextField.setText("http://tile.stamen.com/watercolor/{z}/{x}/{y}.png");
        grid1URLTextField.setPreferredSize(new java.awt.Dimension(600, 28));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        idwTileSetsPanel.add(grid1URLTextField, gridBagConstraints);

        grid1TMSCheckBox.setText("TMS");
        grid1TMSCheckBox.setName("Tiles TMS"); // NOI18N
        grid1TMSCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MapComposerPanel.this.actionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        idwTileSetsPanel.add(grid1TMSCheckBox, gridBagConstraints);

        grid1LoadDirectoryPathButton.setIcon(folderIcon);
        grid1LoadDirectoryPathButton.setPreferredSize(new Dimension(iconW + 18, iconH + 18));
        grid1LoadDirectoryPathButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                grid1LoadDirectoryPathButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        idwTileSetsPanel.add(grid1LoadDirectoryPathButton, gridBagConstraints);

        grid2URLLabel.setText("Grid 2 URL: Vertical Axis");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(30, 0, 0, 0);
        idwTileSetsPanel.add(grid2URLLabel, gridBagConstraints);

        grid2URLTextField.setFont(grid2URLTextField.getFont().deriveFont(grid2URLTextField.getFont().getSize()-2f));
        grid2URLTextField.setText("http://tile.stamen.com/watercolor/{z}/{x}/{y}.png");
        grid2URLTextField.setPreferredSize(new java.awt.Dimension(200, 28));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        idwTileSetsPanel.add(grid2URLTextField, gridBagConstraints);

        grid2TMSCheckBox.setText("TMS");
        grid2TMSCheckBox.setName("Tiles TMS"); // NOI18N
        grid2TMSCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MapComposerPanel.this.actionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        idwTileSetsPanel.add(grid2TMSCheckBox, gridBagConstraints);

        grid2LoadDirectoryPathButton.setIcon(folderIcon);
        grid2LoadDirectoryPathButton.setPreferredSize(new Dimension(iconW + 18, iconH + 18));
        grid2LoadDirectoryPathButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                grid2LoadDirectoryPathButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        idwTileSetsPanel.add(grid2LoadDirectoryPathButton, gridBagConstraints);

        curvesPanel.setLayout(new java.awt.GridBagLayout());

        gradationGraph.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                gradationGraphPropertyChange(evt);
            }
        });
        curvesPanel.add(gradationGraph, new java.awt.GridBagConstraints());

        loadCurveFileButton.setText("Load Photoshop ACV Curve File");
        loadCurveFileButton.setToolTipText("Load an ACV Photoshop curve file");
        loadCurveFileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadCurveFileButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 8, 0);
        curvesPanel.add(loadCurveFileButton, gridBagConstraints);

        resetCurveFileButton.setText("Reset Curves");
        resetCurveFileButton.setToolTipText("Remove the curve");
        resetCurveFileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetCurveFileButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        curvesPanel.add(resetCurveFileButton, gridBagConstraints);

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
                visibleCheckBoxActionPerformed(evt);
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
        gridBagConstraints.gridy = 1;
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
        gridBagConstraints.gridy = 1;
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
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        colorPanel.add(multiplyBlendingRadioButton, gridBagConstraints);

        opacityLabel.setText("Opacity:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
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
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        colorPanel.add(opacitySlider, gridBagConstraints);

        opacityValueLabel.setFont(opacityValueLabel.getFont().deriveFont(opacityValueLabel.getFont().getSize()-2f));
        opacityValueLabel.setText("100");
        opacityValueLabel.setPreferredSize(new java.awt.Dimension(30, 14));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 6;
        colorPanel.add(opacityValueLabel, gridBagConstraints);

        colorSelectionComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "None", "Solid", "Interpolated" }));
        colorSelectionComboBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                colorSelectionComboBoxItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        colorPanel.add(colorSelectionComboBox, gridBagConstraints);

        colorMethodPanel.setLayout(new java.awt.CardLayout());
        colorMethodPanel.add(emptyPanel, "emptyCard");

        tintColorButton.setName("Tint"); // NOI18N
        tintColorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MapComposerPanel.this.actionPerformed(evt);
            }
        });
        solidColorPanel.add(tintColorButton);

        colorMethodPanel.add(solidColorPanel, "solidColorCard");

        interpolatedColorPanel.setLayout(new java.awt.GridBagLayout());

        idwPreview.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        idwPreview.setPreferredSize(new java.awt.Dimension(70, 70));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.gridwidth = 3;
        interpolatedColorPanel.add(idwPreview, gridBagConstraints);

        colorMethodPanel.add(interpolatedColorPanel, "interpolatedColorCard");

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.gridheight = 3;
        colorPanel.add(colorMethodPanel, gridBagConstraints);

        colorLabel.setText("Color:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        colorPanel.add(colorLabel, gridBagConstraints);

        curvesButton.setText("Curves…");
        curvesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                curvesButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        colorPanel.add(curvesButton, gridBagConstraints);

        settingsTabbedPane.addTab("Color", colorPanel);

        tilesPanel.setLayout(new java.awt.GridBagLayout());

        urlLabel.setText("URL");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        tilesPanel.add(urlLabel, gridBagConstraints);

        urlTextField.setFont(urlTextField.getFont().deriveFont(urlTextField.getFont().getSize()-2f));
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

        loadDirectoryPathButton.setIcon(folderIcon);
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

        urlHintTextArea.setEditable(false);
        urlHintTextArea.setColumns(20);
        urlHintTextArea.setFont(urlHintTextArea.getFont().deriveFont(urlHintTextArea.getFont().getSize()-2f));
        urlHintTextArea.setLineWrap(true);
        urlHintTextArea.setRows(4);
        urlHintTextArea.setText("Examples:\nhttp://tile.openstreetmap.org/{z}/{x}/{y}.png\nhttp://tile.stamen.com/watercolor/{z}/{x}/{y}.png\nhttp://services.arcgisonline.com/ArcGIS/rest/services/NatGeo_World_Map/MapServer/tile/{z}/{y}/{x}.png");
        urlHintTextArea.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        tilesPanel.add(urlHintTextArea, gridBagConstraints);

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

        maskUrlTextField.setFont(maskUrlTextField.getFont().deriveFont(maskUrlTextField.getFont().getSize()-2f));
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

        maskValuesTextField.setText("11 40");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        maskPanel.add(maskValuesTextField, gridBagConstraints);

        jLabel7.setText("Mask Values");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
        maskPanel.add(jLabel7, gridBagConstraints);

        settingsTabbedPane.addTab("Mask", maskPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        bottomPanel.add(settingsTabbedPane, gridBagConstraints);

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

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        bottomPanel.add(effectsTabbedPane, gridBagConstraints);

        layersPanel.add(bottomPanel, java.awt.BorderLayout.SOUTH);

        add(layersPanel, java.awt.BorderLayout.WEST);

        mapPanel.setLayout(new java.awt.BorderLayout());
        add(mapPanel, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Event handler for moving a layer downwards in the layers hierarchy.
     *
     * @param evt
     */
    private void moveDownLayerButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveDownLayerButtonActionPerformed
        try {
            updating = true;
            int selectedLayerID = layerList.getSelectedIndex();
            if (selectedLayerID < 0 || selectedLayerID >= map.getLayerCount() - 1) {
                return;
            }
            Layer layer = map.removeLayer(selectedLayerID);
            map.addLayer(++selectedLayerID, layer);
            updateLayerList();
            layerList.setSelectedIndex(selectedLayerID);
        } finally {
            updating = false;
        }
        writeGUI(true);
        addUndo("Move Layer Down");
    }//GEN-LAST:event_moveDownLayerButtonActionPerformed

    /**
     * Event handler for moving a layer upwards in the layers hierarchy.
     *
     * @param evt
     */
    private void moveUpLayerButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveUpLayerButtonActionPerformed
        try {
            updating = true;
            int selectedLayerID = layerList.getSelectedIndex();
            if (selectedLayerID <= 0) {
                return;
            }
            Layer layer = map.removeLayer(selectedLayerID);
            map.addLayer(--selectedLayerID, layer);
            updateLayerList();
            layerList.setSelectedIndex(selectedLayerID);
        } finally {
            updating = false;
        }
        writeGUI(true);
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
        String html = JavaFXMap.fillHTMLMapTemplate(canAddColorPoints(), zoom, centerLon, centerLat);
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
        assert SwingUtilities.isEventDispatchThread();

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
            readGUIAndRenderMap();
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
        readGUIAndRenderMap();
        addUndoFromNamedComponent((JComponent) (evt.getSource()));
    }//GEN-LAST:event_actionPerformed

    /**
     * Event handler called when the selection in the layers list changes.
     *
     * @param evt
     */
    private void layerListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_layerListValueChanged
        if (!evt.getValueIsAdjusting() && !updating) {
            writeGUI(true);
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
                writeGUI(true);
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
            try {
                layer.setTextureTileFilePath(null);
            } catch (IOException ex) {
                // setting texture tile to null will not throw an IOException.
            }
        }
        writeGUI(true);
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
        String fileURL = "file:///" + filePath;
        getSelectedMapLayer().loadCurve(fileURL);
        gradationGraph.setCurves(getSelectedMapLayer().getCurves());
        reloadMapTiles();
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

    private void resetCurveFileButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetCurveFileButtonActionPerformed
        Curve curve = new Curve();
        getSelectedMapLayer().setCurve(curve);
        gradationGraph.setCurve(curve);
        reloadMapTiles();
        addUndo("Reset Curve");
    }//GEN-LAST:event_resetCurveFileButtonActionPerformed

    private String askTilesDirectory(String msg) {
        assert SwingUtilities.isEventDispatchThread();

        String directoryPath;
        try {
            directoryPath = FileUtils.askDirectory(GUIUtil.getOwnerFrame(this), msg, true, null);
        } catch (IOException ex) {
            Logger.getLogger(MapComposerPanel.class.getName()).log(Level.SEVERE, null, ex);
            directoryPath = null;
        }
        return directoryPath;
    }

    private boolean askIsImageTileSet() {
        assert SwingUtilities.isEventDispatchThread();

        String msg = "Is this an image or grid tile set?";
        String title = "Tile Set Type";
        Object[] selectionValues = {"Image", "Grid"};
        String typeStr = (String) JOptionPane.showInputDialog(this, msg, title,
                JOptionPane.QUESTION_MESSAGE, null, selectionValues, selectionValues[0]);
        return typeStr.equals(selectionValues[0]);
    }
    private void loadDirectoryPathButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadDirectoryPathButtonActionPerformed
        String directoryPath = askTilesDirectory("Select Tiles Directory");
        if (directoryPath != null) {
            String extension = askIsImageTileSet() ? "png" : "bil";
            urlTextField.setText("file:///" + directoryPath + "/{z}/{x}/{y}." + extension);
            readGUIAndRenderMap();
            addUndo("Load Tiles Directory");
        }
    }//GEN-LAST:event_loadDirectoryPathButtonActionPerformed

    private void maskLoadDirectoryPathButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_maskLoadDirectoryPathButtonActionPerformed
        String directoryPath = askTilesDirectory("Select Mask Directory with Tiles");
        if (directoryPath != null) {
            String extension = askIsImageTileSet() ? "png" : "bil";
            maskUrlTextField.setText("file:///" + directoryPath + "/{z}/{x}/{y}." + extension);
            readGUIAndRenderMap();
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
                writeGUI(false);
                addUndoFromNamedComponent((JComponent) (evt.getSource()));
            } finally {
                this.updating = false;
            }
        }
    }//GEN-LAST:event_lockedCheckBoxActionPerformed

    private void colorSelectionComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_colorSelectionComboBoxItemStateChanged
        if (evt.getStateChange() != ItemEvent.SELECTED) {
            return;
        }
        int selectedMenuItem = colorSelectionComboBox.getSelectedIndex();
        CardLayout cl = (CardLayout) (colorMethodPanel.getLayout());
        switch (selectedMenuItem) {
            case 0:
                cl.show(colorMethodPanel, "emptyCard");
                break;
            case 1:
                cl.show(colorMethodPanel, "solidColorCard");
                break;
            case 2:
                cl.show(colorMethodPanel, "interpolatedColorCard");
                break;
        }
        readGUIAndRenderMap();

        // show dialog to select IDW tile sets if they have not been defined.
        if (selectedMenuItem == 2 /* interpolated colors */) {
            if (getSelectedMapLayer().isIDWGridTileURLTemplatesValid() == false) {
                selectIDWTileSets();
                // if user did not select tiles sets, switch back to solid color
                if (getSelectedMapLayer().isIDWGridTileURLTemplatesValid() == false) {
                    colorSelectionComboBox.setSelectedIndex(0);
                }
            }
        }
    }//GEN-LAST:event_colorSelectionComboBoxItemStateChanged

    private void idwExponentSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_idwExponentSliderStateChanged
        double exp = idwExponentSlider.getValue() / 10d;
        getSelectedMapLayer().getIdwTileRenderer().setExponentP(exp);
        idwExponentValueLabel.setText(Double.toString(exp));
        idwPanel.repaint();
        idwPreview.repaint();
        if (idwExponentSlider.getValueIsAdjusting() == false) {
            reloadMapTiles();
        }
    }//GEN-LAST:event_idwExponentSliderStateChanged

    private void grid1LoadDirectoryPathButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_grid1LoadDirectoryPathButtonActionPerformed
        String directoryPath = askTilesDirectory("Select Grid Tiles Directory");
        if (directoryPath != null) {
            grid1URLTextField.setText("file:///" + directoryPath + "/{z}/{x}/{y}.bil");
            readGUIAndRenderMap();
            addUndo("Load Grid Tiles Directory");
        }
    }//GEN-LAST:event_grid1LoadDirectoryPathButtonActionPerformed

    private void grid2LoadDirectoryPathButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_grid2LoadDirectoryPathButtonActionPerformed
        String directoryPath = askTilesDirectory("Select Grid Tiles Directory");
        if (directoryPath != null) {
            grid2URLTextField.setText("file:///" + directoryPath + "/{z}/{x}/{y}.bil");
            readGUIAndRenderMap();
            addUndo("Load Grid Tiles Directory");
        }
    }//GEN-LAST:event_grid2LoadDirectoryPathButtonActionPerformed

    private void idwPanelPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_idwPanelPropertyChange
        System.out.println("Property changed: " + evt.getPropertyName());
        System.out.println(idwPanel.isValueAdjusting());
        if ("selectedPoint".equals(evt.getPropertyName())) {
            
            // update color of color chooser after click on a color point
            if (evt.getNewValue() == null) {
                // a new point was added
                idwColorChooser.setColor(Color.BLACK);
            } else {
                // clicked on existing point
                IDWPoint pt = (IDWPoint) (evt.getNewValue());
                idwColorChooser.setColor(pt.getColor());
            }
        }
        
        // color changed or point was moved
        if ("colorChanged".equals(evt.getPropertyName()) || "colorDeleted".equals(evt.getPropertyName())) {
            idwPanel.getIdw().colorPointsChanged();
            idwPreview.repaint();
            if (idwPanel.isValueAdjusting() == false || "colorDeleted".equals(evt.getPropertyName())) {
                reloadMapTiles();
            }
        }
    }//GEN-LAST:event_idwPanelPropertyChange

    /**
     * extract values from grid TileSet
     *
     * @param zoom
     * @param lon
     * @param lat
     * @param tileSet
     * @return
     */
    private static float getValueFromGridTileSet(int zoom, double lon, double lat, TileSet tileSet) throws IOException {
        double[] mxy = new double[2];
        double[] pxy = new double[2];
        double[] tltxy = new double[2];
        Tile tile = tileSet.getTile(zoom, lon, lat);
        // convert latitude/longitude to meters
        TileSet.latLonToMeters(lat, lon, mxy);
        // convert from meters to pixel coordinates
        TileSet.metersToPixels(mxy[0], mxy[1], zoom, pxy);
        // convert to pixel coordinates relative to top-left corner.
        TileSet.pixelsToTopLeftTilePixels(pxy[0], pxy[1], tltxy);
        // get grid value
        Grid grid = (Grid) tile.fetch();
        //FIXME
        grid.setCellSize(1);
        return grid.getBilinearInterpol(tltxy[0], -tltxy[1]);
    }

    /**
     * Reads IDW color points from the map and passes them to the
     * IDWGridTileRenderer of the currently selected layer. Retains color points
     * that are not georeferenced.
     */
    private void readIDWPoints(final boolean reloadMapTiles) {
        final Layer selectedLayer = getSelectedMapLayer();
        if (selectedLayer == null) {
            return;
        }
        final TileSet tileSet1 = selectedLayer.getGrid1TileSet();
        final TileSet tileSet2 = selectedLayer.getGrid2TileSet();
        if (!tileSet1.isURLTemplateValid() || !tileSet2.isURLTemplateValid()) {
            String msg = "Tile sets have not been selected.";
            ErrorDialog.showErrorDialog(msg, "MapComposer Error", null, this);
        }

        // array with final points
        final ArrayList<IDWPoint> points = new ArrayList<>();

        // copy color points that don't have a valid geographic location, that is,
        // they are contained in the diagram, but not in the map.
        final IDWGridTileRenderer idwRenderer = selectedLayer.getIdwTileRenderer();
        ArrayList<IDWPoint> oldPoints = idwRenderer.getPoints();
        for (IDWPoint p : oldPoints) {
            if (!p.isLonLatDefined()) {
                points.add(p);
            }
        }

        // read points from map and extract values for each point from grid tile sets
        // run in JavaFX thread
        Platform.runLater(new Runnable() {

            ArrayList<IDWPoint> pointsOnMap;

            @Override
            public void run() {
                // run JavaScript code and parse the result
                try {
                    pointsOnMap = javaFXMap.getColorPoints();
                } catch (Throwable ex) {
                    pointsOnMap = new ArrayList<>();
                }

                // for each point on the map, read values from grid tile sets
                // run in Swing event dispatch thread
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            for (IDWPoint p : pointsOnMap) {
                                // FIXME TODO
                                int z = 8;

                                double lon = p.getLon();
                                double lat = p.getLat();
                                float v1 = getValueFromGridTileSet(z, lon, lat, tileSet1);
                                float v2 = getValueFromGridTileSet(z, lon, lat, tileSet2);
                                if (!Float.isNaN(v1) && !Float.isNaN(v2)) {
                                    p.setAttribute1(v1);
                                    p.setAttribute2(v2);
                                    points.add(p);
                                }
                            }
                            idwRenderer.setColorPoints(points);
                            idwPanel.repaint();
                            idwPreview.repaint();
                            if (reloadMapTiles) {
                                reloadMapTiles();
                            }
                        } catch (Throwable ex) {
                            Logger.getLogger(MapComposerPanel.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                });
            }
        });
    }

    private void selectIDWTileSets() {
        assert SwingUtilities.isEventDispatchThread();
        Object[] options = {"OK"};
        String title = "Tile Sets for IDW Interpolation";
        JOptionPane.showOptionDialog(this, idwTileSetsPanel, title,
                JOptionPane.YES_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        readGUIAndRenderMap();
    }

    private void idwApplyButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_idwApplyButtonActionPerformed
        reloadMapTiles();
    }//GEN-LAST:event_idwApplyButtonActionPerformed

    private void idwTileSetsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_idwTileSetsButtonActionPerformed
        selectIDWTileSets();
    }//GEN-LAST:event_idwTileSetsButtonActionPerformed

    private void curvesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_curvesButtonActionPerformed
        Object[] options = {"OK"};
        String title = "Curves";
        gradationGraph.setCurves(getSelectedMapLayer().getCurves());
        JOptionPane.showOptionDialog(this, curvesPanel, title,
                JOptionPane.YES_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        readGUIAndRenderMap();
    }//GEN-LAST:event_curvesButtonActionPerformed

    private void gradationGraphPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_gradationGraphPropertyChange
        if ("GradationGraph curve changed".equals(evt.getPropertyName())) {
            reloadMapTiles();
        }
    }//GEN-LAST:event_gradationGraphPropertyChange

    private void visibleCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_visibleCheckBoxActionPerformed
        boolean visible = visibleCheckBox.isSelected();
        Layer layer = getSelectedMapLayer();
        if (layer != null) {
            this.updating = true;
            try {
                layer.setVisible(visible);
                writeGUI(true);
                addUndoFromNamedComponent((JComponent) (evt.getSource()));
            } finally {
                this.updating = false;
            }
        }

    }//GEN-LAST:event_visibleCheckBoxActionPerformed

    private void idwRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_idwRadioButtonActionPerformed
        Layer layer = getSelectedMapLayer();
        if (layer != null) {
            final IDWGridTileRenderer idwRenderer = layer.getIdwTileRenderer();
            idwRenderer.setUseIDW(idwRadioButton.isSelected());
            idwPanel.repaint();
            idwPreview.repaint();
            reloadMapTiles();
        }
    }//GEN-LAST:event_idwRadioButtonActionPerformed

    /**
     * Updates the value of the texture scale slider
     *
     * @param textureScale
     */
    private void writeTextureScale(float textureScale) {
        assert SwingUtilities.isEventDispatchThread();
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
        assert SwingUtilities.isEventDispatchThread();
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
        assert SwingUtilities.isEventDispatchThread();
        int selectedID = layerList.getSelectedIndex();
        layerList.setListData(map.getLayers());
        layerList.setSelectedIndex(selectedID);
    }

    /**
     * Writes settings of the currently selected layer to the GUI.
     */
    private void writeGUI(boolean renderMap) {
        assert SwingUtilities.isEventDispatchThread();
        boolean initialUpdating = updating;
        this.updating = true;
        try {
            int selectedLayerID = layerList.getSelectedIndex();
            Layer selectedLayer = getSelectedMapLayer();

            // enable or disable user interface elements depending on whether
            // a layer is currently selected
            final boolean on = selectedLayer != null
                    && !selectedLayer.isLocked()
                    && selectedLayer.isVisible();
            this.visibleCheckBox.setEnabled(selectedLayer != null
                    && !selectedLayer.isLocked());
            this.urlTextField.setEnabled(on);
            this.loadDirectoryPathButton.setEnabled(on);
            this.tmsCheckBox.setEnabled(on);
            this.normalBlendingRadioButton.setEnabled(on);
            this.multiplyBlendingRadioButton.setEnabled(on);
            this.opacitySlider.setEnabled(on);
            this.curvesButton.setEnabled(on);
            this.resetCurveFileButton.setEnabled(on);
            this.colorSelectionComboBox.setEnabled(on);
            this.tintColorButton.setEnabled(on);
            this.grid1URLTextField.setEnabled(on);
            this.grid1TMSCheckBox.setEnabled(on);
            this.grid1LoadDirectoryPathButton.setEnabled(on);
            this.grid2URLTextField.setEnabled(on);
            this.grid2TMSCheckBox.setEnabled(on);
            this.grid2LoadDirectoryPathButton.setEnabled(on);
            this.idwExponentSlider.setEnabled(on);
            this.idwRadioButton.setEnabled(on);
            this.gaussRadioButton.setEnabled(on);
            this.idwPreview.setEnabled(on);
            this.textureSelectionButton.setEnabled(on);
            this.textureClearButton.setEnabled(on);
            this.textureURLLabel.setEnabled(on);
            this.textureScaleSlider.setEnabled(on);
            this.maskUrlTextField.setEnabled(on);
            this.maskTMSCheckBox.setEnabled(on);
            this.maskBlurSlider.setEnabled(on);
            this.maskLoadDirectoryPathButton.setEnabled(on);
            this.maskInvertCheckBox.setEnabled(on);
            this.maskValuesTextField.setEnabled(on);
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
                this.maskUrlTextField.setText(null);
                return;
            }

            if (initialUpdating) {
                return;
            }

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

            // curves
            Curve[] curves = selectedLayer.getCurves();
            gradationGraph.setCurves(curves);

            // tinting
            switch (selectedLayer.getColorType()) {
                case NONE:
                    colorSelectionComboBox.setSelectedIndex(0);
                    break;
                case SOLID:
                    colorSelectionComboBox.setSelectedIndex(1);
                    break;
                case INTERPOLATE:
                    colorSelectionComboBox.setSelectedIndex(2);
                    break;
            }

            tintColorButton.setColor(selectedLayer.getTint().getTintColor());
            grid1URLTextField.setText(selectedLayer.getGrid1TileSet().getUrlTemplate());
            grid1TMSCheckBox.setSelected(selectedLayer.getGrid1TileSet().isTMSSchema());
            grid2URLTextField.setText(selectedLayer.getGrid2TileSet().getUrlTemplate());
            grid2TMSCheckBox.setSelected(selectedLayer.getGrid2TileSet().isTMSSchema());
            int exp = (int) Math.round(selectedLayer.getIdwTileRenderer().getExponentP() * 10);
            idwExponentSlider.setValue(exp);
            idwRadioButton.setSelected(selectedLayer.getIdwTileRenderer().isUseIDW());
            idwPreview.setIdw(selectedLayer.getIdwTileRenderer());

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
            this.maskValuesTextField.setText(selectedLayer.getMaskValues());

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
            this.updating = initialUpdating;
            if (renderMap) {
                reloadMapTiles();
            }
        }
    }

    /**
     * Reads user settings from the GUI and passes the settings to the currently
     * selected layer.
     */
    protected void readGUIAndRenderMap() {
        assert SwingUtilities.isEventDispatchThread();

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

        // URL
        layer.setTileSetURLTemplate(urlTextField.getText());

        // TMS
        layer.setTileSetTMSSchema(tmsCheckBox.isSelected());

        // mask
        layer.setMaskTileSetURLTemplate(maskUrlTextField.getText());
        layer.setInvertMask(this.maskInvertCheckBox.isSelected());
        layer.setMaskTileSetTMSSchema(maskTMSCheckBox.isSelected());
        layer.setMaskBlur(this.maskBlurSlider.getValue() / 10f);
        layer.setMaskValues(maskValuesTextField.getText());

        // curves
        getSelectedMapLayer().setCurves(gradationGraph.getCurves());

        // tint
        switch (colorSelectionComboBox.getSelectedIndex()) {
            case 0:
                getSelectedMapLayer().setColorType(Layer.ColorType.NONE);
                break;
            case 1:
                getSelectedMapLayer().setColorType(Layer.ColorType.SOLID);
                layer.getTint().setTintColor(this.tintColorButton.getColor());
                break;
            case 2:
                getSelectedMapLayer().setColorType(Layer.ColorType.INTERPOLATE);
                String url1 = grid1URLTextField.getText();
                String url2 = grid2URLTextField.getText();
                layer.setIDWGridTileURLTemplates(url1, url2);
                layer.getGrid1TileSet().setTMSSchema(grid1TMSCheckBox.isSelected());
                layer.getGrid2TileSet().setTMSSchema(grid2TMSCheckBox.isSelected());
                layer.getIdwTileRenderer().setExponentP(idwExponentSlider.getValue() / 10d);
                layer.getIdwTileRenderer().setUseIDW(idwRadioButton.isSelected());
                break;
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
        reloadMapTiles();
    }

    public Map getMap() {
        return map;
    }

    public void setMap(Map map) {
        assert SwingUtilities.isEventDispatchThread();
        this.map = map;
        updateLayerList();
        layerList.setSelectedIndex(layerList.getFirstVisibleIndex());
        writeGUI(false);
    }

    void addLayer(boolean focusList) {
        assert SwingUtilities.isEventDispatchThread();
        try {
            updating = true;
            int layerID = this.layerList.getSelectedIndex() + 1;
            String name = "Layer " + (++layerCounter);
            map.addLayer(layerID, new Layer(name));
            updateLayerList();
            layerList.setSelectedIndex(layerID);
            if (focusList) {
                layerList.requestFocus();
            }
        } finally {
            updating = false;
        }
        writeGUI(true);
        addUndo("Add Layer");
    }

    void removeLayer() {
        assert SwingUtilities.isEventDispatchThread();

        try {
            updating = true;
            int selectedLayerID = layerList.getSelectedIndex();
            if (selectedLayerID < 0) {
                return;
            }
            map.removeLayer(selectedLayerID);
            updateLayerList();
            layerList.setSelectedIndex(--selectedLayerID);
        } finally {
            updating = false;
        }
        writeGUI(true);
        addUndo("Remove Layer");
    }

    void removeAllLayers() {
        assert SwingUtilities.isEventDispatchThread();

        try {
            updating = true;
            map.removeAllLayers();
            updateLayerList();
        } finally {
            updating = false;
        }
        writeGUI(true);
        addUndo("Remove All Layer");
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addLayerButton;
    private javax.swing.ButtonGroup blendingButtonGroup;
    private javax.swing.JPanel blurPanel;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.JLabel colorLabel;
    private javax.swing.JPanel colorMethodPanel;
    private javax.swing.JPanel colorPanel;
    private javax.swing.JTextArea colorPointsTextArea;
    private javax.swing.JComboBox colorSelectionComboBox;
    private javax.swing.JButton curvesButton;
    private javax.swing.JPanel curvesPanel;
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
    private javax.swing.JRadioButton gaussRadioButton;
    private edu.oregonstate.carto.mapcomposer.gui.GradationGraph gradationGraph;
    private javax.swing.JButton grid1LoadDirectoryPathButton;
    private javax.swing.JCheckBox grid1TMSCheckBox;
    private javax.swing.JTextField grid1URLTextField;
    private javax.swing.JButton grid2LoadDirectoryPathButton;
    private javax.swing.JCheckBox grid2TMSCheckBox;
    private javax.swing.JTextField grid2URLTextField;
    private javax.swing.JButton idwApplyButton;
    private javax.swing.JColorChooser idwColorChooser;
    private javax.swing.JPanel idwColorPanel;
    private javax.swing.JSlider idwExponentSlider;
    private javax.swing.JLabel idwExponentValueLabel;
    private javax.swing.JLabel idwHorizontalLabel;
    private edu.oregonstate.carto.mapcomposer.gui.IDWPanel idwPanel;
    private edu.oregonstate.carto.mapcomposer.gui.IDWPreview idwPreview;
    private javax.swing.JRadioButton idwRadioButton;
    private javax.swing.JButton idwTileSetsButton;
    private javax.swing.JPanel idwTileSetsPanel;
    private edu.oregonstate.carto.mapcomposer.gui.RotatedLabel idwVerticalLabel;
    private javax.swing.ButtonGroup interpolationMethodButtonGroup;
    private javax.swing.JFormattedTextField jFormattedTextField3;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private edu.oregonstate.carto.mapcomposer.gui.DraggableList layerList;
    private javax.swing.JScrollPane layerListScrollPane;
    private javax.swing.JToolBar layerListToolBar;
    private javax.swing.JPanel layersPanel;
    private javax.swing.JButton loadCurveFileButton;
    private javax.swing.JButton loadDirectoryPathButton;
    private javax.swing.JCheckBox lockedCheckBox;
    private javax.swing.JPanel mapPanel;
    private javax.swing.JSlider maskBlurSlider;
    private javax.swing.JCheckBox maskInvertCheckBox;
    private javax.swing.JButton maskLoadDirectoryPathButton;
    private javax.swing.JCheckBox maskTMSCheckBox;
    private javax.swing.JTextField maskUrlTextField;
    private javax.swing.JTextField maskValuesTextField;
    private javax.swing.JSpinner maxZoomSpinner;
    private javax.swing.JSpinner minZoomSpinner;
    private javax.swing.JButton moveDownLayerButton;
    private javax.swing.JButton moveUpLayerButton;
    private javax.swing.JRadioButton multiplyBlendingRadioButton;
    private javax.swing.JRadioButton normalBlendingRadioButton;
    private javax.swing.JFormattedTextField northField;
    private javax.swing.JSlider opacitySlider;
    private javax.swing.JLabel opacityValueLabel;
    private javax.swing.JButton removeLayerButton;
    private javax.swing.JButton resetCurveFileButton;
    private javax.swing.JCheckBox shadowCheckBox;
    private edu.oregonstate.carto.mapcomposer.gui.ColorButton shadowColorButton;
    private javax.swing.JSlider shadowFuziSlider;
    private javax.swing.JLabel shadowOffsetLabel;
    private javax.swing.JSlider shadowOffsetSlider;
    private javax.swing.JFormattedTextField southField;
    private javax.swing.JScrollPane textScrollPane;
    private javax.swing.JButton textureClearButton;
    private javax.swing.JLabel texturePreviewLabel;
    private javax.swing.JFormattedTextField textureScaleFormattedTextField;
    private javax.swing.JSlider textureScaleSlider;
    private javax.swing.JButton textureSelectionButton;
    private javax.swing.JLabel textureURLLabel;
    private javax.swing.JPanel tilesPanel;
    private edu.oregonstate.carto.mapcomposer.gui.ColorButton tintColorButton;
    private javax.swing.JCheckBox tmsCheckBox;
    private javax.swing.JTextField urlTextField;
    private javax.swing.JCheckBox visibleCheckBox;
    private javax.swing.JFormattedTextField westField;
    // End of variables declaration//GEN-END:variables

}
