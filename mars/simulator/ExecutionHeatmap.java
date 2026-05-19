package mars.simulator;

import java.util.*;
import java.awt.*;
import mars.*;

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
 * Singleton service to track source code line execution frequency
 * and provide heatmap visualization colors.
 * 
 * Tracks which source lines are executed and how many times,
 * then maps that to colors for visualization in the editor.
 * 
 * @author Pete Sanderson and Ken Vollmar
 * @version 2025
 */
public class ExecutionHeatmap {
    
    private static ExecutionHeatmap instance = null;
    
    // Track execution frequency per source line
    private HashMap<Integer, Integer> lineExecutionCounts;  // Line number -> execution count
    private boolean isRecording;
    private int maxLineCount;  // Maximum execution count for any line
    
    /**
     * Private constructor - Singleton pattern
     */
    private ExecutionHeatmap() {
        lineExecutionCounts = new HashMap<Integer, Integer>();
        isRecording = true;
        maxLineCount = 0;
    }
    
    /**
     * Get singleton instance
     */
    public static ExecutionHeatmap getInstance() {
        if (instance == null) {
            instance = new ExecutionHeatmap();
        }
        return instance;
    }
    
    /**
     * Reset heatmap data at start of simulation
     */
    public void reset() {
        lineExecutionCounts.clear();
        maxLineCount = 0;
        isRecording = true;
    }
    
    /**
     * Record execution of a source line
     * @param lineNumber the source code line number (1-based)
     */
    public void recordLineExecution(int lineNumber) {
        if (!isRecording || lineNumber <= 0) {
            return;
        }
        
        int count = lineExecutionCounts.getOrDefault(lineNumber, 0) + 1;
        lineExecutionCounts.put(lineNumber, count);
        
        // Track maximum for normalization
        if (count > maxLineCount) {
            maxLineCount = count;
        }
    }
    
    /**
     * Start recording
     */
    public void startRecording() {
        isRecording = true;
    }
    
    /**
     * Stop recording
     */
    public void stopRecording() {
        isRecording = false;
    }
    
    /**
     * Get execution count for a specific line
     * @param lineNumber the source line number
     * @return execution count (0 if never executed)
     */
    public int getLineExecutionCount(int lineNumber) {
        return lineExecutionCounts.getOrDefault(lineNumber, 0);
    }
    
    /**
     * Get the maximum execution count (for normalization)
     * @return max count
     */
    public int getMaxExecutionCount() {
        return maxLineCount;
    }
    
    /**
     * Get all line execution data
     * @return HashMap of line number to execution count
     */
    public HashMap<Integer, Integer> getLineExecutionCounts() {
        return new HashMap<Integer, Integer>(lineExecutionCounts);
    }
    
    /**
     * Get execution count for a specific line
     * @param lineNumber the source line number
     * @return execution count for that line, or 0 if not executed
     */
    public int getExecutionCount(int lineNumber) {
        return lineExecutionCounts.getOrDefault(lineNumber, 0);
    }
    
    /**
     * Get color for a line based on execution frequency
     * Uses heat map: Red (high), Orange (medium), Yellow (medium-low), Green (low), White (none)
     * 
     * @param lineNumber the source line number
     * @return Color to use for that line's background
     */
    public Color getHeatmapColor(int lineNumber) {
        int count = getLineExecutionCount(lineNumber);
        
        // No execution
        if (count == 0) {
            return Color.WHITE;
        }
        
        // If max is 0, just executed once
        if (maxLineCount == 0) {
            return new Color(200, 255, 200);  // Light green
        }
        
        // Normalize to 0-1 range
        double normalized = (double) count / maxLineCount;
        
        // Map to colors using heat gradient
        // 0.0-0.2 = Green, 0.2-0.4 = Yellow, 0.4-0.6 = Orange, 0.6-1.0 = Red
        if (normalized < 0.2) {
            // Green (low execution)
            return interpolateColor(new Color(144, 238, 144), new Color(100, 200, 100), normalized / 0.2);
        } else if (normalized < 0.4) {
            // Yellow (medium-low)
            return interpolateColor(new Color(100, 200, 100), new Color(255, 255, 100), (normalized - 0.2) / 0.2);
        } else if (normalized < 0.6) {
            // Orange (medium)
            return interpolateColor(new Color(255, 255, 100), new Color(255, 165, 0), (normalized - 0.4) / 0.2);
        } else if (normalized < 0.8) {
            // Dark Orange/Red (high)
            return interpolateColor(new Color(255, 165, 0), new Color(255, 69, 0), (normalized - 0.6) / 0.2);
        } else {
            // Red (very high execution)
            return interpolateColor(new Color(255, 69, 0), new Color(200, 0, 0), (normalized - 0.8) / 0.2);
        }
    }
    
    /**
     * Interpolate between two colors
     * @param color1 starting color
     * @param color2 ending color
     * @param ratio interpolation ratio (0.0 to 1.0)
     * @return interpolated color
     */
    private Color interpolateColor(Color color1, Color color2, double ratio) {
        int r = (int) (color1.getRed() * (1 - ratio) + color2.getRed() * ratio);
        int g = (int) (color1.getGreen() * (1 - ratio) + color2.getGreen() * ratio);
        int b = (int) (color1.getBlue() * (1 - ratio) + color2.getBlue() * ratio);
        
        return new Color(
            Math.min(255, Math.max(0, r)),
            Math.min(255, Math.max(0, g)),
            Math.min(255, Math.max(0, b))
        );
    }
    
    /**
     * Get a descriptive label for the execution level
     * @param lineNumber the source line number
     * @return String describing the execution level
     */
    public String getExecutionLevel(int lineNumber) {
        int count = getLineExecutionCount(lineNumber);
        
        if (count == 0) {
            return "Not executed";
        }
        
        double normalized = maxLineCount > 0 ? (double) count / maxLineCount : 0;
        
        if (normalized >= 0.8) {
            return "Very High (" + count + "x)";
        } else if (normalized >= 0.6) {
            return "High (" + count + "x)";
        } else if (normalized >= 0.4) {
            return "Medium (" + count + "x)";
        } else if (normalized >= 0.2) {
            return "Low (" + count + "x)";
        } else {
            return "Very Low (" + count + "x)";
        }
    }
    
    /**
     * Check if any lines have been executed
     * @return true if heatmap has data
     */
    public boolean hasData() {
        return !lineExecutionCounts.isEmpty();
    }
}
