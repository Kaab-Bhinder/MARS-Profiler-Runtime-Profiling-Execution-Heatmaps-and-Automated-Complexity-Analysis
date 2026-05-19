# HOW TO RUN MARS FULL - COMPLETE SETUP GUIDE

## Prerequisites

1. **Java JDK** (Java 8 or higher)
```bash
java -version
javac -version
```

If not installed:
```bash
# Ubuntu/Debian
sudo apt-get install default-jdk

# macOS
brew install openjdk

# Windows - Download from java.com
```

---

## STEP 1: Navigate to MARS Directory

```bash
cd /home/kaab/Desktop/MARS-MIPS-main
ls -la
```

You should see:
- `mars/` (source code directory)
- `bubble_sort.asm`, `linear_search.asm`, `heatmap_test.asm` (sample programs)
- `Mars.java` (main entry point)

---

## STEP 2: Compile Everything

### Option A: Full Compilation (Recommended)
```bash
# Compile all simulator and profiler classes
javac mars/simulator/*.java 2>&1

# Compile assembler
javac mars/assembler/*.java 2>&1

# Compile MIPS hardware components
javac mars/mips/hardware/*.java 2>&1

# Compile instructions
javac mars/mips/instructions/*.java 2>&1

# Compile utility classes
javac mars/util/*.java 2>&1

# Compile main classes
javac mars/Mars.java mars/MarsLaunch.java 2>&1
```

### Option B: Compile Everything at Once
```bash
find mars -name "*.java" | xargs javac 2>&1
```

### Option C: Using existing build script (if available)
```bash
# If CreateMarsJar.bat exists (Windows)
# Convert to Linux/Mac version:
javac -d . $(find mars -name "*.java")
```

---

## STEP 3: Run MARS with Different Interfaces

### Option 1: GUI (Venus IDE) - Interactive
```bash
java mars.MarsLaunch
```

This opens the Venus IDE where you can:
- Edit MIPS programs
- Assemble code
- Run step-by-step or continuously
- View registers, memory, symbols
- See execution heatmap
- View profiler data

### Option 2: Splash Screen (Startup)
```bash
java mars.MarsSplashScreen
```

Shows splash screen then launches GUI.

### Option 3: Command Line (Headless)
```bash
# Simple run
java mars.Mars bubble_sort.asm

# With verbose output
java mars.Mars -d bubble_sort.asm

# Multiple files
java mars.Mars file1.asm file2.asm file3.asm
```

---

## STEP 4: Run Full Analysis (Profiler + Heatmap + Complexity)

### Method A: Create Quick Analyzer Script

Create file `run_full_analysis.java`:

```java
import mars.simulator.*;
import mars.*;

public class run_full_analysis {
    public static void main(String[] args) throws Exception {
        String program = args.length > 0 ? args[0] : "bubble_sort.asm";
        
        System.out.println("\n╔" + "═".repeat(86) + "╗");
        System.out.println("║  MARS FULL ANALYSIS - Profiler + Heatmap + Complexity Analysis" + " ".repeat(20) + "║");
        System.out.println("╚" + "═".repeat(86) + "╝\n");
        
        try {
            // Reset all profilers
            ProfilerService.getInstance().reset();
            ExecutionHeatmap.getInstance().reset();
            
            System.out.println("▶  Program: " + program);
            System.out.println("▶  Running...\n");
            
            // Execute program
            Mars mars = new Mars();
            mars.assemble(new String[]{program}, true, true);
            mars.simulate();
            
            System.out.println("\n✅ Execution Complete!\n");
            
            // Generate full report with all visualizations
            PerformanceAnalysisReport report = new PerformanceAnalysisReport();
            report.printFullReport();
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

Run it:
```bash
javac run_full_analysis.java
java run_full_analysis bubble_sort.asm
```

---

## STEP 5: Different Analysis Views

### View 1: Just Instruction Heatmap

```java
import mars.simulator.*;

// After running a program:
PerformanceAnalysisReport report = new PerformanceAnalysisReport();
report.printInstructionHeatmap();
```

Output shows:
- Each instruction with count
- Color intensity bars
- Execution percentage

### View 2: Categorical Breakdown

```java
PerformanceAnalysisReport report = new PerformanceAnalysisReport();
report.printCategoricalHeatmap();
```

Shows:
- Arithmetic instructions
- Memory loads/stores
- Branches
- Other operations

### View 3: Top N Instructions

```java
PerformanceAnalysisReport report = new PerformanceAnalysisReport();
report.printTopInstructions(15);  // Top 15
```

### View 4: Full Comprehensive Report (Everything)

```java
PerformanceAnalysisReport report = new PerformanceAnalysisReport();
report.printFullReport();
```

Contains:
✅ Algorithm Complexity Analysis
✅ Instruction Frequency Heatmap
✅ Categorical Heatmap
✅ Detailed Performance Metrics
✅ Register Utilization
✅ Memory Access Patterns
✅ Code Hotspots
✅ Optimization Recommendations

### View 5: Summary Report (One Page)

```java
PerformanceAnalysisReport report = new PerformanceAnalysisReport();
report.printSummaryReport();
```

Quick overview with key metrics.

---

## COMPLETE EXAMPLE: Full Workflow

Create file `FullDemo.java`:

```java
import mars.simulator.*;
import mars.*;

public class FullDemo {
    public static void main(String[] args) throws Exception {
        
        // ═══════════════════════════════════════════════════════════════════
        // 1. RESET PROFILERS
        // ═══════════════════════════════════════════════════════════════════
        System.out.println("\n[1/4] Resetting profilers...");
        ProfilerService profiler = ProfilerService.getInstance();
        ExecutionHeatmap heatmap = ExecutionHeatmap.getInstance();
        profiler.reset();
        heatmap.reset();
        
        // ═══════════════════════════════════════════════════════════════════
        // 2. RUN PROGRAM
        // ═══════════════════════════════════════════════════════════════════
        System.out.println("[2/4] Running bubble_sort.asm...");
        Mars mars = new Mars();
        mars.assemble(new String[]{"bubble_sort.asm"}, true, true);
        mars.simulate();
        System.out.println("      ✓ Execution complete");
        
        // ═══════════════════════════════════════════════════════════════════
        // 3. ANALYZE RESULTS
        // ═══════════════════════════════════════════════════════════════════
        System.out.println("[3/4] Analyzing profiler data...");
        
        System.out.println("\n  Total Instructions: " + profiler.getTotalInstructions());
        System.out.println("  Total Cycles: " + profiler.getTotalCycles());
        System.out.println("  CPI: " + String.format("%.2f", profiler.getCyclesPerInstruction()));
        System.out.println("  Memory Reads: " + profiler.getTotalMemoryReads());
        System.out.println("  Memory Writes: " + profiler.getTotalMemoryWrites());
        
        // ═══════════════════════════════════════════════════════════════════
        // 4. GENERATE REPORTS
        // ═══════════════════════════════════════════════════════════════════
        System.out.println("[4/4] Generating reports...\n");
        
        PerformanceAnalysisReport report = new PerformanceAnalysisReport();
        report.printFullReport();
    }
}
```

Run it:
```bash
javac FullDemo.java
java FullDemo
```

---

## Quick Reference: All Commands

### Compile
```bash
# All at once
find mars -name "*.java" | xargs javac

# Specific components
javac mars/simulator/*.java
javac mars/assembler/*.java
javac mars/mips/hardware/*.java
```

### Run GUI
```bash
java mars.MarsLaunch          # Main UI
java mars.MarsSplashScreen    # With splash
```

### Run CLI
```bash
java mars.Mars bubble_sort.asm        # Simple
java mars.Mars -d bubble_sort.asm     # Debug mode
java mars.Mars file1.asm file2.asm    # Multiple files
```

### Run Analysis
```bash
# Create analysis file
cat > Analyze.java << 'EOF'
import mars.simulator.*;
import mars.*;
public class Analyze {
    public static void main(String[] a) throws Exception {
        ProfilerService.getInstance().reset();
        ExecutionHeatmap.getInstance().reset();
        Mars m = new Mars();
        m.assemble(new String[]{a.length > 0 ? a[0] : "bubble_sort.asm"}, true, true);
        m.simulate();
        new PerformanceAnalysisReport().printFullReport();
    }
}
EOF

javac Analyze.java
java Analyze bubble_sort.asm
```

---

## Test Programs Included

### 1. Bubble Sort (O(n²))
```bash
java mars.Mars bubble_sort.asm
```
Expected:
- Quadratic complexity detected
- Nesting level: 2
- ADD instruction dominant

### 2. Linear Search (O(n))
```bash
java mars.Mars linear_search.asm
```
Expected:
- Linear complexity detected
- Nesting level: 1
- BEQ instruction dominant

### 3. Heatmap Test
```bash
java mars.Mars heatmap_test.asm
```
Expected:
- Shows execution frequency visualization
- Demonstrates hotspot detection

---

## Expected Output Format

When you run the full analysis, you'll see:

```
╔════════════════════════════════════════════════════════════════════════════════╗
║                     MARS PERFORMANCE ANALYSIS REPORT                          ║
╚════════════════════════════════════════════════════════════════════════════════╝

────────────────────────────────────────────────────────────────────────────────
EXECUTION SUMMARY:
────────────────────────────────────────────────────────────────────────────────
  Total Instructions Executed:    49,232
  Total Clock Cycles:             73,848
  Average Cycles Per Instruction: 1.50

════════════════════════════════════════════════════════════════════════════════════
                    INSTRUCTION FREQUENCY HEATMAP
════════════════════════════════════════════════════════════════════════════════════

Instruction        Count      Percent Heatmap Visualization                 INTENSITY
─────────────────────────────────────────────────────────────────────────────────
add              12,453     25.30%  [████████████████░░░░░░░░░░░░░░░░]  Very High
lw                8,932     18.15%  [██████████░░░░░░░░░░░░░░░░░░░░░░]  High
sw                5,421     11.02%  [███████░░░░░░░░░░░░░░░░░░░░░░░░░]  High
beq               3,214      6.53%  [████░░░░░░░░░░░░░░░░░░░░░░░░░░░░]  Medium
...

[More sections for complexity, metrics, recommendations]
```

---

## Troubleshooting

### Issue: "Cannot find symbol"
**Solution:**
```bash
# Recompile everything
find mars -name "*.java" | xargs javac -cp .
```

### Issue: "Exception in thread main"
**Solution:**
```bash
# Check if classes were compiled
ls mars/simulator/*.class | head

# If not, compile with verbose
javac -verbose mars/simulator/*.java
```

### Issue: "File not found"
**Solution:**
```bash
# Ensure you're in the right directory
pwd  # Should be /home/kaab/Desktop/MARS-MIPS-main

# Check program exists
ls bubble_sort.asm linear_search.asm heatmap_test.asm
```

### Issue: Heatmap not showing colors
**Solution:**
```bash
# Some terminals need UTF-8
export LC_ALL=en_US.UTF-8
java mars.Mars bubble_sort.asm
```

---

## Summary: Absolute Quickest Start

```bash
# 1. Navigate
cd /home/kaab/Desktop/MARS-MIPS-main

# 2. Compile
find mars -name "*.java" | xargs javac 2>/dev/null

# 3. Run with analysis
java mars.Mars bubble_sort.asm

# 4. See full report with heatmap
java -cp . << 'EOF'
import mars.simulator.*;
import mars.*;
class X {
    public static void main(String[] a) throws Exception {
        ProfilerService.getInstance().reset();
        ExecutionHeatmap.getInstance().reset();
        Mars m = new Mars();
        m.assemble(new String[]{"bubble_sort.asm"}, true, true);
        m.simulate();
        new PerformanceAnalysisReport().printFullReport();
    }
}
EOF
```

Done! 🚀
