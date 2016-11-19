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
    /** box occurence within its parent. >0 means from beginning, <0 means from end */
    private int boxIndex;
    private int pClassIndex;
    private int pBlockIndex;
    

    public BoxSignature(Box src)
    {
        computeSignature(src);
    }

    @Override
    public String toString()
    {
        String ret = "<#" + (pId == null ? "*" : pId);
        ret += "|." + (pClass == null ? "*" : pClass) + ":" + pClassIndex;
        ret += "|" + (pBlock == null ? "*" : pBlock) + ":" + pBlockIndex;
        ret += ">/<";
        ret += boxName;
        if (boxId != null)
            ret += "#" + boxId;
        if (boxClass != null)
            ret += "." + boxClass;
        ret += ":" + boxIndex;
        ret += ">";
        return ret;
    }
    
    public boolean matches(Box other)
    {
        return matches(new BoxSignature(other));
    }
    
    public boolean matches(BoxSignature sig)
    {
        return boxMatches(sig.boxName, sig.boxId, sig.boxClass)
                //&& sig.boxIndex == boxIndex
                && parentsMatch(sig.pBlock, sig.pId, sig.pClass)
                && (sig.pBlock == null || sig.pBlockIndex == pBlockIndex)
                && (sig.pClass == null || sig.pClassIndex == pClassIndex);
    }
    
    public boolean equalsExactly(BoxSignature other)
    {
        return 
                equalsNoIndex(other)
                && boxIndex == other.boxIndex;
    }
    
    
    public boolean equalsNoIndex(BoxSignature other)
    {
        if (boxClass == null)
        {
            if (other.boxClass != null) return false;
        }
        else if (!boxClass.equals(other.boxClass)) return false;
        if (boxId == null)
        {
            if (other.boxId != null) return false;
        }
        else if (!boxId.equals(other.boxId)) return false;
        //if (boxIndex != other.boxIndex) return false;
        if (boxName == null)
        {
            if (other.boxName != null) return false;
        }
        else if (!boxName.equals(other.boxName)) return false;
        if (pBlock == null)
        {
            if (other.pBlock != null) return false;
        }
        else if (!pBlock.equals(other.pBlock)) return false;
        if (pBlockIndex != other.pBlockIndex) return false;
        if (pClass == null)
        {
            if (other.pClass != null) return false;
        }
        else if (!pClass.equals(other.pClass)) return false;
        if (pClassIndex != other.pClassIndex) return false;
        if (pId == null)
        {
            if (other.pId != null) return false;
        }
        else if (!pId.equals(other.pId)) return false;
        return true;
    }

    //================================================================================================================
    
    private void computeSignature(Box src)
    {
        //general properties
        boxName = src.getTagName();
        if (boxName == null)
            boxName = "";
        boxId = src.getAttribute("id");
        boxClass = src.getAttribute("class");
        if (src.getParentBox() != null)
            boxIndex = computeBoxIndex(src.getParentBox(), src);
        //get the ancestor statistics
        List<Box> anc = getAncestors(src);
        for (Box box : anc)
        {
            if (pId == null && box.getAttribute("id") != null)
                pId = box.getAttribute("id");
            if (pClass == null && box.getParentBox() != null && box.getAttribute("class") != null)
            {
                pClass = box.getAttribute("class");
                if (box.getParentBox() != null)
                    pClassIndex = computeClassIndex(box.getParentBox(), box, pClass);
            }
            if (pBlock == null && box.getDisplayType() == DisplayType.BLOCK)
            {
                pBlock = box.getTagName();
                if (box.getParentBox() != null)
                    pBlockIndex = computeNameIndex(box.getParentBox(), box, pBlock);
            }
        }
    }
    
    private boolean boxMatches(String name, String id, String cls)
    {
        return boxName.equals(name)
                && ((boxId == null && id == null) || (boxId != null && id != null && boxId.equals(id)))
                && ((boxClass == null && cls == null) || (boxClass != null && cls != null && boxClass.equals(cls)));
    }
    
    private boolean parentsMatch(String block, String id, String cls)
    {
        return ((pBlock == null && block == null) || (pBlock != null && block != null && pBlock.equals(block)))
                && ((pId == null && id == null) || (pId != null && id != null && pId.equals(id)))
                && ((pClass == null && cls == null) || (pClass != null && cls != null && pClass.equals(cls)));
    }

    private int computeBoxIndex(Box parent, Box search)
    {
        int cnt = 0;
        for (int i = 0; i < parent.getChildCount(); i++)
        {
            Box src = parent.getChildBox(i);
            String name = src.getTagName();
            String id = src.getAttribute("id");
            String cls = src.getAttribute("class");
            if (boxMatches(name, id, cls))
                cnt++;
            if (search != null && src == search)
                break;
        }
        return cnt;
    }
    
    private int computeClassIndex(Box parent, Box search, String clsname)
    {
        int cnt = 0;
        for (int i = 0; i < parent.getChildCount(); i++)
        {
            Box src = parent.getChildBox(i);
            String cls = src.getAttribute("class");
            if (cls != null && cls.equals(clsname))
                cnt++;
            if (search != null && src == search)
                break;
        }
        return cnt;
    }
    
    private int computeNameIndex(Box parent, Box search, String name)
    {
        int cnt = 0;
        for (int i = 0; i < parent.getChildCount(); i++)
        {
            Box src = parent.getChildBox(i);
            String tagname = src.getTagName();
            if (tagname != null && tagname.equals(name))
                cnt++;
            if (search != null && src == search)
                break;
        }
        return cnt;
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

    //================================================================================================================
    
    
}
