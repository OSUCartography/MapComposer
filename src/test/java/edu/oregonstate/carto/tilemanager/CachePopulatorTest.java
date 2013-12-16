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
public class CachePopulatorTest {
    
    public CachePopulatorTest() {
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
     * Test of populate method, of class CachePopulator.
     */
//    @Test
//    public void blodgett() {
//        System.out.println("blodgett");
//        CachePopulator instance = new CachePopulator(
//                "http://services.arcgisonline.com/ArcGIS/rest/services/USA_Topo_Maps/MapServer/tile/{z}/{y}/{x}",
//                44.515359, -123.688545, 44.650582, -123.363762,
//                11, 13);
//        instance.populate();
//    }

    @Test
    public void oregon() {
        System.out.println("oregon");
        CachePopulator instance = new CachePopulator(
                "http://services.arcgisonline.com/ArcGIS/rest/services/USA_Topo_Maps/MapServer/tile/{z}/{y}/{x}",
                41.951320, -124.584961, 46.346928, -116.345215,
                9, 15);
        instance.populate(2);
    }
}