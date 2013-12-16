/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.oregonstate.carto.mapcomposer.map.style;

import edu.oregonstate.carto.mapcomposer.utils.ColorJaxbAdaptor;
import java.awt.Color;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 *
 * @author administrator
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Shadow {
    
    @XmlElement(name = "shadowOffset")
    private int shadowOffset = 1;
    
    @XmlJavaTypeAdapter(ColorJaxbAdaptor.class)
    @XmlElement(name="shadowColor")
    private Color shadowColor = Color.BLACK;
    
    @XmlElement(name="shadowFuziness")
    private int shadowFuziness = 10;
    
    @XmlTransient
    public int getShadowOffset() {
        return shadowOffset;
    }
    
    public void setShadowOffset(int shadowOffset) {
        this.shadowOffset = shadowOffset;
    }
    
    @XmlTransient
    public Color getShadowColor() {
        return shadowColor;
    }

    public void setShadowColor(Color shadowColor) {
        this.shadowColor = shadowColor;
    }
    
    @XmlTransient
    public int getShadowFuziness() {
        return shadowFuziness;
    }

    public void setShadowFuziness(int shadowFuziness) {
        this.shadowFuziness = shadowFuziness;
    }
    
}
