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
import org.fit.layout.gui.RectangleSelectionListener;
import org.fit.layout.gui.TreeListener;
import org.fit.layout.impl.DefaultArea;
import org.fit.layout.impl.DefaultAreaTree;
import org.fit.layout.model.Area;
import org.fit.layout.model.AreaTree;
import org.fit.layout.model.Box;
import org.fit.layout.model.LogicalAreaTree;
import org.fit.layout.model.Page;
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
import javax.swing.JButton;

/**
 * 
 * @author burgetr
 */
public class AnnotatorPlugin implements BrowserPlugin, RectangleSelectionListener, TreeListener, AreaSelectionListener
{
    private Browser browser;
    private Rectangular lastSelection = new Rectangular();
    private Area selectedParent;
    
    private JPanel mainPanel;
    private JToggleButton annotateButton;

    @Override
    public boolean init(Browser browser)
    {
        this.browser = browser;
        this.browser.addToolPanel("Segment Annotator", getMainPanel());
        this.browser.addTreeListener(this);
        this.browser.addAreaSelectionListener(this);
        //this.browser.addRectangleSelectionListener(this);
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
            GridBagLayout gbl_toolPanel = new GridBagLayout();
            gbl_toolPanel.columnWidths = new int[]{100, 0};
            gbl_toolPanel.rowHeights = new int[]{25, 0, 0};
            gbl_toolPanel.columnWeights = new double[]{0.0, Double.MIN_VALUE};
            gbl_toolPanel.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
            toolPanel.setLayout(gbl_toolPanel);
            
            GridBagConstraints gbc_annotateButton = new GridBagConstraints();
            gbc_annotateButton.fill = GridBagConstraints.HORIZONTAL;
            gbc_annotateButton.insets = new Insets(0, 0, 5, 0);
            gbc_annotateButton.anchor = GridBagConstraints.NORTHWEST;
            gbc_annotateButton.gridx = 0;
            gbc_annotateButton.gridy = 0;
            toolPanel.add(getAnnotateButton(), gbc_annotateButton);
            
            JButton addButton = new JButton("Add");
            addButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    addAreaFromSelection();
                }
            });
            GridBagConstraints gbc_addButton = new GridBagConstraints();
            gbc_addButton.fill = GridBagConstraints.HORIZONTAL;
            gbc_addButton.gridx = 0;
            gbc_addButton.gridy = 1;
            toolPanel.add(addButton, gbc_addButton);
            
            JScrollPane tableScrollPane = new JScrollPane();
            GridBagConstraints gbc_tableScrollPane = new GridBagConstraints();
            gbc_tableScrollPane.weightx = 1.0;
            gbc_tableScrollPane.insets = new Insets(0, 0, 5, 0);
            gbc_tableScrollPane.fill = GridBagConstraints.BOTH;
            gbc_tableScrollPane.gridx = 1;
            gbc_tableScrollPane.gridy = 0;
            mainPanel.add(tableScrollPane, gbc_tableScrollPane);
            
            JList areaList =new JList();
            tableScrollPane.setViewportView(areaList);
            
            JPanel propertyPanel = new JPanel();
            GridBagConstraints gbc_propertyPanel = new GridBagConstraints();
            gbc_propertyPanel.insets = new Insets(0, 0, 0, 5);
            gbc_propertyPanel.fill = GridBagConstraints.BOTH;
            gbc_propertyPanel.gridx = 2;
            gbc_propertyPanel.gridy = 0;
            mainPanel.add(propertyPanel, gbc_propertyPanel);
            GridBagLayout gbl_propertyPanel = new GridBagLayout();
            gbl_propertyPanel.columnWidths = new int[]{0, 0};
            gbl_propertyPanel.rowHeights = new int[]{0, 0, 0, 0, 0};
            gbl_propertyPanel.columnWeights = new double[]{0.0, Double.MIN_VALUE};
            gbl_propertyPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
            propertyPanel.setLayout(gbl_propertyPanel);
            
            JButton deleteButton = new JButton("Delete");
            GridBagConstraints gbc_deleteButton = new GridBagConstraints();
            gbc_deleteButton.insets = new Insets(0, 0, 5, 0);
            gbc_deleteButton.fill = GridBagConstraints.BOTH;
            gbc_deleteButton.gridx = 0;
            gbc_deleteButton.gridy = 0;
            propertyPanel.add(deleteButton, gbc_deleteButton);
            
            JButton saveButton = new JButton("Save");
            GridBagConstraints gbc_saveButton = new GridBagConstraints();
            gbc_saveButton.gridx = 0;
            gbc_saveButton.gridy = 3;
            propertyPanel.add(saveButton, gbc_saveButton);
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
    public void pageRendered(Page page)
    {
        //check whether an area tree exists and create an empty one when not
        if (browser.getAreaTree() == null)
        {
            DefaultAreaTree atree = new DefaultAreaTree(browser.getPage());
            DefaultArea root = new DefaultArea(browser.getPage().getRoot());
            atree.setRoot(root);
            selectedParent = root;
            browser.setAreaTree(atree);
            browser.refreshView();
        }
    }


    @Override
    public void areaTreeUpdated(AreaTree tree)
    {
    }


    @Override
    public void logicalAreaTreeUpdated(LogicalAreaTree tree)
    {
    }
    
    @Override
    public void areaSelected(Area area)
    {
        selectedParent = area;
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
        lastSelection = r;
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
    
    private void addAreaFromSelection()
    {
        if (lastSelection != null && selectedParent != null)
        {
            List<Box> boxes = browser.getPage().getBoxesInRegion(lastSelection);
            if (!boxes.isEmpty())
            {
                DefaultArea newarea = new DefaultArea(boxes);
                if (selectedParent instanceof DefaultArea)
                    ((DefaultArea) selectedParent).appendChild(newarea);
                else
                    System.err.println("Cannot add areas to this type of nodes"); //TODO
            }
            browser.refreshView();
        }
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
