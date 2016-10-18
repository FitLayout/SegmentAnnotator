/**
 * ClassificationPlugin.java
 *
 * Created on 23. 1. 2015, 21:44:40 by burgetr
 */
package org.fit.layout.sa.gui;

import javax.swing.JPanel;

import org.fit.layout.gui.AreaSelectionListener;
import org.fit.layout.gui.Browser;
import org.fit.layout.gui.BrowserPlugin;
import org.fit.layout.model.Area;

/**
 * 
 * @author burgetr
 */
public class AnnotatorPlugin implements BrowserPlugin, AreaSelectionListener
{
    private Browser browser;
    
    private JPanel mainPanel;

    @Override
    public boolean init(Browser browser)
    {
        this.browser = browser;
        this.browser.addToolPanel("Segment Annotator", getMainPanel());
        this.browser.addAreaSelectionListener(this);
        return true;
    }
    
    
    /**
     * @wbp.parser.entryPoint
     */
    private JPanel getMainPanel()
    {
        if (mainPanel == null)
        {
            mainPanel = new JPanel();
        }
        return mainPanel;
    }
    
    //=================================================================
    
    @Override
    public void areaSelected(Area area)
    {
    }
    
}
