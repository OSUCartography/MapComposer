package edu.oregonstate.carto.tilemanager;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Merges multiple tiles to a megatile.
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State
 * University
 */
public class ImageTileMerger {

    /**
     * Creates a Buffered image of 9 tiles with this tile being the center. This
     * is needed when a rendering engine needs data of the surrounding
     * neighborhood tiles.
     *
     * For the tiles around the center tile, rather than throwing an IO
     * exception, I fill them with white. This is to avoid the problem of having
     * the whole mega tile fail if any images around the center image are not
     * available.
     *
     * It throws IOException only when it cannot load the center image.
     *
     * @return A BufferedImage of the 9 tiles.
     */
    public static BufferedImage createMegaTile(Tile centerTile) throws IOException {
        int megaTileSize = Tile.TILE_SIZE * 3;

        BufferedImage megaTile = new BufferedImage(megaTileSize, megaTileSize, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = megaTile.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        try {
            BufferedImage topLeftTile = (BufferedImage) centerTile.getTopLeftTile().fetch();
            g2d.drawImage(topLeftTile, 0, 0, null);
        } catch (IOException ex) {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE);
        }

        try {
            BufferedImage topTile = (BufferedImage) centerTile.getTopTile().fetch();
            g2d.drawImage(topTile, Tile.TILE_SIZE, 0, null);
        } catch (IOException ex) {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(Tile.TILE_SIZE, 0, Tile.TILE_SIZE, Tile.TILE_SIZE);
        }

        try {
            BufferedImage topRightTile = (BufferedImage) centerTile.getTopRightTile().fetch();
            g2d.drawImage(topRightTile, Tile.TILE_SIZE * 2, 0, null);
        } catch (IOException ex) {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(Tile.TILE_SIZE * 2, 0, Tile.TILE_SIZE, Tile.TILE_SIZE);
        }

        try {
            BufferedImage leftTile = (BufferedImage) centerTile.getLeftTile().fetch();
            g2d.drawImage(leftTile, 0, Tile.TILE_SIZE, null);
        } catch (IOException ex) {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, Tile.TILE_SIZE, Tile.TILE_SIZE, Tile.TILE_SIZE);
        }

        // The tile in the center is this tile.
        g2d.drawImage((BufferedImage)centerTile.fetch(), Tile.TILE_SIZE, Tile.TILE_SIZE, null);

        try {
            BufferedImage rightTile = (BufferedImage) centerTile.getRightTile().fetch();
            g2d.drawImage(rightTile, Tile.TILE_SIZE * 2, Tile.TILE_SIZE, null);
        } catch (IOException ex) {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(Tile.TILE_SIZE * 2, Tile.TILE_SIZE, Tile.TILE_SIZE, Tile.TILE_SIZE);
        }

        try {
            BufferedImage bottomLeftTile = (BufferedImage) centerTile.getBottomLeftTile().fetch();
            g2d.drawImage(bottomLeftTile, 0, Tile.TILE_SIZE * 2, null);
        } catch (IOException ex) {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, Tile.TILE_SIZE * 2, Tile.TILE_SIZE, Tile.TILE_SIZE);
        }

        try {
            BufferedImage bottomTile = (BufferedImage) centerTile.getBottomTile().fetch();
            g2d.drawImage(bottomTile, Tile.TILE_SIZE, Tile.TILE_SIZE * 2, null);
        } catch (IOException ex) {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(Tile.TILE_SIZE, Tile.TILE_SIZE * 2, Tile.TILE_SIZE, Tile.TILE_SIZE);
        }

        try {
            BufferedImage bottomRightTile = (BufferedImage) centerTile.getBottomRightTile().fetch();
            g2d.drawImage(bottomRightTile, Tile.TILE_SIZE * 2, Tile.TILE_SIZE * 2, null);
        } catch (IOException ex) {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(Tile.TILE_SIZE * 2, Tile.TILE_SIZE * 2, Tile.TILE_SIZE, Tile.TILE_SIZE);
        }

        return megaTile;
    }
}
