/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.oregonstate.carto.tilemanager;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author nick
 */
public class SQLiteCacheTest {

    private SQLiteCache sqliteCache;

    public SQLiteCacheTest() {
        sqliteCache = SQLiteCache.getInstance();
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of insertImage method, of class SQLiteCache.
     */
//    @Test
//    public void testInsertImage() throws MalformedURLException, IOException {
//        System.out.println("insertImage");
//        URL url = new URL("http://a.tile.openstreetmap.org/12/785/1219.png");
//        BufferedImage img = ImageIO.read(url);
//        sqliteCache.insertImage(url, img);
//    }
//
//    /**
//     * Test of fetchImage method, of class SQLiteCache.
//     */
//    @Test
//    public void testFetchImage() throws IOException {
//        System.out.println("fetchImage");
//        URL url = new URL("http://a.tile.openstreetmap.org/12/785/1219.png");
//        BufferedImage img = sqliteCache.fetchImage(url);
//        File f = new File("C:/tmp/sqliteimg.png");
//        ImageIO.write(img, "png", f);
//    }
    /**
     * Test of put method, of class SQLiteCache.
     */
    @Test
    public void testPut() throws IOException {
        System.out.println("put");
        URL url;
        try {
            url = new URL("http://a.tile.openstreetmap.org/12/785/1219.png");

            TileSet tileSet = new HTTPTileSet("http://a.tile.openstreetmap.org/{z}/{x}/{y}.png");
            Tile t = tileSet.getTile(12, 785, 1219);
            sqliteCache.put(t);

            t = tileSet.getTile(12, 785, 1218);
            t.fetch();
            sqliteCache.put(t);

        } catch (MalformedURLException ex) {
            Logger.getLogger(SQLiteCacheTest.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * Test of get method, of class SQLiteCache.
     */
    @Test
    public void testGet() throws MalformedURLException, IOException {
        URL url = new URL("http://a.tile.openstreetmap.org/12/785/1219.png");

        TileSet tileSet = new HTTPTileSet("http://a.tile.openstreetmap.org/{z}/{x}/{y}.png");
        Tile tile = sqliteCache.get(url, tileSet);
        ImageTile imageTile = (ImageTile)tile;
        BufferedImage image = imageTile.fetch();
        ImageIO.write(image, "png", new File("test-output/testGet.png"));
    }
}