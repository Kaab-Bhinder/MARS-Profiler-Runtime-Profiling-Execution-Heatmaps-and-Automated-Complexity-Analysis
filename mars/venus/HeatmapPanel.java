package mars.venus;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import mars.simulator.*;
import mars.venus.editors.MARSTextEditingArea;

/*
Copyright (c) 2025, Pete Sanderson and Kenneth Vollmar

Permission is hereby granted, free of charge, to any person obtaining 
a copy of this software and associated documentation files (the 
"Software"), to deal in the Software without restriction, including 
without limitation the rights to use, copy, modify, merge, publish, 
distribute, sublicense, and/or sell copies of the Software, and to 
permit persons to whom the Software is furnished to do so, subject 
to the following conditions:

The above copyright notice and this permission notice shall be 
included in all copies or substantial portions of the Software.

(MIT license, http://www.opensource.org/licenses/mit-license.html)
*/

/**
 * Heatmap visualization panel for the text editor.
 * Displays colored backgrounds representing execution frequency of each line.
 * Colors range from red (heavily executed) to green (rarely executed) to white (not executed).
 * 
 * @author Pete Sanderson and Ken Vollmar
 * @version 2025
 */
public class HeatmapPanel extends JPanel {
    
    private JTextPane textPane;
    private MARSTextEditingArea marsTextArea;
    private Dimension preferredSize;
    private int lineHeight = 15;
    private int lineWidth = 20;
    private boolean enabled = true;
    
    // Tooltip support
    private JToolTip currentTooltip;
    private Timer tooltipTimer;
    
    /**
     * Constructor
     * @param textPane the text editor pane to synchronize with
     */
    public HeatmapPanel(MARSTextEditingArea textArea) {
        this.marsTextArea = textArea;
        this.textPane = null;
        this.setPreferredSize(new Dimension(lineWidth, 500));
        this.setBackground(Color.WHITE);
        
        // Add mouse listener for tooltips
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                showHeatmapTooltip(e);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                hideTooltip();
            }
        });
        
        // Add mouse motion listener
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                showHeatmapTooltip(e);
            }
        });
    }
    
    /**
     * Enable or disable the heatmap display
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        repaint();
    }
    
    /**
     * Check if heatmap is enabled
     */
    public boolean isHeatmapEnabled() {
        return enabled;
    }
    
    /**
     * Paint the heatmap
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (!enabled) {
            return;
        }

        // Resolve document from whichever editor is available
        Document doc = null;
        if (marsTextArea != null) {
            doc = marsTextArea.getDocument();
        } else if (textPane != null) {
            doc = textPane.getDocument();
        }
        if (doc == null) return;

        ExecutionHeatmap heatmap = ExecutionHeatmap.getInstance();
        if (!heatmap.hasData()) {
            return;
        }
        
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Get document info
        int totalLines = doc.getDefaultRootElement().getElementCount();
        
        // Draw heatmap for each line
        int y = 0;
        for (int lineNum = 1; lineNum <= totalLines; lineNum++) {
            Color heatColor = heatmap.getHeatmapColor(lineNum);
            
            // Draw colored rectangle for this line
            g2d.setColor(heatColor);
            g2d.fillRect(0, y, lineWidth, lineHeight);
            
            // Draw border
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.drawRect(0, y, lineWidth, lineHeight);
            
            y += lineHeight;
        }
    }
    
    /**
     * Show tooltip with execution count
     */
    private void showHeatmapTooltip(MouseEvent e) {
        int lineNum = e.getY() / lineHeight + 1;
        ExecutionHeatmap heatmap = ExecutionHeatmap.getInstance();
        
        String tooltipText = "<html>";
        tooltipText += "Line " + lineNum + ":<br>";
        tooltipText += heatmap.getExecutionLevel(lineNum);
        tooltipText += "</html>";
        
        setToolTipText(tooltipText);
    }
    
    /**
     * Hide tooltip
     */
    private void hideTooltip() {
        setToolTipText(null);
    }
    
    /**
     * Update line height based on font
     */
    public void updateLineHeight(FontMetrics fm) {
        if (fm != null) {
            lineHeight = fm.getHeight();
        }
    }
    
    /**
     * Get preferred size
     */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(lineWidth, 500);
    }
    
    /**
     * Refresh the heatmap display
     */
    public void refresh() {
        repaint();
    }
    
    /**
     * Get the minimum size to ensure panel is visible
     */
    @Override
    public Dimension getMinimumSize() {
        return new Dimension(30, 100);
    }
}
