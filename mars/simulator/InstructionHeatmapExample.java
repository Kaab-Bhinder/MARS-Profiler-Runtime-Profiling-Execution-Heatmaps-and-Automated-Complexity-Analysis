package mars.simulator;

/**
 * ENHANCED EXAMPLES: Instruction Heatmap Visualization
 * 
 * Demonstrates how to use the new InstructionHeatmapVisualizer
 * to display instruction frequencies with heat-based color coding.
 */
public class InstructionHeatmapExample {
    
    /**
     * Example 1: Basic instruction heatmap display
     */
    public static void example1_BasicHeatmap() {
        System.out.println("\n" + "═".repeat(90));
        System.out.println("EXAMPLE 1: Basic Instruction Heatmap");
        System.out.println("═".repeat(90));
        
        ProfilerService profiler = ProfilerService.getInstance();
        
        if (profiler.getTotalInstructions() == 0) {
            System.out.println("No execution data available. Run a MIPS program first.");
            return;
        }
        
        // Create visualizer
        InstructionHeatmapVisualizer heatmap = new InstructionHeatmapVisualizer(profiler);
        
        // Display instruction heatmap
        heatmap.printInstructionHeatmap();
    }
    
    /**
     * Example 2: Categorical heatmap (group by instruction type)
     */
    public static void example2_CategoricalHeatmap() {
        System.out.println("\n" + "═".repeat(90));
        System.out.println("EXAMPLE 2: Categorical Instruction Heatmap");
        System.out.println("═".repeat(90));
        
        ProfilerService profiler = ProfilerService.getInstance();
        
        if (profiler.getTotalInstructions() == 0) {
            System.out.println("No execution data available.");
            return;
        }
        
        InstructionHeatmapVisualizer heatmap = new InstructionHeatmapVisualizer(profiler);
        heatmap.printCategoricalHeatmap();
    }
    
    /**
     * Example 3: Top N instructions with heatmap
     */
    public static void example3_TopInstructions() {
        System.out.println("\n" + "═".repeat(90));
        System.out.println("EXAMPLE 3: Top 10 Most Frequent Instructions");
        System.out.println("═".repeat(90));
        
        ProfilerService profiler = ProfilerService.getInstance();
        
        if (profiler.getTotalInstructions() == 0) {
            System.out.println("No execution data available.");
            return;
        }
        
        InstructionHeatmapVisualizer heatmap = new InstructionHeatmapVisualizer(profiler);
        heatmap.printTopInstructions(10);
    }
    
    /**
     * Example 4: Full performance report with integrated heatmaps
     */
    public static void example4_FullReportWithHeatmaps() {
        System.out.println("\n" + "═".repeat(90));
        System.out.println("EXAMPLE 4: Full Report with Instruction Heatmaps");
        System.out.println("═".repeat(90));
        
        ProfilerService profiler = ProfilerService.getInstance();
        
        if (profiler.getTotalInstructions() == 0) {
            System.out.println("No execution data available.");
            return;
        }
        
        PerformanceAnalysisReport report = new PerformanceAnalysisReport();
        report.printFullReport();  // Now includes heatmap sections
    }
    
    /**
     * Example 5: Individual heatmap queries
     */
    public static void example5_QueryHeatmapIntensity() {
        System.out.println("\n" + "═".repeat(90));
        System.out.println("EXAMPLE 5: Query Individual Instruction Intensities");
        System.out.println("═".repeat(90));
        
        ProfilerService profiler = ProfilerService.getInstance();
        
        if (profiler.getTotalInstructions() == 0) {
            System.out.println("No execution data available.");
            return;
        }
        
        InstructionHeatmapVisualizer heatmap = new InstructionHeatmapVisualizer(profiler);
        
        System.out.println("\nInstruction Heatmap Intensities:\n");
        System.out.println(String.format("%-15s %15s", "Instruction", "Heatmap Intensity"));
        System.out.println("-".repeat(30));
        
        java.util.Map<String, Integer> instCounts = profiler.getInstructionCounts();
        java.util.List<java.util.Map.Entry<String, Integer>> sorted = 
            new java.util.ArrayList<>(instCounts.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        int count = 0;
        for (java.util.Map.Entry<String, Integer> entry : sorted) {
            if (count++ >= 10) break;
            
            String instruction = entry.getKey();
            int intensity = heatmap.getInstructionHeatmapIntensity(instruction);
            
            // Display intensity as percentage + bar
            String intensityBar = createIntensityBar(intensity);
            System.out.println(String.format("%-15s %3d%% %s", instruction, intensity, intensityBar));
        }
    }
    
    /**
     * Example 6: Profiler section tool integration example
     * Shows how to add heatmap to existing profiler UI components
     */
    public static void example6_ProfilerToolIntegration() {
        System.out.println("\n" + "═".repeat(90));
        System.out.println("EXAMPLE 6: Integration with Profiler Tool");
        System.out.println("═".repeat(90));
        
        ProfilerService profiler = ProfilerService.getInstance();
        
        if (profiler.getTotalInstructions() == 0) {
            System.out.println("No execution data available.");
            return;
        }
        
        System.out.println("\nIn your profiler tool window, you can now include:\n");
        
        // Show what the profiler section would look like
        PerformanceAnalysisReport report = new PerformanceAnalysisReport();
        InstructionHeatmapVisualizer heatmapVisualizer = report.getInstructionHeatmap();
        
        // Get just the instruction heatmap section
        String heatmapReport = heatmapVisualizer.generateInstructionHeatmapReport();
        System.out.print(heatmapReport);
        
        System.out.println("\nIntegration tip:");
        System.out.println("Add the following to your profiler tool:");
        System.out.println("  PerformanceAnalysisReport report = new PerformanceAnalysisReport();");
        System.out.println("  InstructionHeatmapVisualizer viz = report.getInstructionHeatmap();");
        System.out.println("  textArea.setText(viz.generateInstructionHeatmapReport());");
    }
    
    /**
     * Example 7: Color intensity comparison
     */
    public static void example7_IntensityComparison() {
        System.out.println("\n" + "═".repeat(90));
        System.out.println("EXAMPLE 7: Understanding Heatmap Color Intensity");
        System.out.println("═".repeat(90));
        
        System.out.println("\nHeatmap Color Zones Explained:\n");
        
        String[][] intensities = {
            {"[░░░░░░░░]", "0-10%", "Very Low", "Green - Rarely executed"},
            {"[░░░░░░░░]", "10-20%", "Low", "Light Green - Infrequent"},
            {"[▒▒▒▒░░░░]", "20-40%", "Medium", "Yellow - Moderate frequency"},
            {"[▓▓▓▓▒▒░░]", "40-60%", "High", "Orange - Frequently executed"},
            {"[████▓▓▒▒]", "60-80%", "Very High", "Red-Orange - Very frequent"},
            {"[████████]", "80-100%", "Extreme", "Dark Red - Critical hotspot"}
        };
        
        for (String[] row : intensities) {
            System.out.println(String.format("%s  %6s  %-12s  %s", row[0], row[1], row[2], row[3]));
        }
        
        System.out.println("\nUsage:");
        System.out.println("  • Red instructions are hot spots that dominate execution");
        System.out.println("  • Orange instructions are frequently executed");
        System.out.println("  • Yellow instructions have moderate frequency");
        System.out.println("  • Green instructions are rarely executed");
        System.out.println("  • Focus optimization efforts on RED instructions");
    }
    
    /**
     * Helper to create intensity bar visualization
     */
    private static String createIntensityBar(int intensity) {
        int blocks = intensity / 10;
        StringBuilder bar = new StringBuilder("[");
        
        for (int i = 0; i < 10; i++) {
            if (i < blocks) {
                bar.append("█");
            } else {
                bar.append("░");
            }
        }
        
        bar.append("]");
        return bar.toString();
    }
    
    /**
     * Main - uncomment examples to run
     */
    public static void main(String[] args) {
        System.out.println("\n╔" + "═".repeat(88) + "╗");
        System.out.println("║" + " ".repeat(18) + 
                          "MARS Instruction Heatmap Visualization Examples" + 
                          " ".repeat(18) + "║");
        System.out.println("╚" + "═".repeat(88) + "╝");
        
        System.out.println("\nNote: Run a MIPS program first to populate profiler data.");
        System.out.println("Example: mars bubble_sort.asm\n");
        
        System.out.println("Available examples (uncomment to run):");
        System.out.println("  1. example1_BasicHeatmap()");
        System.out.println("  2. example2_CategoricalHeatmap()");
        System.out.println("  3. example3_TopInstructions()");
        System.out.println("  4. example4_FullReportWithHeatmaps()");
        System.out.println("  5. example5_QueryHeatmapIntensity()");
        System.out.println("  6. example6_ProfilerToolIntegration()");
        System.out.println("  7. example7_IntensityComparison()\n");
        
        // Uncomment to run:
        // example1_BasicHeatmap();
        // example2_CategoricalHeatmap();
        // example3_TopInstructions();
        // example4_FullReportWithHeatmaps();
        // example5_QueryHeatmapIntensity();
        // example6_ProfilerToolIntegration();
        // example7_IntensityComparison();
        
        System.out.println("═".repeat(90));
        System.out.println("Quick Start Usage:");
        System.out.println("  PerformanceAnalysisReport report = new PerformanceAnalysisReport();");
        System.out.println("  report.printInstructionHeatmap();        // Just heatmap");
        System.out.println("  report.printCategoricalHeatmap();       // Grouped by type");
        System.out.println("  report.printTopInstructions(10);        // Top 10");
        System.out.println("  report.printFullReport();               // Complete report");
        System.out.println("═".repeat(90));
    }
}
