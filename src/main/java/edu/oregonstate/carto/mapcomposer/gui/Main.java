package edu.oregonstate.carto.mapcomposer.gui;

import javax.swing.UIManager;

/**
 * Main method for Map Composer.
 *
 * @author bernie
 */
public class Main {

    /**
     * 
     * @param args 
     */
    public static void main(String args[]) {

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
                frame.pack();
                frame.setVisible(true);
            }
        });
    }
}
