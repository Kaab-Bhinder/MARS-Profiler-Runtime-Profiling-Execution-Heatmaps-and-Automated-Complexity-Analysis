# HOW TO RUN THE INSTRUCTION HEATMAP & TIME COMPLEXITY ANALYSIS

## Quick Start (5 minutes)

### Step 1: Compile the Project
```bash
cd /home/kaab/Desktop/MARS-MIPS-main
javac -d . mars/simulator/*.java mars/assembler/*.java mars/mips/hardware/*.java mars/simulator/*.java
```

Or if you have a build script:
```bash
./CreateMarsJar.bat  # On Windows
# or
ant build  # If Ant is configured
```

### Step 2: Run a Sample Program with Analysis

#### Option A: Using GUI (Venus IDE)
```bash
java mars.MarsSplashScreen
# OR
java mars.MarsLaunch
```

Then:
1. Open a MIPS file: File → Open → `bubble_sort.asm`
2. Run the program: Run → Go (or press F5)
3. The profiler automatically collects data
4. Check Tools menu for profiler output

#### Option B: Using Command Line
```bash
# Run program and collect profiling data
java mars.Mars bubble_sort.asm

# With profiling enabled (if integrated)
java mars.Mars -profile bubble_sort.asm
```

---

## Display the Heatmap Reports

### Method 1: Via Performance Report (Recommended)

Add this to your Java code after running a MARS program:

```java
import mars.simulator.*;

public class HeatmapDemo {
    public static void main(String[] args) {
        // After running MARS program (profiler data is collected)
        
        // Create report
        PerformanceAnalysisReport report = new PerformanceAnalysisReport();
        
        // Display full report with heatmaps
        report.printFullReport();
    }
}
```

Compile and run:
```bash
javac -cp /home/kaab/Desktop/MARS-MIPS-main HeatmapDemo.java
java -cp /home/kaab/Desktop/MARS-MIPS-main:. HeatmapDemo
```

### Method 2: Direct Heatmap Visualization

```java
import mars.simulator.*;

PerformanceAnalysisReport report = new PerformanceAnalysisReport();

// Just the instruction heatmap
report.printInstructionHeatmap();

// Or categorical breakdown
report.printCategoricalHeatmap();

// Or top 10 instructions
report.printTopInstructions(10);
```

### Method 3: From MARS Directly

Integrate into existing MARS profiler tool:

```java
import mars.simulator.*;

public class ProfilerToolIntegration {
    public void displayHeatmap() {
        PerformanceAnalysisReport report = new PerformanceAnalysisReport();
        InstructionHeatmapVisualizer viz = report.getInstructionHeatmap();
        
        // Get the heatmap report as string
        String heatmapText = viz.generateInstructionHeatmapReport();
        
        // Display in your tool window
        this.textArea.setText(heatmapText);
    }
}
```

---

## Complete Example: Bubble Sort Analysis

### Step 1: Create Test Program (if needed)
The file `bubble_sort.asm` should already be in the directory.

### Step 2: Create Analysis Script

Create file `AnalyzeProgram.java`:

```java
import mars.simulator.*;
import mars.*;

public class AnalyzeProgram {
    public static void main(String[] args) throws Exception {
        // Reset profilers
        ProfilerService.getInstance().reset();
        ExecutionHeatmap.getInstance().reset();
        
        // Run a MARS program
        String[] programArgs = {"bubble_sort.asm"};
        
        // Execute via MARS
        Mars mars = new Mars();
        mars.assemble(programArgs, true, true);
        mars.simulate();
        
        // Now generate reports
        System.out.println("\n" + "=".repeat(90));
        System.out.println("BUBBLE SORT - PERFORMANCE ANALYSIS");
        System.out.println("=".repeat(90));
        
        // Show full report (includes heatmaps)
        PerformanceAnalysisReport report = new PerformanceAnalysisReport();
        report.printFullReport();
    }
}
```

### Step 3: Run Analysis

```bash
cd /home/kaab/Desktop/MARS-MIPS-main
javac AnalyzeProgram.java
java AnalyzeProgram
```

---

## What You'll See

The output will include:

### 1. **Instruction Frequency Heatmap**
```
═════════════════════════════════════════════════════════════════════════════════════╗
                    INSTRUCTION FREQUENCY HEATMAP
═════════════════════════════════════════════════════════════════════════════════════╗

Instruction        Count      Percent Heatmap Visualization                 INTENSITY
─────────────────────────────────────────────────────────────────────────────────────
add              12,453     25.30%  [████████████████░░░░░░░░░░░░░░░░░░░]  Very High    (████)
lw                8,932     18.15%  [██████████████░░░░░░░░░░░░░░░░░░░░░░]  High         (▓▓▓▓)
sw                5,421     11.02%  [████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░]  High         (▓▓▓▓)
beq               3,214      6.53%  [████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░]  Medium       (▒▒▒▒)
addi              2,890      5.88%  [███░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░]  Medium       (▒▒▒▒)
slt               1,543      3.14%  [██░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░]  Low          (░░░░)
j                   876      1.78%  [█░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░]  Very Low     (░░░░)

Legend:
  [░░░░] = Low    (0-20%)      Green zone   - Low frequency
  [▒▒▒▒] = Medium (20-40%)     Yellow zone  - Moderate frequency
  [▓▓▓▓] = High   (40-60%)     Orange zone  - Frequently executed
  [████] = Very High (60-100%) Red zone     - Very frequent
═════════════════════════════════════════════════════════════════════════════════════╗
```

### 2. **Categorical Heatmap**
```
              INSTRUCTION CATEGORY HEATMAP
─────────────────────────────────────────────────────────────────────────────────────
Category       Count      Percent Heatmap Visualization                  INTENSITY
─────────────────────────────────────────────────────────────────────────────────────
Arithmetic    25,896     52.65%  [████████████████████░░░░░░░░░░░░░░░░]  Very High
Memory Load    8,932     18.15%  [██████████░░░░░░░░░░░░░░░░░░░░░░░░░░]  High
Memory Store   5,421     11.02%  [███████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░]  High
Branches       4,090     8.32%   [█████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░]  Medium
Other          3,661     7.44%   [█████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░]  Medium
```

### 3. **Algorithm Complexity**
```
ESTIMATED COMPLEXITY:
─────────────────────────────────────────────────────────────────────────────────────
  O(n²) - Quadratic complexity detected
```

---

## Example Programs to Test

### 1. **Bubble Sort** (Should show O(n²))
```bash
java mars.Mars bubble_sort.asm
# Watch for:
# - Nested loops (nesting level: 2)
# - Quadratic complexity
# - High ADD and LW instructions
```

### 2. **Linear Search** (Should show O(n))
```bash
java mars.Mars linear_search.asm
# Watch for:
# - Single loop (nesting level: 1)
# - Linear complexity
# - High BEQ (branch) instructions
```

### 3. **Heatmap Test** (Execution pattern demo)
```bash
java mars.Mars heatmap_test.asm
# Visualizes hotspots and execution frequency
```

---

## Integration with Venus UI

To add heatmap visualization to Venus:

### In `mars/venus/MainUIListener.java`:

```java
// Add menu item
JMenuItem heatmapItem = new JMenuItem("Instruction Heatmap");
heatmapItem.addActionListener(e -> {
    PerformanceAnalysisReport report = new PerformanceAnalysisReport();
    String heatmapText = report.getInstructionHeatmap()
                               .generateInstructionHeatmapReport();
    
    // Display in new window
    JTextArea textArea = new JTextArea(heatmapText);
    textArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
    
    JFrame frame = new JFrame("Instruction Heatmap");
    frame.add(new JScrollPane(textArea));
    frame.setSize(800, 600);
    frame.setVisible(true);
});

toolsMenu.add(heatmapItem);
```

---

## Command-Line Usage

### Quick Profile with Output

Create `profile.sh`:

```bash
#!/bin/bash
cd /home/kaab/Desktop/MARS-MIPS-main

# Run MARS and pipe output
java mars.Mars $1 2>&1 | tee mars_output.txt

# Generate analysis
java -cp . mars.simulator.ComplexityAnalysisExample

echo "Analysis complete. Check output above."
```

Usage:
```bash
chmod +x profile.sh
./profile.sh bubble_sort.asm
```

---

## Troubleshooting

### Problem: "Class not found" error
**Solution:** Ensure all simulator classes are compiled:
```bash
javac mars/simulator/*.java
javac mars/assembler/*.java
javac mars/mips/hardware/*.java
javac mars/mips/instructions/*.java
```

### Problem: No profiler data
**Solution:** Profiler is automatically enabled. If it doesn't work:
```java
// Ensure profiler is recording
ProfilerService profiler = ProfilerService.getInstance();
profiler.startRecording();
```

### Problem: Heatmap not showing colors in terminal
**Solution:** Some terminals don't support Unicode. Use text-only version:
```java
// Instead of Unicode chars, use ASCII
report.getInstructionHeatmap().generateInstructionHeatmapReport();
// Contains [████] style bars compatible with all terminals
```

---

## Display Formats

### 1. **Console/Terminal** (ASCII)
```java
report.printFullReport();  // Default - uses Unicode but compatible
```

### 2. **HTML** (for web UI)
```java
String html = report.getInstructionHeatmap().generateHTMLHeatmap();
// Suitable for integration with web dashboard
```

### 3. **Programmatic** (for custom UI)
```java
InstructionHeatmapVisualizer viz = report.getInstructionHeatmap();
int intensity = viz.getInstructionHeatmapIntensity("add");
// Returns 0-100 for custom color mapping
```

---

## Summary

### Quickest Way to See Heatmap:

```bash
cd /home/kaab/Desktop/MARS-MIPS-main
javac mars/simulator/*.java
java mars.Mars bubble_sort.asm
# Then in your code:
# PerformanceAnalysisReport report = new PerformanceAnalysisReport();
# report.printFullReport();
```

### Key Files:
- `mars/simulator/InstructionHeatmapVisualizer.java` - Generates heatmaps
- `mars/simulator/PerformanceAnalysisReport.java` - Combines reports
- `mars/simulator/ProfilerService.java` - Collects data
- `InstructionHeatmapExample.java` - Example usage

Enjoy your heatmap visualizations! 🔥
