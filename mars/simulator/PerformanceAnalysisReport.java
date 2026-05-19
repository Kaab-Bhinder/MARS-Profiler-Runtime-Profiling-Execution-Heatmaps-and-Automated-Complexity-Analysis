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
 * Generates comprehensive performance analysis reports combining profiling,
 * heatmap, and complexity analysis data.
 * 
 * Provides:
 * - Time complexity analysis (Big-O estimation)
 * - Performance metrics (cycles, instructions, CPI)
 * - Memory usage patterns
 * - Optimization recommendations
 * 
 * @author MARS Contributors
 * @version 2025
 */
public class PerformanceAnalysisReport {
    
    private ProfilerService profiler;
    private ExecutionHeatmap heatmap;
    private AlgorithmComplexityAnalyzer complexityAnalyzer;
    private InstructionHeatmapVisualizer instructionHeatmap;
    
    /**
     * Constructor - initializes with profiler and heatmap
     */
    public PerformanceAnalysisReport() {
        this.profiler = ProfilerService.getInstance();
        this.heatmap = ExecutionHeatmap.getInstance();
        this.complexityAnalyzer = new AlgorithmComplexityAnalyzer(profiler, heatmap);
        this.instructionHeatmap = new InstructionHeatmapVisualizer(profiler);
    }
    
    /**
     * Generate comprehensive performance report
     * @return formatted report string
     */
    public String generateFullReport() {
        StringBuilder report = new StringBuilder();
        
        report.append("\n");
        report.append("╔════════════════════════════════════════════════════════════════════════════════╗\n");
        report.append("║                     MARS PERFORMANCE ANALYSIS REPORT                          ║\n");
        report.append("╚════════════════════════════════════════════════════════════════════════════════╝\n");
        report.append("\n");
        
        // Section 1: Complexity Analysis
        report.append(complexityAnalyzer.generateReport());
        
        // Section 2: Instruction Heatmap (NEW)
        report.append(instructionHeatmap.generateInstructionHeatmapReport());
        
        // Section 3: Categorical Heatmap (NEW)
        report.append(instructionHeatmap.generateCategoricalHeatmap());
        
        // Section 4: Detailed Metrics
        report.append(generateDetailedMetrics());
        
        // Section 5: Optimization Recommendations
        report.append(generateOptimizationRecommendations());
        
        return report.toString();
    }
    
    /**
     * Generate detailed performance metrics section
     */
    private String generateDetailedMetrics() {
        StringBuilder metrics = new StringBuilder();
        
        metrics.append("DETAILED PERFORMANCE METRICS:\n");
        metrics.append("================================================================================\n\n");
        
        // Instruction Category Analysis
        metrics.append("INSTRUCTION CATEGORIES:\n");
        metrics.append("-".repeat(80)).append("\n");
        
        Map<String, Integer> instCounts = profiler.getInstructionCounts();
        int arithCount = 0, memCount = 0, branchCount = 0, otherCount = 0;
        
        for (Map.Entry<String, Integer> entry : instCounts.entrySet()) {
            String inst = entry.getKey();
            int count = entry.getValue();
            
            if (inst.matches("add|addi|sub|mult|div|and|or|xor|nor|sll|sra|srl|slt|mfhi|mflo")) {
                arithCount += count;
            } else if (inst.matches("lw|lb|lh|sw|sb|sh|lwu|lbu|lhu")) {
                memCount += count;
            } else if (inst.matches("beq|bne|j|jr|jal|jalr|blez|bgtz|bltz|bgez")) {
                branchCount += count;
            } else {
                otherCount += count;
            }
        }
        
        int total = profiler.getTotalInstructions();
        metrics.append(String.format("  Arithmetic Instructions:  %,8d  (%.1f%%)\n", arithCount, (arithCount * 100.0) / total));
        metrics.append(String.format("  Memory Instructions:      %,8d  (%.1f%%)\n", memCount, (memCount * 100.0) / total));
        metrics.append(String.format("  Branch Instructions:      %,8d  (%.1f%%)\n", branchCount, (branchCount * 100.0) / total));
        metrics.append(String.format("  Other Instructions:       %,8d  (%.1f%%)\n", otherCount, (otherCount * 100.0) / total));
        metrics.append("\n");
        metrics.append("Note: See INSTRUCTION FREQUENCY HEATMAP section above for detailed per-instruction breakdown\n");
        metrics.append("      with visual intensity indicators.\n\n");
        
        // Register Usage
        metrics.append("REGISTER UTILIZATION:\n");
        metrics.append("-".repeat(80)).append("\n");
        
        Map<Integer, Integer> regReads = profiler.getRegisterReads();
        Map<Integer, Integer> regWrites = profiler.getRegisterWrites();
        
        Set<Integer> regSet = new HashSet<>(regReads.keySet());
        regSet.addAll(regWrites.keySet());
        List<Integer> allRegs = new ArrayList<>(regSet);
        Collections.sort(allRegs);
        
        String[] regNames = {"$zero", "$at", "$v0", "$v1", "$a0", "$a1", "$a2", "$a3",
                            "$t0", "$t1", "$t2", "$t3", "$t4", "$t5", "$t6", "$t7",
                            "$s0", "$s1", "$s2", "$s3", "$s4", "$s5", "$s6", "$s7",
                            "$t8", "$t9", "$k0", "$k1", "$gp", "$sp", "$fp", "$ra"};
        
        metrics.append("Most Used Registers (Top 5):\n");
        List<Integer> topRegs = new ArrayList<>(allRegs);
        topRegs.sort((a, b) -> {
            int aTotal = regReads.getOrDefault(a, 0) + regWrites.getOrDefault(a, 0);
            int bTotal = regReads.getOrDefault(b, 0) + regWrites.getOrDefault(b, 0);
            return Integer.compare(bTotal, aTotal);
        });
        
        for (int i = 0; i < Math.min(5, topRegs.size()); i++) {
            int reg = topRegs.get(i);
            String name = reg < regNames.length ? regNames[reg] : "R" + reg;
            int reads = regReads.getOrDefault(reg, 0);
            int writes = regWrites.getOrDefault(reg, 0);
            metrics.append(String.format("    %6s: %5d reads, %5d writes (total: %5d)\n", name, reads, writes, reads + writes));
        }
        metrics.append("\n");
        
        // Memory Access Pattern
        metrics.append("MEMORY ACCESS ANALYSIS:\n");
        metrics.append("-".repeat(80)).append("\n");
        
        int memReads = profiler.getTotalMemoryReads();
        int memWrites = profiler.getTotalMemoryWrites();
        int memAccessRatio = (memReads + memWrites > 0) ? 
                             ((memReads * 100) / (memReads + memWrites)) : 0;
        
        metrics.append(String.format("  Total Memory Accesses:     %,d\n", memReads + memWrites));
        metrics.append(String.format("  Memory Reads:              %,d  (%.1f%%)\n", memReads, memAccessRatio));
        metrics.append(String.format("  Memory Writes:             %,d  (%.1f%%)\n", memWrites, 100 - memAccessRatio));
        
        // Estimate memory access intensity
        double memIntensity = (double)(memReads + memWrites) / total;
        metrics.append(String.format("  Memory Intensity:          %.2f (accesses per instruction)\n", memIntensity));
        
        if (memIntensity > 0.3) {
            metrics.append("  → This is a MEMORY-INTENSIVE algorithm\n");
        } else if (memIntensity > 0.1) {
            metrics.append("  → This is a MODERATELY MEMORY-INTENSIVE algorithm\n");
        } else {
            metrics.append("  → This is a COMPUTE-INTENSIVE algorithm\n");
        }
        metrics.append("\n");
        
        // Code Hotspots
        metrics.append("CODE HOTSPOTS (Most Executed Lines):\n");
        metrics.append("-".repeat(80)).append("\n");
        
        Map<Integer, Integer> lineExec = heatmap.getLineExecutionCounts();
        List<Map.Entry<Integer, Integer>> hotspots = new ArrayList<>(lineExec.entrySet());
        hotspots.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        
        int count = 0;
        for (Map.Entry<Integer, Integer> entry : hotspots) {
            if (count++ >= 5) break;
            double percent = (entry.getValue() * 100.0) / heatmap.getMaxExecutionCount();
            metrics.append(String.format("  Line %4d: %,9d executions (%.1f%% of hottest line)\n", 
                          entry.getKey(), entry.getValue(), percent));
        }
        metrics.append("\n");
        
        return metrics.toString();
    }
    
    /**
     * Generate optimization recommendations based on analysis
     */
    private String generateOptimizationRecommendations() {
        StringBuilder recommendations = new StringBuilder();
        
        recommendations.append("OPTIMIZATION RECOMMENDATIONS:\n");
        recommendations.append("================================================================================\n\n");
        
        AlgorithmComplexityAnalyzer.ComplexityAnalysis analysis = complexityAnalyzer.analyze();
        
        // Check for memory bottleneck
        int memOps = profiler.getTotalMemoryReads() + profiler.getTotalMemoryWrites();
        double memIntensity = (double) memOps / profiler.getTotalInstructions();
        
        if (memIntensity > 0.2) {
            recommendations.append("🔴 MEMORY BOTTLENECK DETECTED:\n");
            recommendations.append("   - High ratio of memory operations relative to computation\n");
            recommendations.append("   - Recommendation: Consider data locality improvements\n");
            recommendations.append("   - Optimize memory access patterns (spatial/temporal locality)\n\n");
        }
        
        // Check for high branching
        Map<String, Integer> instCounts = profiler.getInstructionCounts();
        int branchCount = instCounts.getOrDefault("beq", 0) + instCounts.getOrDefault("bne", 0) +
                         instCounts.getOrDefault("j", 0) + instCounts.getOrDefault("jr", 0);
        double branchIntensity = (double) branchCount / profiler.getTotalInstructions();
        
        if (branchIntensity > 0.1) {
            recommendations.append("🟡 HIGH BRANCH PRESSURE:\n");
            recommendations.append("   - Frequent branch instructions detected\n");
            recommendations.append("   - Recommendation: Consider unrolling loops or reducing conditionals\n");
            recommendations.append("   - Pipeline may suffer from branch mispredictions\n\n");
        }
        
        // Check for algorithmic inefficiency
        String complexity = analysis.getEstimatedComplexity();
        if (complexity.contains("O(n³)") || complexity.contains("O(n²)")) {
            recommendations.append("🔴 ALGORITHMIC INEFFICIENCY:\n");
            recommendations.append("   - Current algorithm has high time complexity\n");
            recommendations.append("   - Recommendation: Consider a more efficient algorithm\n");
            if (complexity.contains("O(n³)")) {
                recommendations.append("   - Cubic complexity: Consider divide-and-conquer or dynamic programming\n");
            } else {
                recommendations.append("   - Quadratic complexity: Consider binary search or sorting improvements\n");
            }
            recommendations.append("\n");
        }
        
        // Check for CPI efficiency
        double cpi = profiler.getCyclesPerInstruction();
        if (cpi > 3.0) {
            recommendations.append("🟡 SUBOPTIMAL INSTRUCTION PIPELINING:\n");
            recommendations.append(String.format("   - Current CPI: %.2f (ideal is close to 1.0)\n", cpi));
            recommendations.append("   - Recommendation: Reduce memory stalls and data dependencies\n");
            recommendations.append("   - Schedule instructions to minimize pipeline hazards\n\n");
        } else {
            recommendations.append("✅ GOOD PIPELINE EFFICIENCY:\n");
            recommendations.append(String.format("   - Current CPI: %.2f\n", cpi));
            recommendations.append("   - Good instruction-level parallelism\n\n");
        }
        
        // Check register usage
        double regIntensity = (double)(profiler.getTotalRegisterReads() + profiler.getTotalRegisterWrites())
                             / profiler.getTotalInstructions();
        if (regIntensity < 1.0) {
            recommendations.append("⚠️  LOW REGISTER USAGE:\n");
            recommendations.append("   - Few register accesses relative to instructions\n");
            recommendations.append("   - May indicate excessive memory usage\n\n");
        }
        
        recommendations.append("================================================================================\n");
        
        return recommendations.toString();
    }
    
    /**
     * Generate a summary report (one-page version)
     */
    public String generateSummaryReport() {
        StringBuilder summary = new StringBuilder();
        
        summary.append("\n");
        summary.append("╔════════════════════════════════════════════════════════════════════════════════╗\n");
        summary.append("║                         PERFORMANCE SUMMARY REPORT                            ║\n");
        summary.append("╚════════════════════════════════════════════════════════════════════════════════╝\n\n");
        
        AlgorithmComplexityAnalyzer.ComplexityAnalysis analysis = complexityAnalyzer.analyze();
        
        summary.append(String.format("  Instructions Executed:  %,d\n", analysis.getTotalInstructions()));
        summary.append(String.format("  Total Cycles:           %,d\n", analysis.getTotalCycles()));
        summary.append(String.format("  CPI (Cycles/Instr):     %.2f\n", analysis.getCyclesPerInstruction()));
        summary.append(String.format("  Estimated Complexity:   %s\n\n", analysis.getEstimatedComplexity()));
        
        summary.append(String.format("  Loop Nesting Level:     %d\n", analysis.getLoopStructure().getNestingLevel()));
        summary.append(String.format("  Max Line Execution:     %,d times\n", analysis.getLoopStructure().getMaxExecutionCount()));
        summary.append("\n");
        
        return summary.toString();
    }
    
    /**
     * Print full report to stdout
     */
    public void printFullReport() {
        System.out.print(generateFullReport());
    }
    
    /**
     * Print summary report to stdout
     */
    public void printSummaryReport() {
        System.out.print(generateSummaryReport());
    }
    
    /**
     * Print just the instruction heatmap visualization
     */
    public void printInstructionHeatmap() {
        instructionHeatmap.printInstructionHeatmap();
    }
    
    /**
     * Print just the categorical heatmap visualization
     */
    public void printCategoricalHeatmap() {
        instructionHeatmap.printCategoricalHeatmap();
    }
    
    /**
     * Print top N instructions with heatmap
     */
    public void printTopInstructions(int topN) {
        instructionHeatmap.printTopInstructions(topN);
    }
    
    /**
     * Get instruction heatmap visualizer for direct access
     */
    public InstructionHeatmapVisualizer getInstructionHeatmap() {
        return instructionHeatmap;
    }
}
