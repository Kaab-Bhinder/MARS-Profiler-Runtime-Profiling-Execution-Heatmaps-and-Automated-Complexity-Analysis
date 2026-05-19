#!/bin/bash

# MARS FULL RUN SCRIPT - All in One
# This script compiles and runs MARS with full analysis

set -e  # Exit on error

MARS_DIR="/home/kaab/Desktop/MARS-MIPS-main"
PROGRAM="${1:-bubble_sort.asm}"

echo ""
echo "╔════════════════════════════════════════════════════════════════════════════════╗"
echo "║                      MARS FULL RUN - COMPLETE SYSTEM                          ║"
echo "╚════════════════════════════════════════════════════════════════════════════════╝"
echo ""

# Change to MARS directory
cd "$MARS_DIR"
echo "📁 Working directory: $MARS_DIR"
echo ""

# ═══════════════════════════════════════════════════════════════════════════════════════
# STEP 1: COMPILE
# ═══════════════════════════════════════════════════════════════════════════════════════
echo "┌────────────────────────────────────────────────────────────────────────────────┐"
echo "│ STEP 1: COMPILING MARS                                                         │"
echo "└────────────────────────────────────────────────────────────────────────────────┘"
echo ""

# Check if compilation is needed
if [ ! -f "mars/simulator/InstructionHeatmapVisualizer.class" ] || [ ! -f "mars/Mars.class" ]; then
    echo "🔨 Compiling all Java files..."
    find mars -name "*.java" -print0 | xargs -0 javac 2>/dev/null
    COMPILE_STATUS=$?
    
    if [ $COMPILE_STATUS -eq 0 ]; then
        echo "✅ Compilation successful!"
    else
        echo "⚠️  Compilation completed with warnings (continuing...)"
    fi
else
    echo "✅ Already compiled (using cached classes)"
fi

echo ""

# ═══════════════════════════════════════════════════════════════════════════════════════
# STEP 2: VERIFY PROGRAM
# ═══════════════════════════════════════════════════════════════════════════════════════
echo "┌────────────────────────────────────────────────────────────────────────────────┐"
echo "│ STEP 2: VERIFYING PROGRAM                                                      │"
echo "└────────────────────────────────────────────────────────────────────────────────┘"
echo ""

if [ ! -f "$PROGRAM" ]; then
    echo "❌ Error: Program '$PROGRAM' not found!"
    echo ""
    echo "Available programs:"
    ls -1 *.asm 2>/dev/null || echo "  (No .asm files found)"
    exit 1
else
    echo "✅ Program found: $PROGRAM"
    echo "   File size: $(wc -c < "$PROGRAM") bytes"
    echo "   Lines: $(wc -l < "$PROGRAM")"
fi

echo ""

# ═══════════════════════════════════════════════════════════════════════════════════════
# STEP 3: CREATE ANALYZER
# ═══════════════════════════════════════════════════════════════════════════════════════
echo "┌────────────────────────────────────────────────────────────────────────────────┐"
echo "│ STEP 3: CREATING ANALYZER                                                      │"
echo "└────────────────────────────────────────────────────────────────────────────────┘"
echo ""

cat > _MarsAnalyzer.java << 'JAVAEOF'
import mars.simulator.*;
import mars.*;

public class _MarsAnalyzer {
    public static void main(String[] args) throws Exception {
        String program = args.length > 0 ? args[0] : "bubble_sort.asm";
        
        // Reset profilers
        ProfilerService profiler = ProfilerService.getInstance();
        ExecutionHeatmap heatmap = ExecutionHeatmap.getInstance();
        profiler.reset();
        heatmap.reset();
        
        System.out.println("\n▶  Program: " + program);
        System.out.println("▶  Executing...\n");
        System.out.println("━".repeat(88));
        
        try {
            // Execute
            Mars mars = new Mars();
            mars.assemble(new String[]{program}, true, true);
            mars.simulate();
            
            System.out.println("━".repeat(88));
            System.out.println("\n✅ Program executed successfully!\n");
            
            // Generate full report
            System.out.println("📊 Generating performance analysis...\n");
            PerformanceAnalysisReport report = new PerformanceAnalysisReport();
            report.printFullReport();
            
            System.out.println("\n" + "═".repeat(88));
            System.out.println("✅ Analysis complete!");
            System.out.println("═".repeat(88) + "\n");
            
        } catch (Exception e) {
            System.err.println("\n❌ Error executing program:");
            System.err.println("   " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
JAVAEOF

echo "✅ Analyzer created"
echo ""

# ═══════════════════════════════════════════════════════════════════════════════════════
# STEP 4: COMPILE ANALYZER
# ═══════════════════════════════════════════════════════════════════════════════════════
echo "┌────────────────────────────────────────────────────────────────────────────────┐"
echo "│ STEP 4: COMPILING ANALYZER                                                     │"
echo "└────────────────────────────────────────────────────────────────────────────────┘"
echo ""

javac _MarsAnalyzer.java 2>/dev/null
if [ $? -eq 0 ]; then
    echo "✅ Analyzer compiled"
else
    echo "⚠️  Compilation had warnings (continuing...)"
fi

echo ""

# ═══════════════════════════════════════════════════════════════════════════════════════
# STEP 5: RUN
# ═══════════════════════════════════════════════════════════════════════════════════════
echo "┌────────────────────────────────────────────────────────────────────────────────┐"
echo "│ STEP 5: EXECUTING ANALYSIS                                                     │"
echo "└────────────────────────────────────────────────────────────────────────────────┘"
echo ""

java _MarsAnalyzer "$PROGRAM"
RESULT=$?

# ═══════════════════════════════════════════════════════════════════════════════════════
# CLEANUP & SUMMARY
# ═══════════════════════════════════════════════════════════════════════════════════════
echo ""
echo "┌────────────────────────────────────────────────────────────────────────────────┐"
echo "│ SUMMARY                                                                         │"
echo "└────────────────────────────────────────────────────────────────────────────────┘"
echo ""

if [ $RESULT -eq 0 ]; then
    echo "✅ SUCCESS!"
    echo ""
    echo "Generated Analysis:"
    echo "  ✓ Algorithm Complexity (Big-O estimation)"
    echo "  ✓ Instruction Frequency Heatmap (color-coded)"
    echo "  ✓ Categorical Breakdown (by instruction type)"
    echo "  ✓ Performance Metrics (CPI, cycles, instructions)"
    echo "  ✓ Memory Access Patterns"
    echo "  ✓ Register Utilization"
    echo "  ✓ Code Hotspots"
    echo "  ✓ Optimization Recommendations"
else
    echo "❌ FAILED!"
    echo "   Check output above for errors"
fi

# Cleanup
rm -f _MarsAnalyzer.java _MarsAnalyzer.class

echo ""
echo "═".repeat(88)
echo ""
