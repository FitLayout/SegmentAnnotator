/**
 * GroupByExampleOperator.java
 *
 * Created on 24. 10. 2016, 21:32:40 by burgetr
 */
package org.fit.layout.sa.op;

import java.util.ArrayList;
import java.util.Collections;
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
        
        int cnf = doScan(atree, leaves);
        
        scanForMatches(atree, leaves, cnf, groupsFound, false);
        System.out.println("Found " + groupsFound.size() + " matches");
        root.updateTopologies();
        for (List<Area> group : groupsFound)
            createSuperArea(group);
    }
    
    //==============================================================================
    
    private int doScan(AreaTree atree, List<Area> leaves)
    {
        for (int i = 0; i < 100; i++)
        {
            List<List<Area>> dest = new ArrayList<>();
            List<PatternMatch> matches = scanForMatches(atree, leaves, i, dest, true);
            if (matches != null && !matches.isEmpty())
            {
                log.trace("#{} valid, {} matches", i, matches.size());
                //System.out.println(matches);
                return i;
            }
            else
                log.trace("#{} invalid (overlap)", i);
        }
        return 0;
    }
    
    /**
     * Scans the area tree and fills {@code leafBoxes} with all the boxes that correspond
     * to the leaf areas of the area tree.
     * @param root
     * @return true when the result for the given config is valid
     */
    private List<PatternMatch> scanForMatches(AreaTree atree, List<Area> leaves, int config, List<List<Area>> dest, boolean trial)
    {
        List<PatternMatch> ret = new ArrayList<>(20);
        
        int mode = 0;
        PatternMatch match = null;
        PatternMatch endmatch = null;
        List<Area> newgroup = null;
        //Area newparent = null;
        Area area = null; //current area
        Iterator<Area> it = leaves.iterator();
        while (it.hasNext())
        {
            if (area == null)
                area = (Area) it.next();
            for (Box box : area.getBoxes())
            {
                //System.out.println("(" + mode + ") " + box);
                switch (mode)
                {
                    case 0: //scan for start node
                        //find the match according to the config
                        List<PatternMatch> matches = new ArrayList<>(); 
                        recursiveFindAllMatches(box, true, matches);
                        if (matches.isEmpty())
                            match = null; //no match for this box, carry on
                        else
                        {
                            int pi = config % matches.size();
                            config = config / matches.size();
                            /*System.out.println(pi + " out of " + matches.size());
                            if (matches.size() > 1)
                            {
                                System.out.println("BOX: " + box);
                                for (int ii = 0; ii < matches.size(); ii++)
                                    System.out.println("  " + matches.get(ii));
                            }*/
                            match = matches.get(pi);
                            ret.add(match);
                        }
                        
                        if (match != null)
                        {
                            //System.out.println("MATCH start: " + box + " matches " + match.getPattern());

                            //create a new group
                            newgroup = new ArrayList<>();
                            //create a new parent area when necessary
                            if (match.getBox() != box)
                            {
                                Area matcharea = atree.createArea(match.getBox());
                                newgroup.add(matcharea);
                                if (!trial)
                                {
                                    //matcharea.setName("<area>");
                                    //replace the area with the matching one
                                    Area parent = area.getParentArea();
                                    parent.appendChild(matcharea);
                                    parent.removeChild(area);
                                }
                            }
                            else
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
                                dest.add(newgroup);
                                newgroup = new ArrayList<>();
                            }
                            else
                            {
                                mode = 2; //find the end match
                            }
                        }
                        else
                        {
                            //System.out.println("Skipping " + area);
                            if (!trial)
                                area.getParentArea().removeChild(area);
                            area = null;
                        }
                        break;
                    case 2: //scan for end node
                        endmatch = recursiveScanBoxTree(box, false, match.getPattern());
                        if (endmatch != null)
                        {
                            //System.out.println("MATCH end: " + box + " matches " + match.getPattern());
                            //System.out.println("  endmatch: " + endmatch);
                            if (endmatch.getBox() != box)
                            {
                                Area matcharea = atree.createArea(endmatch.getBox());
                                newgroup.add(matcharea);
                                if (!trial)
                                {
                                    //matcharea.setName("<area>");
                                    Area oldparent = area.getParentArea();
                                    oldparent.appendChild(matcharea);
                                    oldparent.removeChild(area);
                                }
                            }
                            else
                                newgroup.add(area);
                            
                            mode = 3;
                        }
                        else
                        {
                            //check if we are still at least in the required parent box
                            if (findRootMatch(box, match.getPattern()) != null)
                            {
                                newgroup.add(area); //just add to the current group
                            }
                            else
                            {
                                //System.out.println("Ran out of root, finishing group");
                                if (!newgroup.isEmpty())
                                {
                                    dest.add(newgroup);
                                    newgroup = new ArrayList<>();
                                    mode = 0;
                                }
                            }
                        }
                        area = null; //read next
                        break;
                    case 3: //skip nodes with matched ending parent
                        if (!isAncestorOrSelf(endmatch.getBox(), box))
                        { //out of the matched subtree
                            dest.add(newgroup);
                            newgroup = new ArrayList<>();
                            mode = 0;
                        }
                        else
                        {
                            //System.out.println("Skipping at end " + area);
                            if (!trial)
                                area.getParentArea().removeChild(area);
                            area = null;
                        }
                        break;
                }
            }
        }
        if (newgroup != null && !newgroup.isEmpty())
            dest.add(newgroup);
        
        //trial run, check if the areas are overlapping
        if (trial)
        {
            List<Rectangular> groups = new ArrayList<>(dest.size());
            for (List<Area> group : dest)
                groups.add(computeGroupBounds(group));
            if (checkOverlaps(groups))
                return null; //areas are overlapping invalidate the result
        }
        return ret;
    }

    private void recursiveFindAllMatches(Box box, boolean start, List<PatternMatch> dest)
    {
        List<AreaPattern> list = findAllMatches(box, start);
        for (AreaPattern pat : list)
            dest.add(new PatternMatch(pat, box));
        
        if (box.getParentBox() != null)
            recursiveFindAllMatches(box.getParentBox(), start, dest);
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
    
    private List<AreaPattern> findAllMatches(Box box, boolean start)
    {
        if (box.getParentBox() != null)
        {
            List<AreaPattern> ret = new ArrayList<>();
            for (AreaPattern pat : patterns)
            {
                //if (box.toString().contains("He has served") && pat.toString().contains("<:1>"))
                //    System.out.println("he?");
                if (findRootMatch(box, pat) != null)
                {
                    if ((start && pat.matchesStart(box)) || (!start && pat.matchesEnd(box)))
                    {
                        ret.add(pat);
                    }
                }
            }
            return ret;
        }
        else
            return Collections.emptyList();
    }
    
    private AreaPattern findMatch(Box box, boolean start)
    {
        if (box.getParentBox() != null)
        {
            AreaPattern ret = null;
            int cnt = 0;
            for (AreaPattern pat : patterns)
            {
                if (findRootMatch(box, pat) != null)
                {
                    if ((start && pat.matchesStart(box)) || (!start && pat.matchesEnd(box)))
                    {
                        ret = pat;
                        cnt++;
                        System.out.println("Found " + pat);
                    }
                }
            }
            if (cnt > 1)
                System.out.println(" Multiple matches " + cnt + " for " + box);
            return ret;
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
    
    private boolean checkOverlaps(List<Rectangular> list)
    {
        for (Rectangular r1 : list)
        {
            for (Rectangular r2 : list)
            {
                if (r1 != r2 && r1.intersects(r2))
                {
                    log.trace("OVERLAP {} x {}", r1, r2);
                    return true;
                }
            }
        }
        return false;
    }
    
    //==============================================================================
    
    private void createSuperArea(List<Area> group)
    {
        if (group.get(0).getParentArea() != null)
        {
            if (group.size() > 1)
            {
                Area parent = group.get(0).getParentArea();
                //compute the bounds
                Rectangular gp = computeGroupGP(parent, group);
                //create the super area
                parent.createSuperArea(gp, group, "<area>");
            }
            else
                group.get(0).setName("<area>"); //one-member group
        }
    }
    
    private Rectangular computeGroupGP(Area parent, List<Area> group)
    {
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
        return gp;
    }
    
    private Rectangular computeGroupBounds(List<Area> group)
    {
        Rectangular ret = null;
        for (Area area : group)
        {
            Rectangular agp = area.getBounds();
            if (ret == null)
                ret = new Rectangular(agp);
            else
                ret.expandToEnclose(agp);
        }
        return ret;
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
        if (patterns.contains(pat))
            System.out.println("ALREADY THERE");
        else
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
