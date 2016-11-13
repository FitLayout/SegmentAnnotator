/**
 * GroupByExampleOperator.java
 *
 * Created on 24. 10. 2016, 21:32:40 by burgetr
 */
package org.fit.layout.sa.op;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.fit.layout.impl.BaseOperator;
import org.fit.layout.model.Area;
import org.fit.layout.model.AreaTree;
import org.fit.layout.model.Box;
import org.fit.layout.model.Rectangular;
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
    
    /** List of area groups that should form the new areas */
    private List<List<Area>> groupsFound;
    
    
    public GroupByExampleOperator()
    {
        patterns = new ArrayList<>();
        groupsFound = new ArrayList<>();
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
        groupsFound.clear();
        List<Area> leaves = new ArrayList<>();
        findLeafAreas(root, leaves);
        scanForMatches(leaves);
        System.out.println("Found " + groupsFound.size() + " matches");
        for (List<Area> group : groupsFound)
            createSuperArea(group);
    }
    
    //==============================================================================
    
    /**
     * Scans the area tree and fills {@code leafBoxes} with all the boxes that correspond
     * to the leaf areas of the area tree.
     * @param root
     */
    private void scanForMatches(List<Area> leaves)
    {
        int mode = 0;
        PatternMatch match = null;
        PatternMatch endmatch = null;
        List<Area> newgroup = null;
        Area area = null; 
        Iterator<Area> it = leaves.iterator();
        while (it.hasNext())
        {
            if (area == null)
                area = (Area) it.next();
            for (Box box : area.getBoxes())
            {
                System.out.println("(" + mode + ") " + box);
                switch (mode)
                {
                    case 0: //scan for start node
                        match = recursiveScanBoxTree(box, true);
                        if (match != null)
                        {
                            System.out.println("MATCH start: " + box + " matches " + match.getPattern());
                            newgroup = new ArrayList<>();
                            newgroup.add(area);
                            mode = 1;
                        }
                        area = null; //read next
                        break;
                    case 1: //skip nodes with matched parent
                        if (!isAncestorOrSelf(match.getBox(), box))
                        { //out of the matched subtree
                            if (match.getPattern().getGroupCount() == 1)
                            {
                                mode = 0; //a single group, match finished
                                groupsFound.add(newgroup);
                            }
                            else
                            {
                                mode = 2; //find the end match
                            }
                        }
                        else
                        {
                            System.out.println("Skipping " + area);
                            newgroup.add(area);
                            area = null;
                        }
                        break;
                    case 2: //scan for end node
                        endmatch = recursiveScanBoxTree(box, false, match.getPattern());
                        if (endmatch != null)
                        {
                            System.out.println("MATCH end: " + box + " matches " + match.getPattern());
                            System.out.println("  endmatch: " + endmatch);
                            newgroup.add(area);
                            mode = 3;
                        }
                        /*else TODO
                        {
                            newgroup.add(area); //just add to the current group
                            if (newgroup.size() > match.getPattern().getGroupCount() * 2)
                            {
                                log.error("Couldn't find end match for {} within a limit, giving up", match);
                                newgroup = null;
                                mode = 0;
                            }
                        }*/
                        area = null; //read next
                        break;
                    case 3: //skip nodes with matched ending parent
                        if (!isAncestorOrSelf(endmatch.getBox(), box))
                        { //out of the matched subtree
                            groupsFound.add(newgroup);
                            mode = 0;
                        }
                        else
                        {
                            System.out.println("Skipping at end " + area);
                            newgroup.add(area);
                            area = null;
                        }
                        break;
                }
            }
        }
    }

    private PatternMatch recursiveScanBoxTree(Box box, boolean start)
    {
        AreaPattern pat = findMatch(box, start);
        if (pat != null)
        {
            return new PatternMatch(pat, box);
        }
        else
        {
            if (box.getParentBox() != null)
                return recursiveScanBoxTree(box.getParentBox(), start);
            else
                return null;
        }
    }
    
    private PatternMatch recursiveScanBoxTree(Box box, boolean start, AreaPattern pattern)
    {
        AreaPattern pat = findMatch(box, start, pattern);
        if (pat != null)
        {
            return new PatternMatch(pat, box);
        }
        else
        {
            if (box.getParentBox() != null)
                return recursiveScanBoxTree(box.getParentBox(), start, pattern);
            else
                return null;
        }
    }
    
    private AreaPattern findMatch(Box box, boolean start)
    {
        if (box.getParentBox() != null)
        {
            for (AreaPattern pat : patterns)
            {
                if (findRootMatch(box, pat) != null)
                {
                    if ((start && pat.matchesStart(box)) || (!start && pat.matchesEnd(box)))
                        return pat;
                }
            }
            return null; //no pattern matched
        }
        else
            return null;
    }
    
    private AreaPattern findMatch(Box box, boolean start, AreaPattern pat)
    {
        if (box.getParentBox() != null)
        {
            if (findRootMatch(box, pat) != null)
            {
                if ((start && pat.matchesStart(box)) || (!start && pat.matchesEnd(box)))
                    return pat;
            }
            return null;
        }
        else
            return null;
    }
    
    private Box findRootMatch(Box box, AreaPattern pat)
    {
        Box cur = box;
        while (cur.getParentBox() != null)
        {
            cur = cur.getParentBox();
            if (pat.matchesRoot(cur))
                return cur;
        }
        return null;
    }
    
    private void findLeafAreas(Area root, List<Area> leaves)
    {
        if (root.isLeaf())
            leaves.add(root);
        else
        {
            for (Area child : root.getChildAreas())
                findLeafAreas(child, leaves);
        }
    }
    
    private boolean isAncestorOrSelf(Box anc, Box box)
    {
        Box cur = box;
        while (cur != null)
        {
            if (cur == anc)
                return true;
            else
                cur = cur.getParentBox();
        }
        return false;
    }
    
    //==============================================================================
    
    private void createSuperArea(List<Area> group)
    {
        if (group.size() > 0 && group.get(0).getParentArea() != null)
        {
            Area parent = group.get(0).getParentArea();
            //compute the bounds
            Rectangular gp = null;
            for (Area area : group)
            {
                Rectangular agp = parent.getTopology().getPosition(area);
                if (agp != null)
                {
                    if (gp == null)
                        gp = new Rectangular(agp);
                    else
                        gp.expandToEnclose(agp);
                }
                else
                    log.error("Couldn't create super area for {} because of a different parent. The tree should be flattened before applying the GroupByExample operator", area);
            }
            //create the super area
            parent.createSuperArea(gp, group, "<area>");
        }        
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
        List<Box> groups = getGroupCommonAncestors(boxes, cparent, area);
        
        if ((groups.isEmpty() || (groups.size() == 1 && groups.get(0) == cparent))
                && cparent.getParentBox() != null)
        {
            //if there are no groups, we probably take the whole subtree. The parent is then one level up.
            if (groups.isEmpty())
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
     * Obtains all the ancestor of the given boxes that are the descendant nodes of the given parent and
     * are fully contained in a given area.
     * @param boxes
     * @param parent
     * @return
     */
    private List<Box> getGroupCommonAncestors(List<Box> boxes, Box parent, Area area)
    {
        List<Box> ret = new ArrayList<>();
        for (Box box : boxes)
        {
            Box anc = getAncestorUntilParent(box, parent, area);
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
    private Box getAncestorUntilParent(Box box, Box parent, Area area)
    {
        Rectangular ab = area.getBounds();
        Box cur = box;
        do
        {
            Box cparent = cur.getParentBox();
            if (cparent == parent) 
                return cur; //stop on the given parent
            else
            {
                Rectangular pb = cparent.getVisualBounds();
                if (!ab.encloses(pb))
                    return cur; //stop because the parent box does not fit to the area
            }
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
