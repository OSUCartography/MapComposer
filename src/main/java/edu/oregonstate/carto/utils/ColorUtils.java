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
}
