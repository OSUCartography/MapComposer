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
    @Test
    public void testInsertImage() throws MalformedURLException, IOException {
        System.out.println("insertImage");
        URL url = new URL("http://a.tile.openstreetmap.org/12/785/1219.png");
        BufferedImage img = ImageIO.read(url);
        sqliteCache.insertImage(url, img);
    }

    /**
     * Test of fetchImage method, of class SQLiteCache.
     */
    @Test
    public void testFetchImage() throws IOException {
        System.out.println("fetchImage");
        URL url = new URL("http://a.tile.openstreetmap.org/12/785/1219.png");
        BufferedImage img = sqliteCache.fetchImage(url);
        File f = new File("C:/tmp/sqliteimg.png");
        ImageIO.write(img, "png", f);
    }
}