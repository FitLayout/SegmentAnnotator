/**
 * GroupByExampleOperator.java
 *
 * Created on 24. 10. 2016, 21:32:40 by burgetr
 */
package org.fit.layout.sa.op;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.fit.layout.impl.BaseOperator;
import org.fit.layout.model.Area;
import org.fit.layout.model.AreaTree;
import org.fit.layout.model.Box;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author burgetr
 */
public class GroupByExampleOperator extends BaseOperator
{
    private static Logger log = LoggerFactory.getLogger(GroupByExampleOperator.class);

    protected final String[] paramNames = { };
    protected final ValueType[] paramTypes = { };
    
    private AreaTree exampleTree;
    private List<AreaPattern> patterns;
    
    
    public GroupByExampleOperator()
    {
        patterns = new ArrayList<>();
    }
    
    @Override
    public String getId()
    {
        return "FitLayout.Segm.GroupByExample";
    }
    
    @Override
    public String getName()
    {
        return "Group by example";
    }

    @Override
    public String getDescription()
    {
        return "..."; //TODO
    }

    @Override
    public String[] getParamNames()
    {
        return paramNames;
    }

    @Override
    public ValueType[] getParamTypes()
    {
        return paramTypes;
    }
    
    //==============================================================================
    
    public AreaTree getExampleTree()
    {
        return exampleTree;
    }

    public void setExampleTree(AreaTree exampleTree)
    {
        this.exampleTree = exampleTree;
        patterns.clear();
        recursiveAnalyzeAreas(exampleTree.getRoot());
        log.info("{} patterns collected", patterns.size());
    }
    
    //==============================================================================

    @Override
    public void apply(AreaTree atree)
    {
        apply(atree, atree.getRoot());
    }

    @Override
    public void apply(AreaTree atree, Area root)
    {
        recursiveScanAreaTree(root);
    }
    
    //==============================================================================
    
    private void recursiveScanAreaTree(Area root)
    {
        AreaPattern pat = findMatch(root);
        if (pat != null)
            System.out.println("MATCH start: " + root + " matches " + pat);
        
        for (Area child : root.getChildAreas())
            recursiveScanAreaTree(child);
    }
    
    private AreaPattern findMatch(Area area)
    {
        if (!area.getBoxes().isEmpty())
        {
            Box box = area.getBoxes().firstElement();
            if (box.getParentBox() != null)
            {
                Box parent = box.getParentBox();
                for (AreaPattern pat : patterns)
                {
                    if (pat.matchesRoot(parent))
                    {
                        if (pat.matchesStart(box))
                            return pat;
                    }
                }
                return null; //no pattern matched
            }
            else
                return null;
        }
        else
            return null; //no boxes in this area
    }
    
    //==============================================================================
    
    private void recursiveAnalyzeAreas(Area root)
    {
        if (root.getParentArea() != null) //do not analyze the root area
            analyzeArea(root);
        for (Area child : root.getChildAreas())
            recursiveAnalyzeAreas(child);
    }
    
    private void analyzeArea(Area area)
    {
        System.out.println("Area: " + area);
        
        List<Box> boxes = area.getAllBoxes();
        Box cparent = getCommonAncestor(boxes);
        List<Box> groups = getGroupCommonAncestors(boxes, cparent);
        
        if (groups.isEmpty() && cparent.getParentBox() != null)
        {
            //if there are no groups, we probably take the whole subtree. The parent is then one level up.
            groups.add(cparent);
            cparent = cparent.getParentBox();
        }
        
        BoxSignature psig = new BoxSignature(cparent);
        AreaPattern pat = new AreaPattern(psig);
        System.out.println("  Parent: " + cparent + " : " + psig);
        
        System.out.println("  Groups: " + groups);
        for (Box box : groups)
        {
            BoxSignature sig = new BoxSignature(box);
            pat.addGroupSignature(sig);
            System.out.println("    " + sig);
        }
        patterns.add(pat);
    }
    
    /**
     * Obtains the neares common ancestor for all the boxes in the list.
     * @param boxes
     * @return
     */
    private Box getCommonAncestor(List<Box> boxes)
    {
        Set<Box> candidates = null;
        for (Box box : boxes)
        {
            List<Box> anc = getAncestors(box);
            if (candidates == null)
                candidates = new HashSet<>(anc);
            else
                candidates.retainAll(anc);
        }
        //find the bottom-most one
        int maxd = -1;
        Box ret = null;
        for (Box box : candidates)
        {
            int d = getDepth(box); 
            if (d > maxd)
            {
                maxd = d;
                ret = box;
            }
        }
        return ret;
    }
    
    /**
     * Obtains all the ancestor of the given boxes that are the child nodes of the given parent.
     * @param boxes
     * @param parent
     * @return
     */
    private List<Box> getGroupCommonAncestors(List<Box> boxes, Box parent)
    {
        List<Box> ret = new ArrayList<>();
        for (Box box : boxes)
        {
            Box anc = getAncestorWithParent(box, parent);
            if (anc != null)
                ret.add(anc);
            /*else
                log.error("Couldn't find ancestor for {} with parent {}", box, parent);*/
        }
        return ret;
    }
    
    /**
     * Gets a list of ancestors of the given box including the box itself (ancestor-or-self)
     * @param box
     * @return
     */
    private List<Box> getAncestors(Box box)
    {
        List<Box> ret = new ArrayList<>();
        Box cur = box;
        ret.add(cur);
        while (cur.getParentBox() != null)
        {
            cur = cur.getParentBox();
            ret.add(cur);
        }
        return ret;
    }
    
    /**
     * Gets the ancestor of the given box that is a child node of the given parent.
     * @param box
     * @param parent
     * @return The ancestor found or {@code null} when no such node may be found.
     */
    private Box getAncestorWithParent(Box box, Box parent)
    {
        Box cur = box;
        do
        {
            if (cur.getParentBox() == parent)
                return cur;
            cur = cur.getParentBox();
        } while (cur != null);
        return null; //nothing found
    }
    
    /**
     * Computes the distance of the given box from the box tree root.
     * @param box the box
     * @return the distance from root (returns 0 for the root node)
     */
    private int getDepth(Box box)
    {
        int ret = 0;
        Box cur = box;
        while (cur.getParentBox() != null)
        {
            cur = cur.getParentBox();
            ret++;
        }
        return ret;
    }
    
}
