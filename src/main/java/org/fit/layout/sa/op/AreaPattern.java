/**
 * AreaPattern.java
 *
 * Created on 27. 10. 2016, 13:32:27 by burgetr
 */
package org.fit.layout.sa.op;

import java.util.ArrayList;
import java.util.List;

import org.fit.layout.model.Box;

/**
 * 
 * @author burgetr
 */
public class AreaPattern
{
    private BoxSignature rootSignature;
    private List<BoxSignature> groupSignatures;
    private int fontSize;
    
    
    public AreaPattern(BoxSignature rootSignature)
    {
        this.rootSignature = rootSignature;
        this.groupSignatures = new ArrayList<>();
    }
    
    public AreaPattern(BoxSignature rootSignature, List<BoxSignature> groupSignatures)
    {
        this.rootSignature = rootSignature;
        this.groupSignatures = groupSignatures;
    }

    public String toString()
    {
        return "{" + fontSize + "}" + rootSignature.toString() + groupSignatures.toString();
    }
    
    public int getFontSize()
    {
        return fontSize;
    }

    public void setFontSize(int fontSize)
    {
        this.fontSize = fontSize;
    }

    public BoxSignature getRootSignature()
    {
        return rootSignature;
    }

    public void setRootSignature(BoxSignature rootSignature)
    {
        this.rootSignature = rootSignature;
    }

    public List<BoxSignature> getGroupSignatures()
    {
        return groupSignatures;
    }

    public void setGroupSignatures(List<BoxSignature> groupSignatures)
    {
        this.groupSignatures = groupSignatures;
    }
    
    public void addGroupSignature(BoxSignature sig)
    {
        groupSignatures.add(sig);
    }
    
    public int getGroupCount()
    {
        return groupSignatures.size();
    }

    public boolean matchesRoot(Box box)
    {
        return rootSignature.matches(box);
    }
    
    public boolean matchesStart(Box box)
    {
        if (fontSize ==  getStartingFontSize(box))
        {
            if (!groupSignatures.isEmpty())
            {
                BoxSignature sig = groupSignatures.get(0);
                return sig.matches(box);
            }
            else
                return false;
        }
        else
            return false;
    }
    
    public boolean matchesEnd(Box box)
    {
        if (!groupSignatures.isEmpty())
        {
            BoxSignature sig = groupSignatures.get(groupSignatures.size() - 1);
            return sig.matches(box);
        }
        else
            return false;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof AreaPattern)
        {
            AreaPattern other = (AreaPattern) obj;
            if (fontSize != other.fontSize)
                return false;
            if (!rootSignature.equalsExactly(other.rootSignature))
                return false;
            if (groupSignatures.size() != other.groupSignatures.size())
                return false;
            for (int i = 0; i < groupSignatures.size(); i++)
            {
                BoxSignature s1 = groupSignatures.get(i);
                BoxSignature s2 = other.groupSignatures.get(i);
                if (!s1.equalsNoIndex(s2))
                    return false;
            }
            return true;
        }
        else
            return false;
    }
    
    public static Box getFirstTextLeaf(Box root)
    {
        if (root.getChildCount() == 0)
        {
            if (root.getText().isEmpty())
                return null;
            else
                return root;
        }
        else
        {
            for (int i = 0; i < root.getChildCount(); i++)
            {
                Box ret = getFirstTextLeaf(root.getChildAt(i));
                if (ret != null)
                    return ret;
            }
            return null;
        }
    }
    
    public static int getStartingFontSize(Box root)
    {
        Box leaf = getFirstTextLeaf(root);
        return (leaf == null) ? 0 : Math.round(leaf.getFontSize());
    }

    
}
