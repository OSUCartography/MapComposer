package edu.oregonstate.carto.mapcomposer;

import com.jhlabs.composite.MultiplyComposite;
import com.jhlabs.image.BicubicScaleFilter;
import com.jhlabs.image.BoxBlurFilter;
import com.jhlabs.image.GaussianFilter;
import com.jhlabs.image.ImageUtils;
import com.jhlabs.image.LightFilter;
import com.jhlabs.image.ShadowFilter;
import com.jhlabs.image.TileImageFilter;
import edu.oregonstate.carto.grid.operators.GridBinarizeOperator;
import edu.oregonstate.carto.grid.operators.GridToImageOperator;
import edu.oregonstate.carto.importer.AdobeCurveReader;
import edu.oregonstate.carto.mapcomposer.tilerenderer.IDWGridTileRenderer;
import edu.oregonstate.carto.mapcomposer.tilerenderer.ImageTileRenderer;
import edu.oregonstate.carto.mapcomposer.tilerenderer.ShadingGridTileRenderer;
import edu.oregonstate.carto.mapcomposer.utils.TintFilter;
import edu.oregonstate.carto.tilemanager.DumbCache;
import edu.oregonstate.carto.tilemanager.GridTile;
import edu.oregonstate.carto.tilemanager.ImageTile;
import edu.oregonstate.carto.tilemanager.Tile;
import edu.oregonstate.carto.tilemanager.TileSet;
import edu.oregonstate.carto.tilemanager.util.Grid;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;

/**
 * A map layer.
 *
 * @author Nicholas Hallahan nick@theoutpost.io
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State
 * University
 */
//Every non static, non transient field in a JAXB-bound class will be 
//automatically bound to XML, unless annotated by @XmlTransient
@XmlAccessorType(XmlAccessType.FIELD)

public class Layer {

    public enum BlendType {

        NORMAL, MULTIPLY
    }

    public enum ColorType {

        NONE, SOLID, INTERPOLATE
    }

    private final TileSet tileSet;

    private final TileSet maskTileSet = new TileSet(null);

    private boolean visible = true;

    private boolean locked = false;

    private String name;

    private String textureTileFilePath;

    @XmlTransient
    private BufferedImage textureTile;

    private BlendType blending = BlendType.NORMAL;

    private float opacity = 1;

    private Curve[] curves = new Curve[]{new Curve()};

    private Tint tint = new Tint();

    private float textureScale = 1f;

    private boolean invertMask = false;

    private float maskBlur = 0;

    private String maskValues = "0";

    private Shadow shadow = null;

    private Emboss emboss = null;

    //gaussian blur
    private float gaussBlur = 0;

    private final IDWGridTileRenderer idwTileRenderer = new IDWGridTileRenderer();
    private final TileSet grid1TileSet = new TileSet(null, new DumbCache(), true);
    private final TileSet grid2TileSet = new TileSet(null, new DumbCache(), true);

    private ColorType colorType = ColorType.NONE;

    public Layer() {
        tileSet = new TileSet(null);
    }

    public Layer(String layerName) {
        this.name = layerName;
        tileSet = new TileSet(null);
    }

    public Layer(String layerName, String urlTemplate) {
        this.name = layerName;
        tileSet = new TileSet(urlTemplate);
    }

    /**
     * Render a tile of this layer.
     *
     * @param g2d Graphics2D destination: render into this canvas
     * @param z Zoom level of tile
     * @param x Horizontal x coordinate of tile.
     * @param y Vertical y coordinate of tile.
     */
    public void renderToTile(Graphics2D g2d, int z, int x, int y) {

        if (isBlendingNormal()) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, getOpacity()));
        } else {
            g2d.setComposite(new MultiplyComposite(getOpacity()));
        }

        BufferedImage image = null;
        if (textureTileFilePath != null) {
            //try {
            // FIXME a hack to load texture tiles after unmarshaling from XML
            // with transient textureTile.
            //if (textureTile == null) {
            //    loadTextureTile();
            //}

            // scale texture patch if needed
            if (textureScale != 1f) {
                int textureW = (int) (textureTile.getWidth() * this.textureScale);
                int textureH = (int) (textureTile.getHeight() * this.textureScale);
                BicubicScaleFilter scaleFilter = new BicubicScaleFilter(textureW, textureH);
                textureTile = scaleFilter.filter(textureTile, null);
            }

            TileImageFilter tiler = new TileImageFilter();
            tiler.setHeight(Tile.TILE_SIZE * 3);
            tiler.setWidth(Tile.TILE_SIZE * 3);
            BufferedImage dst = new BufferedImage(Tile.TILE_SIZE * 3, Tile.TILE_SIZE * 3, BufferedImage.TYPE_INT_ARGB);
            image = tiler.filter(textureTile, dst);
            /*} catch (IOException ex) {
             image = null;
             // FIXME
             System.err.println("could not load texture image");
             }*/
        }

        // load tile image
        if (isTileSetValid()) {
            Tile tile = tileSet.getTile(z, x, y);
            if (tile instanceof ImageTile) {
                image = new ImageTileRenderer().render(tile);
            } else {
                image = new ShadingGridTileRenderer().render(tile);
            }
            // convert to ARGB. All following manipulations are optimized for 
            // this modus.
            // image = ImageUtils.convertImageToARGB(image);
        }

        // tinting
        switch (colorType) {
            case NONE:
                break;

            case SOLID:
                // use the pre-existing image for modulating brightness if the image
                // exists (i.e. a texture image has been created or an image has
                // been loaded).
                if (image != null) {
                    TintFilter tintFilter = new TintFilter();
                    tintFilter.setTint(tint.getTintColor());
                    image = tintFilter.filter(image, null);
                } else {
                    // no pre-existing image, create a solid color image
                    image = solidColorImage(Tile.TILE_SIZE * 3, Tile.TILE_SIZE * 3, this.tint.getTintColor());
                }
                break;

            case INTERPOLATE:
                if (idwTileRenderer != null
                        && grid1TileSet.isURLTemplateValid()
                        && grid2TileSet.isURLTemplateValid()) {
                    Tile gridTile1 = grid1TileSet.getTile(z, x, y);
                    Tile gridTile2 = grid2TileSet.getTile(z, x, y);
                    image = idwTileRenderer.render(gridTile1, gridTile2);
                }

                break;
        }

        // create solid white background image if no image has been loaded 
        if (image == null) {
            image = createWhiteMegaTile();
        }

        // gradation curve
        image = curve(image);

        // masking
        if (isMaskTileSetValid()) {
            BufferedImage maskImage = null;
            Tile maskTile = maskTileSet.getTile(z, x, y);
            if (maskTile instanceof GridTile && maskValues != null && !maskValues.isEmpty()) {
                try {
                    Grid mergedGrid = ((GridTile) maskTile).createMegaTile();
                    Grid maskGrid = new GridBinarizeOperator(maskValues).operate(mergedGrid);
                    maskImage = new GridToImageOperator().operate(maskGrid, 0, 1);
                } catch (IOException ex) {
                }
            } else {
                maskImage = new ImageTileRenderer().render(maskTile);
            }

            if (maskImage != null) {
                if (this.maskBlur > 0) {
                    BoxBlurFilter blurFilter = new BoxBlurFilter();
                    blurFilter.setHRadius(this.maskBlur);
                    blurFilter.setVRadius(this.maskBlur);
                    blurFilter.setPremultiplyAlpha(false);
                    blurFilter.setIterations(1);
                    maskImage = blurFilter.filter(maskImage, null);
                }

                image = alphaChannelFromGrayImage(image, maskImage, this.invertMask);
            }
        }

        // embossing
        if (this.emboss != null) {
            // this solution works fine, but is slow
            LightFilter lightFilter = new LightFilter();
            lightFilter.setBumpSource(LightFilter.BUMPS_FROM_IMAGE_ALPHA);
            lightFilter.setBumpHeight(this.emboss.getEmbossHeight());
            lightFilter.setBumpSoftness(this.emboss.getEmbossSoftness());
            LightFilter.Light forestLight = (LightFilter.Light) (lightFilter.getLights().get(0));
            forestLight.setAzimuth((float) Math.toRadians(this.emboss.getEmbossAzimuth() - 90));
            forestLight.setElevation((float) Math.toRadians(this.emboss.getEmbossElevation()));
            forestLight.setDistance(0);
            forestLight.setIntensity(1f);
            //lightFilter.getMaterial().highlight = 10f;
            lightFilter.getMaterial().highlight = 10f;
            image = lightFilter.filter(image, null);
        }

        // drop shadow: draw it onto the destination image
        if (this.shadow != null) {
            BufferedImage shadowImage = ImageUtils.cloneImage(image);
            //x negative : left  -  x positive : right
            //y negative : down  -  y positive : up
            //TODO : distinguish x and y offset OR use a mouving offset !!
            ShadowFilter shadowFilter = new ShadowFilter(this.shadow.getShadowFuziness(), this.shadow.getShadowOffset(), -this.shadow.getShadowOffset(), 1f);
            shadowFilter.setShadowColor(this.shadow.getShadowColor().getRGB());
            shadowImage = shadowFilter.filter(shadowImage, null);
            shadowImage = shadowImage.getSubimage(Tile.TILE_SIZE, Tile.TILE_SIZE, Tile.TILE_SIZE, Tile.TILE_SIZE);
            g2d.drawImage(shadowImage, null, null);
        }

        // Gaussian Blur
        if (this.gaussBlur > 0) {
            GaussianFilter gaussFilter = new GaussianFilter();
            gaussFilter.setRadius(this.gaussBlur);
            image = gaussFilter.filter(image, image);
        }

        // draw this layer into the destination image
        BufferedImage tileImage = image.getSubimage(Tile.TILE_SIZE, Tile.TILE_SIZE, Tile.TILE_SIZE, Tile.TILE_SIZE);
        g2d.drawImage(tileImage, null, null);
    }

    /**
     * Applies curve to image.
     *
     * @param image
     * @return
     */
    private BufferedImage curve(BufferedImage image) {
        // TODO don't apply linear curve
        // apply curve to image
        CurvesFilter curvesFilter = new CurvesFilter();
        curvesFilter.setCurves(curves);
        return curvesFilter.filter(image, null);
    }

    /**
     * Use a grayscale image as alpha channel for another image.
     */
    private static BufferedImage alphaChannelFromGrayImage(BufferedImage image,
            BufferedImage mask, boolean invertMask) {

        image = ImageUtils.convertImageToARGB(image);

        // convert mask to grayscale image if necessary
        if (mask.getType() != BufferedImage.TYPE_BYTE_GRAY) {
            //System.out.println("!!!! Alpha Mask not in Grayscale Modus !!!!");
            BufferedImage tmpMask = new BufferedImage(mask.getWidth(),
                    mask.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
            ColorConvertOp toGray = new ColorConvertOp(null);
            mask = toGray.filter(mask, tmpMask);
        }

        // for TYPE_INT_ARGB with a TYPE_BYTE_GRAY mask
        if (mask.getType() == BufferedImage.TYPE_BYTE_GRAY) {
            byte ad[] = ((DataBufferByte) mask.getRaster().getDataBuffer()).getData();
            int d[] = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
            int size = image.getWidth() * image.getHeight();
            if (!invertMask) {
                for (int i = 0; i < size; i++) {
                    d[i] = (d[i] & 0xFFFFFF) | ((((int) ad[i]) << 24) ^ 0xff000000);
                }
            } else {
                for (int i = 0; i < size; i++) {
                    d[i] = (d[i] & 0xFFFFFF) | (((int) ad[i]) << 24);
                }
            }
        }
        return image;
    }

    private static BufferedImage solidColorImage(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(color);
        g2d.fillRect(0, 0, width, height);
        g2d.dispose();
        return image;
    }

    private static BufferedImage createWhiteMegaTile() {
        BufferedImage whiteMegaTile = new BufferedImage(Tile.TILE_SIZE * 3, Tile.TILE_SIZE * 3, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = whiteMegaTile.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, Tile.TILE_SIZE * 3, Tile.TILE_SIZE * 3);
        g.dispose();
        return whiteMegaTile;
    }

    /**
     * @return the visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * @param visible the visible to set
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * @return the locked
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     * @param locked the locked to set
     */
    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the layerName to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the tileSet
     */
    public TileSet getTileSet() {
        return tileSet;
    }

    public void setTileSetURLTemplate(String urlTemplate) {
        tileSet.setUrlTemplate(urlTemplate);
    }

    public void setTileSetTMSSchema(boolean tmsSchema) {
        tileSet.setTMSSchema(tmsSchema);
    }

    public boolean isTileSetValid() {
        return tileSet.isURLTemplateValid();
    }

    /**
     * @return the textureTileFilePath
     */
    public String getTextureTileFilePath() {
        return textureTileFilePath;
    }

    /**
     * @param textureTileFilePath File path for a single tile that is used to
     * texture the layer.
     */
    public void setTextureTileFilePath(String textureTileFilePath) throws IOException {
        this.textureTileFilePath = textureTileFilePath;
        if (textureTileFilePath == null) {
            textureTile = null;
        } else {
            loadTextureTile();
        }
    }

    public boolean isTextureTileFilePathValid() {
        return textureTileFilePath != null && new File(textureTileFilePath).isFile();
        // FIXME should test for valid image file here
    }

    protected void loadTextureTile() throws IOException {
        if (isTextureTileFilePathValid()) {
            textureTile = ImageIO.read(new File(textureTileFilePath));
            textureTile = ImageUtils.convertImageToARGB(textureTile);
        }
    }

    /**
     * @return the maskTileSet
     */
    public TileSet getMaskTileSet() {
        return maskTileSet;
    }

    public void setMaskTileSetURLTemplate(String maskTileSetURL) {
        maskTileSet.setUrlTemplate(maskTileSetURL);
    }

    public void setMaskTileSetTMSSchema(boolean tmsSchema) {
        maskTileSet.setTMSSchema(tmsSchema);
    }

    /**
     *
     * @return true if the blend type is normal
     */
    public boolean isBlendingNormal() {
        return blending == BlendType.NORMAL;
    }

    /**
     * Setting the blending type.
     *
     * @param blending type
     */
    public void setBlending(BlendType blending) {
        this.blending = blending;
    }

    /**
     * @return the opacity
     */
    public float getOpacity() {
        return opacity;
    }

    /**
     * @param opacity the opacity to set
     */
    public void setOpacity(float opacity) {
        this.opacity = opacity;
    }

    /**
     * @param curveURL the curveURL to set
     */
    public void loadCurve(String curveURL) {
        if (curveURL == null || curveURL.trim().isEmpty()) {
            return;
        }

        // load curve from URL
        try {
            AdobeCurveReader acr = new AdobeCurveReader();
            acr.readACV(new URL(curveURL));
            curves = acr.getCurves();
            for (Curve c : curves) {
                c.normalize();
            }
        } catch (IOException ex) {
            Logger.getLogger(Layer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Curve[] getCurves() {
        return curves;
    }

    public void setCurve(Curve curve) {
        curves = new Curve[]{curve};
    }

    public void setCurves(Curve[] curves) {
        this.curves = curves;
    }

    /**
     * @return the textureScale
     */
    public float getTextureScale() {
        return textureScale;
    }

    /**
     * @param textureScale the textureScale to set
     */
    public void setTextureScale(float textureScale) {
        this.textureScale = textureScale;
    }

    /**
     * @return the invertMask
     */
    public boolean isInvertMask() {
        return invertMask;
    }

    /**
     * @param invertMask the invertMask to set
     */
    public void setInvertMask(boolean invertMask) {
        this.invertMask = invertMask;
    }

    /**
     * @return the maskBlur
     */
    public float getMaskBlur() {
        return maskBlur;
    }

    /**
     * @param maskBlur the maskBlur to set
     */
    public void setMaskBlur(float maskBlur) {
        this.maskBlur = maskBlur;
    }

    public boolean isMaskTileSetValid() {
        return maskTileSet.isURLTemplateValid();
    }

    /**
     * @return the blending
     */
    public BlendType getBlending() {
        return blending;
    }

    /**
     * @return the tint
     */
    public Tint getTint() {
        return tint;
    }

    /**
     * @param tint the tint to set
     */
    public void setTint(Tint tint) {
        this.tint = tint;
    }

    /**
     * @return the shadow
     */
    public Shadow getShadow() {
        return shadow;
    }

    /**
     * @param shadow the shadow to set
     */
    public void setShadow(Shadow shadow) {
        this.shadow = shadow;
    }

    /**
     * @return the emboss
     */
    public Emboss getEmboss() {
        return emboss;
    }

    /**
     * @param emboss the emboss to set
     */
    public void setEmboss(Emboss emboss) {
        this.emboss = emboss;
    }

    //When I refactored/encapsulated "gaussBlur", it put these under the Layer
    //method, so I moved them down here with the others:
    /**
     * @return the gaussBlur
     */
    public float getGaussBlur() {
        return gaussBlur;
    }

    /**
     * @param gaussBlur the gaussBlur to set
     */
    public void setGaussBlur(float gaussBlur) {
        this.gaussBlur = gaussBlur;
    }

    @Override
    public String toString() {
        return getName();
    }

    public void setIDWGridTileURLTemplates(String urlTemplate1, String urlTemplate2) {
        grid1TileSet.setUrlTemplate(urlTemplate1);
        grid2TileSet.setUrlTemplate(urlTemplate2);
    }

    /**
     * @return the maskValues
     */
    public String getMaskValues() {
        return maskValues;
    }

    /**
     * @param maskValues the maskValues to set
     */
    public void setMaskValues(String maskValues) {
        if (maskValues != null) {
            maskValues = maskValues.trim();
        }
        this.maskValues = maskValues;
    }

    /**
     * @return the colorType
     */
    public ColorType getColorType() {
        return colorType;
    }

    /**
     * @param colorType the colorType to set
     */
    public void setColorType(ColorType colorType) {
        this.colorType = colorType;
    }

    /**
     * @return the idwTileRenderer
     */
    public IDWGridTileRenderer getIdwTileRenderer() {
        return idwTileRenderer;
    }

    /**
     * @return the grid1TileSet
     */
    public TileSet getGrid1TileSet() {
        return grid1TileSet;
    }

    /**
     * @return the grid2TileSet
     */
    public TileSet getGrid2TileSet() {
        return grid2TileSet;
    }

}
