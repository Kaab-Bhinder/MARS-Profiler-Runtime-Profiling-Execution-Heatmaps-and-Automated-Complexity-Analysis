#!/bin/bash

# MASTER RUN SCRIPT - MARS FULL SYSTEM
# Usage: bash mars.sh [program.asm]
# Example: bash mars.sh bubble_sort.asm

cd /home/kaab/Desktop/MARS-MIPS-main

PROGRAM="${1:-bubble_sort.asm}"

echo "════════════════════════════════════════════════════════════════════════════════"
echo "                     MARS FULL ANALYSIS SYSTEM"
echo "════════════════════════════════════════════════════════════════════════════════"
echo ""
echo "📁 Directory: $(pwd)"
echo "📝 Program: $PROGRAM"
echo ""

# Compile
echo "🔨 Compiling MARS..."
find mars -name "*.java" -print0 | xargs -0 javac 2>/dev/null
echo "✅ Compilation complete"
echo ""

# Create analyzer
echo "⚙️  Setting up analyzer..."
cat > _RUN_ANALYSIS.java << 'EOF'
import mars.simulator.*;
import mars.*;

public class _RUN_ANALYSIS {
    public static void main(String[] args) throws Exception {
        String program = args.length > 0 ? args[0] : "bubble_sort.asm";
        ProfilerService.getInstance().reset();
        ExecutionHeatmap.getInstance().reset();
        Mars m = new Mars();
        m.assemble(new String[]{program}, true, true);
        m.simulate();
        new PerformanceAnalysisReport().printFullReport();
    }
}
EOF

javac _RUN_ANALYSIS.java 2>/dev/null
echo "✅ Setup complete"
echo ""

# Run
echo "🚀 Executing: $PROGRAM"
echo "════════════════════════════════════════════════════════════════════════════════"
echo ""
java _RUN_ANALYSIS "$PROGRAM"

# Cleanup
rm -f _RUN_ANALYSIS.java _RUN_ANALYSIS.class

echo ""
echo "════════════════════════════════════════════════════════════════════════════════"
echo "✅ Analysis complete!"
echo "════════════════════════════════════════════════════════════════════════════════"
