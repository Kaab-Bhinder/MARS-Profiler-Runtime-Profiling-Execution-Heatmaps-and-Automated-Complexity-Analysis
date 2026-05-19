#!/bin/bash
# Simple script to run instruction heatmap analysis on a MARS program

echo "╔════════════════════════════════════════════════════════════════════════════════╗"
echo "║          MARS Instruction Heatmap Analysis Tool                                ║"
echo "╚════════════════════════════════════════════════════════════════════════════════╝"
echo ""

# Check if program argument provided
if [ -z "$1" ]; then
    echo "Usage: ./run_heatmap.sh <program.asm>"
    echo ""
    echo "Available sample programs:"
    echo "  - bubble_sort.asm"
    echo "  - linear_search.asm"
    echo "  - heatmap_test.asm"
    echo ""
    echo "Example:"
    echo "  ./run_heatmap.sh bubble_sort.asm"
    exit 1
fi

PROGRAM=$1
MARS_DIR="/home/kaab/Desktop/MARS-MIPS-main"

# Check if program exists
if [ ! -f "$MARS_DIR/$PROGRAM" ]; then
    echo "❌ Error: Program not found at $MARS_DIR/$PROGRAM"
    exit 1
fi

echo "📊 Running: $PROGRAM"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Navigate to MARS directory
cd "$MARS_DIR"

# Compile (if needed)
if [ ! -f "mars/simulator/InstructionHeatmapVisualizer.class" ]; then
    echo "🔨 Compiling MARS simulator classes..."
    javac mars/simulator/*.java mars/assembler/*.java mars/mips/hardware/*.java 2>/dev/null
    if [ $? -eq 0 ]; then
        echo "✅ Compilation successful"
    else
        echo "⚠️ Compilation warnings (continuing...)"
    fi
    echo ""
fi

# Create temporary analysis Java file
cat > TempAnalyzer.java << 'EOF'
import mars.simulator.*;
import mars.*;

public class TempAnalyzer {
    public static void main(String[] args) throws Exception {
        try {
            // Get profilers
            ProfilerService profiler = ProfilerService.getInstance();
            ExecutionHeatmap heatmap = ExecutionHeatmap.getInstance();
            
            // Reset for fresh analysis
            profiler.reset();
            heatmap.reset();
            
            // Run the program
            String programName = args[0];
            String[] marsArgs = {programName};
            
            System.out.println("▶ Executing: " + programName);
            System.out.println("━".repeat(88));
            
            // Execute MARS
            Mars mars = new Mars();
            mars.assemble(marsArgs, true, true);
            mars.simulate();
            
            System.out.println("\n✅ Program execution complete!");
            System.out.println("━".repeat(88));
            
            // Generate reports
            PerformanceAnalysisReport report = new PerformanceAnalysisReport();
            
            // Display full analysis
            report.printFullReport();
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
EOF

# Compile and run analyzer
echo "🚀 Analyzing program..."
echo ""

javac TempAnalyzer.java 2>/dev/null
if [ $? -eq 0 ]; then
    java TempAnalyzer "$PROGRAM" 2>/dev/null
    RESULT=$?
    
    if [ $RESULT -eq 0 ]; then
        echo ""
        echo "━".repeat(88)
        echo "✅ Analysis complete!"
        echo ""
        echo "📈 Report includes:"
        echo "   • Algorithm Complexity Analysis (Big-O Estimation)"
        echo "   • Instruction Frequency Heatmap"
        echo "   • Categorical Breakdown (Arithmetic/Memory/Branches)"
        echo "   • Performance Metrics (CPI, Cycles, Instructions)"
        echo "   • Memory Access Patterns"
        echo "   • Optimization Recommendations"
    else
        echo "⚠️ Analysis completed with warnings. Check output above."
    fi
else
    echo "❌ Compilation failed. Run from MARS directory."
    exit 1
fi

# Cleanup
rm -f TempAnalyzer.java TempAnalyzer.class

echo ""
echo "━".repeat(88)
