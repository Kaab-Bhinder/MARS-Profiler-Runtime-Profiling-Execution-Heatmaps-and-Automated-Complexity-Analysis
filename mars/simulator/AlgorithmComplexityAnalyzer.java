package mars.simulator;

import java.util.*;
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
 * Analyzes MIPS program execution to estimate algorithm time complexity.
 * 
 * Detects:
 * - Nested loop structures
 * - Execution patterns (linear, quadratic, cubic, exponential, logarithmic)
 * - Memory access patterns
 * - Instruction distribution
 * 
 * Uses profiler data and execution heatmap to estimate Big-O complexity.
 * 
 * @author MARS Contributors
 * @version 2025
 */
public class AlgorithmComplexityAnalyzer {
    
    private ProfilerService profiler;
    private ExecutionHeatmap heatmap;
    
    /**
     * Constructor - takes profiler and heatmap instances
     * @param profilerService the ProfilerService singleton
     * @param executionHeatmap the ExecutionHeatmap singleton
     */
    public AlgorithmComplexityAnalyzer(ProfilerService profilerService, ExecutionHeatmap executionHeatmap) {
        this.profiler = profilerService;
        this.heatmap = executionHeatmap;
    }
    
    /**
     * Analyze algorithm complexity based on execution profile
     * @return ComplexityAnalysis object with detected patterns and Big-O estimate
     */
    public ComplexityAnalysis analyze() {
        ComplexityAnalysis analysis = new ComplexityAnalysis();
        
        if (profiler.getTotalInstructions() == 0) {
            return analysis;  // No execution data
        }
        
        // Analyze execution heatmap to detect loops
        LoopStructure loops = detectLoopStructure();
        analysis.setLoopStructure(loops);
        
        // Analyze instruction patterns
        Map<String, Double> instructionDistribution = analyzeInstructionDistribution();
        analysis.setInstructionDistribution(instructionDistribution);
        
        // Estimate complexity based on patterns
        String estimatedComplexity = estimateComplexity(loops, instructionDistribution);
        analysis.setEstimatedComplexity(estimatedComplexity);
        
        // Calculate actual metrics
        long totalCycles = profiler.getTotalCycles();
        int totalInstructions = profiler.getTotalInstructions();
        double cpi = profiler.getCyclesPerInstruction();
        
        analysis.setTotalCycles(totalCycles);
        analysis.setTotalInstructions(totalInstructions);
        analysis.setCyclesPerInstruction(cpi);
        
        return analysis;
    }
    
    /**
     * Detect loop structures from execution heatmap
     * @return LoopStructure with detected nesting levels
     */
    private LoopStructure detectLoopStructure() {
        LoopStructure structure = new LoopStructure();
        
        Map<Integer, Integer> execCounts = heatmap.getLineExecutionCounts();
        
        if (execCounts.isEmpty()) {
            return structure;
        }
        
        // Find lines with highest execution frequency (likely loop bodies)
        int maxCount = heatmap.getMaxExecutionCount();
        List<Integer> hotLines = new ArrayList<>();
        
        for (Map.Entry<Integer, Integer> entry : execCounts.entrySet()) {
            // Lines executed > 50% of max are considered hot
            if (entry.getValue() > maxCount * 0.5) {
                hotLines.add(entry.getKey());
            }
        }
        
        // Estimate nesting level from hottest line execution count
        // If a line executes N times, it's likely in an N-nested loop structure
        if (!hotLines.isEmpty()) {
            int hotLineCount = Collections.max(execCounts.values());
            
            if (hotLineCount > 1000) {
                structure.setNestingLevel(3);  // Cubic or worse
                structure.setMaxExecutionCount(hotLineCount);
            } else if (hotLineCount > 100) {
                structure.setNestingLevel(2);  // Quadratic
                structure.setMaxExecutionCount(hotLineCount);
            } else if (hotLineCount > 10) {
                structure.setNestingLevel(1);  // Linear
                structure.setMaxExecutionCount(hotLineCount);
            } else {
                structure.setNestingLevel(0);  // Constant or logarithmic
                structure.setMaxExecutionCount(hotLineCount);
            }
            
            structure.setHotLineCount(hotLines.size());
        }
        
        return structure;
    }
    
    /**
     * Analyze distribution of instruction types
     * @return Map of instruction type to percentage of total
     */
    private Map<String, Double> analyzeInstructionDistribution() {
        Map<String, Double> distribution = new HashMap<>();
        Map<String, Integer> instCounts = profiler.getInstructionCounts();
        int total = profiler.getTotalInstructions();
        
        for (String inst : instCounts.keySet()) {
            double percentage = (instCounts.get(inst) * 100.0) / total;
            distribution.put(inst, percentage);
        }
        
        return distribution;
    }
    
    /**
     * Estimate algorithm complexity based on patterns
     * @param loops detected loop structure
     * @param distribution instruction distribution
     * @return String representation of estimated complexity (e.g., "O(n^2)")
     */
    private String estimateComplexity(LoopStructure loops, Map<String, Double> distribution) {
        int nestingLevel = loops.getNestingLevel();
        int maxExecCount = loops.getMaxExecutionCount();
        
        // Analyze for memory vs arithmetic intensive patterns
        double memoryOps = distribution.getOrDefault("lw", 0.0) + 
                          distribution.getOrDefault("sw", 0.0) +
                          distribution.getOrDefault("lb", 0.0) +
                          distribution.getOrDefault("sb", 0.0) +
                          distribution.getOrDefault("lh", 0.0) +
                          distribution.getOrDefault("sh", 0.0);
        
        double branchOps = distribution.getOrDefault("beq", 0.0) +
                          distribution.getOrDefault("bne", 0.0) +
                          distribution.getOrDefault("j", 0.0) +
                          distribution.getOrDefault("jr", 0.0) +
                          distribution.getOrDefault("jal", 0.0) +
                          distribution.getOrDefault("jalr", 0.0);
        
        // Estimate based on nesting and execution pattern
        if (nestingLevel >= 3 || maxExecCount > 1000) {
            return "O(n³) or O(n² log n) - Cubic or worse complexity detected";
        } else if (nestingLevel == 2 || (maxExecCount > 100 && maxExecCount <= 1000)) {
            return "O(n²) - Quadratic complexity detected";
        } else if (nestingLevel == 1 || (maxExecCount > 10 && maxExecCount <= 100)) {
            if (branchOps > 20.0) {
                return "O(n log n) - Linear-logarithmic complexity (divide and conquer pattern)";
            } else {
                return "O(n) - Linear complexity detected";
            }
        } else if (branchOps > 10.0) {
            return "O(log n) - Logarithmic complexity (binary search pattern)";
        } else {
            return "O(1) - Constant time complexity";
        }
    }
    
    /**
     * Generate a detailed complexity analysis report
     * @return Formatted report string
     */
    public String generateReport() {
        ComplexityAnalysis analysis = analyze();
        StringBuilder report = new StringBuilder();
        
        report.append("\n");
        report.append("================================================================================\n");
        report.append("                    ALGORITHM COMPLEXITY ANALYSIS REPORT\n");
        report.append("================================================================================\n\n");
        
        // Execution Summary
        report.append("EXECUTION SUMMARY:\n");
        report.append("-".repeat(80)).append("\n");
        report.append(String.format("  Total Instructions Executed:    %,d\n", analysis.getTotalInstructions()));
        report.append(String.format("  Total Clock Cycles:             %,d\n", analysis.getTotalCycles()));
        report.append(String.format("  Average Cycles Per Instruction: %.2f\n", analysis.getCyclesPerInstruction()));
        report.append("\n");
        
        // Loop Structure Analysis
        LoopStructure loops = analysis.getLoopStructure();
        report.append("LOOP STRUCTURE ANALYSIS:\n");
        report.append("-".repeat(80)).append("\n");
        report.append(String.format("  Detected Loop Nesting Level:    %d\n", loops.getNestingLevel()));
        report.append(String.format("  Maximum Line Execution Count:   %,d\n", loops.getMaxExecutionCount()));
        report.append(String.format("  Number of Hot Code Lines:       %d\n", loops.getHotLineCount()));
        report.append("\n");
        
        // Instruction Distribution
        report.append("INSTRUCTION DISTRIBUTION (Top 10):\n");
        report.append("-".repeat(80)).append("\n");
        report.append(String.format("%-20s %10s %10s\n", "Instruction", "Count", "Percent"));
        report.append("-".repeat(80)).append("\n");
        
        Map<String, Integer> instCounts = profiler.getInstructionCounts();
        List<Map.Entry<String, Integer>> sortedInst = 
            new ArrayList<>(instCounts.entrySet());
        sortedInst.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        int count = 0;
        for (Map.Entry<String, Integer> entry : sortedInst) {
            if (count++ >= 10) break;
            double percent = (entry.getValue() * 100.0) / analysis.getTotalInstructions();
            report.append(String.format("%-20s %10,d %9.2f%%\n", entry.getKey(), entry.getValue(), percent));
        }
        report.append("\n");
        
        // Complexity Estimate
        report.append("ESTIMATED COMPLEXITY:\n");
        report.append("-".repeat(80)).append("\n");
        report.append(String.format("  %s\n", analysis.getEstimatedComplexity()));
        report.append("\n");
        
        // Profiling Metrics
        int totalReads = profiler.getTotalRegisterReads() + profiler.getTotalMemoryReads();
        int totalWrites = profiler.getTotalRegisterWrites() + profiler.getTotalMemoryWrites();
        
        report.append("MEMORY ACCESS PATTERNS:\n");
        report.append("-".repeat(80)).append("\n");
        report.append(String.format("  Total Register Reads:           %,d\n", profiler.getTotalRegisterReads()));
        report.append(String.format("  Total Register Writes:          %,d\n", profiler.getTotalRegisterWrites()));
        report.append(String.format("  Total Memory Reads:             %,d\n", profiler.getTotalMemoryReads()));
        report.append(String.format("  Total Memory Writes:            %,d\n", profiler.getTotalMemoryWrites()));
        report.append(String.format("  Total Data Accesses:            %,d\n", totalReads + totalWrites));
        report.append("\n");
        
        report.append("================================================================================\n");
        
        return report.toString();
    }
    
    /**
     * Print complexity analysis report to stdout
     */
    public void printReport() {
        System.out.print(generateReport());
    }
    
    // ==================== Inner Classes ====================
    
    /**
     * Container for complexity analysis results
     */
    public static class ComplexityAnalysis {
        private LoopStructure loopStructure;
        private Map<String, Double> instructionDistribution;
        private String estimatedComplexity;
        private long totalCycles;
        private int totalInstructions;
        private double cyclesPerInstruction;
        
        public LoopStructure getLoopStructure() { return loopStructure; }
        public void setLoopStructure(LoopStructure structure) { this.loopStructure = structure; }
        
        public Map<String, Double> getInstructionDistribution() { return instructionDistribution; }
        public void setInstructionDistribution(Map<String, Double> dist) { this.instructionDistribution = dist; }
        
        public String getEstimatedComplexity() { return estimatedComplexity; }
        public void setEstimatedComplexity(String complexity) { this.estimatedComplexity = complexity; }
        
        public long getTotalCycles() { return totalCycles; }
        public void setTotalCycles(long cycles) { this.totalCycles = cycles; }
        
        public int getTotalInstructions() { return totalInstructions; }
        public void setTotalInstructions(int instructions) { this.totalInstructions = instructions; }
        
        public double getCyclesPerInstruction() { return cyclesPerInstruction; }
        public void setCyclesPerInstruction(double cpi) { this.cyclesPerInstruction = cpi; }
    }
    
    /**
     * Container for detected loop structure information
     */
    public static class LoopStructure {
        private int nestingLevel;
        private int maxExecutionCount;
        private int hotLineCount;
        
        public int getNestingLevel() { return nestingLevel; }
        public void setNestingLevel(int level) { this.nestingLevel = level; }
        
        public int getMaxExecutionCount() { return maxExecutionCount; }
        public void setMaxExecutionCount(int count) { this.maxExecutionCount = count; }
        
        public int getHotLineCount() { return hotLineCount; }
        public void setHotLineCount(int count) { this.hotLineCount = count; }
    }
}
