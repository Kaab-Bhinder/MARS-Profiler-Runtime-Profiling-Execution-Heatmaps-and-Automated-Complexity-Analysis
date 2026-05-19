package mars.tools;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.*;
import mars.simulator.*;
import mars.mips.hardware.*;
import mars.util.*;

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
 * Resource Profiler Tool - displays comprehensive profiling statistics
 * for executed MIPS programs including instruction frequency, register
 * utilization, memory access patterns, and branch statistics.
 * 
 * @author Pete Sanderson and Ken Vollmar
 * @version 2025
 */
public class ResourceProfiler extends AbstractMarsToolAndApplication {
    
    private static String name = "Resource Profiler";
    private static String heading = "MIPS Resource Profiler";
    
    // GUI Components
    private JTabbedPane tabbedPane;
    private JPanel instructionPanel;
    private JPanel registerPanel;
    private JPanel memoryPanel;
    private JPanel branchPanel;
    
    // Table Models
    private DefaultTableModel instructionTableModel;
    private DefaultTableModel registerTableModel;
    private DefaultTableModel memoryTableModel;
    private DefaultTableModel branchTableModel;
    
    // Summary Labels
    private JLabel totalInstructionsLabel;
    private JLabel totalRegistersLabel;
    private JLabel totalMemoryLabel;
    private JLabel branchStatsLabel;
    
    // Color scheme
    private Font tableFont = new Font("Monospaced", Font.PLAIN, 11);
    private Font labelFont = new Font("Arial", Font.BOLD, 12);
    private EmptyBorder standardBorder = new EmptyBorder(4, 4, 4, 4);
    
    // Refresh tracking
    private int lastInstructionCount = 0;
    private JButton refreshButton;
    
    /**
     * No-argument constructor required for MARS tool discovery
     */
    public ResourceProfiler() {
        super(name, heading);
    }
    
    /**
     * Return the tool name for the Tools menu
     */
    public String getName() {
        return name;
    }
    
    /**
     * Build the main display area with tabbed interface
     */
    protected JComponent buildMainDisplayArea() {
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(standardBorder);
        
        // Control panel with refresh button
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        
        refreshButton = new JButton("Refresh Display");
        refreshButton.addActionListener(e -> updateDisplay());
        controlPanel.add(refreshButton);
        
        JLabel helpLabel = new JLabel("Click 'Refresh' or 'Connect' after executing a program to update statistics");
        helpLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        controlPanel.add(helpLabel);
        
        // Tabbed pane
        tabbedPane = new JTabbedPane();
        
        // Create tabs for different profiling views
        tabbedPane.addTab("Instructions", createInstructionPanel());
        tabbedPane.addTab("Registers", createRegisterPanel());
        tabbedPane.addTab("Memory", createMemoryPanel());
        tabbedPane.addTab("Branch Stats", createBranchPanel());
        
        // Add listener to refresh when tab changes
        tabbedPane.addChangeListener(e -> {
            if (ProfilerService.getInstance().getTotalInstructions() > lastInstructionCount) {
                updateDisplay();
                lastInstructionCount = ProfilerService.getInstance().getTotalInstructions();
            }
        });
        
        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        return mainPanel;
    }
    
    /**
     * Create the Instruction profiling panel
     */
    private JComponent createInstructionPanel() {
        instructionPanel = new JPanel(new BorderLayout(5, 5));
        instructionPanel.setBorder(standardBorder);
        
        // Summary section
        JPanel summaryPanel = new JPanel();
        summaryPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        totalInstructionsLabel = new JLabel("Total Instructions: 0");
        totalInstructionsLabel.setFont(labelFont);
        summaryPanel.add(totalInstructionsLabel);
        
        // Table section
        instructionTableModel = new DefaultTableModel(
            new String[]{"Instruction", "Count", "Percent", "Heatmap"}, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable instructionTable = new JTable(instructionTableModel);
        instructionTable.setFont(tableFont);
        instructionTable.setRowHeight(25);
        
        // Set column widths
        instructionTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        instructionTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        instructionTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        instructionTable.getColumnModel().getColumn(3).setPreferredWidth(250);
        
        // Apply custom renderer for heatmap column
        instructionTable.getColumnModel().getColumn(3).setCellRenderer(new HeatmapCellRenderer());
        
        JScrollPane scrollPane = new JScrollPane(instructionTable);
        
        instructionPanel.add(summaryPanel, BorderLayout.NORTH);
        instructionPanel.add(scrollPane, BorderLayout.CENTER);
        
        return instructionPanel;
    }
    
    /**
     * Create the Register utilization panel
     */
    private JComponent createRegisterPanel() {
        registerPanel = new JPanel(new BorderLayout(5, 5));
        registerPanel.setBorder(standardBorder);
        
        // Summary section
        JPanel summaryPanel = new JPanel();
        summaryPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        totalRegistersLabel = new JLabel("Total Reads: 0  |  Total Writes: 0");
        totalRegistersLabel.setFont(labelFont);
        summaryPanel.add(totalRegistersLabel);
        
        // Table section
        registerTableModel = new DefaultTableModel(
            new String[]{"Register", "Reads", "Writes", "Total", "Usage %", "Heatmap"}, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable registerTable = new JTable(registerTableModel);
        registerTable.setFont(tableFont);
        registerTable.setRowHeight(25);
        registerTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        registerTable.getColumnModel().getColumn(1).setPreferredWidth(60);
        registerTable.getColumnModel().getColumn(2).setPreferredWidth(60);
        registerTable.getColumnModel().getColumn(3).setPreferredWidth(60);
        registerTable.getColumnModel().getColumn(4).setPreferredWidth(70);
        registerTable.getColumnModel().getColumn(5).setPreferredWidth(200);
        
        registerTable.getColumnModel().getColumn(5).setCellRenderer(new HeatmapCellRenderer());
        
        JScrollPane scrollPane = new JScrollPane(registerTable);
        
        registerPanel.add(summaryPanel, BorderLayout.NORTH);
        registerPanel.add(scrollPane, BorderLayout.CENTER);
        
        return registerPanel;
    }
    
    /**
     * Create the Memory access panel
     */
    private JComponent createMemoryPanel() {
        memoryPanel = new JPanel(new BorderLayout(5, 5));
        memoryPanel.setBorder(standardBorder);
        
        // Summary section
        JPanel summaryPanel = new JPanel();
        summaryPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        totalMemoryLabel = new JLabel("Total Reads: 0  |  Total Writes: 0  |  Unique Addresses: 0");
        totalMemoryLabel.setFont(labelFont);
        summaryPanel.add(totalMemoryLabel);
        
        // Table section
        memoryTableModel = new DefaultTableModel(
            new String[]{"Address", "Reads", "Writes", "Total", "Heatmap"}, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable memoryTable = new JTable(memoryTableModel);
        memoryTable.setFont(tableFont);
        memoryTable.setRowHeight(25);
        memoryTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        memoryTable.getColumnModel().getColumn(1).setPreferredWidth(70);
        memoryTable.getColumnModel().getColumn(2).setPreferredWidth(70);
        memoryTable.getColumnModel().getColumn(3).setPreferredWidth(70);
        memoryTable.getColumnModel().getColumn(4).setPreferredWidth(200);
        
        memoryTable.getColumnModel().getColumn(4).setCellRenderer(new HeatmapCellRenderer());
        
        JScrollPane scrollPane = new JScrollPane(memoryTable);
        
        memoryPanel.add(summaryPanel, BorderLayout.NORTH);
        memoryPanel.add(scrollPane, BorderLayout.CENTER);
        
        return memoryPanel;
    }
    
    /**
     * Create the Branch statistics panel
     */
    private JComponent createBranchPanel() {
        branchPanel = new JPanel(new BorderLayout(5, 5));
        branchPanel.setBorder(standardBorder);
        
        // Summary section
        JPanel summaryPanel = new JPanel();
        summaryPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        branchStatsLabel = new JLabel("Branch Statistics");
        branchStatsLabel.setFont(labelFont);
        summaryPanel.add(branchStatsLabel);
        
        // Table section
        branchTableModel = new DefaultTableModel(
            new String[]{"Branch Type", "Count", "Percent", "Heatmap"}, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable branchTable = new JTable(branchTableModel);
        branchTable.setFont(tableFont);
        branchTable.setRowHeight(25);
        branchTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        branchTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        branchTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        branchTable.getColumnModel().getColumn(3).setPreferredWidth(200);
        
        branchTable.getColumnModel().getColumn(3).setCellRenderer(new HeatmapCellRenderer());
        
        JScrollPane scrollPane = new JScrollPane(branchTable);
        
        branchPanel.add(summaryPanel, BorderLayout.NORTH);
        branchPanel.add(scrollPane, BorderLayout.CENTER);
        
        return branchPanel;
    }
    
    /**
     * Override reset to clear display when user clicks Reset button
     */
    public void reset() {
        clearDisplay();
        lastInstructionCount = 0;
    }
    
    /**
     * Override to register as observer for execution changes
     */
    protected void addAsObserver() {
        addAsObserver(Memory.textBaseAddress, Memory.textLimitAddress);
    }
    
    /**
     * Process memory updates and refresh display periodically
     */
    protected void processMIPSUpdate(Observable resource, AccessNotice notice) {
        // Update display if instruction count has changed significantly
        int currentCount = ProfilerService.getInstance().getTotalInstructions();
        if (currentCount > lastInstructionCount && currentCount % 10 == 0) {
            updateDisplay();
            lastInstructionCount = currentCount;
        }
    }
    
    /**
     * Clear all table data
     */
    private void clearDisplay() {
        while (instructionTableModel.getRowCount() > 0) {
            instructionTableModel.removeRow(0);
        }
        while (registerTableModel.getRowCount() > 0) {
            registerTableModel.removeRow(0);
        }
        while (memoryTableModel.getRowCount() > 0) {
            memoryTableModel.removeRow(0);
        }
        while (branchTableModel.getRowCount() > 0) {
            branchTableModel.removeRow(0);
        }
        
        totalInstructionsLabel.setText("Total Instructions: 0");
        totalRegistersLabel.setText("Total Reads: 0  |  Total Writes: 0");
        totalMemoryLabel.setText("Total Reads: 0  |  Total Writes: 0  |  Unique Addresses: 0");
        branchStatsLabel.setText("Branch Statistics");
    }
    
    /**
     * Override to update display after execution completes
     */
    protected void updateDisplay() {
        updateInstructionDisplay();
        updateRegisterDisplay();
        updateMemoryDisplay();
        updateBranchDisplay();
    }
    
    /**
     * Update instruction profiling table
     */
    private void updateInstructionDisplay() {
        ProfilerService profiler = ProfilerService.getInstance();
        int totalInstructions = profiler.getTotalInstructions();
        
        clearTableModel(instructionTableModel);
        
        if (totalInstructions == 0) {
            totalInstructionsLabel.setText("Total Instructions: 0");
            return;
        }
        
        // Get sorted instruction data
        HashMap<String, Integer> instCounts = profiler.getInstructionCounts();
        java.util.List<java.util.Map.Entry<String, Integer>> sortedList = 
            new ArrayList<java.util.Map.Entry<String, Integer>>(instCounts.entrySet());
        sortedList.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        // Find max count for heatmap normalization
        int maxCount = sortedList.isEmpty() ? 1 : sortedList.get(0).getValue();
        
        // Add rows to table
        for (java.util.Map.Entry<String, Integer> entry : sortedList) {
            String instruction = entry.getKey();
            int count = entry.getValue();
            double percent = (count * 100.0) / totalInstructions;
            // Normalize heatmap intensity relative to total instructions (0-100%).
            // Fallback to maxCount if totalInstructions is zero (shouldn't happen).
            double intensity = totalInstructions > 0 ? (count * 100.0) / totalInstructions
                                                      : (count * 100.0) / maxCount;
            
            instructionTableModel.addRow(new Object[]{
                instruction,
                count,
                String.format("%.2f%%", percent),
                intensity  // Pass intensity for heatmap rendering
            });
        }
        
        totalInstructionsLabel.setText(
            "Total Instructions: " + totalInstructions);
    }
    
    /**
     * Update register profiling table
     */
    private void updateRegisterDisplay() {
        ProfilerService profiler = ProfilerService.getInstance();
        int totalReads = profiler.getTotalRegisterReads();
        int totalWrites = profiler.getTotalRegisterWrites();
        
        clearTableModel(registerTableModel);
        
        if (totalReads == 0 && totalWrites == 0) {
            totalRegistersLabel.setText("Total Reads: 0  |  Total Writes: 0");
            return;
        }
        
        // Get register data
        HashMap<Integer, Integer> reads = profiler.getRegisterReads();
        HashMap<Integer, Integer> writes = profiler.getRegisterWrites();
        
        // Combine for sorting
        HashMap<Integer, Integer> combined = new HashMap<Integer, Integer>();
        for (Integer reg : reads.keySet()) {
            combined.put(reg, reads.get(reg));
        }
        for (Integer reg : writes.keySet()) {
            int total = combined.getOrDefault(reg, 0) + writes.get(reg);
            combined.put(reg, total);
        }
        
        // Sort by total accesses
        java.util.List<java.util.Map.Entry<Integer, Integer>> sortedList = 
            new ArrayList<java.util.Map.Entry<Integer, Integer>>(combined.entrySet());
        sortedList.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        // Add rows
        int totalAccesses = totalReads + totalWrites;
        int maxAccesses = sortedList.isEmpty() ? 1 : sortedList.get(0).getValue();
        
        for (java.util.Map.Entry<Integer, Integer> entry : sortedList) {
            int regNum = entry.getKey();
            int regReads = reads.getOrDefault(regNum, 0);
            int regWrites = writes.getOrDefault(regNum, 0);
            int regTotal = regReads + regWrites;
            double usagePercent = (regTotal * 100.0) / totalAccesses;
            // Normalize by total accesses so heatmap shows proportion of whole.
            double intensity = totalAccesses > 0 ? (regTotal * 100.0) / totalAccesses
                                                  : (regTotal * 100.0) / maxAccesses;  // Fallback
            
            registerTableModel.addRow(new Object[]{
                getRegisterName(regNum),
                regReads,
                regWrites,
                regTotal,
                String.format("%.2f%%", usagePercent),
                intensity
            });
        }
        
        totalRegistersLabel.setText(
            "Total Reads: " + totalReads + "  |  Total Writes: " + totalWrites);
    }
    
    /**
     * Update memory access table
     */
    private void updateMemoryDisplay() {
        ProfilerService profiler = ProfilerService.getInstance();
        int totalReads = profiler.getTotalMemoryReads();
        int totalWrites = profiler.getTotalMemoryWrites();
        
        clearTableModel(memoryTableModel);
        
        if (totalReads == 0 && totalWrites == 0) {
            totalMemoryLabel.setText("Total Reads: 0  |  Total Writes: 0  |  Unique Addresses: 0");
            return;
        }
        
        // Get memory data
        HashMap<Integer, Integer> reads = profiler.getMemoryReads();
        HashMap<Integer, Integer> writes = profiler.getMemoryWrites();
        
        // Combine for sorting
        HashMap<Integer, Integer> combined = new HashMap<Integer, Integer>();
        for (Integer addr : reads.keySet()) {
            combined.put(addr, reads.get(addr));
        }
        for (Integer addr : writes.keySet()) {
            int total = combined.getOrDefault(addr, 0) + writes.get(addr);
            combined.put(addr, total);
        }
        
        // Sort by total accesses (top 30 only)
        java.util.List<java.util.Map.Entry<Integer, Integer>> sortedList = 
            new ArrayList<java.util.Map.Entry<Integer, Integer>>(combined.entrySet());
        sortedList.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        int displayed = 0;
        int maxMemoryAccess = sortedList.isEmpty() ? 1 : sortedList.get(0).getValue();
        
        for (java.util.Map.Entry<Integer, Integer> entry : sortedList) {
            if (displayed >= 30) break;
            
            int addr = entry.getKey();
            int addrReads = reads.getOrDefault(addr, 0);
            int addrWrites = writes.getOrDefault(addr, 0);
            int addrTotal = addrReads + addrWrites;
            // Normalize by total memory accesses (reads+writes) where possible.
            int totalMemAccesses = totalReads + totalWrites;
            double intensity = totalMemAccesses > 0 ? (addrTotal * 100.0) / totalMemAccesses
                                                      : (addrTotal * 100.0) / maxMemoryAccess;
            
            memoryTableModel.addRow(new Object[]{
                String.format("0x%08X", addr),
                addrReads,
                addrWrites,
                addrTotal,
                intensity
            });
            
            displayed++;
        }
        
        totalMemoryLabel.setText(
            "Total Reads: " + totalReads + "  |  Total Writes: " + totalWrites + 
            "  |  Unique Addresses: " + combined.size());
    }
    
    /**
     * Update branch statistics table
     */
    private void updateBranchDisplay() {
        ProfilerService profiler = ProfilerService.getInstance();
        HashMap<String, Integer> instCounts = profiler.getInstructionCounts();
        
        clearTableModel(branchTableModel);
        
        int totalInstructions = profiler.getTotalInstructions();
        if (totalInstructions == 0) {
            return;
        }
        
        // Extract branch statistics
        int unconditionalBranches = instCounts.getOrDefault("j", 0) + 
                                    instCounts.getOrDefault("jal", 0);
        int conditionalBranches = instCounts.getOrDefault("beq", 0) +
                                 instCounts.getOrDefault("bne", 0) +
                                 instCounts.getOrDefault("blt", 0) +
                                 instCounts.getOrDefault("ble", 0) +
                                 instCounts.getOrDefault("bgt", 0) +
                                 instCounts.getOrDefault("bge", 0);
        int jumps = instCounts.getOrDefault("jr", 0) +
                   instCounts.getOrDefault("jalr", 0);
        
        int totalBranches = unconditionalBranches + conditionalBranches + jumps;
        int maxBranchType = Math.max(Math.max(unconditionalBranches, conditionalBranches), jumps);
        if (maxBranchType == 0) maxBranchType = 1;
        
        // Add branch statistics
        if (unconditionalBranches > 0) {
            // Normalize branch heatmap by totalBranches to avoid >100% totals.
            double intensity = totalBranches > 0 ? (unconditionalBranches * 100.0) / totalBranches
                                                 : (unconditionalBranches * 100.0) / maxBranchType;
            branchTableModel.addRow(new Object[]{
                "Unconditional Branches (j, jal)",
                unconditionalBranches,
                String.format("%.2f%%", (unconditionalBranches * 100.0) / totalInstructions),
                intensity
            });
        }
        
        if (conditionalBranches > 0) {
            double intensity = totalBranches > 0 ? (conditionalBranches * 100.0) / totalBranches
                                                 : (conditionalBranches * 100.0) / maxBranchType;
            branchTableModel.addRow(new Object[]{
                "Conditional Branches (beq, bne, etc)",
                conditionalBranches,
                String.format("%.2f%%", (conditionalBranches * 100.0) / totalInstructions),
                intensity
            });
        }
        
        if (jumps > 0) {
            double intensity = totalBranches > 0 ? (jumps * 100.0) / totalBranches
                                                 : (jumps * 100.0) / maxBranchType;
            branchTableModel.addRow(new Object[]{
                "Register Jumps (jr, jalr)",
                jumps,
                String.format("%.2f%%", (jumps * 100.0) / totalInstructions),
                intensity
            });
        }
        
        if (totalBranches > 0) {
            double intensity = totalBranches > 0 ? (totalBranches * 100.0) / totalBranches
                                                 : (totalBranches * 100.0) / maxBranchType;
            branchTableModel.addRow(new Object[]{
                "--- TOTAL BRANCHES ---",
                totalBranches,
                String.format("%.2f%%", (totalBranches * 100.0) / totalInstructions),
                intensity
            });
        }
        
        branchStatsLabel.setText(
            "Branch Statistics - Total: " + totalBranches + " (" + 
            String.format("%.2f%%", (totalBranches * 100.0) / totalInstructions) + 
            " of all instructions)");
    }
    
    /**
     * Get human-readable register name
     */
    private String getRegisterName(int regNum) {
        if (regNum == 32) return "PC";
        if (regNum == 33) return "HI";
        if (regNum == 34) return "LO";
        if (regNum >= 0 && regNum <= 31) {
            String[] regNames = {"$zero", "$at", "$v0", "$v1", "$a0", "$a1", "$a2", "$a3",
                                 "$t0", "$t1", "$t2", "$t3", "$t4", "$t5", "$t6", "$t7",
                                 "$s0", "$s1", "$s2", "$s3", "$s4", "$s5", "$s6", "$s7",
                                 "$t8", "$t9", "$k0", "$k1", "$gp", "$sp", "$fp", "$ra"};
            return regNames[regNum];
        }
        return "REG" + regNum;
    }
    
    /**
     * Clear all rows from a table model
     */
    private void clearTableModel(DefaultTableModel model) {
        while (model.getRowCount() > 0) {
            model.removeRow(0);
        }
    }
    
    /**
     * Custom cell renderer for heatmap visualization
     * Shows color-coded intensity bars for instruction frequencies
     */
    private static class HeatmapCellRenderer extends JPanel implements TableCellRenderer {
        private double intensity = 0;  // 0-100
        private Color barColor;
        private Font labelFont = new Font("Arial", Font.BOLD, 9);
        
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            if (value instanceof Number) {
                intensity = ((Number) value).doubleValue();
            }
            
            // Determine color based on intensity
            if (intensity < 20) {
                barColor = new Color(144, 238, 144);  // Light green
            } else if (intensity < 40) {
                barColor = new Color(255, 255, 150);  // Yellow
            } else if (intensity < 60) {
                barColor = new Color(255, 200, 100);  // Orange
            } else if (intensity < 80) {
                barColor = new Color(255, 150, 100);  // Dark orange
            } else {
                barColor = new Color(255, 80, 80);    // Red
            }
            
            setBackground(isSelected ? new Color(200, 200, 200) : Color.WHITE);
            return this;
        }
        
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int width = getWidth();
            int height = getHeight();
            int barWidth = (int) (width * intensity / 100.0);
            
            // Draw background
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, width, height);
            
            // Draw colored bar
            g2.setColor(barColor);
            g2.fillRect(0, 4, barWidth, height - 8);
            
            // Draw border
            g2.setColor(Color.GRAY);
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRect(0, 4, width - 1, height - 9);
            
            // Draw percentage text
            g2.setColor(Color.BLACK);
            g2.setFont(labelFont);
            String text = String.format("%.0f%%", intensity);
            FontMetrics fm = g2.getFontMetrics();
            int textX = 5;
            int textY = (height - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(text, textX, textY);
        }
    }
}
