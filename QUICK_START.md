# MARS RUN - QUICK REFERENCE

## ABSOLUTE QUICKEST WAY (Copy & Paste)

### Option 1: One Command (Everything)
```bash
cd /home/kaab/Desktop/MARS-MIPS-main && \
find mars -name "*.java" | xargs javac 2>/dev/null && \
cat > A.java << 'E' && javac A.java && java A bubble_sort.asm
import mars.simulator.*;import mars.*;
class A{public static void main(String[]a)throws Exception{
ProfilerService.getInstance().reset();ExecutionHeatmap.getInstance().reset();
Mars m=new Mars();m.assemble(new String[]{a.length>0?a[0]:"bubble_sort.asm"},true,true);
m.simulate();new PerformanceAnalysisReport().printFullReport();}}E
```

---

## SIMPLE 3-STEP APPROACH

### Step 1: Compile (1 command)
```bash
cd /home/kaab/Desktop/MARS-MIPS-main && find mars -name "*.java" | xargs javac 2>/dev/null
```

### Step 2: Create Analyzer (1 command)
```bash
cd /home/kaab/Desktop/MARS-MIPS-main && cat > RunAnalysis.java << 'EOF'
import mars.simulator.*;import mars.*;
class RunAnalysis{
  public static void main(String[]a)throws Exception{
    ProfilerService.getInstance().reset();
    ExecutionHeatmap.getInstance().reset();
    Mars m=new Mars();
    m.assemble(new String[]{a.length>0?a[0]:"bubble_sort.asm"},true,true);
    m.simulate();
    new PerformanceAnalysisReport().printFullReport();
  }
}
EOF
```

### Step 3: Run (1 command)
```bash
cd /home/kaab/Desktop/MARS-MIPS-main && javac RunAnalysis.java && java RunAnalysis bubble_sort.asm
```

---

## USING PROVIDED SCRIPTS

### GUI Mode
```bash
cd /home/kaab/Desktop/MARS-MIPS-main
java mars.MarsLaunch
```

### Command Line Mode
```bash
cd /home/kaab/Desktop/MARS-MIPS-main
java mars.Mars bubble_sort.asm
```

### Full Analysis (No GUI)
```bash
cd /home/kaab/Desktop/MARS-MIPS-main
bash mars_full_run.sh bubble_sort.asm
```

---

## Test Different Programs

### Bubble Sort (O(n²))
```bash
cd /home/kaab/Desktop/MARS-MIPS-main
java mars.Mars bubble_sort.asm
```

### Linear Search (O(n))
```bash
cd /home/kaab/Desktop/MARS-MIPS-main
java mars.Mars linear_search.asm
```

### Heatmap Demo
```bash
cd /home/kaab/Desktop/MARS-MIPS-main
java mars.Mars heatmap_test.asm
```

---

## What You'll See

Each run produces:

```
═══════════════════════════════════════════════════════════════════════════════════════
                     MARS PERFORMANCE ANALYSIS REPORT
═══════════════════════════════════════════════════════════════════════════════════════

EXECUTION SUMMARY:
  Total Instructions Executed:    49,232
  Total Clock Cycles:             73,848
  Average Cycles Per Instruction: 1.50

INSTRUCTION FREQUENCY HEATMAP:
Instruction        Count      Percent Heatmap Visualization                 INTENSITY
─────────────────────────────────────────────────────────────────────────────────
add              12,453     25.30%  [████████████████░░░░░░░░░░░░░░░░]  Very High
lw                8,932     18.15%  [██████████░░░░░░░░░░░░░░░░░░░░░░]  High
sw                5,421     11.02%  [███████░░░░░░░░░░░░░░░░░░░░░░░░░]  High
beq               3,214      6.53%  [████░░░░░░░░░░░░░░░░░░░░░░░░░░░░]  Medium
[... more ...]

ESTIMATED COMPLEXITY:
  O(n²) - Quadratic complexity detected

[... more metrics, recommendations ...]
```

---

## Files Created

✅ **InstructionHeatmapVisualizer.java** - Color-coded instruction frequency
✅ **PerformanceAnalysisReport.java** - Generates complete reports
✅ **AlgorithmComplexityAnalyzer.java** - Estimates Big-O complexity
✅ **ProfilerService.java** (Enhanced) - Cycle counting + metrics
✅ **ExecutionHeatmap.java** - Line execution frequency
✅ **mars_full_run.sh** - Automated full execution script
✅ **MARS_FULL_RUN_GUIDE.md** - Complete documentation
✅ **TIME_COMPLEXITY_GUIDE.md** - Integration guide
✅ **ComplexityAnalysisExample.java** - Usage examples
✅ **InstructionHeatmapExample.java** - Heatmap examples

---

## Common Issues & Solutions

### "Command not found: javac"
```bash
# Install Java
# Ubuntu: sudo apt-get install default-jdk
# macOS: brew install openjdk
# Windows: Download from java.com
```

### "Cannot find symbol"
```bash
# Recompile everything
cd /home/kaab/Desktop/MARS-MIPS-main
find mars -name "*.java" | xargs javac -cp .
```

### "File not found: bubble_sort.asm"
```bash
# Check directory
cd /home/kaab/Desktop/MARS-MIPS-main
ls *.asm
```

### "No output / Heatmap not showing"
```bash
# Ensure proper terminal encoding
export LC_ALL=en_US.UTF-8
export LANG=en_US.UTF-8
java mars.Mars bubble_sort.asm
```

---

## What Each Component Shows

| Component | Shows |
|-----------|-------|
| **Instruction Heatmap** | Each instruction with color intensity (red=frequent, green=rare) |
| **Categorical Heatmap** | Instructions grouped by type (arithmetic/memory/branches) |
| **Complexity Analysis** | Estimated Big-O (O(n²), O(n log n), etc.) |
| **Performance Metrics** | Instructions, cycles, CPI, memory accesses |
| **Hotspots** | Most executed source code lines |
| **Recommendations** | Optimization suggestions |

---

## Keyboard Shortcuts (GUI Mode)

| Key | Action |
|-----|--------|
| F5 | Run program |
| Ctrl+F5 | Step instruction |
| Ctrl+Shift+F5 | Reset |
| Ctrl+D | View details |

---

## Summary

**Just run this:**
```bash
cd /home/kaab/Desktop/MARS-MIPS-main && \
find mars -name "*.java" | xargs javac 2>/dev/null && \
java mars.MarsLaunch
```

Then:
1. Open `bubble_sort.asm`
2. Press F5 to run
3. Check Tools menu for profiler
4. View heatmap and analysis

That's it! 🚀
