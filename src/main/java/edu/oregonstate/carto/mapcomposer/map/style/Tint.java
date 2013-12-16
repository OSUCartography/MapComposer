/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.oregonstate.carto.mapcomposer.map.style;

import edu.oregonstate.carto.mapcomposer.utils.ColorJaxbAdaptor;
import java.awt.Color;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 *
 * @author administrator
 */
public class Tint {
    
    @XmlJavaTypeAdapter(ColorJaxbAdaptor.class)
    @XmlElement(name="tintColor")
    private Color tintColor = new Color(200, 250, 95);
    
    @XmlTransient
    public Color getTintColor() {
        return this.tintColor;
    }
    
    public void setTintColor(Color tintColor) {
        this.tintColor = tintColor;
    }
    
}
