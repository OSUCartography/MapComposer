/*
 * GridOperator.java
 *
 * Created on January 28, 2006, 2:10 PM
 *
 */

package edu.oregonstate.carto.grid.operators;

import edu.oregonstate.carto.tilemanager.util.Grid;

/**
 * A GridOperator derives a new grid from an existing grid.
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public interface GridOperator {
    
    /** Returns a descriptive name of this GridOperator
     * @return The name of this GridOperator.
     */
    public String getName();
    
    /**
     * Start operating on the passed GeoGrid.
     * @param grid The Grid to operate on.
     * @return A new Grid containing the result. The resulting GeoGrid may
     * be of a different size than the passed GeoGrid.
     */
    public Grid operate (Grid grid);
    
}
