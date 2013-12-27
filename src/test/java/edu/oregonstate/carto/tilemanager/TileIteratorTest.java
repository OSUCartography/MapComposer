/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.oregonstate.carto.tilemanager;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Nicholas Hallahan nick@theoutpost.io
 */
public class TileIteratorTest {

    TileSet topoSet;
    TileIterator corvallis1;

    public TileIteratorTest() {
        topoSet = new TileSet("http://services.arcgisonline.com/ArcGIS/rest/services/USA_Topo_Maps/MapServer/tile/{z}/{y}/{x}");
        corvallis1 = new TileIterator(topoSet,
                44.524538, -123.386676, 44.612849, -123.190727,
                13, 13);
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
     * Test of next method, of class TileIterator.
     */
    @Test
    public void testNext() {
        System.out.println("next");
        Tile t = corvallis1.next();
        System.out.println(t.toString());
        int z = t.getZ();
        assertEquals(13, z);
        assertEquals(true, corvallis1.hasNext());
    }

    @Test
    public void testNext2() {
        System.out.println("iterate through corvallis nexts");
        TileIterator corvallis2 = new TileIterator(topoSet,
                44.524538, -123.386676, 44.612849, -123.190727,
                13,14);
        while (corvallis2.hasNext()) {
            Tile t = corvallis2.next();
            System.out.println(t.toString());
        }
    }
    
    @Test
    public void testNext3() {
        System.out.println("iterate through corvallis nexts more efficiently");
        TileIterator corvallis3 = new TileIterator(topoSet,
                44.524538, -123.386676, 44.612849, -123.190727,
                13,14);
        Tile t = corvallis3.next();
        while (t != null) {
            System.out.println(t.toString());
            t = corvallis3.next();
        }
    }
    @Test
    public void iterateOregon() {
        System.out.println("Iterating through huge Oregon tile set...");
        TileIterator oregon = new TileIterator(topoSet,
                43.145086,-128.853149,47.088826,-118.399658,
                4, 9);
        Tile t = oregon.next();
        while (t != null) {
            System.out.println(t.toString());
            assert t.getZ() >= 0;
            assert t.getX() >= 0;
            assert t.getY() >= 0;
            t = oregon.next();
        }
    }
//    @Test
    public void iterateOregon2() {
        System.out.println("Iterating through huge Oregon tile set inefficiently...");
        TileIterator oregon = new TileIterator(topoSet,
                43.145086,-128.853149,47.088826,-118.399658,
                7, 16);
        Tile t;
        while (oregon.hasNext()) {
            t = oregon.next();
            System.out.println(t.toString());
        }
    }
    
    @Test
    public void testIterateWorld() {
        System.out.println("Iterating the world...");
        TileIterator oregon = new TileIterator(topoSet,
                -85, -180, 85, 180,
                0, 1);
        while (oregon.hasNext()) {
            Tile t = oregon.next();
            System.out.println(t.toString());
            assert t.getZ() >= 0;
            assert t.getX() >= 0;
            assert t.getY() >= 0;
        }
    }
    
    @Test
    public void testIterateWorld0() {
        System.out.println("World zoom 0 only...");
        TileIterator oregon = new TileIterator(topoSet,
                -85, -179.9999999, 85, 179.9999999,
                0, 0);
        while (oregon.hasNext()) {
            Tile t = oregon.next();
            System.out.println(t.toString());
            assert t.getZ() == 0;
            assert t.getX() == 0;
            assert t.getY() == 0;
        }
    }
    
    @Test
    public void testIterateWorld2() {
        System.out.println("Iterating the world 0 - 10 zoom...");
        TileIterator world = new TileIterator(topoSet,
                -85, -180, 85, 180,
                0, 9);
        while (world.hasNext()) {
            Tile t = world.next();
            System.out.println(t.toString());
            assert t.getZ() >= 0;
            assert t.getX() >= 0;
            assert t.getY() >= 0;
        }
    }
    
    @Test
    public void testTileCountForZoomLevel0() {
        System.out.println("Testing the count of tiles forzoom level 0.");
        int zoom = 0;
        TileIterator world = new TileIterator(topoSet,
                -85, -180, 85, 180,
                zoom, zoom);
        int count = 0;
        while (world.hasNext()) {
            ++count;
            world.next();
        }
        System.out.println("Count = " + count);
        System.out.println(Math.pow(2,zoom));
        assert count == Math.pow(2, zoom) * Math.pow(2, zoom);
    }
    
    @Test
    public void testTileCountForZoomLevel5() {
        System.out.println("Testing the count of tiles forzoom level 5.");
        int zoom = 5;
        TileIterator world = new TileIterator(topoSet,
                -85, -180, 85, 180,
                zoom, zoom);
        int count = 0;
        while (world.hasNext()) {
            ++count;
            world.next();
        }
        System.out.println("Count = " + count);
        System.out.println(Math.pow(2,zoom));
        assert count == Math.pow(2, zoom) * Math.pow(2, zoom);
    }
    
    // NH FIXME Why does this test fail? Something about doubles maybe?
//    @Test
    public void testTileCountForEachZoomLevel() {
        System.out.println("Testing the count of tiles for each zoom level.");
        for (int zoom=0; zoom <= 19; ++zoom) {
            TileIterator world = new TileIterator(topoSet,
                    -85, -180, 85, 180,
                    zoom, zoom);
            int count = 0;
            while (world.hasNext()) {
                ++count;
                world.next();
            }
            System.out.println("Zoom = " + zoom);
            System.out.println("Count = " + count);
            double pow = Math.pow(2, zoom) * Math.pow(2, zoom);
            System.out.println("POW = " + pow);
            assert count == pow;
        }
    }
}
