/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.oregonstate.carto.mapcomposer.map.style;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

/**
 *
 * @author Charlotte Hoarau, COGIT Laboratory, IGN France
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Emboss {
    @XmlElement(name = "embossHeight")
    private float embossHeight = 0.8f;
    
    @XmlElement(name = "embossSoftness")
    private float embossSoftness = 10f;
    
    @XmlElement(name = "embossAzimuth")
    private float embossAzimuth = 315;
    
    @XmlElement(name = "embossElevation")
    private float embossElevation = 45;
    
    public float getEmbossHeight() {
        return embossHeight;
    }

    public void setEmbossHeight(float embossHeight) {
        this.embossHeight = embossHeight;
    }

    public float getEmbossSoftness() {
        return embossSoftness;
    }

    public void setEmbossSoftness(float embossSoftness) {
        this.embossSoftness = embossSoftness;
    }

    public float getEmbossAzimuth() {
        return embossAzimuth;
    }

    public void setEmbossAzimuth(float embossAzimuth) {
        this.embossAzimuth = embossAzimuth;
    }

    public float getEmbossElevation() {
        return embossElevation;
    }

    public void setEmbossElevation(float embossElevation) {
        this.embossElevation = embossElevation;
    }
    
}
