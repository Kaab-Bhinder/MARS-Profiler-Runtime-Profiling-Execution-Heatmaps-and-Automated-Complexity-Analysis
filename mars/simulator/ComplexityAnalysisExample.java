package mars.simulator;

/**
 * EXAMPLE: How to use the Time Complexity Analysis features
 * 
 * This example demonstrates how to integrate complexity analysis
 * into your MARS simulator code or UI components.
 * 
 * Compile and run after a MIPS program simulation to see
 * detailed complexity analysis and performance reports.
 */
public class ComplexityAnalysisExample {
    
    /**
     * Example 1: Basic complexity analysis
     */
    public static void exampleBasicAnalysis() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("EXAMPLE 1: Basic Complexity Analysis");
        System.out.println("=".repeat(80));
        
        // Get singleton instances (automatically populated during simulation)
        ProfilerService profiler = ProfilerService.getInstance();
        ExecutionHeatmap heatmap = ExecutionHeatmap.getInstance();
        
        // If no execution data, skip
        if (profiler.getTotalInstructions() == 0) {
            System.out.println("No execution data available. Run a MIPS program first.");
            return;
        }
        
        // Create complexity analyzer
        AlgorithmComplexityAnalyzer analyzer = 
            new AlgorithmComplexityAnalyzer(profiler, heatmap);
        
        // Get analysis results
        AlgorithmComplexityAnalyzer.ComplexityAnalysis analysis = analyzer.analyze();
        
        // Display results
        System.out.println("\nQuick Analysis Results:");
        System.out.println("  Total Instructions: " + analysis.getTotalInstructions());
        System.out.println("  Total Cycles: " + analysis.getTotalCycles());
        System.out.println("  CPI (Cycles/Instruction): " + 
                          String.format("%.2f", analysis.getCyclesPerInstruction()));
        System.out.println("  Estimated Big-O Complexity: " + 
                          analysis.getEstimatedComplexity());
        System.out.println("  Loop Nesting Level: " + 
                          analysis.getLoopStructure().getNestingLevel());
    }
    
    /**
     * Example 2: Generate detailed report
     */
    public static void exampleDetailedReport() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("EXAMPLE 2: Generate Full Performance Report");
        System.out.println("=".repeat(80));
        
        ProfilerService profiler = ProfilerService.getInstance();
        if (profiler.getTotalInstructions() == 0) {
            System.out.println("No execution data available.");
            return;
        }
        
        // Create report generator
        PerformanceAnalysisReport report = new PerformanceAnalysisReport();
        
        // Generate and print full report
        report.printFullReport();
    }
    
    /**
     * Example 3: Summary report (one-page)
     */
    public static void exampleSummaryReport() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("EXAMPLE 3: Summary Report");
        System.out.println("=".repeat(80));
        
        ProfilerService profiler = ProfilerService.getInstance();
        if (profiler.getTotalInstructions() == 0) {
            System.out.println("No execution data available.");
            return;
        }
        
        PerformanceAnalysisReport report = new PerformanceAnalysisReport();
        report.printSummaryReport();
    }
    
    /**
     * Example 4: Accessing individual metrics
     */
    public static void exampleCustomAnalysis() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("EXAMPLE 4: Custom Analysis - Access Individual Metrics");
        System.out.println("=".repeat(80));
        
        ProfilerService profiler = ProfilerService.getInstance();
        ExecutionHeatmap heatmap = ExecutionHeatmap.getInstance();
        
        if (profiler.getTotalInstructions() == 0) {
            System.out.println("No execution data available.");
            return;
        }
        
        System.out.println("\nInstruction Profile:");
        System.out.println("  Total instructions executed: " + profiler.getTotalInstructions());
        System.out.println("  Total register reads: " + profiler.getTotalRegisterReads());
        System.out.println("  Total register writes: " + profiler.getTotalRegisterWrites());
        System.out.println("  Total memory reads: " + profiler.getTotalMemoryReads());
        System.out.println("  Total memory writes: " + profiler.getTotalMemoryWrites());
        
        System.out.println("\nTiming Profile:");
        System.out.println("  Total cycles: " + profiler.getTotalCycles());
        System.out.println("  Average CPI: " + 
                          String.format("%.2f", profiler.getCyclesPerInstruction()));
        
        System.out.println("\nExecution Hotspots:");
        java.util.Map<Integer, Integer> execCounts = heatmap.getLineExecutionCounts();
        java.util.List<java.util.Map.Entry<Integer, Integer>> topLines = 
            new java.util.ArrayList<>(execCounts.entrySet());
        topLines.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        int count = 0;
        for (java.util.Map.Entry<Integer, Integer> entry : topLines) {
            if (count++ >= 5) break;
            System.out.println("  Line " + entry.getKey() + 
                              ": executed " + entry.getValue() + " times");
        }
    }
    
    /**
     * Example 5: Analyze instruction distribution
     */
    public static void exampleInstructionAnalysis() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("EXAMPLE 5: Instruction Type Distribution");
        System.out.println("=".repeat(80));
        
        ProfilerService profiler = ProfilerService.getInstance();
        
        if (profiler.getTotalInstructions() == 0) {
            System.out.println("No execution data available.");
            return;
        }
        
        java.util.Map<String, Integer> instCounts = profiler.getInstructionCounts();
        java.util.List<java.util.Map.Entry<String, Integer>> sortedInst = 
            new java.util.ArrayList<>(instCounts.entrySet());
        sortedInst.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        System.out.println("\nTop 10 Instructions by Frequency:");
        System.out.println(String.format("%-15s %10s %10s", "Instruction", "Count", "Percent"));
        System.out.println("-".repeat(35));
        
        int count = 0;
        for (java.util.Map.Entry<String, Integer> entry : sortedInst) {
            if (count++ >= 10) break;
            double percent = (entry.getValue() * 100.0) / profiler.getTotalInstructions();
            System.out.println(String.format("%-15s %10d %9.2f%%", 
                              entry.getKey(), entry.getValue(), percent));
        }
        
        // Categorize instructions
        int arithmeticOps = 0, memoryOps = 0, branchOps = 0;
        
        for (java.util.Map.Entry<String, Integer> entry : instCounts.entrySet()) {
            String inst = entry.getKey();
            int execCount = entry.getValue();
            
            if (inst.matches("add|addi|sub|mult|div|and|or|xor|nor|sll|sra|srl|slt")) {
                arithmeticOps += execCount;
            } else if (inst.matches("lw|lb|lh|sw|sb|sh")) {
                memoryOps += execCount;
            } else if (inst.matches("beq|bne|j|jr|jal|jalr")) {
                branchOps += execCount;
            }
        }
        
        System.out.println("\nInstruction Categories:");
        System.out.println("  Arithmetic: " + arithmeticOps + 
                          " (" + (arithmeticOps * 100 / profiler.getTotalInstructions()) + "%)");
        System.out.println("  Memory: " + memoryOps + 
                          " (" + (memoryOps * 100 / profiler.getTotalInstructions()) + "%)");
        System.out.println("  Control Flow: " + branchOps + 
                          " (" + (branchOps * 100 / profiler.getTotalInstructions()) + "%)");
    }
    
    /**
     * Example 6: Memory vs Compute analysis
     */
    public static void exampleMemoryVsCompute() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("EXAMPLE 6: Algorithm Classification");
        System.out.println("=".repeat(80));
        
        ProfilerService profiler = ProfilerService.getInstance();
        
        if (profiler.getTotalInstructions() == 0) {
            System.out.println("No execution data available.");
            return;
        }
        
        int memOps = profiler.getTotalMemoryReads() + profiler.getTotalMemoryWrites();
        int totalOps = profiler.getTotalInstructions();
        double memIntensity = (double) memOps / totalOps;
        
        System.out.println("\nMemory Access Pattern Analysis:");
        System.out.println("  Total Memory Operations: " + memOps);
        System.out.println("  Memory Intensity: " + String.format("%.3f", memIntensity) + 
                          " (ops per instruction)");
        
        System.out.println("\nAlgorithm Classification:");
        if (memIntensity > 0.3) {
            System.out.println("  ✓ MEMORY-INTENSIVE algorithm");
            System.out.println("  - Focus: Optimize memory access patterns");
            System.out.println("  - Examples: Sorting, matrix operations, database queries");
        } else if (memIntensity > 0.1) {
            System.out.println("  ✓ BALANCED algorithm");
            System.out.println("  - Balance of computation and memory access");
            System.out.println("  - Examples: Most practical algorithms");
        } else {
            System.out.println("  ✓ COMPUTE-INTENSIVE algorithm");
            System.out.println("  - Focus: Optimize instruction-level parallelism");
            System.out.println("  - Examples: Cryptography, FFT, Monte Carlo simulations");
        }
    }
    
    /**
     * Main method - uncomment examples to run them
     */
    public static void main(String[] args) {
        System.out.println("\n╔" + "═".repeat(78) + "╗");
        System.out.println("║" + " ".repeat(20) + 
                          "MARS Time Complexity Analysis Examples" + 
                          " ".repeat(20) + "║");
        System.out.println("╚" + "═".repeat(78) + "╝");
        
        System.out.println("\nNote: Run a MIPS program first to populate profiler data.");
        System.out.println("Example usage:");
        System.out.println("  mars bubble_sort.asm");
        System.out.println("  // Then call examples below to analyze the run\n");
        
        // Uncomment the examples you want to run:
        
        // exampleBasicAnalysis();
        // exampleDetailedReport();
        // exampleSummaryReport();
        // exampleCustomAnalysis();
        // exampleInstructionAnalysis();
        // exampleMemoryVsCompute();
        
        System.out.println("\n═".repeat(80));
        System.out.println("To use these examples in your code:");
        System.out.println("1. Import: import mars.simulator.*;");
        System.out.println("2. After running a MIPS program:");
        System.out.println("   PerformanceAnalysisReport report = new PerformanceAnalysisReport();");
        System.out.println("   report.printFullReport();");
        System.out.println("═".repeat(80));
    }
}
