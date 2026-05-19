package mars.tools;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.geom.*;
import mars.simulator.*;
import mars.Globals;

/**
 * Benchmark Comparison Tool for MARS
 * Compares execution metrics and displays time complexity analysis with graphs
 * 
 * @author Pete Sanderson and Ken Vollmar
 * @version 2025
 */
public class BenchmarkComparatorTool extends AbstractMarsToolAndApplication {
    
    private static final String VERSION = "1.0";
    private static final String HEADING = "Benchmark Comparator";
    private static final String TOOLTIP = "Compare algorithms with time complexity analysis";
    
    private JTabbedPane tabbedPane;
    private JPanel comparisonPanel;
    private JPanel graphPanel;
    private JTextArea comparisonDisplay;
    private JButton captureProgram1Button;
    private JButton captureProgram2Button;
    private JButton compareButton;
    private JButton resetButton;
    private JLabel program1NameLabel;
    private JLabel program2NameLabel;
    private JLabel statusLabel;
    
    private String program1Name = "Program 1 (not captured)";
    private String program2Name = "Program 2 (not captured)";
    private boolean program1Captured = false;
    private boolean program2Captured = false;
    private int program1Instructions = 0;
    private int program2Instructions = 0;
    
    public BenchmarkComparatorTool() {
        super(HEADING, TOOLTIP);
    }
    
    @Override
    public String getName() {
        return "Benchmark Comparator";
    }
    
    @Override
    protected void addAsObserver() {
        Simulator.getInstance().addObserver(this);
    }
    
    @Override
    protected void deleteAsObserver() {
        Simulator.getInstance().deleteObserver(this);
    }
    
    @Override
    protected JComponent buildMainDisplayArea() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        tabbedPane = new JTabbedPane();
        comparisonPanel = buildComparisonPanel();
        graphPanel = buildGraphPanel();
        
        tabbedPane.addTab("Comparison", comparisonPanel);
        tabbedPane.addTab("Graph", graphPanel);
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        return mainPanel;
    }
    
    private JPanel buildComparisonPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JPanel controlPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        controlPanel.setBorder(new TitledBorder("Capture Metrics"));
        
        program1NameLabel = new JLabel(program1Name);
        program1NameLabel.setFont(new Font("Arial", Font.BOLD, 12));
        program1NameLabel.setForeground(new Color(0, 100, 200));
        
        captureProgram1Button = new JButton("Capture Program 1");
        captureProgram1Button.addActionListener(e -> captureProgram1());
        controlPanel.add(program1NameLabel);
        controlPanel.add(captureProgram1Button);
        
        program2NameLabel = new JLabel(program2Name);
        program2NameLabel.setFont(new Font("Arial", Font.BOLD, 12));
        program2NameLabel.setForeground(new Color(200, 100, 0));
        
        captureProgram2Button = new JButton("Capture Program 2");
        captureProgram2Button.addActionListener(e -> captureProgram2());
        controlPanel.add(program2NameLabel);
        controlPanel.add(captureProgram2Button);
        
        panel.add(controlPanel, BorderLayout.NORTH);
        
        comparisonDisplay = new JTextArea();
        comparisonDisplay.setFont(new Font("Courier New", Font.PLAIN, 11));
        comparisonDisplay.setEditable(false);
        comparisonDisplay.setBackground(new Color(240, 240, 240));
        comparisonDisplay.setText("Execute two programs to compare\n");
        
        JScrollPane scrollPane = new JScrollPane(comparisonDisplay);
        scrollPane.setBorder(new TitledBorder("Results"));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        
        compareButton = new JButton("Compare & Analyze");
        compareButton.setEnabled(false);
        compareButton.addActionListener(e -> showComparison());
        buttonPanel.add(compareButton);
        
        resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> resetComparison());
        buttonPanel.add(resetButton);
        
        statusLabel = new JLabel("Ready: Execute programs");
        buttonPanel.add(statusLabel);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel buildGraphPanel() {
        return new GraphVisualizationPanel();
    }
    
    private void captureProgram1() {
        ProfilerService profiler = ProfilerService.getInstance();
        program1Instructions = profiler.getTotalInstructions();
        program1Name = "Program 1: " + program1Instructions + " instr";
        program1NameLabel.setText(program1Name);
        program1Captured = true;
        statusLabel.setText("Program 1 captured");
        updateCompareButton();
        updateGraph();
    }
    
    private void captureProgram2() {
        ProfilerService profiler = ProfilerService.getInstance();
        program2Instructions = profiler.getTotalInstructions();
        program2Name = "Program 2: " + program2Instructions + " instr";
        program2NameLabel.setText(program2Name);
        program2Captured = true;
        statusLabel.setText("Program 2 captured");
        updateCompareButton();
        updateGraph();
    }
    
    private void showComparison() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("========================================\n");
        sb.append("  BENCHMARK COMPARISON\n");
        sb.append("========================================\n\n");
        
        sb.append("Program 1: ").append(program1Instructions).append(" instructions\n");
        sb.append("Program 2: ").append(program2Instructions).append(" instructions\n\n");
        
        double ratio = (double) program2Instructions / program1Instructions;
        if (ratio > 1) {
            sb.append("Program 2 is ").append(String.format("%.2f", ratio)).append("x SLOWER\n\n");
        } else {
            sb.append("Program 2 is ").append(String.format("%.2f", 1/ratio)).append("x FASTER\n\n");
        }
        
        String complexity1 = detectComplexity(program1Instructions);
        String complexity2 = detectComplexity(program2Instructions);
        
        sb.append("========================================\n");
        sb.append("  TIME COMPLEXITY ANALYSIS\n");
        sb.append("========================================\n\n");
        
        sb.append("Program 1 Complexity: ").append(complexity1).append("\n");
        sb.append("Program 2 Complexity: ").append(complexity2).append("\n\n");
        
        if (ratio > 2.0) {
            sb.append("⚠️  Program 2 is significantly slower\n");
            sb.append("    Possibly different algorithm complexity class\n");
        } else if (ratio < 0.5) {
            sb.append("✓ Program 2 is significantly faster\n");
            sb.append("    Better algorithm or better optimizations\n");
        }
        
        sb.append("\n========================================\n");
        sb.append("Recommendation: Use ");
        sb.append(program1Instructions < program2Instructions ? "Program 1" : "Program 2");
        sb.append(" for better performance\n");
        sb.append("========================================\n");
        
        comparisonDisplay.setText(sb.toString());
    }
    
    private String detectComplexity(int instructions) {
        if (instructions < 100) return "O(1) - Constant";
        if (instructions < 300) return "O(log n) - Logarithmic";
        if (instructions < 800) return "O(n) - Linear";
        if (instructions < 2000) return "O(n log n) - Linearithmic";
        if (instructions < 5000) return "O(n²) - Quadratic";
        return "O(n²) or worse";
    }
    
    private int getComplexityClass(int instructions) {
        if (instructions < 100) return 0; // O(1)
        if (instructions < 300) return 1; // O(log n)
        if (instructions < 800) return 2; // O(n)
        if (instructions < 2000) return 3; // O(n log n)
        if (instructions < 5000) return 4; // O(n²)
        return 5; // O(n³) or worse
    }
    
    private void updateGraph() {
        if (graphPanel instanceof GraphVisualizationPanel) {
            ((GraphVisualizationPanel) graphPanel).updateData(program1Instructions, program2Instructions);
            graphPanel.repaint();
        }
    }
    
    private void resetComparison() {
        program1Name = "Program 1 (not captured)";
        program2Name = "Program 2 (not captured)";
        program1NameLabel.setText(program1Name);
        program2NameLabel.setText(program2Name);
        program1Captured = false;
        program2Captured = false;
        program1Instructions = 0;
        program2Instructions = 0;
        comparisonDisplay.setText("Execute two programs to compare\n");
        statusLabel.setText("Ready: Execute programs");
        updateCompareButton();
        updateGraph();
    }
    
    private void updateCompareButton() {
        compareButton.setEnabled(program1Captured && program2Captured);
    }
    
    private class GraphVisualizationPanel extends JPanel {
        private int prog1Value = 0;
        private int prog2Value = 0;
        private int[] prog1Curve = new int[31];
        private int[] prog2Curve = new int[31];
        private int prog1Complexity = 0;
        private int prog2Complexity = 0;
        private static final int BASE_INPUT_SIZE = 7;
        
        GraphVisualizationPanel() {
            setBackground(Color.WHITE);
        }
        
        public void updateData(int p1, int p2) {
            this.prog1Value = p1;
            this.prog2Value = p2;
            this.prog1Complexity = getComplexityClass(p1);
            this.prog2Complexity = getComplexityClass(p2);
            calculateCurves();
        }
        
        private void calculateCurves() {
            // Calculate instruction counts for input sizes 1-30
            // Calibrate using current values at BASE_INPUT_SIZE (7)
            for (int n = 1; n <= 30; n++) {
                prog1Curve[n] = (int) projectInstructions(prog1Value, prog1Complexity, n);
                prog2Curve[n] = (int) projectInstructions(prog2Value, prog2Complexity, n);
            }
        }
        
        private double projectInstructions(int baseValue, int complexity, int inputSize) {
            // Project instruction count based on complexity class
            // baseValue is at inputSize=7
            double ratio = (double) inputSize / BASE_INPUT_SIZE;
            
            switch (complexity) {
                case 0: // O(1)
                    return baseValue;
                case 1: // O(log n)
                    return baseValue * (Math.log(inputSize) / Math.log(BASE_INPUT_SIZE));
                case 2: // O(n)
                    return baseValue * ratio;
                case 3: // O(n log n)
                    return baseValue * ratio * (Math.log(inputSize) / Math.log(BASE_INPUT_SIZE));
                case 4: // O(n²)
                    return baseValue * ratio * ratio;
                case 5: // O(n³)
                    return baseValue * ratio * ratio * ratio;
                default:
                    return baseValue;
            }
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            if (prog1Value == 0 || prog2Value == 0) {
                g.setFont(new Font("Arial", Font.BOLD, 14));
                g.setColor(new Color(100, 100, 100));
                g.drawString("Run both programs to see time complexity graph", 30, 50);
                return;
            }
            
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            int width = getWidth();
            int height = getHeight();
            int margin = 70;
            int graphWidth = width - 2 * margin;
            int graphHeight = height - 2 * margin;
            
            // Draw title
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.setColor(Color.BLACK);
            g2d.drawString("Time Complexity Growth Curves", margin, 25);
            
            // Draw grid
            drawGrid(g2d, margin, height, graphWidth, graphHeight);
            
            // Calculate max for scaling
            int maxInstructions = 0;
            for (int n = 1; n <= 30; n++) {
                maxInstructions = Math.max(maxInstructions, Math.max(prog1Curve[n], prog2Curve[n]));
            }
            maxInstructions = (int) (maxInstructions * 1.1); // 10% padding
            
            double xScale = (double) graphWidth / 30;
            double yScale = (double) graphHeight / maxInstructions;
            
            // Draw axes
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(margin, height - margin, margin, margin);
            g2d.drawLine(margin, height - margin, width - margin, height - margin);
            
            // Draw axis labels
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.drawString("Input Size (n)", width / 2 - 40, height - 20);
            g2d.rotate(-Math.PI / 2);
            g2d.drawString("Instructions", -height / 2 - 30, 20);
            g2d.rotate(Math.PI / 2);
            
            // Draw curves
            drawCurve(g2d, prog1Curve, margin, height - margin, xScale, yScale, new Color(0, 100, 200), 3);
            drawCurve(g2d, prog2Curve, margin, height - margin, xScale, yScale, new Color(255, 140, 0), 3);
            
            // Draw axis tick marks and labels
            g2d.setFont(new Font("Arial", Font.PLAIN, 10));
            for (int n = 0; n <= 30; n += 5) {
                int x = (int) (margin + n * xScale);
                g2d.drawLine(x, height - margin, x, height - margin + 5);
                if (n == 0 || n == 15 || n == 30) {
                    g2d.drawString(String.valueOf(n), x - 10, height - margin + 20);
                }
            }
            
            int yTick = maxInstructions / 5;
            for (int i = 0; i <= 5; i++) {
                int y = (int) (height - margin - i * graphHeight / 5);
                g2d.drawLine(margin - 5, y, margin, y);
                g2d.drawString(String.valueOf(i * yTick), margin - 50, y + 4);
            }
            
            // Draw legend
            drawLegend(g2d, width, margin);
        }
        
        private void drawGrid(Graphics2D g2d, int margin, int height, int graphWidth, int graphHeight) {
            g2d.setColor(new Color(230, 230, 230));
            g2d.setStroke(new BasicStroke(1));
            
            // Vertical grid lines
            for (int i = 0; i <= 30; i += 5) {
                int x = (int) (margin + i * graphWidth / 30.0);
                g2d.drawLine(x, margin, x, height - margin);
            }
            
            // Horizontal grid lines
            for (int i = 1; i < 5; i++) {
                int y = (int) (height - margin - i * graphHeight / 5.0);
                g2d.drawLine(margin, y, margin + graphWidth, y);
            }
        }
        
        private void drawCurve(Graphics2D g2d, int[] curve, int baseX, int baseY, 
                              double xScale, double yScale, Color color, int thickness) {
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            
            Path2D path = new Path2D.Double();
            boolean first = true;
            for (int n = 1; n <= 30; n++) {
                int x = (int) (baseX + n * xScale);
                int y = (int) (baseY - curve[n] * yScale);
                
                if (first) {
                    path.moveTo(x, y);
                    first = false;
                } else {
                    path.lineTo(x, y);
                }
            }
            g2d.draw(path);
            
            // Draw points
            g2d.fillOval(baseX + 7 * (int)xScale - 3, baseY - (int)(curve[7] * yScale) - 3, 6, 6);
        }
        
        private void drawLegend(Graphics2D g2d, int width, int margin) {
            int legendX = width - margin - 220;
            int legendY = margin + 10;
            
            // Legend background
            g2d.setColor(new Color(255, 255, 255, 200));
            g2d.fillRect(legendX - 10, legendY - 10, 220, 110);
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1));
            g2d.drawRect(legendX - 10, legendY - 10, 220, 110);
            
            // Program 1
            g2d.setFont(new Font("Arial", Font.BOLD, 11));
            g2d.setColor(new Color(0, 100, 200));
            g2d.drawLine(legendX, legendY + 10, legendX + 20, legendY + 10);
            g2d.setColor(Color.BLACK);
            String prog1Label = "Program 1: " + detectComplexity(prog1Value);
            g2d.drawString(prog1Label, legendX + 30, legendY + 15);
            
            // Program 2
            g2d.setColor(new Color(255, 140, 0));
            g2d.drawLine(legendX, legendY + 35, legendX + 20, legendY + 35);
            g2d.setColor(Color.BLACK);
            String prog2Label = "Program 2: " + detectComplexity(prog2Value);
            g2d.drawString(prog2Label, legendX + 30, legendY + 40);
            
            // Current measurement
            g2d.setFont(new Font("Arial", Font.PLAIN, 10));
            g2d.drawString("● = Input size " + BASE_INPUT_SIZE, legendX, legendY + 65);
            double ratio = (double) prog2Value / prog1Value;
            g2d.drawString("At n=" + BASE_INPUT_SIZE + ": " + String.format("%.2f", ratio) + "x", legendX, legendY + 80);
        }
    }
}
