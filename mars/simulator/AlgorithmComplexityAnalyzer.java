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
 * Reports the time complexity of a MIPS program.
 *
 * The authoritative classification comes from {@link ComplexityFitter}, which
 * fits a growth curve measured across several input sizes.  This class does not
 * classify complexity from a single execution.  It cannot: the number of
 * instructions a program executes at one input size carries no information
 * about how that number grows, and any rule that maps a magnitude onto a Big-O
 * class is guessing.  An earlier version of this class did exactly that, with
 * thresholds such as "hot line executed more than 1000 times implies O(n^3)";
 * those thresholds have been removed.  Among other failures they could not
 * classify a genuinely constant-time program, because a constant-time program
 * with a large constant is indistinguishable from a growing one when you only
 * look at one point.
 *
 * What a single run can still contribute is a STRUCTURAL HINT: the loop nesting
 * depth of the source, obtained by static analysis of backward control
 * transfers.  That is reported separately and is explicitly subordinate to the
 * empirical fit.  Nesting depth is only an upper bound on the loop-driven part
 * of the cost and says nothing about how many times each loop runs, so it
 * misreads any loop whose trip count is sublinear -- binary search, for
 * instance, has depth 1 and would be hinted as O(n) while actually being
 * O(log n).  When the hint and the empirical fit disagree, both are reported
 * and the disagreement is flagged rather than hidden.
 *
 * @author MARS Contributors
 * @version 2025
 */
public class AlgorithmComplexityAnalyzer {

    private ProfilerService profiler;
    private ExecutionHeatmap heatmap;

    /** Empirical fit for the program under analysis; null until one is supplied. */
    private ComplexityFitter.FitReport empiricalFit;

    /** Source lines used for static structural analysis; null if unavailable. */
    private List<String> sourceLines;

    /**
     * Constructor - takes profiler and heatmap instances
     * @param profilerService the ProfilerService singleton
     * @param executionHeatmap the ExecutionHeatmap singleton
     */
    public AlgorithmComplexityAnalyzer(ProfilerService profilerService, ExecutionHeatmap executionHeatmap) {
        this.profiler = profilerService;
        this.heatmap = executionHeatmap;
        this.empiricalFit = null;
        this.sourceLines = null;
    }

    /**
     * Supply the multi-size empirical fit for this program.  Once set, it is
     * the reported classification.
     * @param fitReport ranked fit produced by ComplexityFitter, or null to clear
     */
    public void setEmpiricalFit(ComplexityFitter.FitReport fitReport) {
        this.empiricalFit = fitReport;
    }

    /**
     * @return the empirical fit, or null if none has been supplied
     */
    public ComplexityFitter.FitReport getEmpiricalFit() {
        return empiricalFit;
    }

    /**
     * Supply the program source so that loop nesting can be analysed
     * statically.  If not set, the analyzer falls back to the currently loaded
     * program, and reports the hint as unavailable if there is none.
     * @param sourceLines the MIPS source, one String per line
     */
    public void setSourceLines(List<String> sourceLines) {
        this.sourceLines = sourceLines;
    }

    /**
     * Analyze the program.
     *
     * The returned analysis always carries the measured execution facts.  It
     * carries a complexity CLASSIFICATION only if an empirical fit has been
     * supplied via {@link #setEmpiricalFit}; otherwise the classification field
     * states that a multi-size run is required.
     *
     * @return ComplexityAnalysis with measurements, structural hint and, if
     *         available, the empirical classification
     */
    public ComplexityAnalysis analyze() {
        ComplexityAnalysis analysis = new ComplexityAnalysis();

        // Measured facts about this single run.
        analysis.setTotalCycles(profiler.getTotalCycles());
        analysis.setTotalInstructions(profiler.getTotalInstructions());
        analysis.setCyclesPerInstruction(profiler.getCyclesPerInstruction());
        analysis.setInstructionDistribution(analyzeInstructionDistribution());

        // Structural hint from static analysis of the source, plus the observed
        // hot-line count.  Both are descriptive; neither classifies.
        StructuralHint hint = computeStructuralHint();
        analysis.setStructuralHint(hint);
        analysis.setLoopStructure(buildLoopStructure(hint));

        // Authoritative classification, if we have one.
        analysis.setEmpiricalFit(empiricalFit);
        analysis.setEstimatedComplexity(describeClassification(hint));

        return analysis;
    }

    /**
     * Compose the string reported as the program's complexity.  With an
     * empirical fit this is the fitted class (or "inconclusive"); without one
     * it says plainly that no classification is available.
     */
    private String describeClassification(StructuralHint hint) {
        if (empiricalFit == null) {
            String hintText = (hint != null && hint.isAvailable())
                ? " Static structure hints at " + hint.getImpliedClass()
                  + " (loop nesting depth " + hint.getNestingDepth()
                  + "), but nesting depth is an upper bound on loop cost only and"
                  + " is not a measurement."
                : "";
            return "Not determined - complexity requires execution at multiple input"
                + " sizes; run EmpiricalComplexityRunner over this program." + hintText;
        }

        ComplexityFitter.FitResult best = empiricalFit.getBest();
        if (best == null) {
            return "Not determined - the empirical fit produced no candidates.";
        }

        StringBuilder sb = new StringBuilder();
        if (empiricalFit.isNoCandidateFit()) {
            // Distinct from "inconclusive": no candidate describes the data at
            // all, so naming the least-bad one would be misleading.
            sb.append("No candidate class fits - closest is ")
              .append(best.getComplexityClass());
            sb.append(String.format(" at normalised MSE %.3e, above the %.1e ceiling",
                best.getNormalisedMse(), ComplexityFitter.MAX_ACCEPTABLE_NMSE));
            if (!Double.isNaN(empiricalFit.getEstimatedExponent())) {
                sb.append(String.format("; measured growth is closer to n^%.3f",
                    empiricalFit.getEstimatedExponent()));
            }
        } else if (empiricalFit.isInconclusive()) {
            ComplexityFitter.FitResult runnerUp = empiricalFit.getRunnerUp();
            sb.append("Inconclusive - best fit ").append(best.getComplexityClass());
            if (runnerUp != null) {
                sb.append(" is not separable from ").append(runnerUp.getComplexityClass());
            }
            sb.append(String.format(" (confidence %.3f, below the %.2f threshold)",
                best.getConfidence(), ComplexityFitter.LOW_CONFIDENCE_THRESHOLD));
        } else {
            sb.append(best.getComplexityClass());
            sb.append(String.format(" (empirical fit, confidence %.3f, R^2 %.4f)",
                best.getConfidence(), best.getRSquared()));
        }

        if (hint != null && hint.isAvailable()
                && !hint.getImpliedClass().equals(best.getComplexityClass())) {
            sb.append(" [DISAGREES with structural hint ").append(hint.getImpliedClass())
              .append("]");
        }
        return sb.toString();
    }

    /**
     * Run static structural analysis over whatever source is available.
     */
    private StructuralHint computeStructuralHint() {
        List<String> lines = sourceLines;
        if (lines == null && Globals.program != null) {
            ArrayList programSource = Globals.program.getSourceList();
            if (programSource != null) {
                lines = new ArrayList<String>();
                for (int i = 0; i < programSource.size(); i++) {
                    Object entry = programSource.get(i);
                    lines.add(entry == null ? "" : entry.toString());
                }
            }
        }
        if (lines == null) {
            return StructuralHint.unavailable("no source available for static analysis");
        }
        return analyzeStaticStructure(lines);
    }

    /**
     * Populate the legacy LoopStructure view.  Nesting level now comes from
     * static analysis rather than from thresholds on execution counts; the
     * execution counts it carries are reported as the raw measurements they
     * are.
     */
    private LoopStructure buildLoopStructure(StructuralHint hint) {
        LoopStructure structure = new LoopStructure();
        structure.setNestingLevel(hint != null && hint.isAvailable() ? hint.getNestingDepth() : -1);

        Map<Integer, Integer> execCounts = heatmap.getLineExecutionCounts();
        if (execCounts.isEmpty()) {
            return structure;
        }

        int maxCount = heatmap.getMaxExecutionCount();
        structure.setMaxExecutionCount(maxCount);

        // "Hot" means executed at least half as often as the hottest line.
        // This is a descriptive summary of where time went, not a classifier.
        int hotLines = 0;
        Iterator<Map.Entry<Integer, Integer>> it = execCounts.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Integer> entry = it.next();
            if (entry.getValue() > maxCount * 0.5) {
                hotLines++;
            }
        }
        structure.setHotLineCount(hotLines);
        return structure;
    }

    /**
     * Analyze distribution of instruction types
     * @return Map of instruction type to percentage of total
     */
    private Map<String, Double> analyzeInstructionDistribution() {
        Map<String, Double> distribution = new HashMap<String, Double>();
        Map<String, Integer> instCounts = profiler.getInstructionCounts();
        int total = profiler.getTotalInstructions();

        if (total == 0) {
            return distribution;
        }

        Iterator<String> it = instCounts.keySet().iterator();
        while (it.hasNext()) {
            String inst = it.next();
            distribution.put(inst, (instCounts.get(inst) * 100.0) / total);
        }
        return distribution;
    }

    // ==================== Static structural analysis ====================

    /**
     * Determine loop nesting depth by static analysis of the source.
     *
     * A loop is identified by a BACK EDGE: a jump or branch whose target label
     * is defined earlier in the file than the transfer itself.  Each back edge
     * spans the interval from its target label to the transfer instruction, and
     * the nesting depth is the largest number of such intervals covering any
     * single line.
     *
     * This is a genuine syntactic property of the program.  What it is not is a
     * complexity measurement: it counts loops, not iterations.  A loop that
     * halves its range each pass counts the same as one that steps through
     * every element.  The result is therefore reported as a hint only.
     *
     * @param sourceLines the MIPS source, one String per line
     * @return the structural hint derived from the source
     */
    public static StructuralHint analyzeStaticStructure(List<String> sourceLines) {
        if (sourceLines == null || sourceLines.isEmpty()) {
            return StructuralHint.unavailable("source was empty");
        }

        // Pass 1: record where each label is defined.
        Map<String, Integer> labelLines = new HashMap<String, Integer>();
        for (int i = 0; i < sourceLines.size(); i++) {
            String code = stripComment(sourceLines.get(i));
            int colon = code.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String label = code.substring(0, colon).trim();
            if (isIdentifier(label) && !labelLines.containsKey(label)) {
                labelLines.put(label, Integer.valueOf(i));
            }
        }

        // Pass 2: find control transfers that target an earlier line.  Several
        // back edges may target the same label -- a loop body with more than
        // one "continue" path does so -- and those are ONE loop, not nested
        // ones, so they are merged by target label into a single interval that
        // runs from the label to the last transfer that jumps back to it.
        Map<String, int[]> loopsByLabel = new LinkedHashMap<String, int[]>();
        for (int i = 0; i < sourceLines.size(); i++) {
            String code = stripComment(sourceLines.get(i));
            int colon = code.indexOf(':');
            if (colon > 0 && isIdentifier(code.substring(0, colon).trim())) {
                code = code.substring(colon + 1);
            }
            code = code.trim();
            if (code.length() == 0) {
                continue;
            }

            String[] parts = code.split("[\\s,]+");
            if (!isControlTransfer(parts[0])) {
                continue;
            }
            // The branch target is the last operand for every MIPS branch and
            // jump form: j label, beq rs rt label, bgez rs label, and so on.
            String targetLabel = parts[parts.length - 1];
            Integer targetLine = labelLines.get(targetLabel);
            if (targetLine != null && targetLine.intValue() < i) {
                int[] existing = loopsByLabel.get(targetLabel);
                if (existing == null) {
                    loopsByLabel.put(targetLabel, new int[] {targetLine.intValue(), i});
                } else if (i > existing[1]) {
                    existing[1] = i;
                }
            }
        }

        if (loopsByLabel.isEmpty()) {
            return new StructuralHint(0, 0, "no back edges found; straight-line code");
        }

        List<int[]> loops = new ArrayList<int[]>(loopsByLabel.values());

        // Depth = maximum number of loop intervals covering any single line.
        int depth = 0;
        for (int line = 0; line < sourceLines.size(); line++) {
            int covering = 0;
            for (int k = 0; k < loops.size(); k++) {
                int[] loop = loops.get(k);
                if (line >= loop[0] && line <= loop[1]) {
                    covering++;
                }
            }
            if (covering > depth) {
                depth = covering;
            }
        }

        return new StructuralHint(depth, loops.size(),
            loops.size() + " loop(s) detected, maximum nesting depth " + depth);
    }

    /** Remove a trailing '#' comment. */
    private static String stripComment(String line) {
        if (line == null) {
            return "";
        }
        int hash = line.indexOf('#');
        return (hash >= 0) ? line.substring(0, hash) : line;
    }

    /** True for a plain assembler identifier (label name). */
    private static boolean isIdentifier(String s) {
        if (s == null || s.length() == 0) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '.' && c != '$') {
                return false;
            }
        }
        return !s.startsWith(".");   // exclude directives such as .data
    }

    /**
     * True if the mnemonic transfers control to a label WITHIN the current
     * procedure.  Covers the MIPS branch and jump instructions plus the
     * pseudo-branches MARS expands (bge, ble, bgt, blt, bnez, beqz).
     *
     * Deliberately excluded:
     *
     *   jr, jalr   register-indirect, so they have no static label target.
     *
     *   jal, jalr  these are CALLS, not loop back edges.  A call leaves the
     *              procedure and control returns through jr, so a jal whose
     *              target label happens to sit earlier in the file is not a
     *              loop.  Counting it as one produced a false positive on any
     *              subroutine defined above its call site: straight-line code
     *              consisting of "helper: ... jr $ra" followed by "main: jal
     *              helper" was reported as nesting depth 1, implying O(n), for
     *              a program containing no loop whatsoever.
     *
     * Excluding jal is also why recursion is invisible to this analyzer, which
     * is the documented limitation the recursive benchmark exists to expose:
     * recursive_binary_search reports nesting depth 0 and hints O(1) while its
     * measured growth is logarithmic.  That is not repaired here.  Detecting
     * recursion properly requires interprocedural call-graph analysis, and
     * even then the nesting depth of a recursive call says nothing about the
     * recursion DEPTH, which is what determines the cost.  The empirical fit
     * answers that question; static analysis of this kind cannot.
     */
    private static boolean isControlTransfer(String mnemonic) {
        if (mnemonic == null) {
            return false;
        }
        String m = mnemonic.toLowerCase();
        return m.equals("j") || m.equals("b")
            || m.equals("beq") || m.equals("bne")
            || m.equals("bge") || m.equals("bgt") || m.equals("ble") || m.equals("blt")
            || m.equals("bgeu") || m.equals("bgtu") || m.equals("bleu") || m.equals("bltu")
            || m.equals("bgez") || m.equals("bgtz") || m.equals("blez") || m.equals("bltz")
            || m.equals("bgezal") || m.equals("bltzal")
            || m.equals("beqz") || m.equals("bnez");
    }

    // ==================== Reporting ====================

    /**
     * Generate a detailed complexity analysis report
     * @return Formatted report string
     */
    public String generateReport() {
        ComplexityAnalysis analysis = analyze();
        StringBuilder report = new StringBuilder();

        report.append("\n");
        report.append(line('=', 80)).append("\n");
        report.append("                    ALGORITHM COMPLEXITY ANALYSIS REPORT\n");
        report.append(line('=', 80)).append("\n\n");

        // Execution Summary
        report.append("EXECUTION SUMMARY (single run):\n");
        report.append(line('-', 80)).append("\n");
        report.append(String.format("  Total Instructions Executed:    %,d\n", analysis.getTotalInstructions()));
        report.append(String.format("  Total Clock Cycles:             %,d\n", analysis.getTotalCycles()));
        report.append(String.format("  Average Cycles Per Instruction: %.2f\n", analysis.getCyclesPerInstruction()));
        report.append("\n");

        // Authoritative classification
        report.append("COMPLEXITY CLASSIFICATION (empirical, multi-size curve fit):\n");
        report.append(line('-', 80)).append("\n");
        ComplexityFitter.FitReport fit = analysis.getEmpiricalFit();
        if (fit == null) {
            report.append("  No multi-size measurement available.\n");
            report.append("  A complexity class cannot be determined from one input size.\n");
            report.append("  Run mars.simulator.EmpiricalComplexityRunner to produce a fit.\n");
        } else {
            report.append(String.format("  Classification:                 %s\n",
                fit.getClassification()));
            ComplexityFitter.FitResult best = fit.getBest();
            if (best != null) {
                report.append(String.format("  Scaling constant c:             %.4f\n", best.getScalingConstant()));
                report.append(String.format("  Normalised MSE:                 %.6e\n", best.getNormalisedMse()));
                report.append(String.format("  R^2:                            %.4f\n", best.getRSquared()));
                report.append(String.format("  Confidence over runner-up:      %.3f\n", best.getConfidence()));
            }
            if (!Double.isNaN(fit.getEstimatedExponent())) {
                report.append(String.format("  Estimated exponent p (diag.):   %.3f  (log-log R^2 %.4f)\n",
                    fit.getEstimatedExponent(), fit.getExponentRSquared()));
            }
            if (fit.isNoCandidateFit()) {
                report.append("  NOTE: NO CANDIDATE CLASS FITS. The best candidate exceeds the\n");
                report.append(String.format("        absolute normalised-MSE ceiling of %.1e, so the true growth\n",
                    ComplexityFitter.MAX_ACCEPTABLE_NMSE));
                report.append("        lies outside the candidate set. Consult the estimated exponent.\n");
            } else if (fit.isInconclusive()) {
                report.append("  NOTE: the top candidates are not separable by this data;\n");
                report.append("        the result is reported as inconclusive, not forced.\n");
                report.append("        Widen the range of input sizes and re-measure.\n");
            }
            report.append("\n  Ranked candidates:\n");
            report.append(indent(ComplexityFitter.formatRanking(fit), "  "));
        }
        report.append("\n");

        // Subordinate structural hint
        StructuralHint hint = analysis.getStructuralHint();
        report.append("STRUCTURAL HINT (static analysis - NOT a classification):\n");
        report.append(line('-', 80)).append("\n");
        if (hint == null || !hint.isAvailable()) {
            report.append(String.format("  Unavailable: %s\n",
                hint == null ? "not computed" : hint.getDetail()));
        } else {
            report.append(String.format("  Loop Nesting Depth:             %d\n", hint.getNestingDepth()));
            report.append(String.format("  Loops Detected:                 %d\n", hint.getLoopCount()));
            report.append(String.format("  Implied Class (upper bound):    %s\n", hint.getImpliedClass()));
            report.append("  Nesting depth counts loops, not iterations, so it over-states\n");
            report.append("  the cost of any loop whose trip count is sublinear.\n");

            if (fit != null && fit.getBest() != null
                    && !hint.getImpliedClass().equals(fit.getBest().getComplexityClass())) {
                report.append("\n  ** DISAGREEMENT **\n");
                report.append(String.format("  Structural hint says %s; empirical fit says %s.\n",
                    hint.getImpliedClass(), fit.getBest().getComplexityClass()));
                report.append("  The empirical fit is authoritative. A disagreement usually means\n");
                report.append("  a loop's trip count is not proportional to the input size.\n");
            }
        }
        report.append("\n");

        // Observed hotspots
        LoopStructure loops = analysis.getLoopStructure();
        report.append("OBSERVED EXECUTION PROFILE (this run only):\n");
        report.append(line('-', 80)).append("\n");
        report.append(String.format("  Maximum Line Execution Count:   %,d\n", loops.getMaxExecutionCount()));
        report.append(String.format("  Number of Hot Code Lines:       %d\n", loops.getHotLineCount()));
        report.append("\n");

        // Instruction Distribution
        report.append("INSTRUCTION DISTRIBUTION (Top 10):\n");
        report.append(line('-', 80)).append("\n");
        report.append(String.format("%-20s %10s %10s\n", "Instruction", "Count", "Percent"));
        report.append(line('-', 80)).append("\n");

        Map<String, Integer> instCounts = profiler.getInstructionCounts();
        List<Map.Entry<String, Integer>> sortedInst =
            new ArrayList<Map.Entry<String, Integer>>(instCounts.entrySet());
        Collections.sort(sortedInst, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                return b.getValue().compareTo(a.getValue());
            }
        });

        int total = analysis.getTotalInstructions();
        int count = 0;
        for (int i = 0; i < sortedInst.size(); i++) {
            if (count++ >= 10) break;
            Map.Entry<String, Integer> entry = sortedInst.get(i);
            double percent = (total == 0) ? 0.0 : (entry.getValue() * 100.0) / total;
            report.append(String.format("%-20s %10d %9.2f%%\n", entry.getKey(), entry.getValue(), percent));
        }
        report.append("\n");

        // Profiling Metrics
        int totalReads = profiler.getTotalRegisterReads() + profiler.getTotalMemoryReads();
        int totalWrites = profiler.getTotalRegisterWrites() + profiler.getTotalMemoryWrites();

        report.append("MEMORY ACCESS PATTERNS:\n");
        report.append(line('-', 80)).append("\n");
        report.append(String.format("  Total Register Reads:           %,d\n", profiler.getTotalRegisterReads()));
        report.append(String.format("  Total Register Writes:          %,d\n", profiler.getTotalRegisterWrites()));
        report.append(String.format("  Total Memory Reads:             %,d\n", profiler.getTotalMemoryReads()));
        report.append(String.format("  Total Memory Writes:            %,d\n", profiler.getTotalMemoryWrites()));
        report.append(String.format("  Total Data Accesses:            %,d\n", totalReads + totalWrites));
        report.append("\n");

        report.append(line('=', 80)).append("\n");

        return report.toString();
    }

    /**
     * Print complexity analysis report to stdout
     */
    public void printReport() {
        System.out.print(generateReport());
    }

    private static String line(char c, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    private static String indent(String text, String prefix) {
        StringBuilder sb = new StringBuilder();
        String[] parts = text.split("\n", -1);
        for (int i = 0; i < parts.length; i++) {
            if (i == parts.length - 1 && parts[i].length() == 0) {
                break;
            }
            sb.append(prefix).append(parts[i]).append("\n");
        }
        return sb.toString();
    }

    // ==================== Inner Classes ====================

    /**
     * Loop nesting depth obtained by static analysis of the source.
     *
     * This is a HINT, subordinate to the empirical fit.  It reports how many
     * loops are nested, which bounds the loop-driven cost from above but says
     * nothing about how many times any of them runs.
     */
    public static class StructuralHint {
        private final boolean available;
        private final int nestingDepth;
        private final int loopCount;
        private final String detail;

        StructuralHint(int nestingDepth, int loopCount, String detail) {
            this.available = true;
            this.nestingDepth = nestingDepth;
            this.loopCount = loopCount;
            this.detail = detail;
        }

        private StructuralHint(String detail) {
            this.available = false;
            this.nestingDepth = -1;
            this.loopCount = 0;
            this.detail = detail;
        }

        static StructuralHint unavailable(String reason) {
            return new StructuralHint(reason);
        }

        /** False when no source was available to analyse. */
        public boolean isAvailable() { return available; }

        /** Maximum number of nested loops, or -1 if unavailable. */
        public int getNestingDepth() { return nestingDepth; }

        /** Number of distinct loops (merged back edges) found. */
        public int getLoopCount() { return loopCount; }

        /** Human-readable explanation of how the hint was derived. */
        public String getDetail() { return detail; }

        /**
         * The class that the nesting depth alone would suggest, assuming every
         * loop iterates proportionally to the input size.  That assumption is
         * frequently wrong, which is exactly why this is not the classification.
         *
         * @return a Big-O label, or "unknown" if the hint is unavailable
         */
        public String getImpliedClass() {
            if (!available) {
                return "unknown";
            }
            switch (nestingDepth) {
                case 0:  return ComplexityFitter.CLASS_CONSTANT;
                case 1:  return ComplexityFitter.CLASS_LINEAR;
                case 2:  return ComplexityFitter.CLASS_QUADRATIC;
                case 3:  return ComplexityFitter.CLASS_CUBIC;
                default: return "O(n^" + nestingDepth + ")";
            }
        }

        public String toString() {
            if (!available) {
                return "structural hint unavailable (" + detail + ")";
            }
            return "nesting depth " + nestingDepth + " => hints " + getImpliedClass();
        }
    }

    /**
     * Container for complexity analysis results
     */
    public static class ComplexityAnalysis {
        private LoopStructure loopStructure;
        private StructuralHint structuralHint;
        private ComplexityFitter.FitReport empiricalFit;
        private Map<String, Double> instructionDistribution;
        private String estimatedComplexity;
        private long totalCycles;
        private int totalInstructions;
        private double cyclesPerInstruction;

        public LoopStructure getLoopStructure() { return loopStructure; }
        public void setLoopStructure(LoopStructure structure) { this.loopStructure = structure; }

        /** Static loop-nesting hint; subordinate to the empirical fit. */
        public StructuralHint getStructuralHint() { return structuralHint; }
        public void setStructuralHint(StructuralHint hint) { this.structuralHint = hint; }

        /** The authoritative multi-size fit, or null if none was supplied. */
        public ComplexityFitter.FitReport getEmpiricalFit() { return empiricalFit; }
        public void setEmpiricalFit(ComplexityFitter.FitReport fit) { this.empiricalFit = fit; }

        /**
         * True when the static hint and the empirical fit name different classes.
         * Worth surfacing: it usually indicates a loop whose trip count is not
         * proportional to the input size.
         */
        public boolean hasStructuralDisagreement() {
            if (structuralHint == null || !structuralHint.isAvailable()) {
                return false;
            }
            if (empiricalFit == null || empiricalFit.getBest() == null) {
                return false;
            }
            return !structuralHint.getImpliedClass()
                        .equals(empiricalFit.getBest().getComplexityClass());
        }

        public Map<String, Double> getInstructionDistribution() { return instructionDistribution; }
        public void setInstructionDistribution(Map<String, Double> dist) { this.instructionDistribution = dist; }

        /**
         * The reported complexity.  This is the empirical classification when
         * one is available; otherwise it is a statement that no classification
         * can be made from a single run.  It is never inferred from execution
         * magnitude.
         */
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
     * Container for loop structure information.
     *
     * The nesting level is now the statically determined depth rather than a
     * value inferred from execution-count thresholds, and is -1 when no source
     * was available.  The execution counts are raw measurements of the run.
     */
    public static class LoopStructure {
        private int nestingLevel = -1;
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
