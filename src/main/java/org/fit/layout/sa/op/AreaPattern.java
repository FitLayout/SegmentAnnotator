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
        return rootSignature.toString() + groupSignatures.toString();
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

    public boolean matchesRoot(Box box)
    {
        return rootSignature.matches(box);
    }
    
    public boolean matchesStart(Box box)
    {
        if (!groupSignatures.isEmpty())
        {
            BoxSignature sig = groupSignatures.get(0);
            return sig.matches(box);
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
    
}
