package mars.simulator;

import java.io.*;
import java.util.*;
import mars.*;
import mars.assembler.SymbolTable;
import mars.mips.hardware.*;

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
 * Runs a MIPS benchmark headlessly at a series of input sizes and records the
 * dynamic instruction count at each size, producing the sample set that
 * {@link ComplexityFitter} needs in order to infer a complexity class.
 *
 * COST METRIC.  The cost recorded per run is the total number of instructions
 * actually executed by the simulator, as counted by {@link ProfilerService}.
 * Wall-clock time is deliberately not used.  Instruction count is exactly
 * reproducible, is unaffected by host CPU speed, cache state, JIT warm-up or
 * system load, and is identical on every machine that runs the same benchmark
 * at the same input size.  That determinism is what makes the resulting fits
 * reproducible by a third party, which timing-based measurement cannot offer.
 *
 * PARAMETERISATION.  Benchmarks are not edited between runs.  Each benchmark
 * declares the labels {@code size}, {@code target} and {@code arr} in its data
 * segment; this runner assembles the program, resolves those labels through the
 * symbol table, writes the input size and the array contents directly into
 * simulated memory, and only then starts execution.  Filling the array from the
 * host rather than from a setup loop inside the benchmark matters: a setup loop
 * would itself cost O(n) instructions and would swamp the signal of any
 * algorithm cheaper than linear.
 *
 * ISOLATION.  All profiling state is reset between runs, and the register file,
 * coprocessors and data segment are returned to a known state, so counts never
 * accumulate across sizes.
 *
 * @author MARS Contributors
 * @version 2025
 */
public class EmpiricalComplexityRunner {

    /** Input sizes used unless the caller supplies its own sequence. */
    public static final int[] DEFAULT_SIZES = {10, 20, 40, 80, 160, 320};

    /** Data-segment label holding the input size, patched before each run. */
    public static final String LABEL_SIZE = "size";

    /** Data-segment label holding the search key, patched before each run. */
    public static final String LABEL_TARGET = "target";

    /** Data-segment label of the array buffer, filled before each run. */
    public static final String LABEL_ARRAY = "arr";

    /** Safety limit so a malformed benchmark cannot hang the runner forever. */
    private static final int DEFAULT_MAX_STEPS = 200000000;

    /** How the runner should populate the benchmark's array before execution. */
    public static final int FILL_NONE = 0;
    /** Strictly ascending 1..n; required by anything that assumes sorted input. */
    public static final int FILL_ASCENDING = 1;
    /** Strictly descending n..1; the worst case for a comparison sort. */
    public static final int FILL_DESCENDING = 2;

    private int[] sizes;
    private int maxSteps;
    private boolean verbose;

    private static boolean marsInitialised = false;

    public EmpiricalComplexityRunner() {
        this.sizes = DEFAULT_SIZES.clone();
        this.maxSteps = DEFAULT_MAX_STEPS;
        this.verbose = false;
    }

    /**
     * Override the sequence of input sizes.
     * @param sizes ascending, positive, at least ComplexityFitter.MIN_SAMPLES of them
     */
    public void setSizes(int[] sizes) {
        if (sizes == null || sizes.length < ComplexityFitter.MIN_SAMPLES) {
            throw new IllegalArgumentException(
                "need at least " + ComplexityFitter.MIN_SAMPLES + " input sizes");
        }
        for (int i = 0; i < sizes.length; i++) {
            if (sizes[i] <= 0) {
                throw new IllegalArgumentException("input sizes must be positive, got " + sizes[i]);
            }
        }
        this.sizes = sizes.clone();
    }

    public int[] getSizes() {
        return sizes.clone();
    }

    /** Cap on simulated instructions per run; a run that hits it is reported as failed. */
    public void setMaxSteps(int maxSteps) {
        this.maxSteps = maxSteps;
    }

    /** When true, progress is echoed to stdout as each size completes. */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    // ==================== Benchmark description ====================

    /**
     * A benchmark to be swept: where its source lives, how its array should be
     * filled, and what complexity class it is believed to have.
     *
     * The expected class is recorded for reporting only.  It is never consulted
     * by the fitter and has no influence whatsoever on the classification; it
     * exists so that predicted and expected can be tabulated side by side.
     */
    public static class Benchmark {
        private final String name;
        private final String sourcePath;
        private final int fillMode;
        private final String expectedClass;

        public Benchmark(String name, String sourcePath, int fillMode, String expectedClass) {
            this.name = name;
            this.sourcePath = sourcePath;
            this.fillMode = fillMode;
            this.expectedClass = expectedClass;
        }

        public String getName() { return name; }
        public String getSourcePath() { return sourcePath; }
        public int getFillMode() { return fillMode; }
        public String getExpectedClass() { return expectedClass; }
    }

    /**
     * Everything measured and inferred for one benchmark.
     */
    public static class BenchmarkResult {
        private final Benchmark benchmark;
        private final List<ComplexityFitter.Sample> samples;
        private ComplexityFitter.FitReport fitReport;
        private AlgorithmComplexityAnalyzer.StructuralHint structuralHint;
        private String error;

        BenchmarkResult(Benchmark benchmark) {
            this.benchmark = benchmark;
            this.samples = new ArrayList<ComplexityFitter.Sample>();
        }

        public Benchmark getBenchmark() { return benchmark; }
        public List<ComplexityFitter.Sample> getSamples() { return samples; }

        public ComplexityFitter.FitReport getFitReport() { return fitReport; }
        void setFitReport(ComplexityFitter.FitReport r) { this.fitReport = r; }

        public AlgorithmComplexityAnalyzer.StructuralHint getStructuralHint() { return structuralHint; }
        void setStructuralHint(AlgorithmComplexityAnalyzer.StructuralHint h) { this.structuralHint = h; }

        /** Non-null if the sweep failed; the sample list is then incomplete. */
        public String getError() { return error; }
        void setError(String error) { this.error = error; }

        public boolean isSuccessful() { return error == null && fitReport != null; }
    }

    // ==================== Sweep ====================

    /**
     * Run one benchmark across the configured size sequence and fit the result.
     *
     * @param benchmark the benchmark to sweep
     * @return the measured samples plus the ranked fit, or a result carrying an
     *         error message if any size failed to run
     */
    public BenchmarkResult runBenchmark(Benchmark benchmark) {
        BenchmarkResult result = new BenchmarkResult(benchmark);

        List<String> sourceLines = readSourceLines(benchmark.getSourcePath());
        if (sourceLines == null) {
            result.setError("could not read source file: " + benchmark.getSourcePath());
            return result;
        }
        result.setStructuralHint(
            AlgorithmComplexityAnalyzer.analyzeStaticStructure(sourceLines));

        for (int i = 0; i < sizes.length; i++) {
            int n = sizes[i];
            try {
                long instructions = runOnce(benchmark, n);
                result.getSamples().add(new ComplexityFitter.Sample(n, instructions));
                if (verbose) {
                    System.out.println(String.format("  %-16s n=%-6d instructions=%,d",
                        benchmark.getName(), n, instructions));
                }
            } catch (Exception e) {
                result.setError("run failed at n=" + n + ": " + e);
                return result;
            }
        }

        try {
            result.setFitReport(ComplexityFitter.fit(result.getSamples()));
        } catch (IllegalArgumentException e) {
            result.setError("fit failed: " + e.getMessage());
        }
        return result;
    }

    /**
     * Run several benchmarks in sequence.
     *
     * @param benchmarks benchmarks to sweep
     * @return one result per benchmark, in the same order
     */
    public List<BenchmarkResult> runAll(List<Benchmark> benchmarks) {
        List<BenchmarkResult> results = new ArrayList<BenchmarkResult>();
        for (int i = 0; i < benchmarks.size(); i++) {
            results.add(runBenchmark(benchmarks.get(i)));
        }
        return results;
    }

    /**
     * Assemble and execute one benchmark at one input size.
     *
     * @param benchmark the benchmark
     * @param n the input size to patch into the data segment
     * @return total dynamic instruction count for this run
     * @throws ProcessingException if assembly or simulation reports an error
     * @throws AddressErrorException if the data segment cannot be patched
     */
    private long runOnce(Benchmark benchmark, int n)
            throws ProcessingException, AddressErrorException {

        ensureMarsInitialised();

        // Command-line MARS deliberately leaves Globals.program null so that
        // back-stepping stays off; back-stepping would log every register and
        // memory write, which is both slow and irrelevant here.  A GUI session
        // will have set it, so save and restore around the run.
        MIPSprogram savedProgram = Globals.program;
        Globals.program = null;
        int savedExitCode = Globals.exitCode;

        try {
            Globals.exitCode = 0;

            MIPSprogram code = new MIPSprogram();
            // prepareFilesForAssembly decides which MIPSprogram is the lead by
            // string-comparing each filename against leadFilename, so both must
            // be spelled identically or "code" itself is never tokenized and
            // its local symbol table stays null.
            String path = new File(benchmark.getSourcePath()).getAbsolutePath();
            ArrayList<String> filenames = new ArrayList<String>();
            filenames.add(path);

            // Assembly clears the data segment and the global symbol table, so
            // nothing survives from the previous size.
            ArrayList programsToAssemble = code.prepareFilesForAssembly(filenames, path, null);
            code.assemble(programsToAssemble, true, false);

            // Registers are not cleared by assembly; clear them explicitly so
            // that a run can never be influenced by the size that preceded it.
            RegisterFile.resetRegisters();
            Coprocessor0.resetRegisters();
            Coprocessor1.resetRegisters();
            RegisterFile.initializeProgramCounter(true);

            patchInputs(code, benchmark, n);

            // The simulator resets the profiler itself at the start of every
            // run; resetting here as well keeps the invariant explicit and
            // protects against a run that aborts before the simulator starts.
            ProfilerService.getInstance().reset();
            ExecutionHeatmap.getInstance().reset();

            boolean completed = code.simulate(maxSteps);
            if (!completed) {
                throw new IllegalStateException(
                    "benchmark did not terminate within " + maxSteps + " instructions");
            }

            // Read the counter before touching memory or registers: the
            // profiler also records host-side memory reads, and while those do
            // not affect the instruction count, reading first keeps every
            // counter in this snapshot consistent.
            return ProfilerService.getInstance().getTotalInstructions();

        } finally {
            Globals.program = savedProgram;
            Globals.exitCode = savedExitCode;
        }
    }

    /**
     * Write the input size, the search key and the array contents into the
     * assembled data segment.
     *
     * Labels that a benchmark does not declare are skipped silently: a
     * benchmark is allowed to ignore parameters it has no use for, and the
     * fixed-iteration control benchmark ignores the size on purpose.
     */
    private void patchInputs(MIPSprogram code, Benchmark benchmark, int n)
            throws AddressErrorException {

        SymbolTable symbols = code.getLocalSymbolTable();
        if (symbols == null) {
            throw new IllegalStateException(
                "local symbol table is null; the benchmark source was not tokenized");
        }

        int sizeAddress = symbols.getAddressLocalOrGlobal(LABEL_SIZE);
        if (sizeAddress != SymbolTable.NOT_FOUND) {
            Globals.memory.setWord(sizeAddress, n);
        }

        // A key that is absent from a 1..n array forces the worst case for
        // linear search and full depth for binary search, so the cost at each
        // size is the deterministic worst case rather than data dependent.
        int targetAddress = symbols.getAddressLocalOrGlobal(LABEL_TARGET);
        if (targetAddress != SymbolTable.NOT_FOUND) {
            Globals.memory.setWord(targetAddress, -1);
        }

        int arrayAddress = symbols.getAddressLocalOrGlobal(LABEL_ARRAY);
        if (arrayAddress != SymbolTable.NOT_FOUND && benchmark.getFillMode() != FILL_NONE) {
            for (int i = 0; i < n; i++) {
                int value = (benchmark.getFillMode() == FILL_ASCENDING) ? (i + 1) : (n - i);
                Globals.memory.setWord(arrayAddress + i * Memory.WORD_LENGTH_BYTES, value);
            }
        }
    }

    /**
     * Bring MARS up once per JVM.  Globals.initialize is itself a no-op after
     * the first call, so this is safe alongside a running GUI session.
     */
    private static synchronized void ensureMarsInitialised() {
        if (marsInitialised) {
            return;
        }
        if (Globals.getGui() == null) {
            System.setProperty("java.awt.headless", "true");
        }
        Globals.initialize(false);
        MemoryConfigurations.setCurrentConfiguration(
            MemoryConfigurations.getDefaultConfiguration());
        Globals.getSettings().setBooleanSettingNonPersistent(
            Settings.DELAYED_BRANCHING_ENABLED, false);
        Globals.getSettings().setBooleanSettingNonPersistent(
            Settings.SELF_MODIFYING_CODE_ENABLED, false);
        marsInitialised = true;
    }

    /**
     * Read a benchmark's source so its loop structure can be analysed
     * statically.  Returns null if the file cannot be read.
     */
    private static List<String> readSourceLines(String path) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(path));
            List<String> lines = new ArrayList<String>();
            String line = reader.readLine();
            while (line != null) {
                lines.add(line);
                line = reader.readLine();
            }
            return lines;
        } catch (IOException e) {
            return null;
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (IOException ignored) { }
            }
        }
    }

    // ==================== CSV export ====================

    /**
     * Write the raw measurements: one row per (benchmark, input size).
     * This is the table behind the growth-curve figures.
     *
     * @param file destination CSV
     * @param results results to export
     * @throws IOException if the file cannot be written
     */
    public static void exportSamplesCsv(File file, List<BenchmarkResult> results)
            throws IOException {
        PrintWriter out = new PrintWriter(new FileWriter(file));
        try {
            out.println("benchmark,n,instructionCount");
            for (int i = 0; i < results.size(); i++) {
                BenchmarkResult r = results.get(i);
                List<ComplexityFitter.Sample> samples = r.getSamples();
                for (int j = 0; j < samples.size(); j++) {
                    ComplexityFitter.Sample s = samples.get(j);
                    out.println(csv(r.getBenchmark().getName()) + ","
                        + s.getN() + "," + s.getCost());
                }
            }
        } finally {
            out.close();
        }
    }

    /**
     * Write the classification outcome: one row per benchmark, giving the
     * predicted class beside the expected one along with the fit statistics.
     * This is the table behind the results section.
     *
     * @param file destination CSV
     * @param results results to export
     * @throws IOException if the file cannot be written
     */
    public static void exportFitsCsv(File file, List<BenchmarkResult> results)
            throws IOException {
        PrintWriter out = new PrintWriter(new FileWriter(file));
        try {
            // verdict distinguishes the two failure modes that "inconclusive"
            // alone cannot: candidates too close together, versus no candidate
            // fitting at all.  estimatedExponent is the open-ended diagnostic.
            out.println("benchmark,predictedClass,trueClass,normalisedMse,rSquared,"
                + "confidence,verdict,inconclusive,noCandidateFit,"
                + "estimatedExponent,exponentRSquared,structuralHint,agreement");
            for (int i = 0; i < results.size(); i++) {
                BenchmarkResult r = results.get(i);
                String name = csv(r.getBenchmark().getName());
                String trueClass = csv(r.getBenchmark().getExpectedClass());

                if (!r.isSuccessful()) {
                    out.println(name + ",ERROR," + trueClass + ",,,,ERROR,,,,,,"
                        + csv(String.valueOf(r.getError())));
                    continue;
                }

                ComplexityFitter.FitReport report = r.getFitReport();
                ComplexityFitter.FitResult best = report.getBest();
                String hint = (r.getStructuralHint() == null)
                    ? "" : r.getStructuralHint().getImpliedClass();
                String agreement = (r.getStructuralHint() == null) ? ""
                    : (r.getStructuralHint().getImpliedClass().equals(best.getComplexityClass())
                        ? "agree" : "disagree");

                out.println(name + ","
                    + csv(best.getComplexityClass()) + ","
                    + trueClass + ","
                    + String.format("%.10e", best.getNormalisedMse()) + ","
                    + String.format("%.6f", best.getRSquared()) + ","
                    + String.format("%.6f", best.getConfidence()) + ","
                    + csv(report.getVerdict()) + ","
                    + report.isInconclusive() + ","
                    + report.isNoCandidateFit() + ","
                    + (Double.isNaN(report.getEstimatedExponent()) ? ""
                        : String.format("%.6f", report.getEstimatedExponent())) + ","
                    + (Double.isNaN(report.getExponentRSquared()) ? ""
                        : String.format("%.6f", report.getExponentRSquared())) + ","
                    + csv(hint) + ","
                    + agreement);
            }
        } finally {
            out.close();
        }
    }

    /** Quote a CSV field if it contains a comma or a quote. */
    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        if (value.indexOf(',') < 0 && value.indexOf('"') < 0) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
