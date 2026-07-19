package mars.simulator;

import java.io.*;
import java.util.*;

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
 * Command-line driver that reproduces the complexity experiment end to end:
 * sweeps every benchmark across the input-size sequence, prints the measured
 * growth table and the ranked fits, and writes the two CSV files used to build
 * the results tables and figures.
 *
 * Usage:
 *   java mars.simulator.ComplexityExperiment [outputDirectory]
 *
 * The expected class recorded for each benchmark is used only to label the
 * output and to compute the summary agreement column.  It never reaches the
 * fitter and cannot influence a classification.
 *
 * @author MARS Contributors
 * @version 2025
 */
public class ComplexityExperiment {

    /**
     * The benchmark suite.  Between them these span four complexity classes,
     * which is what makes a misclassification visible: a classifier that keyed
     * off execution magnitude would order them correctly by cost while still
     * getting every class wrong.
     */
    public static List<EmpiricalComplexityRunner.Benchmark> standardSuite(String baseDir) {
        List<EmpiricalComplexityRunner.Benchmark> suite =
            new ArrayList<EmpiricalComplexityRunner.Benchmark>();

        suite.add(new EmpiricalComplexityRunner.Benchmark(
            "heatmap_test", new File(baseDir, "heatmap_test.asm").getPath(),
            EmpiricalComplexityRunner.FILL_NONE, ComplexityFitter.CLASS_CONSTANT));

        suite.add(new EmpiricalComplexityRunner.Benchmark(
            "binary_search", new File(baseDir, "binary_search.asm").getPath(),
            EmpiricalComplexityRunner.FILL_ASCENDING, ComplexityFitter.CLASS_LOG));

        // Same algorithm as binary_search, but recursive.  Included because the
        // static structural analyzer follows back edges and not jal/jr, so it
        // sees no loop at all here and hints O(1) while the measured growth is
        // logarithmic.  That disagreement is the reason this entry exists.
        suite.add(new EmpiricalComplexityRunner.Benchmark(
            "recursive_binary_search",
            new File(baseDir, "recursive_binary_search.asm").getPath(),
            EmpiricalComplexityRunner.FILL_ASCENDING, ComplexityFitter.CLASS_LOG));

        suite.add(new EmpiricalComplexityRunner.Benchmark(
            "linear_search", new File(baseDir, "linear_search.asm").getPath(),
            EmpiricalComplexityRunner.FILL_ASCENDING, ComplexityFitter.CLASS_LINEAR));

        suite.add(new EmpiricalComplexityRunner.Benchmark(
            "bubble_sort", new File(baseDir, "bubble_sort.asm").getPath(),
            EmpiricalComplexityRunner.FILL_DESCENDING, ComplexityFitter.CLASS_QUADRATIC));

        return suite;
    }

    public static void main(String[] args) {
        String outputDir = (args.length > 0) ? args[0] : ".";
        String baseDir = (args.length > 1) ? args[1] : ".";

        EmpiricalComplexityRunner runner = new EmpiricalComplexityRunner();
        List<EmpiricalComplexityRunner.Benchmark> suite = standardSuite(baseDir);

        System.out.println();
        System.out.println(rule('='));
        System.out.println("  EMPIRICAL COMPLEXITY EXPERIMENT");
        System.out.println(rule('='));
        System.out.println("  Cost metric : dynamic instruction count (deterministic, "
            + "hardware-independent)");
        System.out.println("  Input sizes : " + Arrays.toString(runner.getSizes()));
        System.out.println("  Benchmarks  : " + suite.size());
        System.out.println();

        List<EmpiricalComplexityRunner.BenchmarkResult> results = runner.runAll(suite);

        for (int i = 0; i < results.size(); i++) {
            printBenchmark(results.get(i));
        }

        printSummary(results);

        // CSV export for the paper.
        try {
            File samplesFile = new File(outputDir, "complexity_samples.csv");
            File fitsFile = new File(outputDir, "complexity_fits.csv");
            EmpiricalComplexityRunner.exportSamplesCsv(samplesFile, results);
            EmpiricalComplexityRunner.exportFitsCsv(fitsFile, results);
            System.out.println("Wrote " + samplesFile.getPath());
            System.out.println("Wrote " + fitsFile.getPath());
        } catch (IOException e) {
            System.out.println("CSV export failed: " + e);
        }
        System.out.println();
    }

    private static void printBenchmark(EmpiricalComplexityRunner.BenchmarkResult result) {
        EmpiricalComplexityRunner.Benchmark b = result.getBenchmark();

        System.out.println(rule('='));
        System.out.println("  BENCHMARK: " + b.getName()
            + "   (expected " + b.getExpectedClass() + ")");
        System.out.println(rule('='));

        if (result.getError() != null) {
            System.out.println("  ERROR: " + result.getError());
            System.out.println();
            return;
        }

        // Measured growth table.
        System.out.println();
        System.out.println("  Measured growth:");
        System.out.println(String.format("    %8s %18s %14s", "n", "instructions", "ratio"));
        System.out.println("    " + rule('-', 42));
        List<ComplexityFitter.Sample> samples = result.getSamples();
        for (int i = 0; i < samples.size(); i++) {
            ComplexityFitter.Sample s = samples.get(i);
            String ratio = "-";
            if (i > 0) {
                long prev = samples.get(i - 1).getCost();
                if (prev > 0) {
                    ratio = String.format("%.3fx", (double) s.getCost() / prev);
                }
            }
            System.out.println(String.format("    %8d %,18d %14s", s.getN(), s.getCost(), ratio));
        }
        System.out.println();
        System.out.println("    (ratio is cost(n) / cost(previous n), with each n double the"
            + " previous;");
        System.out.println("     a doubling ratio near 1 indicates O(1), near 2 indicates O(n),"
            + " near 4 O(n^2))");
        System.out.println();

        // Structural hint.
        AlgorithmComplexityAnalyzer.StructuralHint hint = result.getStructuralHint();
        System.out.println("  Structural hint (static, subordinate): "
            + (hint == null ? "unavailable" : hint.toString()));
        System.out.println();

        // Ranked fits.
        ComplexityFitter.FitReport fit = result.getFitReport();
        if (fit == null) {
            System.out.println("  No fit produced.");
            System.out.println();
            return;
        }
        System.out.println("  Ranked fits:");
        String[] lines = ComplexityFitter.formatRanking(fit).split("\n");
        for (int i = 0; i < lines.length; i++) {
            System.out.println("    " + lines[i]);
        }
        System.out.println();
        System.out.println("  CLASSIFICATION: " + fit.getClassification());
        if (fit.getNote() != null) {
            System.out.println("  Note: " + fit.getNote());
        }
        if (hint != null && hint.isAvailable() && fit.getBest() != null
                && !hint.getImpliedClass().equals(fit.getBest().getComplexityClass())) {
            System.out.println("  ** DISAGREEMENT: structural hint " + hint.getImpliedClass()
                + " vs empirical " + fit.getBest().getComplexityClass() + " **");
        }
        System.out.println();
    }

    private static void printSummary(List<EmpiricalComplexityRunner.BenchmarkResult> results) {
        System.out.println(rule('='));
        System.out.println("  SUMMARY");
        System.out.println(rule('='));
        System.out.println(String.format("  %-24s %-12s %-12s %11s %6s %7s %-6s",
            "benchmark", "predicted", "expected", "nMSE", "conf", "exp p", "match"));
        System.out.println("  " + rule('-', 88));

        int correct = 0;
        for (int i = 0; i < results.size(); i++) {
            EmpiricalComplexityRunner.BenchmarkResult r = results.get(i);
            String expected = r.getBenchmark().getExpectedClass();
            if (!r.isSuccessful()) {
                System.out.println(String.format("  %-24s %-12s %-12s %11s %6s %7s %-6s",
                    r.getBenchmark().getName(), "ERROR", expected, "-", "-", "-", "no"));
                continue;
            }
            ComplexityFitter.FitReport fit = r.getFitReport();
            ComplexityFitter.FitResult best = fit.getBest();
            boolean match = fit.isClassified() && best.getComplexityClass().equals(expected);
            if (match) {
                correct++;
            }
            String predicted;
            if (fit.isNoCandidateFit()) {
                predicted = "NO FIT";
            } else if (fit.isInconclusive()) {
                predicted = "inconclusive";
            } else {
                predicted = best.getComplexityClass();
            }
            System.out.println(String.format("  %-24s %-12s %-12s %11.4e %6.3f %7s %-6s",
                r.getBenchmark().getName(), predicted, expected,
                best.getNormalisedMse(), best.getConfidence(),
                Double.isNaN(fit.getEstimatedExponent()) ? "-"
                    : String.format("%.3f", fit.getEstimatedExponent()),
                match ? "yes" : "NO"));
        }
        System.out.println();
        System.out.println("  " + correct + " of " + results.size()
            + " benchmarks classified as expected.");
        System.out.println();
    }

    private static String rule(char c) {
        return rule(c, 80);
    }

    private static String rule(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
