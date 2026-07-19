package mars.simulator;

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
 * Self-checking regression tests for {@link ComplexityFitter}.
 *
 * Run with:  java mars.simulator.ComplexityFitterTest
 * Exits non-zero if any check fails, so it can be used in a build script.
 *
 * These are deliberately plain main-method assertions rather than a JUnit
 * suite: MARS has no test dependency and this work must not add one.
 *
 * @author MARS Contributors
 * @version 2025
 */
public class ComplexityFitterTest {

    private static int failures = 0;

    /** Input sizes used by the real experiment, so the tests match the paper. */
    private static final int[] SIZES = {10, 20, 40, 80, 160, 320};

    public static void main(String[] args) {
        testGuardsRejectUnusableInput();
        testConstantByDirectObservation();
        testOutOfSetGrowthIsFlagged();
        testRealBenchmarksStillClassify();
        testExponentDiagnosticLimits();

        System.out.println();
        if (failures == 0) {
            System.out.println("ALL CHECKS PASSED");
            System.exit(0);
        }
        System.out.println(failures + " CHECK(S) FAILED");
        System.exit(1);
    }

    // ==================== Tests ====================

    /**
     * Inputs the fitter cannot honestly fit must be rejected, not fitted badly.
     */
    private static void testGuardsRejectUnusableInput() {
        section("Guards reject unusable input");

        expectRejected("fewer than MIN_SAMPLES sizes",
            new int[]{1, 2, 4}, new long[]{1, 2, 4});
        expectRejected("non-positive n (log n undefined)",
            new int[]{0, 2, 4, 8}, new long[]{1, 2, 4, 8});
        expectRejected("too few DISTINCT sizes",
            new int[]{4, 4, 4, 4}, new long[]{1, 2, 3, 4});
        // Checked before the all-identical case, which would otherwise report
        // an unexecuted benchmark as O(1).
        expectRejected("all costs zero (benchmark never ran)",
            new int[]{1, 2, 4, 8}, new long[]{0, 0, 0, 0});
    }

    /**
     * A flat cost curve is constant by direct observation, not by curve fit.
     */
    private static void testConstantByDirectObservation() {
        section("Constant cost is observed, not inferred");

        ComplexityFitter.FitReport r =
            fit(new int[]{10, 20, 40, 80}, new long[]{42, 42, 42, 42});
        check(ComplexityFitter.CLASS_CONSTANT.equals(r.getBest().getComplexityClass()),
            "all-identical costs classify as O(1)");
        check(r.isDegenerateConstant(), "flagged as a direct observation");
        check(r.isClassified(), "and reported as a classification, not a failure");
    }

    /**
     * The case that motivated the absolute goodness-of-fit ceiling.
     *
     * n^1.5 lies between two candidate classes, so no candidate is correct.
     * Crucially its CONFIDENCE is high -- the best candidate separates cleanly
     * from the runner-up -- so the relative check alone accepts it.  Only the
     * absolute ceiling rejects it, and only the open-ended exponent estimate
     * says what the growth actually is.
     */
    private static void testOutOfSetGrowthIsFlagged() {
        section("Growth outside the candidate set is flagged, not classified");

        long[] costs = new long[SIZES.length];
        for (int i = 0; i < SIZES.length; i++) {
            costs[i] = Math.round(Math.pow(SIZES[i], 1.5));
        }
        ComplexityFitter.FitReport r = fit(SIZES, costs);
        ComplexityFitter.FitResult best = r.getBest();

        System.out.println("    verdict        : " + r.getVerdict());
        System.out.println("    classification : " + r.getClassification());
        System.out.println(String.format("    best %s nMSE=%.4e conf=%.4f p=%.4f",
            best.getComplexityClass(), best.getNormalisedMse(),
            best.getConfidence(), r.getEstimatedExponent()));

        check(r.isNoCandidateFit(), "n^1.5 gets the no-candidate-fits verdict");
        check(!r.isClassified(), "n^1.5 is NOT silently classified");
        check(!r.isInconclusive(),
            "and is NOT reported as inconclusive -- a different failure mode");
        check(best.getConfidence() > ComplexityFitter.LOW_CONFIDENCE_THRESHOLD,
            "its confidence (" + String.format("%.3f", best.getConfidence())
            + ") is ABOVE the inconclusive threshold, so only the absolute ceiling catches it");
        check(Math.abs(r.getEstimatedExponent() - 1.5) < 0.02,
            "log-log regression recovers p ~ 1.5 (got "
            + String.format("%.4f", r.getEstimatedExponent()) + ")");
        check(r.getExponentRSquared() > 0.999,
            "with a near-exact log-log fit, confirming it really is a power law");
    }

    /**
     * The measured benchmarks must still pass both checks.  These are the exact
     * instruction counts produced by ComplexityExperiment, so this test fails
     * if a change to the fitter would alter a published result.
     */
    private static void testRealBenchmarksStillClassify() {
        section("Measured benchmarks still classify");

        checkBenchmark("heatmap_test", new long[]{26, 26, 26, 26, 26, 26},
            ComplexityFitter.CLASS_CONSTANT);
        checkBenchmark("binary_search", new long[]{49, 61, 73, 85, 97, 109},
            ComplexityFitter.CLASS_LOG);
        checkBenchmark("recursive_binary_search", new long[]{82, 102, 122, 142, 162, 182},
            ComplexityFitter.CLASS_LOG);
        checkBenchmark("linear_search", new long[]{92, 172, 332, 652, 1292, 2572},
            ComplexityFitter.CLASS_LINEAR);
        checkBenchmark("bubble_sort",
            new long[]{973, 4133, 17053, 69293, 279373, 1121933},
            ComplexityFitter.CLASS_QUADRATIC);
    }

    /**
     * Pins the documented weakness of the exponent diagnostic, so the claim in
     * EMPIRICAL_METHOD.md stays true if the implementation changes.
     */
    private static void testExponentDiagnosticLimits() {
        section("Exponent diagnostic behaves as documented");

        long[] linear = new long[SIZES.length];
        long[] linearithmic = new long[SIZES.length];
        for (int i = 0; i < SIZES.length; i++) {
            linear[i] = 10L * SIZES[i];
            linearithmic[i] = Math.round(10.0 * SIZES[i] * (Math.log(SIZES[i]) / Math.log(2.0)));
        }

        ComplexityFitter.FitReport rl = fit(SIZES, linear);
        ComplexityFitter.FitReport rn = fit(SIZES, linearithmic);
        double pl = rl.getEstimatedExponent();
        double pn = rn.getEstimatedExponent();

        System.out.println(String.format("    O(n)       p=%.3f  log-log R^2=%.6f",
            pl, rl.getExponentRSquared()));
        System.out.println(String.format("    O(n log n) p=%.3f  log-log R^2=%.6f",
            pn, rn.getExponentRSquared()));

        check(Math.abs(pl - 1.0) < 0.001 && rl.getExponentRSquared() > 0.99999,
            "O(n) gives p=1 with an exact log-log fit (a true power law)");
        check(pn > 1.2 && pn < 1.35 && rn.getExponentRSquared() < 0.9999,
            "O(n log n) gives p~1.26 with an INEXACT log-log fit: the log factor "
            + "appears as slope drift, not as an exponent");
        check(Math.abs(pn - pl) < 0.30,
            "the O(n) / O(n log n) exponent gap is only "
            + String.format("%.3f", pn - pl)
            + ", so p alone is a weak discriminator between them");
    }

    // ==================== Helpers ====================

    private static ComplexityFitter.FitReport fit(int[] sizes, long[] costs) {
        List<ComplexityFitter.Sample> samples = new ArrayList<ComplexityFitter.Sample>();
        for (int i = 0; i < sizes.length; i++) {
            samples.add(new ComplexityFitter.Sample(sizes[i], costs[i]));
        }
        return ComplexityFitter.fit(samples);
    }

    private static void checkBenchmark(String name, long[] costs, String expected) {
        ComplexityFitter.FitReport r = fit(SIZES, costs);
        ComplexityFitter.FitResult best = r.getBest();
        check(r.isClassified() && expected.equals(best.getComplexityClass()),
            name + " -> " + best.getComplexityClass()
            + " (verdict " + r.getVerdict()
            + String.format(", nMSE %.3e, p %.3f)", best.getNormalisedMse(),
                r.getEstimatedExponent()));
    }

    private static void expectRejected(String description, int[] sizes, long[] costs) {
        try {
            fit(sizes, costs);
            check(false, description + " -- expected rejection, got a fit");
        } catch (IllegalArgumentException e) {
            check(true, description + " -> " + e.getMessage());
        }
    }

    private static void check(boolean condition, String message) {
        System.out.println((condition ? "  ok   " : "  FAIL ") + message);
        if (!condition) {
            failures++;
        }
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("== " + title + " ==");
    }
}
