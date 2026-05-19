package mars.tools;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import mars.*;
import mars.mips.hardware.*;
import mars.simulator.*;

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
 * Benchmark Comparison Tool
 * Compares execution efficiency of two different MIPS programs/algorithms
 * Tracks instruction counts and generates comparison metrics
 * 
 * @author Pete Sanderson and Ken Vollmar
 * @version 2025
 */
public class BenchmarkComparison extends AbstractMarsToolAndApplication {
    
    private static final String VERSION = "Version 1.0";
    private static final String HEADING = "Benchmark Comparison Tool";
    private static final String DESCRIPTION = "Compare two MIPS algorithms side-by-side";
    
    private JPanel contentPanel;
    private JPanel controlPanel;
    private JPanel resultsPanel;
    private JPanel graphPanel;
    
    private JTextArea algo1TextArea;
    private JTextArea algo2TextArea;
    private JSpinner inputSizeSpinner;
    
    private JButton runBenchmarkButton;
    private JButton clearResultsButton;
    
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    
    private BenchmarkGraphPanel graphComponent;
    
    private ArrayList<BenchmarkResult> results;
    
    /**
     * Constructor - required for MARS tool discovery
     */
    public BenchmarkComparison() {
        super(HEADING, DESCRIPTION);
        results = new ArrayList<>();
    }
    
    /**
     * Initialize the tool
     */
    @Override
    protected void addAsObserver() {
        // No observers needed for this tool
    }
    
    @Override
    protected void deleteAsObserver() {
        // No observers to remove
    }
    
    @Override
    protected JComponent buildMainDisplayArea() {
        contentPanel = new JPanel(new BorderLayout());
        
        // Control panel - top
        controlPanel = createControlPanel();
        contentPanel.add(controlPanel, BorderLayout.NORTH);
        
        // Results panel - center
        resultsPanel = createResultsPanel();
        contentPanel.add(resultsPanel, BorderLayout.CENTER);
        
        // Graph panel - bottom
        graphPanel = createGraphPanel();
        contentPanel.add(graphPanel, BorderLayout.SOUTH);
        
        return contentPanel;
    }
    
    /**
     * Create control panel with input fields
     */
    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Benchmark Configuration"));
        
        // Top section - input size
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inputPanel.add(new JLabel("Input Size:"));
        inputSizeSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 1000, 1));
        inputPanel.add(inputSizeSpinner);
        
        panel.add(inputPanel, BorderLayout.NORTH);
        
        // Middle section - algo text areas side by side
        JPanel algoPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        algoPanel.setBorder(BorderFactory.createTitledBorder("Algorithms"));
        
        // Algorithm 1
        JPanel algo1Panel = new JPanel(new BorderLayout());
        algo1Panel.add(new JLabel("Algorithm 1:"), BorderLayout.NORTH);
        algo1TextArea = new JTextArea(3, 20);
        algo1TextArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        algo1TextArea.setToolTipText("Enter MIPS program 1 or leave empty to use current file");
        algo1Panel.add(new JScrollPane(algo1TextArea), BorderLayout.CENTER);
        algoPanel.add(algo1Panel);
        
        // Algorithm 2
        JPanel algo2Panel = new JPanel(new BorderLayout());
        algo2Panel.add(new JLabel("Algorithm 2:"), BorderLayout.NORTH);
        algo2TextArea = new JTextArea(3, 20);
        algo2TextArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        algo2TextArea.setToolTipText("Enter MIPS program 2 or leave empty to use current file");
        algo2Panel.add(new JScrollPane(algo2TextArea), BorderLayout.CENTER);
        algoPanel.add(algo2Panel);
        
        panel.add(algoPanel, BorderLayout.CENTER);
        
        // Bottom section - buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        runBenchmarkButton = new JButton("Run Benchmark");
        runBenchmarkButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runBenchmark();
            }
        });
        buttonPanel.add(runBenchmarkButton);
        
        clearResultsButton = new JButton("Clear Results");
        clearResultsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearResults();
            }
        });
        buttonPanel.add(clearResultsButton);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * Create results display panel
     */
    private JPanel createResultsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Comparison Results"));
        
        // Table setup
        String[] columnNames = {"Input Size", "Algorithm 1 (Instructions)", "Algorithm 2 (Instructions)", "Difference", "Winner", "Ratio"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        resultsTable = new JTable(tableModel);
        resultsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        resultsTable.setDefaultRenderer(Object.class, new ResultTableRenderer());
        
        panel.add(new JScrollPane(resultsTable), BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Create graph display panel
     */
    private JPanel createGraphPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Performance Graph"));
        panel.setPreferredSize(new Dimension(800, 200));
        
        graphComponent = new BenchmarkGraphPanel();
        panel.add(graphComponent, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Run benchmark comparison
     */
    private void runBenchmark() {
        // TODO: Implement benchmark execution
        // 1. Parse input size
        // 2. Run algorithm 1 with that size
        // 3. Run algorithm 2 with that size
        // 4. Record instruction counts
        // 5. Update table
        // 6. Update graph
        
        JOptionPane.showMessageDialog(this, 
            "Benchmark execution will be implemented to:\n" +
            "1. Compile and run Algorithm 1\n" +
            "2. Compile and run Algorithm 2\n" +
            "3. Compare instruction counts\n" +
            "4. Display results and graph",
            "Benchmark Tool - Coming Soon",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Clear all results
     */
    private void clearResults() {
        results.clear();
        tableModel.setRowCount(0);
        graphComponent.clearData();
        graphComponent.repaint();
    }
    
    /**
     * Store a benchmark result
     */
    private void addResult(int inputSize, long algo1Instructions, long algo2Instructions) {
        BenchmarkResult result = new BenchmarkResult(inputSize, algo1Instructions, algo2Instructions);
        results.add(result);
        
        // Update table
        Object[] row = {
            inputSize,
            algo1Instructions,
            algo2Instructions,
            Math.abs(algo1Instructions - algo2Instructions),
            algo1Instructions < algo2Instructions ? "Algo 1" : "Algo 2",
            String.format("%.2f", (double) algo1Instructions / algo2Instructions)
        };
        tableModel.addRow(row);
        
        // Update graph
        graphComponent.addData(result);
    }
    
    @Override
    public String getName() {
        return "Benchmark Comparison Tool";
    }
    
    /**
     * Benchmark result data class
     */
    private static class BenchmarkResult {
        int inputSize;
        long algo1Instructions;
        long algo2Instructions;
        
        BenchmarkResult(int inputSize, long algo1, long algo2) {
            this.inputSize = inputSize;
            this.algo1Instructions = algo1;
            this.algo2Instructions = algo2;
        }
    }
    
    /**
     * Custom renderer for result table
     */
    private static class ResultTableRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, 
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            if (!isSelected) {
                if (column == 4) { // Winner column
                    String winner = (String) value;
                    if ("Algo 1".equals(winner)) {
                        c.setBackground(new Color(200, 255, 200)); // Light green
                    } else if ("Algo 2".equals(winner)) {
                        c.setBackground(new Color(255, 200, 200)); // Light red
                    }
                } else {
                    c.setBackground(Color.WHITE);
                }
            }
            
            return c;
        }
    }
    
    /**
     * Graph display component for benchmark results
     */
    private static class BenchmarkGraphPanel extends JPanel {
        private ArrayList<BenchmarkResult> data;
        
        BenchmarkGraphPanel() {
            this.data = new ArrayList<>();
            setBackground(Color.WHITE);
        }
        
        void addData(BenchmarkResult result) {
            data.add(result);
            repaint();
        }
        
        void clearData() {
            data.clear();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            if (data.isEmpty()) {
                g.setColor(Color.GRAY);
                g.drawString("Run benchmark to see graph", 20, getHeight() / 2);
                return;
            }
            
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Calculate scales
            int margin = 50;
            int width = getWidth() - 2 * margin;
            int height = getHeight() - 2 * margin;
            
            // Find max values for scaling
            long maxInputSize = 1;
            long maxInstructions = 1;
            
            for (BenchmarkResult r : data) {
                if (r.inputSize > maxInputSize) maxInputSize = r.inputSize;
                if (r.algo1Instructions > maxInstructions) maxInstructions = r.algo1Instructions;
                if (r.algo2Instructions > maxInstructions) maxInstructions = r.algo2Instructions;
            }
            
            // Draw axes
            g2d.setColor(Color.BLACK);
            g2d.drawLine(margin, getHeight() - margin, getWidth() - margin, getHeight() - margin);
            g2d.drawLine(margin, margin, margin, getHeight() - margin);
            
            // Draw axis labels
            g2d.drawString("Input Size", getWidth() / 2, getHeight() - 10);
            g2d.rotate(-Math.PI / 2);
            g2d.drawString("Instructions", -getHeight() / 2, 15);
            g2d.rotate(Math.PI / 2);
            
            // Draw data points and lines
            if (data.size() > 1) {
                // Algorithm 1 - Blue
                g2d.setColor(Color.BLUE);
                g2d.setStroke(new BasicStroke(2));
                for (int i = 0; i < data.size() - 1; i++) {
                    BenchmarkResult r1 = data.get(i);
                    BenchmarkResult r2 = data.get(i + 1);
                    
                    int x1 = margin + (int) (r1.inputSize * width / maxInputSize);
                    int y1 = getHeight() - margin - (int) (r1.algo1Instructions * height / maxInstructions);
                    int x2 = margin + (int) (r2.inputSize * width / maxInputSize);
                    int y2 = getHeight() - margin - (int) (r2.algo1Instructions * height / maxInstructions);
                    
                    g2d.drawLine(x1, y1, x2, y2);
                    g2d.fillOval(x1 - 3, y1 - 3, 6, 6);
                }
                
                // Algorithm 2 - Red
                g2d.setColor(Color.RED);
                for (int i = 0; i < data.size() - 1; i++) {
                    BenchmarkResult r1 = data.get(i);
                    BenchmarkResult r2 = data.get(i + 1);
                    
                    int x1 = margin + (int) (r1.inputSize * width / maxInputSize);
                    int y1 = getHeight() - margin - (int) (r1.algo2Instructions * height / maxInstructions);
                    int x2 = margin + (int) (r2.inputSize * width / maxInputSize);
                    int y2 = getHeight() - margin - (int) (r2.algo2Instructions * height / maxInstructions);
                    
                    g2d.drawLine(x1, y1, x2, y2);
                    g2d.fillOval(x1 - 3, y1 - 3, 6, 6);
                }
                
                // Legend
                g2d.setColor(Color.BLUE);
                g2d.fillRect(10, 10, 12, 12);
                g2d.setColor(Color.BLACK);
                g2d.drawString("Algorithm 1", 25, 20);
                
                g2d.setColor(Color.RED);
                g2d.fillRect(10, 30, 12, 12);
                g2d.setColor(Color.BLACK);
                g2d.drawString("Algorithm 2", 25, 40);
            }
        }
    }
}
