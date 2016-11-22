/**
 * PatternMatch.java
 *
 * Created on 29. 10. 2016, 17:50:21 by burgetr
 */
package org.fit.layout.sa.op;

import java.util.ArrayList;
import java.util.List;

import org.fit.layout.model.Box;

/**
 * 
 * @author burgetr
 */
public class PatternMatch
{
    private AreaPattern pattern;
    private Box box;
    private List<PatternMatch> pairs;
    
    public PatternMatch(AreaPattern pattern, Box box)
    {
        this.pattern = pattern;
        this.box = box;
        this.pairs = new ArrayList<>();
    }
    
    public AreaPattern getPattern()
    {
        return pattern;
    }
    
    public void setPattern(AreaPattern pattern)
    {
        this.pattern = pattern;
    }
    
    public Box getBox()
    {
        return box;
    }
    
    public void setBox(Box box)
    {
        this.box = box;
    }

    public List<PatternMatch> getPairs()
    {
        return pairs;
    }

    public PatternMatch getLastPair()
    {
        if (pairs.isEmpty())
            return null;
        else
            return pairs.get(pairs.size() - 1);
    }
    
    @Override
    public String toString()
    {
        return "{" + box.toString() + " matches " + pattern.toString() + "}";
    }
    
}
