/**
 * BoxSignature.java
 *
 * Created on 26. 10. 2016, 16:35:05 by burgetr
 */
package org.fit.layout.sa.op;

import java.util.ArrayList;
import java.util.List;

import org.fit.layout.model.Box;
import org.fit.layout.model.Box.DisplayType;

/**
 * 
 * @author burgetr
 */
public class BoxSignature
{
    /** nearest ancestor ID or {@code null} when none */
    private String pId;
    /** nearest ancestor class spec or {@code null} when none */
    private String pClass;
    /** nearest block element name or {@code null} when none */
    private String pBlock;
    /** tag name */
    private String boxName;
    /** this box ID */
    private String boxId;
    /** this box class */
    private String boxClass;

    public BoxSignature(Box src)
    {
        computeSignature(src);
    }

    @Override
    public String toString()
    {
        String ret = "[#" + (pId == null ? "*" : pId);
        ret += "|." + (pClass == null ? "*" : pClass);
        ret += "|" + (pBlock == null ? "*" : pBlock);
        ret += "]/[";
        ret += boxName;
        if (boxId != null)
            ret += "#" + boxId;
        if (boxClass != null)
            ret += "." + boxClass;
        ret += "]";
        return ret;
    }
    
    //================================================================================================================
    
    private void computeSignature(Box src)
    {
        //general properties
        boxName = src.getTagName();
        boxId = src.getAttribute("id");
        boxClass = src.getAttribute("class");
        //get the ancestor statistics
        List<Box> anc = getAncestors(src);
        for (Box box : anc)
        {
            if (pId == null && box.getAttribute("id") != null)
                pId = box.getAttribute("id");
            if (pClass == null && box.getParentBox() != null && box.getAttribute("class") != null)
                pClass = box.getAttribute("class");
            if (pBlock == null && box.getDisplayType() == DisplayType.BLOCK)
                pBlock = box.getTagName();
        }
    }
    
    //================================================================================================================
    
    /**
     * Gets a list of ancestors of the given box excluding the box itself (ancestor)
     * @param box
     * @return
     */
    private List<Box> getAncestors(Box box)
    {
        List<Box> ret = new ArrayList<>();
        Box cur = box;
        //ret.add(cur);
        while (cur.getParentBox() != null)
        {
            cur = cur.getParentBox();
            ret.add(cur);
        }
        return ret;
    }

}
