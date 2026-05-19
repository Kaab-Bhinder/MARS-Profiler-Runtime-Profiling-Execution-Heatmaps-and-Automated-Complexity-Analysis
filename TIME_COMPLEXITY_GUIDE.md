/**
 * TIME COMPLEXITY ANALYSIS INTEGRATION GUIDE
 * 
 * MARS MIPS now includes comprehensive time complexity analysis features
 * that estimate Big-O complexity, measure cycle counts, and provide 
 * optimization recommendations.
 * 
 * ============================================================================
 * COMPONENTS
 * ============================================================================
 * 
 * 1. ProfilerService (Enhanced)
 *    - Tracks instruction execution frequencies
 *    - Records register read/write operations
 *    - Tracks memory access patterns
 *    - NOW: Measures clock cycles with MIPS latency model
 *    - NEW METHODS:
 *      - getTotalCycles(): Get total clock cycles executed
 *      - getCyclesPerInstruction(): Get average CPI metric
 *      - recordMemoryLatency(): Track additional memory stalls
 * 
 * 2. ExecutionHeatmap (Existing)
 *    - Visualizes which source lines execute most frequently
 *    - Tracks execution frequency per line
 *    - Used by complexity analyzer to detect loops
 * 
 * 3. InstructionHeatmapVisualizer (NEW)
 *    - Visualizes instruction execution frequencies using heat colors
 *    - Shows each instruction with color-coded frequency
 *    - Displays:
 *      * Individual instruction frequencies with heatmap coloring
 *      * Categorical breakdown (Arithmetic, Memory, Branches)
 *      * Top N instructions with intensity bars
 *      * ASCII bar visualizations
 *      * HTML-compatible output for web UI
 *    - Color scale: White → Green → Yellow → Orange → Red
 * 
 * 4. AlgorithmComplexityAnalyzer (Enhanced)
 *    - Analyzes execution patterns
 *    - Detects loop nesting structures
 *    - Estimates Big-O complexity (O(1), O(log n), O(n), O(n log n), O(n²), O(n³))
 *    - Analyzes instruction distribution
 *    - Categorizes algorithms as compute-intensive or memory-intensive
 * 
 * 5. PerformanceAnalysisReport (Enhanced)
 *    - Generates comprehensive performance reports
 *    - NOW INCLUDES: Instruction frequency heatmaps
 *    - Provides detailed metrics breakdown
 *    - Offers optimization recommendations
 *    - Can generate full or summary reports
 * 
 * ============================================================================
 * USAGE EXAMPLE - Basic Integration
 * ============================================================================
 * 
 * // After running a MIPS program simulation:
 * 
 * import mars.simulator.*;
 * 
 * // Get singleton instances
 * ProfilerService profiler = ProfilerService.getInstance();
 * ExecutionHeatmap heatmap = ExecutionHeatmap.getInstance();
 * 
 * // Create analyzer
 * AlgorithmComplexityAnalyzer analyzer = 
 *     new AlgorithmComplexityAnalyzer(profiler, heatmap);
 * 
 * // Analyze complexity
 * AlgorithmComplexityAnalyzer.ComplexityAnalysis analysis = analyzer.analyze();
 * 
 * // Access results
 * System.out.println("Estimated Complexity: " + analysis.getEstimatedComplexity());
 * System.out.println("Total Instructions: " + analysis.getTotalInstructions());
 * System.out.println("Total Cycles: " + analysis.getTotalCycles());
 * System.out.println("CPI: " + analysis.getCyclesPerInstruction());
 * 
 * ============================================================================
 * USAGE EXAMPLE - Instruction Heatmap (NEW)
 * ============================================================================
 * 
 * // Display instruction execution frequencies with color heatmap
 * 
 * PerformanceAnalysisReport report = new PerformanceAnalysisReport();
 * 
 * // Show all instructions with heatmap coloring
 * report.printInstructionHeatmap();
 * 
 * // Or get as string
 * String heatmapReport = report.getInstructionHeatmap()
 *                              .generateInstructionHeatmapReport();
 * 
 * // Show instructions grouped by category
 * report.printCategoricalHeatmap();
 * 
 * // Show just top N instructions
 * report.printTopInstructions(10);
 * 
 * ============================================================================
 * USAGE EXAMPLE - Full Report Generation
 * ============================================================================
 * 
 * // Generate comprehensive performance report (includes heatmaps)
 * PerformanceAnalysisReport report = new PerformanceAnalysisReport();
 * 
 * // Print full detailed report (NOW INCLUDES INSTRUCTION HEATMAPS)
 * report.printFullReport();
 * 
 * // Or get as string for custom processing
 * String fullReport = report.generateFullReport();
 * String summaryReport = report.generateSummaryReport();
 * 
 * ============================================================================
 * INSTRUCTION HEATMAP FEATURES (NEW)
 * ============================================================================
 * 
 * The InstructionHeatmapVisualizer provides:
 * 
 * 1. FREQUENCY-BASED COLORING:
 *    [░░░░] Green   (0-20%)  - Rarely executed
 *    [▒▒▒▒] Yellow  (20-40%) - Moderate frequency
 *    [▓▓▓▓] Orange  (40-60%) - Frequently executed
 *    [████] Red     (60-100%)- Very frequent (hotspots)
 * 
 * 2. VISUAL INTENSITY INDICATORS:
 *    - Shows each instruction with execution percentage
 *    - ASCII bar visualization for easy scanning
 *    - Intensity labels (Low, Medium, High, Very High, Extreme)
 * 
 * 3. CATEGORICAL ANALYSIS:
 *    - Groups instructions by type:
 *      * Arithmetic operations
 *      * Memory loads
 *      * Memory stores
 *      * Branch/jump instructions
 *      * Other
 * 
 * 4. TOP-N SELECTION:
 *    - Display only top N instructions by frequency
 *    - Useful for focusing on hotspots
 * 
 * 5. MULTIPLE OUTPUT FORMATS:
 *    - ASCII text (console/terminal)
 *    - HTML (for web UI integration)
 *    - Programmatic API for custom displays
 * 
 * ============================================================================
 * HEATMAP USAGE EXAMPLE
 * ============================================================================
 * 
 * // Create report
 * PerformanceAnalysisReport report = new PerformanceAnalysisReport();
 * 
 * // Option 1: Show all instructions with colors
 * System.out.println(report.getInstructionHeatmap()
 *                          .generateInstructionHeatmapReport());
 * 
 * // Option 2: Show categorical breakdown
 * System.out.println(report.getInstructionHeatmap()
 *                          .generateCategoricalHeatmap());
 * 
 * // Option 3: Show top 10 instructions
 * System.out.println(report.getInstructionHeatmap()
 *                          .generateTopInstructionsHeatmap(10));
 * 
 * // Option 4: Get intensity for specific instruction
 * int intensity = report.getInstructionHeatmap()
 *                       .getInstructionHeatmapIntensity("add");
 * System.out.println("ADD instruction intensity: " + intensity + "%");
 * 
 * ============================================================================
 * INTEGRATION WITH PROFILER TOOL SECTION
 * ============================================================================
 * 
 * In your Profiler Tool UI (mars/tools/), you can now add:
 * 
 * 1. New tab: "Instruction Heatmap"
 *    Display: report.getInstructionHeatmap().generateInstructionHeatmapReport()
 * 
 * 2. Enhanced profiler panel with color intensity bars
 *    For each instruction: show frequency + heatmap bar
 * 
 * 3. Category breakdown with heatmap
 *    Display: report.printCategoricalHeatmap()
 * 
 * Example integration code:
 * 
 * PerformanceAnalysisReport report = new PerformanceAnalysisReport();
 * 
 * // Get the underlying complexity analysis
 * AlgorithmComplexityAnalyzer.ComplexityAnalysis analysis = 
 *     report.complexityAnalyzer.analyze();
 * 
 * // Check loop structure
 * int nestingLevel = analysis.getLoopStructure().getNestingLevel();
 * System.out.println("Loop nesting: " + nestingLevel);
 * 
 * // Check instruction distribution
 * Map<String, Double> distribution = 
 *     analysis.getInstructionDistribution();
 * 
 * ============================================================================
 * INTEGRATION WITH VENUS IDE
 * ============================================================================
 * 
 * To add a "Performance Analysis" button to Venus UI:
 * 
 * 1. In mars/venus/MainUIListener.java, add a menu item:
 *    
 *    JMenuItem analyzeComplexity = new JMenuItem("Analyze Complexity...");
 *    analyzeComplexity.addActionListener(e -> {
 *        PerformanceAnalysisReport report = new PerformanceAnalysisReport();
 *        String analysis = report.generateFullReport();
 *        // Display in text window or dialog
 *        displayInWindow(analysis);
 *    });
 * 
 * 2. Or add as a tool in mars/tools/ following the ToolPanes pattern
 * 
 * ============================================================================
 * INTEGRATION WITH COMMAND-LINE INTERFACE
 * ============================================================================
 * 
 * To add complexity analysis output to command-line execution:
 * 
 * 1. In mars/MarsLaunch.java or your CLI handler:
 * 
 *    // After running simulation
 *    if (includeComplexityAnalysis) {
 *        PerformanceAnalysisReport report = 
 *            new PerformanceAnalysisReport();
 *        System.out.println(report.generateFullReport());
 *    }
 * 
 * 2. Add command-line flags like:
 *    mars -complexity-analysis program.asm
 *    mars -profile program.asm
 * 
 * ============================================================================
 * UNDERSTANDING THE COMPLEXITY ESTIMATION
 * ============================================================================
 * 
 * The complexity analyzer uses multiple signals:
 * 
 * 1. EXECUTION HEATMAP:
 *    - Detects which lines execute most frequently
 *    - A line executing N times suggests N-nested loops
 *    - Example: Line executed 1000 times → likely O(n²) or worse
 * 
 * 2. LOOP NESTING LEVEL:
 *    - 0 levels → O(1) constant
 *    - 1 level → O(n) linear
 *    - 2 levels → O(n²) quadratic
 *    - 3+ levels → O(n³) cubic or worse
 * 
 * 3. INSTRUCTION PATTERNS:
 *    - High branch ratio → suggests divide-and-conquer (O(n log n))
 *    - Memory-heavy → may indicate sorting/searching algorithms
 *    - Compute-heavy → matrix operations, cryptography, etc.
 * 
 * 4. CYCLES PER INSTRUCTION (CPI):
 *    - CPI ≈ 1.0 → good pipeline efficiency
 *    - CPI > 2.0 → memory stalls or data dependencies
 *    - CPI > 3.0 → significant performance bottleneck
 * 
 * ============================================================================
 * MIPS INSTRUCTION LATENCIES
 * ============================================================================
 * 
 * The ProfilerService uses realistic MIPS latency model:
 * 
 * - Arithmetic (add, sub, and, or, etc): 1 cycle
 * - Load (lw, lb, lh): 3 cycles (memory access latency)
 * - Store (sw, sb, sh): 1 cycle
 * - Multiply/Divide: 1 cycle start (result latency tracked separately)
 * - Branch/Jump: 1 cycle (delay slot not causing extra stalls here)
 * 
 * Additional memory latency can be recorded via:
 * profiler.recordMemoryLatency(stalledCycles);
 * 
 * ============================================================================
 * METRICS EXPLAINED
 * ============================================================================
 * 
 * Total Instructions:
 *   - Number of MIPS instructions executed
 *   - Useful for comparing algorithms with same input size
 * 
 * Total Cycles:
 *   - Sum of all instruction latencies
 *   - Approximates actual execution time
 * 
 * CPI (Cycles Per Instruction):
 *   - Total Cycles / Total Instructions
 *   - Ideal MIPS CPI ≈ 1.0 in perfect pipeline
 *   - Real systems: 1.5-4.0 depending on memory hierarchy
 * 
 * Memory Intensity:
 *   - (Memory Reads + Memory Writes) / Total Instructions
 *   - < 0.1: Compute-intensive
 *   - 0.1-0.3: Moderately memory-intensive
 *   - > 0.3: Memory-intensive (bandwidth-bound)
 * 
 * ============================================================================
 * EXAMPLE OUTPUTS
 * ============================================================================
 * 
 * BUBBLE SORT (Quadratic):
 *   - Estimated Complexity: O(n²) - Quadratic complexity detected
 *   - Instructions: ~10,000
 *   - Cycles: ~15,000
 *   - CPI: 1.5
 *   - Loop Nesting: 2
 * 
 * LINEAR SEARCH:
 *   - Estimated Complexity: O(n) - Linear complexity detected
 *   - Instructions: ~5,000
 *   - Cycles: ~5,000
 *   - CPI: 1.0
 *   - Loop Nesting: 1
 * 
 * BINARY SEARCH:
 *   - Estimated Complexity: O(log n) - Logarithmic complexity
 *   - Instructions: ~200
 *   - Cycles: ~300
 *   - CPI: 1.5
 *   - Branch intensity: High (divide-and-conquer pattern)
 * 
 * ============================================================================
 * LIMITATIONS & FUTURE IMPROVEMENTS
 * ============================================================================
 * 
 * Current Limitations:
 * - Does not account for cache behavior (simplified model)
 * - Does not detect exponential or factorial algorithms
 * - Branch prediction accuracy not modeled
 * - Floating-point operations not specially profiled
 * 
 * Possible Enhancements:
 * - Integrate cache simulator for realistic memory modeling
 * - Add exponential/factorial pattern detection
 * - Branch prediction statistics
 * - Data dependency analysis
 * - Specialized floating-point profiling
 * - Machine learning based complexity classification
 * 
 * ============================================================================
 * TESTING YOUR ANALYSIS
 * ============================================================================
 * 
 * Test with provided sample programs:
 * 
 * 1. Linear search (should detect O(n)):
 *    > mars linear_search.asm
 * 
 * 2. Bubble sort (should detect O(n²)):
 *    > mars bubble_sort.asm
 * 
 * 3. Heatmap test (should show execution patterns):
 *    > mars heatmap_test.asm
 * 
 * ============================================================================
 * 
 * For more information, see:
 * - mars/simulator/ProfilerService.java
 * - mars/simulator/ExecutionHeatmap.java
 * - mars/simulator/AlgorithmComplexityAnalyzer.java
 * - mars/simulator/PerformanceAnalysisReport.java
 */
