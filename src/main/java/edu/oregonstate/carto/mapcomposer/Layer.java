package edu.oregonstate.carto.mapcomposer;

import com.jhlabs.composite.MultiplyComposite;
import com.jhlabs.image.BicubicScaleFilter;
import com.jhlabs.image.BoxBlurFilter;
import com.jhlabs.image.GaussianFilter;
import com.jhlabs.image.ImageUtils;
import com.jhlabs.image.LightFilter;
import com.jhlabs.image.ShadowFilter;
import com.jhlabs.image.TileImageFilter;
import edu.oregonstate.carto.importer.AdobeCurveReader;
import edu.oregonstate.carto.mapcomposer.imageFilters.CurvesFilter;
import edu.oregonstate.carto.mapcomposer.utils.TintFilter;
import edu.oregonstate.carto.tilemanager.ImageTileMerger;
import edu.oregonstate.carto.tilemanager.Tile;
import edu.oregonstate.carto.tilemanager.TileSet;
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

    //
    @XmlTransient
    private static BufferedImage whiteMegaTile;

    private TileSet imageTileSet;
    
    private TileSet maskTileSet;

    private boolean visible = true;

    private String name;

    private String textureTileFilePath;

    private BlendType blending = BlendType.NORMAL;

    private float opacity = 1;

    private String curveURL;
    
    private CurvesFilter.Curve[] curves = null;

    private Tint tint = null;

    private float textureScale = 1f;

    private boolean invertMask = false;

    private float maskBlur = 0;

    private Shadow shadow = null;

    private Emboss emboss = null;
    
    //gaussian blur
    private float gaussBlur = 0;

    public Layer() {
    }

    public Layer(String layerName) {
        this.name = layerName;
    }

    /**
     * Render a tile of this layer.
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
            try {
                BufferedImage textureTile = ImageIO.read(new File(textureTileFilePath));
                textureTile = ImageUtils.convertImageToARGB(textureTile);

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
            } catch (IOException ex) {
                image = null;
                // FIXME
                System.err.println("could not load texture image");
            }
        }

        // load tile image
        if (imageTileSet != null) {
            Tile tile = imageTileSet.getTile(z, x, y);
            try {
                image = ImageTileMerger.createMegaTile(tile);
                // convert to ARGB. All following manipulations are optimized for 
                // this modus.
                image = ImageUtils.convertImageToARGB(image);
            } catch (IOException exc) {
                image = null;
            }
        }

        // tinting
        if (this.tint != null) {
            // use the pre-existing image for modulating brightness if the image
            // exists (i.e. a texture image has been created or an image has
            // been loaded).
            if (image != null) {
                TintFilter tintFilter = new TintFilter();
                tintFilter.setTint(tint.getTintColor());
                System.out.println(tint.getTintColor().toString());
                image = tintFilter.filter(image, null);
            } else {
                // no pre-existing image, create a solid color image
                image = solidColorImage(Tile.TILE_SIZE * 3, Tile.TILE_SIZE * 3, this.tint.getTintColor());
            }
        }

        // create solid white background image if no image has been loaded 
        if (image == null) {
            image = createWhiteMegaTile();
        }

        // gradation curve
        if (this.curves != null) {
            image = curve(image);
        }
        
        // masking
        if (maskTileSet != null) {
            BufferedImage maskImage;
            Tile tile = maskTileSet.getTile(z, x, y);
            try {
                maskImage = ImageTileMerger.createMegaTile(tile);
                // convert to ARGB. All following manipulations are optimized for 
                // this modus.
                maskImage = ImageUtils.convertImageToARGB(maskImage);
            } catch (IOException exc) {
                maskImage = createWhiteMegaTile();
            }
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
     * @param image
     * @return 
     */
    private BufferedImage curve(BufferedImage image) {
        // apply curve to image
        if (curves != null) {
            CurvesFilter curvesFilter = new CurvesFilter();
            curvesFilter.setCurves(curves);
            return curvesFilter.filter(image, null);
        } else {
            return null;
        }
    }

    /**
     * Use a grayscale image as alpha channel for another image.
     */
    private static BufferedImage alphaChannelFromGrayImage(BufferedImage image,
            BufferedImage mask, boolean invertMask) {

        image = ImageUtils.convertImageToARGB(image);

        // convert mask to grayscale image if necessary
        if (mask.getType() != BufferedImage.TYPE_BYTE_GRAY) {
            System.out.println("!!!! Alpha Mask not in Grayscale Modus !!!!");
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
        if (whiteMegaTile != null) {
            return whiteMegaTile;
        }
        whiteMegaTile = new BufferedImage(Tile.TILE_SIZE * 3, Tile.TILE_SIZE * 3, BufferedImage.TYPE_INT_ARGB);
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
     * @return the imageTileSet
     */
    public TileSet getImageTileSet() {
        return imageTileSet;
    }

    /**
     * @param imageTileSet the imageTileSet to set
     */
    public void setImageTileSet(TileSet imageTileSet) {
        this.imageTileSet = imageTileSet;
    }
    
    public void setImageTileSetURLTemplate(String urlTemplate) {
        if (imageTileSet == null) {
            imageTileSet = new TileSet(urlTemplate);
        } else {
            imageTileSet.setUrlTemplate(urlTemplate);
        }
    }

    public void setImageTileSetTMSSchema(boolean tmsSchema) {
        if (imageTileSet == null) {
            imageTileSet = new TileSet(null);
        }
        imageTileSet.setTMSSchema(tmsSchema);
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
    public void setTextureTileFilePath(String textureTileFilePath) {
        this.textureTileFilePath = textureTileFilePath;
    }

    /**
     * @return the maskTileSet
     */
    public TileSet getMaskTileSet() {
        return maskTileSet;
    }

    /**
     * @param maskTileSet the maskTileSet to set
     */
    public void setMaskTileSet(TileSet maskTileSet) {
        this.maskTileSet = maskTileSet;
    }

    public void setMaskTileSetURLTemplate(String maskTileSetURL) {
        if (maskTileSet == null) {
            maskTileSet = new TileSet(maskTileSetURL);
        } else {
            maskTileSet.setUrlTemplate(maskTileSetURL);
        }
    }

    public void setMaskTileSetTMSSchema(boolean tmsSchema) {
        if (maskTileSet == null) {
            maskTileSet = new TileSet(null);
        }
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
     * @return the curveURL
     */
    public String getCurveURL() {
        return curveURL;
    }

    /**
     * @param curveURL the curveURL to set
     */
    public void setCurveURL(String curveURL) {
        if (curveURL != null && curveURL.trim().isEmpty()) {
            curveURL = null;
        }
        this.curveURL = curveURL;
        
        // reset curves (curveURL can be null)
        curves = null;
        
        // load curve from URL
        try {
            if (curveURL != null && !curveURL.isEmpty()) {
                AdobeCurveReader acr = new AdobeCurveReader();
                acr.readACV(new URL(curveURL));
                curves = acr.getCurves();
                for (CurvesFilter.Curve c : curves) {
                    c.normalize();
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Layer.class.getName()).log(Level.SEVERE, null, ex);
            curves = null;
        }        
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
}
