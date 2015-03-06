package edu.oregonstate.carto.utils;

import java.awt.Color;

public class ColorUtils {

    public static int getBrightness(Color c) {
        final int r = c.getRed();
        final int g = c.getGreen();
        final int b = c.getBlue();
        return (int) Math.sqrt(
                r * r * .241
                + g * g * .691
                + b * b * .068);
    }
    
    public static int difference(Color c1, Color c2) {
        int dR = c1.getRed() - c2.getRed();
        int dG = c1.getGreen() - c2.getGreen();
        int dB = c1.getBlue() - c2.getBlue();
        return (int)Math.sqrt(dR * dR + dG * dG + dB * dB);
    }
}
