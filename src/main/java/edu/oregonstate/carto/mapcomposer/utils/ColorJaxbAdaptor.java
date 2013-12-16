package edu.oregonstate.carto.mapcomposer.utils;

import java.awt.Color;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 *
 * @author administrator
 */
public class ColorJaxbAdaptor  extends XmlAdapter<String, Color> {
    
    @Override
    public Color unmarshal(String s) {

        //The tint color 
        if (s.length() == 9) {
            String sRGB = "#" + s.substring(2, s.length());
            
            return Color.decode(sRGB);
        } else {
            return Color.decode(s);
        }
    
    }
    
    @Override
    public String marshal(Color c) {
        if (c.getAlpha() == 0) {
            return "#" + Integer.toHexString(c.getRGB());
        } else {
            return "#" + Integer.toHexString(c.getRGB()).substring(2);
        }
    }
    
    public static void main(String[] args) {
        ColorJaxbAdaptor colorAdaptor = new ColorJaxbAdaptor();
        
        Color c = new Color(255, 102, 153);
        System.out.println("c : " + c);
        System.out.println(colorAdaptor.marshal(c));
        System.out.println(colorAdaptor.unmarshal(colorAdaptor.marshal(c)));
        System.out.println("");
        
        Color c2 = new Color(255, 102, 153, 0);
        System.out.println("c2 : " + c2);
        System.out.println(colorAdaptor.marshal(c2));
        System.out.println(colorAdaptor.unmarshal(colorAdaptor.marshal(c2)));
        System.out.println("");
        
        String s = "#ff6699";
        System.out.println("s : " + s);
        System.out.println(colorAdaptor.unmarshal(s));
        System.out.println(colorAdaptor.unmarshal(s).getAlpha());
        System.out.println(colorAdaptor.marshal(colorAdaptor.unmarshal(s)));
        System.out.println("");
        
        String s2 = "#99ff6699";
        System.out.println("s2 : " + s2);
        System.out.println(colorAdaptor.unmarshal(s2));
        System.out.println(colorAdaptor.unmarshal(s).getAlpha());
        System.out.println(colorAdaptor.marshal(colorAdaptor.unmarshal(s2)));
        System.out.println("");
    }
    
    
}
