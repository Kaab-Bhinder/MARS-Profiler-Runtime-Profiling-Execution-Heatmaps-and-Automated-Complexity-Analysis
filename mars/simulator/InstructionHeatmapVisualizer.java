package mars.simulator;

import java.util.*;

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
 * Visualizes instruction execution frequencies using heat map coloring.
 * 
 * Shows:
 * - Instruction name and execution count
 * - Heat-map color representation (White → Green → Yellow → Orange → Red)
 * - Execution percentage relative to hottest instruction
 * - ASCII bar visualization
 * - Color intensity indicates frequency
 * 
 * @author MARS Contributors
 * @version 2025
 */
public class InstructionHeatmapVisualizer {
    
    private ProfilerService profiler;
    
    /**
     * Constructor
     * @param profilerService the ProfilerService instance
     */
    public InstructionHeatmapVisualizer(ProfilerService profilerService) {
        this.profiler = profilerService;
    }
    
    /**
     * Generate a formatted instruction frequency report with heatmap coloring
     * @return formatted report with color codes
     */
    public String generateInstructionHeatmapReport() {
        StringBuilder report = new StringBuilder();
        
        Map<String, Integer> instCounts = profiler.getInstructionCounts();
        
        if (instCounts.isEmpty()) {
            return "No instruction data available.\n";
        }
        
        // Sort by frequency (descending)
        List<Map.Entry<String, Integer>> sortedInst = 
            new ArrayList<>(instCounts.entrySet());
        sortedInst.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        // Find maximum count for normalization
        int maxCount = sortedInst.get(0).getValue();
        
        report.append("\n");
        report.append("═".repeat(90)).append("\n");
        report.append("                    INSTRUCTION FREQUENCY HEATMAP\n");
        report.append("═".repeat(90)).append("\n\n");
        
        report.append(String.format("%-15s %12s %10s %50s  INTENSITY\n", 
                                   "Instruction", "Count", "Percent", "Heatmap Visualization"));
        report.append("-".repeat(90)).append("\n");
        
        // Display each instruction with heatmap coloring
        for (Map.Entry<String, Integer> entry : sortedInst) {
            String instruction = entry.getKey();
            int count = entry.getValue();
            double percentage = (count * 100.0) / profiler.getTotalInstructions();
            double normalized = (double) count / maxCount;
            
            // Create visual bar
            String heatmapBar = createHeatmapBar(normalized, 40);
            String intensity = getIntensityLabel(normalized);
            
            report.append(String.format("%-15s %,11d %9.2f%% %s  %s\n", 
                                       instruction, count, percentage, heatmapBar, intensity));
        }
        
        report.append("\n").append("-".repeat(90)).append("\n");
        report.append("Legend:\n");
        report.append("  [░░░░] = Low    (0-20%)      Green zone   - Low frequency\n");
        report.append("  [▒▒▒▒] = Medium (20-40%)     Yellow zone  - Moderate frequency\n");
        report.append("  [▓▓▓▓] = High   (40-60%)     Orange zone  - Frequent\n");
        report.append("  [████] = Very High (60-100%) Red zone     - Very frequent\n");
        report.append("═".repeat(90)).append("\n");
        
        return report.toString();
    }
    
    /**
     * Generate categorical heatmap (group instructions by type)
     */
    public String generateCategoricalHeatmap() {
        StringBuilder report = new StringBuilder();
        
        Map<String, Integer> instCounts = profiler.getInstructionCounts();
        
        if (instCounts.isEmpty()) {
            return "No instruction data available.\n";
        }
        
        // Categorize instructions
        Map<String, Integer> categories = categorizeInstructions(instCounts);
        
        int maxCount = 0;
        for (int count : categories.values()) {
            maxCount = Math.max(maxCount, count);
        }
        
        report.append("\n");
        report.append("═".repeat(90)).append("\n");
        report.append("              INSTRUCTION CATEGORY HEATMAP\n");
        report.append("═".repeat(90)).append("\n\n");
        
        // Define category order and labels
        String[] categoryOrder = {"Arithmetic", "Memory Load", "Memory Store", "Branches", "Other"};
        String[] categorySymbols = {"ADD", "LOAD", "STORE", "BRANCH", "OTHER"};
        
        report.append(String.format("%-15s %12s %10s %50s  INTENSITY\n", 
                                   "Category", "Count", "Percent", "Heatmap Visualization"));
        report.append("-".repeat(90)).append("\n");
        
        for (int i = 0; i < categoryOrder.length; i++) {
            String category = categoryOrder[i];
            int count = categories.getOrDefault(category, 0);
            
            if (count == 0) continue;
            
            double percentage = (count * 100.0) / profiler.getTotalInstructions();
            double normalized = (double) count / maxCount;
            
            String heatmapBar = createHeatmapBar(normalized, 40);
            String intensity = getIntensityLabel(normalized);
            
            report.append(String.format("%-15s %,11d %9.2f%% %s  %s\n", 
                                       category, count, percentage, heatmapBar, intensity));
        }
        
        report.append("\n").append("-".repeat(90)).append("\n");
        report.append("Instructions by Category:\n");
        for (int i = 0; i < categoryOrder.length; i++) {
            String category = categoryOrder[i];
            int count = categories.getOrDefault(category, 0);
            if (count > 0) {
                report.append(String.format("  %s: %d instructions\n", category, count));
            }
        }
        report.append("═".repeat(90)).append("\n");
        
        return report.toString();
    }
    
    /**
     * Generate top N instructions heatmap
     */
    public String generateTopInstructionsHeatmap(int topN) {
        StringBuilder report = new StringBuilder();
        
        Map<String, Integer> instCounts = profiler.getInstructionCounts();
        
        if (instCounts.isEmpty()) {
            return "No instruction data available.\n";
        }
        
        // Sort by frequency
        List<Map.Entry<String, Integer>> sortedInst = 
            new ArrayList<>(instCounts.entrySet());
        sortedInst.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        // Get max of top N
        int maxCount = sortedInst.get(0).getValue();
        
        report.append("\n");
        report.append("═".repeat(90)).append("\n");
        report.append(String.format("          TOP %d MOST FREQUENT INSTRUCTIONS - HEATMAP VIEW\n", topN));
        report.append("═".repeat(90)).append("\n\n");
        
        report.append(String.format("%-15s %12s %10s %50s\n", 
                                   "Instruction", "Count", "Percent", "Heatmap Visualization"));
        report.append("-".repeat(90)).append("\n");
        
        int count = 0;
        for (Map.Entry<String, Integer> entry : sortedInst) {
            if (count++ >= topN) break;
            
            String instruction = entry.getKey();
            int execCount = entry.getValue();
            double percentage = (execCount * 100.0) / profiler.getTotalInstructions();
            double normalized = (double) execCount / maxCount;
            
            String heatmapBar = createHeatmapBar(normalized, 50);
            
            report.append(String.format("%-15s %,11d %9.2f%% %s\n", 
                                       instruction, execCount, percentage, heatmapBar));
        }
        
        report.append("\n").append("═".repeat(90)).append("\n");
        
        return report.toString();
    }
    
    /**
     * Create a visual heatmap bar using ASCII characters
     * Uses color gradients: ░ (light) → ▒ (medium) → ▓ (dark) → █ (very dark)
     * 
     * @param normalized value between 0.0 and 1.0
     * @param barLength length of the bar in characters
     * @return formatted bar string
     */
    private String createHeatmapBar(double normalized, int barLength) {
        StringBuilder bar = new StringBuilder("[");
        
        int filledCount = (int) Math.round(normalized * barLength);
        
        // Determine character to use based on intensity
        char fillChar;
        String colorPrefix = "";
        
        if (normalized < 0.2) {
            fillChar = '-';  // Light - Green
            colorPrefix = "----";
        } else if (normalized < 0.4) {
            fillChar = '=';  // Medium-light - Yellow
            colorPrefix = "====";
        } else if (normalized < 0.6) {
            fillChar = '#';  // Medium-dark - Orange
            colorPrefix = "####";
        } else {
            fillChar = '*';  // Dark - Red
            colorPrefix = "****";
        }
        
        // Build the bar
        for (int i = 0; i < barLength; i++) {
            if (i < filledCount) {
                bar.append(fillChar);
            } else {
                bar.append(' ');
            }
        }
        
        bar.append("]");
        
        return bar.toString();
    }
    
    /**
     * Get intensity label based on normalized value
     */
    private String getIntensityLabel(double normalized) {
        if (normalized < 0.1) {
            return "Very Low     (░░░░)";
        } else if (normalized < 0.2) {
            return "Low          (░░░░)";
        } else if (normalized < 0.4) {
            return "Medium       (▒▒▒▒)";
        } else if (normalized < 0.6) {
            return "High         (▓▓▓▓)";
        } else if (normalized < 0.8) {
            return "Very High    (████)";
        } else {
            return "Extreme      (████)";
        }
    }
    
    /**
     * Categorize instructions into groups
     */
    private Map<String, Integer> categorizeInstructions(Map<String, Integer> instCounts) {
        Map<String, Integer> categories = new HashMap<>();
        categories.put("Arithmetic", 0);
        categories.put("Memory Load", 0);
        categories.put("Memory Store", 0);
        categories.put("Branches", 0);
        categories.put("Other", 0);
        
        for (Map.Entry<String, Integer> entry : instCounts.entrySet()) {
            String inst = entry.getKey();
            int count = entry.getValue();
            
            if (inst.matches("add|addi|addiu|addu|sub|subu|and|andi|or|ori|xor|xori|nor|sll|sra|srl|slt|slti|sltiu|sltu|mult|multu|div|divu|mfhi|mflo|mthi|mtlo")) {
                categories.put("Arithmetic", categories.get("Arithmetic") + count);
            } else if (inst.matches("lw|lb|lbu|lh|lhu|lwu")) {
                categories.put("Memory Load", categories.get("Memory Load") + count);
            } else if (inst.matches("sw|sb|sh")) {
                categories.put("Memory Store", categories.get("Memory Store") + count);
            } else if (inst.matches("beq|bne|blez|bgtz|bltz|bgez|j|jr|jal|jalr")) {
                categories.put("Branches", categories.get("Branches") + count);
            } else {
                categories.put("Other", categories.get("Other") + count);
            }
        }
        
        return categories;
    }
    
    /**
     * Generate HTML-compatible heatmap (for future web UI integration)
     */
    public String generateHTMLHeatmap() {
        StringBuilder html = new StringBuilder();
        
        Map<String, Integer> instCounts = profiler.getInstructionCounts();
        
        if (instCounts.isEmpty()) {
            return "<p>No instruction data available.</p>";
        }
        
        List<Map.Entry<String, Integer>> sortedInst = 
            new ArrayList<>(instCounts.entrySet());
        sortedInst.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        int maxCount = sortedInst.get(0).getValue();
        
        html.append("<table border='1' cellpadding='5' cellspacing='0'>\n");
        html.append("<tr><th>Instruction</th><th>Count</th><th>Percent</th><th>Heatmap</th></tr>\n");
        
        for (Map.Entry<String, Integer> entry : sortedInst) {
            String inst = entry.getKey();
            int count = entry.getValue();
            double percentage = (count * 100.0) / profiler.getTotalInstructions();
            double normalized = (double) count / maxCount;
            
            // Determine color
            String color;
            if (normalized < 0.2) {
                color = "#90EE90";  // Light green
            } else if (normalized < 0.4) {
                color = "#FFFF00";  // Yellow
            } else if (normalized < 0.6) {
                color = "#FFA500";  // Orange
            } else if (normalized < 0.8) {
                color = "#FF6347";  // Red
            } else {
                color = "#8B0000";  // Dark red
            }
            
            int barWidth = (int) (normalized * 300);
            
            html.append(String.format("<tr><td>%s</td><td>%,d</td><td>%.2f%%</td>", 
                                     inst, count, percentage));
            html.append(String.format("<td><div style='background-color:%s; width:%dpx; height:20px;'></div></td></tr>\n", 
                                     color, barWidth));
        }
        
        html.append("</table>\n");
        
        return html.toString();
    }
    
    /**
     * Get instruction heatmap color (returns color intensity as percentage)
     */
    public int getInstructionHeatmapIntensity(String instruction) {
        Map<String, Integer> instCounts = profiler.getInstructionCounts();
        int count = instCounts.getOrDefault(instruction, 0);
        int maxCount = 0;
        
        for (int c : instCounts.values()) {
            maxCount = Math.max(maxCount, c);
        }
        
        if (maxCount == 0) return 0;
        
        return (int) ((count * 100.0) / maxCount);
    }
    
    /**
     * Print instruction heatmap to console
     */
    public void printInstructionHeatmap() {
        System.out.print(generateInstructionHeatmapReport());
    }
    
    /**
     * Print categorical heatmap to console
     */
    public void printCategoricalHeatmap() {
        System.out.print(generateCategoricalHeatmap());
    }
    
    /**
     * Print top N instructions to console
     */
    public void printTopInstructions(int topN) {
        System.out.print(generateTopInstructionsHeatmap(topN));
    }
}
