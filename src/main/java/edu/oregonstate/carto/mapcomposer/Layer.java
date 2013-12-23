package edu.oregonstate.carto.mapcomposer;

import com.jhlabs.composite.MultiplyComposite;
import com.jhlabs.image.BicubicScaleFilter;
import com.jhlabs.image.BoxBlurFilter;
import com.jhlabs.image.ImageUtils;
import com.jhlabs.image.LightFilter;
import com.jhlabs.image.ShadowFilter;
import com.jhlabs.image.TileImageFilter;
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
import javax.imageio.ImageIO;
import javax.xml.bind.annotation.XmlElement;

/**
 * A map layer.
 * @author Nicholas Hallahan nick@theoutpost.io
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State
 * University
 */
public class Layer {

    public enum BlendType {
        NORMAL, MULTIPLY
    }
    
    private static BufferedImage whiteMegaTile;
    
    private TileSet imageTileSet;
    private TileSet maskTileSet;
    
    @XmlElement(name = "visible")
    private boolean visible = true;
    
    @XmlElement(name = "name")
    private String name;
    
    @XmlElement(name = "textureURL")
    private String textureURL;
    
    @XmlElement(name = "blending")
    private BlendType blending = BlendType.NORMAL;
    
    @XmlElement(name = "opacity")
    private float opacity = 1;
    
    @XmlElement(name = "curveURL")
    private String curveURL;
    
    @XmlElement(name = "tint")
    private Tint tint = null;
    
    @XmlElement(name = "textureScale")
    private float textureScale = 1f;
    
    @XmlElement(name = "invertMask")
    private boolean invertMask = false;
    
    @XmlElement(name = "maskBlur")
    private float maskBlur = 0;
    
    @XmlElement(name = "shadow")
    private Shadow shadow = null;
    
    @XmlElement(name = "emboss")
    private Emboss emboss = null;

    public Layer() {
    }
    
    public Layer(String layerName) {
        this.name = layerName;
    }
    
    public void renderToTile(Graphics2D g2d, int z, int x, int y) {

        if (isBlendingNormal()) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, getOpacity()));
        } else {
            g2d.setComposite(new MultiplyComposite(getOpacity()));
        }

        BufferedImage textureImage = null;
        if (textureURL != null) {
            try {
                textureImage = ImageIO.read(new File(textureURL));
                textureImage = ImageUtils.convertImageToARGB(textureImage);

                // scale texture patch if needed
                if (textureScale != 1f) {
                    int textureW = (int) (textureImage.getWidth() * this.textureScale);
                    int textureH = (int) (textureImage.getHeight() * this.textureScale);
                    BicubicScaleFilter scaleFilter = new BicubicScaleFilter(textureW, textureH);
                    textureImage = scaleFilter.filter(textureImage, null);
                }

                TileImageFilter tiler = new TileImageFilter();
                tiler.setHeight(Tile.TILE_SIZE * 3);
                tiler.setWidth(Tile.TILE_SIZE * 3);
                BufferedImage dst = new BufferedImage(Tile.TILE_SIZE * 3, Tile.TILE_SIZE * 3, BufferedImage.TYPE_INT_ARGB);
                textureImage = tiler.filter(textureImage, dst);
            } catch (IOException ex) {
                textureImage = null;
                // FIXME
                System.err.println("could not load texture image");
            }
        }


        // load tile image
        BufferedImage image = null;
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

        // create solid white background image if no image has been loaded 
        if (image == null) {
            image = createWhiteMegaTile();
        }
        
        // gradation curve
        if (this.curveURL != null && this.curveURL.length() > 0) {
            // FIXME
            //image = curve(image, this.curveURL);
        }


        // tinting
        if (this.tint != null) {
            // use the pre-existing image for modulating brightness if the image
            // exists (i.e. a texture image has been created or an image has
            // been loaded).
            if (textureImage != null || image != null) {
                TintFilter tintFilter = new TintFilter();
                tintFilter.setTint(this.tint.getTintColor());
                image = tintFilter.filter(image, null);
            } else {
                // no pre-existing image, create a solid color image
                image = solidColorImage(Tile.TILE_SIZE * 3, Tile.TILE_SIZE * 3, this.tint.getTintColor());
            }
        }

        // masking
        BufferedImage maskImage = null;
        if (maskTileSet != null) {
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

        // draw this layer into the destination image
        BufferedImage tileImage = image.getSubimage(Tile.TILE_SIZE, Tile.TILE_SIZE, Tile.TILE_SIZE, Tile.TILE_SIZE);
        g2d.drawImage(tileImage, null, null);
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
        if (whiteMegaTile != null) return whiteMegaTile;
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

    /**
     * @return the textureURL
     */
    public String getTextureURL() {
        return textureURL;
    }

    /**
     * @param textureURL the textureURL to set
     */
    public void setTextureURL(String textureURL) {
        this.textureURL = textureURL;
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
        this.curveURL = curveURL;
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
    
    @Override
    public String toString() {
        return getName();
    }
}
