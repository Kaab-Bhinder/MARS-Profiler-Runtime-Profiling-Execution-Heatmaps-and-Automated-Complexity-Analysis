package mars.simulator;

import java.util.*;

/**
 * Benchmark Comparison Tool
 * Compares execution metrics between two programs
 * 
 * @author Pete Sanderson and Ken Vollmar
 * @version 2025
 */
public class BenchmarkComparator {
    
    private static BenchmarkComparator instance;
    
    private static class BenchmarkResult {
        String programName;
        int instructions;
        int registers;
        int memory;
        long timestamp;
        
        BenchmarkResult(String name, int instr, int regs, int mem) {
            this.programName = name;
            this.instructions = instr;
            this.registers = regs;
            this.memory = mem;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    private List<BenchmarkResult> results = new ArrayList<>();
    private BenchmarkResult program1LastRun = null;
    private BenchmarkResult program2LastRun = null;
    
    /**
     * Get singleton instance
     */
    public static synchronized BenchmarkComparator getInstance() {
        if (instance == null) {
            instance = new BenchmarkComparator();
        }
        return instance;
    }
    
    /**
     * Record execution results for program 1
     */
    public void recordProgram1Result(String name, int instructions, int registers, int memory) {
        program1LastRun = new BenchmarkResult(name, instructions, registers, memory);
        results.add(program1LastRun);
    }
    
    /**
     * Record execution results for program 2
     */
    public void recordProgram2Result(String name, int instructions, int registers, int memory) {
        program2LastRun = new BenchmarkResult(name, instructions, registers, memory);
        results.add(program2LastRun);
    }
    
    /**
     * Get comparison between last two runs
     */
    public String getComparison() {
        if (program1LastRun == null || program2LastRun == null) {
            return "Need both programs executed to compare";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("  BENCHMARK COMPARISON\n");
        sb.append("========================================\n\n");
        
        // Program 1 info
        sb.append("Program 1: ").append(program1LastRun.programName).append("\n");
        sb.append("  Instructions: ").append(program1LastRun.instructions).append("\n");
        sb.append("  Register Ops: ").append(program1LastRun.registers).append("\n");
        sb.append("  Memory Ops:   ").append(program1LastRun.memory).append("\n\n");
        
        // Program 2 info
        sb.append("Program 2: ").append(program2LastRun.programName).append("\n");
        sb.append("  Instructions: ").append(program2LastRun.instructions).append("\n");
        sb.append("  Register Ops: ").append(program2LastRun.registers).append("\n");
        sb.append("  Memory Ops:   ").append(program2LastRun.memory).append("\n\n");
        
        // Comparison
        sb.append("Comparison:\n");
        
        int instrDiff = program2LastRun.instructions - program1LastRun.instructions;
        double instrRatio = (double) program2LastRun.instructions / program1LastRun.instructions;
        sb.append("  Instructions: ");
        if (instrDiff > 0) {
            sb.append("Program 2 is ").append(String.format("%.2f", instrRatio))
              .append("x slower (").append(instrDiff).append(" more instructions)\n");
        } else {
            sb.append("Program 2 is ").append(String.format("%.2f", instrRatio))
              .append("x faster (").append(-instrDiff).append(" fewer instructions)\n");
        }
        
        int regDiff = program2LastRun.registers - program1LastRun.registers;
        sb.append("  Register Ops: Program 2 has ").append(regDiff > 0 ? "+" : "")
          .append(regDiff).append(" operations\n");
        
        int memDiff = program2LastRun.memory - program1LastRun.memory;
        sb.append("  Memory Ops:   Program 2 has ").append(memDiff > 0 ? "+" : "")
          .append(memDiff).append(" operations\n\n");
        
        // Winner
        sb.append("Winner: ");
        if (program1LastRun.instructions < program2LastRun.instructions) {
            sb.append("Program 1 (by ").append(instrDiff).append(" instructions)\n");
        } else {
            sb.append("Program 2 (by ").append(-instrDiff).append(" instructions)\n");
        }
        
        sb.append("========================================\n");
        return sb.toString();
    }
    
    /**
     * Get all results as CSV format
     */
    public String getResultsAsCSV() {
        StringBuilder sb = new StringBuilder();
        sb.append("Program,Instructions,Registers,Memory\n");
        for (BenchmarkResult r : results) {
            sb.append(r.programName).append(",")
              .append(r.instructions).append(",")
              .append(r.registers).append(",")
              .append(r.memory).append("\n");
        }
        return sb.toString();
    }
    
    /**
     * Clear all results
     */
    public void reset() {
        results.clear();
        program1LastRun = null;
        program2LastRun = null;
    }
    
    /**
     * Check if both programs have been executed
     */
    public boolean hasBothResults() {
        return program1LastRun != null && program2LastRun != null;
    }
}
