package edu.oregonstate.carto.mapcomposer.gui;

import edu.oregonstate.carto.mapprotocol.MapURLStreamHandlerFactory;
import java.net.URL;
import javax.swing.JFrame;
import javax.swing.UIManager;

/**
 * Main method for Map Composer.
 *
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State University
 */
public class Main {

    /**
     *
     * @param args
     */
    public static void main(String args[]) {

        // install map protocol for rendering map tiles on the fly.
        // URL.setURLStreamHandlerFactory can only be called once
        URL.setURLStreamHandlerFactory(new MapURLStreamHandlerFactory());

        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Map Composer");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
        }

        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                MapComposerFrame frame = new MapComposerFrame();
                frame.setVisible(true);
                frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
            }
        });
    }
}
