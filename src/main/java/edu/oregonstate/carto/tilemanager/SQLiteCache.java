package edu.oregonstate.carto.tilemanager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Nicholas Hallahan nick@theoutpost.io
 */
public class SQLiteCache implements Cache {

    private static String DRIVER_NAME = "org.sqlite.JDBC";
    private static String DB_URL = "jdbc:sqlite:data/cache.db";
    private static SQLiteCache singleton = new SQLiteCache();
    private Connection con;
    private PreparedStatement insertStmt;
    private PreparedStatement fetchStmt;

    private SQLiteCache() {
        connect();
    }

    private void connect() {
        try {
            Driver driver = (Driver) Class.forName(DRIVER_NAME).newInstance();
            DriverManager.registerDriver(driver);
            con = DriverManager.getConnection(DB_URL);
            insertStmt = con.prepareStatement("INSERT OR REPLACE INTO cache VALUES(?, ?)");
            fetchStmt = con.prepareStatement("SELECT * FROM cache WHERE cache.url=?");
        } catch (Exception ex) {
            // FIXME
            System.out.println("Could not connect to SQLite: " + DB_URL);
        }
    }

    public static SQLiteCache getInstance() {
        return singleton;
    }

    @Override
    public void put(Tile tile) {
        URL url = tile.getURL();
        ByteArrayOutputStream outStream = null;
        try {
            outStream = new ByteArrayOutputStream();
            DataOutputStream dataOutStream = new DataOutputStream(outStream);
            tile.toBinary(dataOutStream);
            byte[] buf = outStream.toByteArray();
            try {
                insertStmt.setBytes(1, buf);
                insertStmt.setString(2, url.toString());
                insertStmt.executeUpdate();
            } catch (SQLException ex) {
                Logger.getLogger(SQLiteCache.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (IOException ex) {
            // FIXME
            Logger.getLogger(SQLiteCache.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if(outStream != null) {
                try {
                    outStream.close();
                } catch (IOException ex) {
                }
            }
        }        
    }

    @Override
    public Tile get(URL url, TileSet tileSet) {
        String urlStr = url.toString();
        try {
            fetchStmt.setString(1, urlStr);
            ResultSet rs = fetchStmt.executeQuery();
            byte[] bytes = rs.getBytes(1);
            ByteArrayInputStream inStream = new ByteArrayInputStream(bytes);
            DataInputStream dataInputStream = new DataInputStream(inStream);
            Tile tile = new ImageTile(tileSet, dataInputStream);
            return tile;
        } catch (SQLException ex) {
            return null; // We should return null if the tile is not in the cache.
        } catch (IOException ex) {
            Logger.getLogger(SQLiteCache.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
