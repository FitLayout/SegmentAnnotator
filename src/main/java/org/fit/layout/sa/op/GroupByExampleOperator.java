/**
 * GroupByExampleOperator.java
 *
 * Created on 24. 10. 2016, 21:32:40 by burgetr
 */
package org.fit.layout.sa.op;

import org.fit.layout.impl.BaseOperator;
import org.fit.layout.model.Area;
import org.fit.layout.model.AreaTree;
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
    
    
    public GroupByExampleOperator()
    {
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
    
    public AreaTree getExampleTree()
    {
        return exampleTree;
    }

    public void setExampleTree(AreaTree exampleTree)
    {
        this.exampleTree = exampleTree;
    }
    
    //==============================================================================

    @Override
    public void apply(AreaTree atree)
    {
    }

    @Override
    public void apply(AreaTree atree, Area root)
    {
    }
    
    //==============================================================================
    
    
}
