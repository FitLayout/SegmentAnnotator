/**
 * ClassificationPlugin.java
 *
 * Created on 23. 1. 2015, 21:44:40 by burgetr
 */
package org.fit.layout.sa.gui;

import javax.swing.JPanel;

import org.fit.layout.gui.Browser;
import org.fit.layout.gui.BrowserPlugin;
import org.fit.layout.gui.RectangleSelectionListener;
import org.fit.layout.model.Box;
import org.fit.layout.model.Rectangular;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.JScrollPane;
import java.awt.Insets;
import javax.swing.JList;
import javax.swing.JToggleButton;
import java.awt.event.ActionListener;
import java.util.List;
import java.awt.event.ActionEvent;

/**
 * 
 * @author burgetr
 */
public class AnnotatorPlugin implements BrowserPlugin, RectangleSelectionListener
{
    private Browser browser;
    private Rectangular lastSelection = new Rectangular();
    
    private JPanel mainPanel;
    private JToggleButton annotateButton;

    @Override
    public boolean init(Browser browser)
    {
        this.browser = browser;
        this.browser.addToolPanel("Segment Annotator", getMainPanel());
        this.browser.addRectangleSelectionListener(this);
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
            GridBagLayout gbl_mainPanel = new GridBagLayout();
            mainPanel.setLayout(gbl_mainPanel);
            
            JPanel toolPanel = new JPanel();
            GridBagConstraints gbc_toolPanel = new GridBagConstraints();
            gbc_toolPanel.insets = new Insets(0, 0, 5, 5);
            gbc_toolPanel.fill = GridBagConstraints.BOTH;
            gbc_toolPanel.gridx = 0;
            gbc_toolPanel.gridy = 0;
            mainPanel.add(toolPanel, gbc_toolPanel);
            
            toolPanel.add(getAnnotateButton());
            
            JScrollPane tableScrollPane = new JScrollPane();
            GridBagConstraints gbc_tableScrollPane = new GridBagConstraints();
            gbc_tableScrollPane.insets = new Insets(0, 0, 5, 0);
            gbc_tableScrollPane.fill = GridBagConstraints.BOTH;
            gbc_tableScrollPane.gridx = 1;
            gbc_tableScrollPane.gridy = 0;
            mainPanel.add(tableScrollPane, gbc_tableScrollPane);
            
            JList areaList =new JList();
            tableScrollPane.setViewportView(areaList);
            
            JPanel propertyPanel = new JPanel();
            GridBagConstraints gbc_propertyPanel = new GridBagConstraints();
            gbc_propertyPanel.weightx = 1.0;
            gbc_propertyPanel.insets = new Insets(0, 0, 0, 5);
            gbc_propertyPanel.fill = GridBagConstraints.BOTH;
            gbc_propertyPanel.gridx = 2;
            gbc_propertyPanel.gridy = 0;
            mainPanel.add(propertyPanel, gbc_propertyPanel);
        }
        return mainPanel;
    }


    private JToggleButton getAnnotateButton()
    {
        if (annotateButton == null)
        {
            annotateButton = new JToggleButton("Annotate");
            annotateButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (annotateButton.isSelected())
                        startSelection();
                    else
                        stopSelection();
                }
            });
        }
        return annotateButton;
    }

    //=================================================================
    
    @Override
    public void rectangleCreated(Rectangular rect)
    {
        browser.clearSelection();
        Rectangular r = findSelectedRectangle(rect);
        if (r != null)
            browser.setSelection(r);
        else
            browser.clearSelection();
    }
    
    //=================================================================

    private void startSelection()
    {
        browser.addRectangleSelectionListener(this);
    }
    
    private void stopSelection()
    {
        browser.removeRectangleSelectionListener(this);
    }
    
    private Rectangular findSelectedRectangle(Rectangular sel)
    {
        Rectangular r = null;
        List<Box> boxes = browser.getPage().getBoxesInRegion(sel);
        for (Box box : boxes)
        {
            if (r == null)
                r = new Rectangular(box.getBounds());
            else
                r.expandToEnclose(box.getBounds());
        }
        
        return r;
    }
    
}
