package edu.oregonstate.carto.mapcomposer.utils;

import com.jhlabs.image.PointFilter;
import java.awt.*;

/**
 * Takes the brightness from the image that is being filtered and combines it
 * with the hue and saturation of a definable tint.
 */
public class TintFilter extends PointFilter {
    
    private Color tint;
    private float tintHue;
    private float tintSaturation;
    
    @SuppressWarnings("OverridableMethodCallInConstructor")
    public TintFilter() {
        this.setTint(Color.GREEN);
    }
    
    @Override
    public int filterRGB(int x, int y, int rgb) {
        int a = rgb & 0xff000000;
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;
        
        float brightness = ((r * 77 + g * 151 + b * 28) >> 8) / 255f; // NTSC luma
        if (brightness < 0f)
            brightness = 0f;
        else if (brightness > 1.0)
            brightness = 1.0f;
        rgb = Color.HSBtoRGB(this.tintHue, this.tintSaturation, brightness);
        return a | (rgb & 0xffffff);
    }
    
    @Override
    public String toString() {
        return "Tint";
    }
    
    public void setTint(Color tint) {
        this.tint = tint;
        int rgb = tint.getRGB();
        int a = rgb & 0xff000000;
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;
        float[] hsb = new float[3];
        Color.RGBtoHSB(r, g, b, hsb);
        this.tintHue = hsb[0];
        this.tintSaturation = hsb[1];
    }
}

