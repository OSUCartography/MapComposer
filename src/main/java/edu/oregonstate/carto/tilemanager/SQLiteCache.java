package edu.oregonstate.carto.tilemanager;

import edu.oregonstate.carto.tilemanager.util.Grid;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author Nicholas Hallahan nick@theoutpost.io
 */
public class SQLiteCache {

    private static String DRIVER_NAME = "org.sqlite.JDBC";
    private static String DB_URL = "jdbc:sqlite:data/cache.db";
    private static SQLiteCache singleton = new SQLiteCache();
    
    private Connection con;
    PreparedStatement insertStmt, fetchStmt;
    

    private SQLiteCache() {
        connect();
    }

    private void connect() {
        try {
            Driver driver = (Driver) Class.forName(DRIVER_NAME).newInstance();
            DriverManager.registerDriver(driver);
            con = DriverManager.getConnection(DB_URL);
            insertStmt = con.prepareStatement(
                    "INSERT INTO cache VALUES(?, ?)");
            fetchStmt = con.prepareStatement(
                    "SELECT * FROM cache WHERE cache.url=?");
        } catch (Exception ex) {
            System.out.println("Could not connect to SQLite: " + DB_URL);
        }
    }

    public static SQLiteCache getInstance() {
        return singleton;
    }

    public void insertImage(URL url, BufferedImage img) {
        String urlStr = url.toString();
        byte[] bytes = bufferedImageToBytes(img);
        try {
            insertStmt.setBytes(1, bytes);
            insertStmt.setString(2, urlStr);
            insertStmt.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(SQLiteCache.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

//    public void insertGrid(URL url, Grid grid) {
//        
//    }
    
    public BufferedImage fetchImage(URL url) throws IOException {
        String urlStr = url.toString();
        try {
            PreparedStatement fetchStmt = con.prepareStatement(
                    "SELECT * FROM cache WHERE cache.url=?");
            fetchStmt.setString(1, urlStr);
            ResultSet rs = fetchStmt.executeQuery();
            byte[] bytes = rs.getBytes(1);
            InputStream is = new ByteArrayInputStream(bytes);
            BufferedImage bi = ImageIO.read(is);
            return bi;
        } catch (SQLException ex) {
            System.out.println("HTTP Request: " + url.toString());
            BufferedImage img = ImageIO.read(url);
            insertImage(url, img);
            return img;
        }

    }

//    public Grid fetchGrid(URL url) {
//        
//        return null;
//    }
    
    private static byte[] bufferedImageToBytes(BufferedImage bi) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, "png", baos);
            baos.flush();
            byte[] bytes = baos.toByteArray();
            baos.close();
            return bytes;
        } catch (IOException ex) {
            System.out.println("Unable to convert buffered image to byte array");
            return null;
        }
    }
    
    private static InputStream bufferedImageToInputStream(BufferedImage bi) {
        byte[] bytes = bufferedImageToBytes(bi);
        return new ByteArrayInputStream(bytes);
    }
}
