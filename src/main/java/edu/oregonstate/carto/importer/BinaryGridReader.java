package edu.oregonstate.carto.importer;

import edu.oregonstate.carto.tilemanager.util.Grid;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class BinaryGridReader {

    private static final int TILE_SIZE = 256;
    private static final int BYTES_TO_READ = TILE_SIZE * TILE_SIZE * 4;

    
    private BinaryGridReader() {
    }

    /**
     * Read a Grid from a file in ESRI ASCII format.
     *
     * @param filePath The path to the file to be read.
     * @return The read grid.
     */
    public static Grid read(String filePath)
            throws java.io.IOException {
        File file = new File(filePath);
        InputStream fis = new FileInputStream(file.getAbsolutePath());
        BufferedInputStream bis = new BufferedInputStream(fis, BYTES_TO_READ);
        return BinaryGridReader.read(bis);
    }
    
    /**
     * Read a Grid from a URL that is in ESRI ASCII format.
     * 
     * @param url
     * @return The read grid.
     * @throws IOException 
     */
    public static Grid read(URL url) throws IOException {
        InputStream is = url.openStream();
        BufferedInputStream bis = new BufferedInputStream(is, BYTES_TO_READ);
        return read(bis);
    }

    /**
     * Read a grid from a binary stream.
     *
     * @param is The stream to read from. The stream is closed at the end.
     * @return The read grid.
     */
    public static Grid read(InputStream in)
            throws IOException {

        byte[] buf = new byte[BYTES_TO_READ];

        try {
            int totalBytesRead = 0, bytesRead = 0;
            while (totalBytesRead < buf.length && bytesRead >= 0) {
                int bytesRemaining = buf.length - totalBytesRead;
                //input.read() returns -1, 0, or more :
                bytesRead = in.read(buf, totalBytesRead, bytesRemaining);
                if (bytesRead > 0) {
                    totalBytesRead += bytesRead;
                }
            }
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception exc) {
            }
        }
        
        ByteBuffer bb = ByteBuffer.wrap(buf);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        FloatBuffer fb = bb.asFloatBuffer();
        
        float[][] grid = new float[TILE_SIZE][TILE_SIZE];
        for (int r = 0; r < TILE_SIZE; r++) {
            final float[] row = grid[r];
            fb.get(row);
        }

        return new Grid(grid, 1);
    }
    
}
