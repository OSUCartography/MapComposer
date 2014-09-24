package edu.oregonstate.carto.mapcomposer.tilerenderer;

import edu.oregonstate.carto.tilemanager.Tile;
import edu.oregonstate.carto.tilemanager.TileRenderer;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 *
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State
 * University
 */
public class ImageTileRenderer implements TileRenderer{
    @Override
    public BufferedImage render(Tile tile) {
        Color BACKGROUND_COLOR = Color.YELLOW;
        int megaTileSize = Tile.TILE_SIZE * 3;

        BufferedImage megaTile = new BufferedImage(megaTileSize, megaTileSize, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = megaTile.createGraphics();
        g2d.setColor(BACKGROUND_COLOR);
        g2d.fillRect(0, 0, megaTile.getWidth(), megaTile.getHeight());
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        try {
            BufferedImage topLeftTile = (BufferedImage) tile.getTopLeftTile().fetch();
            g2d.drawImage(topLeftTile, 0, 0, null);
        } catch (IOException ex) {
        }

        try {
            BufferedImage topTile = (BufferedImage) tile.getTopTile().fetch();
            g2d.drawImage(topTile, Tile.TILE_SIZE, 0, null);
        } catch (IOException ex) {
        }

        try {
            BufferedImage topRightTile = (BufferedImage) tile.getTopRightTile().fetch();
            g2d.drawImage(topRightTile, Tile.TILE_SIZE * 2, 0, null);
        } catch (IOException ex) {
        }

        try {
            BufferedImage leftTile = (BufferedImage) tile.getLeftTile().fetch();
            g2d.drawImage(leftTile, 0, Tile.TILE_SIZE, null);
        } catch (IOException ex) {
        }

        try {
            // tile at the center
            g2d.drawImage((BufferedImage) tile.fetch(), Tile.TILE_SIZE, Tile.TILE_SIZE, null);
        } catch (IOException ex) {
        }

        try {
            BufferedImage rightTile = (BufferedImage) tile.getRightTile().fetch();
            g2d.drawImage(rightTile, Tile.TILE_SIZE * 2, Tile.TILE_SIZE, null);
        } catch (IOException ex) {
        }

        try {
            BufferedImage bottomLeftTile = (BufferedImage) tile.getBottomLeftTile().fetch();
            g2d.drawImage(bottomLeftTile, 0, Tile.TILE_SIZE * 2, null);
        } catch (IOException ex) {
        }

        try {
            BufferedImage bottomTile = (BufferedImage) tile.getBottomTile().fetch();
            g2d.drawImage(bottomTile, Tile.TILE_SIZE, Tile.TILE_SIZE * 2, null);
        } catch (IOException ex) {
        }

        try {
            BufferedImage bottomRightTile = (BufferedImage) tile.getBottomRightTile().fetch();
            g2d.drawImage(bottomRightTile, Tile.TILE_SIZE * 2, Tile.TILE_SIZE * 2, null);
        } catch (IOException ex) {
        }
        return megaTile;
    }

}
