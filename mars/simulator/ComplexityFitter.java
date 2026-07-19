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
 * Fits an observed cost-growth curve against a set of candidate complexity
 * classes and ranks them by goodness of fit.
 *
 * This class contains no thresholds on absolute magnitude.  A classification
 * is derived only from the SHAPE of the growth curve measured across several
 * input sizes, which is the accepted empirical method for complexity
 * inference.  A single measurement can never determine a complexity class;
 * accordingly {@link #fit} requires at least {@link #MIN_SAMPLES} samples.
 *
 * Method, for each candidate model f(n):
 *
 *   1. Evaluate the basis vector f_i = f(n_i) over all sampled input sizes.
 *   2. Least-squares fit the single scaling constant c that minimises
 *      sum((c*f_i - cost_i)^2).  Setting the derivative to zero gives
 *      c = sum(f_i*cost_i) / sum(f_i*f_i).
 *   3. Normalise both the predicted vector (c*f_i) and the observed vector
 *      (cost_i) to unit L2 length, then take the mean squared error between
 *      them.  Normalisation is essential: raw MSE grows with the magnitude of
 *      the data, so fast-growing classes would be penalised purely for being
 *      large, not for fitting worse.
 *
 * Note that after L2 normalisation the scaling constant cancels
 * (c*f / ||c*f|| == f / ||f||), so the normalised MSE is a pure, scale-free
 * measure of curve shape.  The constant c is still reported because it is the
 * per-element cost of the algorithm and is of independent interest.
 *
 * @author MARS Contributors
 * @version 2025
 */
public class ComplexityFitter {

    /** A curve fit needs at least this many distinct input sizes to be meaningful. */
    public static final int MIN_SAMPLES = 4;

    /**
     * If the normalised MSE of the best and second-best candidate are within
     * this relative distance the two classes are not distinguishable by the
     * available data, and the result is reported as inconclusive rather than
     * forced into a classification.
     */
    public static final double LOW_CONFIDENCE_THRESHOLD = 0.10;

    /**
     * Absolute goodness-of-fit ceiling on the best candidate's normalised MSE.
     * Above this, no candidate class describes the data and the result is
     * reported as "no candidate fits" rather than as a classification.
     *
     * This is a genuinely different failure mode from a low confidence.  The
     * confidence metric measures SEPARATION between the best and second-best
     * candidate, so it says nothing about whether either of them is any good; a
     * curve that matches no candidate at all can still separate cleanly from
     * the runner-up and be reported with high confidence.  Only an absolute
     * check catches that.
     *
     * Derivation of the value.  It is fixed by two measured anchors, both taken
     * before the recursive benchmark existed:
     *
     *   worst correctly-classified fit  binary_search   nMSE = 1.6554e-04
     *   closest misfit to be rejected   n^1.5 data      nMSE = 1.5973e-03
     *
     * The two differ by roughly one order of magnitude, so the threshold is
     * placed at their GEOMETRIC mean -- the midpoint in log space, which is the
     * natural centre for a quantity spanning orders of magnitude:
     *
     *   sqrt(1.6554e-04 * 1.5973e-03) = 5.142e-04
     *
     * rounded down to 5.0e-04, the conservative direction, since erring low
     * flags a borderline result rather than silently accepting it.  That leaves
     * a factor of 3.02 above the worst fit that must be accepted and a factor
     * of 3.19 below the misfit that must be rejected.
     *
     * This separation is NOT clean in general, and the limit should be read as
     * a guard against gross misfits rather than as a decision boundary.  Data
     * generated as n^1.3 fits O(n log n) at nMSE 2.2606e-04 -- a real misfit
     * that scores BETTER than the correctly-classified binary_search, and so
     * passes this check.  No single threshold can separate those two cases,
     * because over a 32x range of n the curves genuinely are that similar.  The
     * open-ended exponent estimate reported alongside (see
     * {@link FitReport#getEstimatedExponent}) is the diagnostic that exposes
     * such cases.
     */
    public static final double MAX_ACCEPTABLE_NMSE = 5.0e-04;

    /** Verdict: a single candidate class fits and is clearly separated. */
    public static final String VERDICT_CLASSIFIED = "classified";

    /** Verdict: a candidate fits, but the top two cannot be told apart. */
    public static final String VERDICT_INCONCLUSIVE = "inconclusive";

    /** Verdict: no candidate class describes the measured growth curve. */
    public static final String VERDICT_NO_FIT = "no-candidate-fits";

    // Candidate complexity classes, in conventional increasing order of growth.
    public static final String CLASS_CONSTANT     = "O(1)";
    public static final String CLASS_LOG          = "O(log n)";
    public static final String CLASS_LINEAR       = "O(n)";
    public static final String CLASS_LINEARITHMIC = "O(n log n)";
    public static final String CLASS_QUADRATIC    = "O(n^2)";
    public static final String CLASS_CUBIC        = "O(n^3)";

    private static final String[] CANDIDATES = {
        CLASS_CONSTANT, CLASS_LOG, CLASS_LINEAR,
        CLASS_LINEARITHMIC, CLASS_QUADRATIC, CLASS_CUBIC
    };

    /**
     * A single (input size, measured cost) observation.
     */
    public static class Sample {
        private final int n;
        private final long cost;

        public Sample(int n, long cost) {
            this.n = n;
            this.cost = cost;
        }

        public int getN() { return n; }
        public long getCost() { return cost; }

        public String toString() {
            return "n=" + n + " cost=" + cost;
        }
    }

    /**
     * The result of fitting one candidate model to the observed samples.
     */
    public static class FitResult {
        private final String complexityClass;
        private final double scalingConstant;
        private final double normalisedMse;
        private final double rSquared;
        private double confidence;

        FitResult(String complexityClass, double scalingConstant,
                  double normalisedMse, double rSquared) {
            this.complexityClass = complexityClass;
            this.scalingConstant = scalingConstant;
            this.normalisedMse = normalisedMse;
            this.rSquared = rSquared;
            this.confidence = 0.0;
        }

        /** Big-O label of the fitted model, e.g. "O(n^2)". */
        public String getComplexityClass() { return complexityClass; }

        /** Least-squares scaling constant c in cost ~ c * f(n). */
        public double getScalingConstant() { return scalingConstant; }

        /** Scale-free goodness of fit; lower is better, 0.0 is exact. */
        public double getNormalisedMse() { return normalisedMse; }

        /**
         * Coefficient of determination of the raw (un-normalised) fit.
         * Because the model has no intercept term this may be negative for a
         * badly matched class; that is expected and is not clamped.
         */
        public double getRSquared() { return rSquared; }

        /**
         * Separation between this fit and the next-best one, in [0,1].
         * Only the top-ranked result carries a meaningful confidence.
         */
        public double getConfidence() { return confidence; }

        void setConfidence(double confidence) { this.confidence = confidence; }

        public String toString() {
            return String.format("%-12s c=%.4f  nMSE=%.6e  R2=%.4f  conf=%.3f",
                complexityClass, scalingConstant, normalisedMse, rSquared, confidence);
        }
    }

    /**
     * The complete outcome of a fit: every candidate ranked best-first, plus
     * the interpretation of that ranking.
     */
    public static class FitReport {
        private final List<FitResult> ranked;
        private final boolean degenerateConstant;
        private final String note;
        private final double estimatedExponent;
        private final double exponentRSquared;

        FitReport(List<FitResult> ranked, boolean degenerateConstant, String note,
                  double estimatedExponent, double exponentRSquared) {
            this.ranked = ranked;
            this.degenerateConstant = degenerateConstant;
            this.note = note;
            this.estimatedExponent = estimatedExponent;
            this.exponentRSquared = exponentRSquared;
        }

        /**
         * Open-ended estimate of the exponent p in cost ~ n^p, obtained by
         * regressing log(cost) on log(n).  DIAGNOSTIC ONLY: it never overrides
         * the candidate-set classification.
         *
         * Its value is that it is not restricted to the candidate set.  Data
         * generated as n^1.5 recovers p close to 1.5, which no member of the
         * candidate list can express, so a p that sits well away from 0, 1, 2
         * or 3 is a signal that the true growth lies between the candidates.
         *
         * It cannot replace the candidate fit, because a power law is the wrong
         * model for several candidates and the exponent alone does not name a
         * class.  Measured over n = 10..320:
         *
         *   O(n)        p = 1.000, log-log R^2 = 1.0000  (a true power law)
         *   O(n log n)  p = 1.262, log-log R^2 &lt; 1       (not a power law)
         *   O(log n)    p = 0.228, log-log R^2 &lt; 1       (not a power law)
         *
         * The log n factor does not appear as a distinct exponent; it appears
         * as a slowly DRIFTING slope, since d(log(n log n))/d(log n) = 1 +
         * 1/ln(n), which falls from 1.43 at n=10 to 1.17 at n=320.  Regression
         * averages that drift into a single number near 1.26, which is not a
         * recognisable landmark and does not by itself say "there is a log
         * factor here".  The tell is the R^2: a genuine power law fits the
         * log-log line exactly, and anything less indicates the growth is not
         * of the form n^p at all.
         *
         * So the two methods answer different questions -- the candidate fit
         * names a class from a fixed menu, the exponent measures growth without
         * a menu -- and are reported together.
         *
         * @return the estimated exponent, or Double.NaN if it could not be
         *         computed (any cost was zero, so log(cost) is undefined)
         */
        public double getEstimatedExponent() { return estimatedExponent; }

        /**
         * Coefficient of determination of the log-log regression.  A value near
         * 1 means the data really does follow a power law and the exponent is
         * meaningful; a lower value means the growth is not power-law shaped
         * (logarithmic or exponential growth, for instance) and the exponent
         * should be read with care.
         *
         * @return the R^2 of the log-log fit, or Double.NaN if unavailable
         */
        public double getExponentRSquared() { return exponentRSquared; }

        /** All candidate fits, sorted by ascending normalised MSE (best first). */
        public List<FitResult> getRanked() { return ranked; }

        /** The best-fitting candidate, or null if nothing could be fitted. */
        public FitResult getBest() {
            return ranked.isEmpty() ? null : ranked.get(0);
        }

        /** The runner-up, or null if there is only one candidate. */
        public FitResult getRunnerUp() {
            return ranked.size() < 2 ? null : ranked.get(1);
        }

        /**
         * True when every measured cost was identical, which is a direct
         * observation of constant behaviour rather than an inferred fit.
         */
        public boolean isDegenerateConstant() { return degenerateConstant; }

        /**
         * The outcome of the fit, as one of {@link #VERDICT_CLASSIFIED},
         * {@link #VERDICT_NO_FIT} or {@link #VERDICT_INCONCLUSIVE}.
         *
         * The absolute check is applied BEFORE the separation check.  If no
         * candidate describes the data then the ranking among candidates is
         * meaningless, and how well the best one separates from the second is
         * not a question worth answering.
         *
         * @return the verdict string
         */
        public String getVerdict() {
            FitResult best = getBest();
            if (best == null) {
                return VERDICT_NO_FIT;
            }
            if (degenerateConstant) {
                // Every measured cost was identical; O(1) is observed directly
                // and exactly, so neither check applies.
                return VERDICT_CLASSIFIED;
            }
            if (best.getNormalisedMse() > MAX_ACCEPTABLE_NMSE) {
                return VERDICT_NO_FIT;
            }
            if (best.getConfidence() < LOW_CONFIDENCE_THRESHOLD) {
                return VERDICT_INCONCLUSIVE;
            }
            return VERDICT_CLASSIFIED;
        }

        /**
         * True when no candidate class describes the measured growth at all,
         * as judged by the absolute normalised-MSE ceiling.
         *
         * Distinct from {@link #isInconclusive}: that means two candidates fit
         * about equally well, this means none of them fits.
         */
        public boolean isNoCandidateFit() {
            return VERDICT_NO_FIT.equals(getVerdict());
        }

        /**
         * True when the top two candidates are too close to separate.  Callers
         * must surface this rather than reporting the winner as fact.
         */
        public boolean isInconclusive() {
            return VERDICT_INCONCLUSIVE.equals(getVerdict());
        }

        /**
         * True when the fit produced a usable single classification.
         */
        public boolean isClassified() {
            return VERDICT_CLASSIFIED.equals(getVerdict());
        }

        /**
         * The classification to report.  Either the best-fitting class, or a
         * statement of which failure mode applies -- the two failure modes are
         * reported differently because they call for different responses:
         * "inconclusive" means widen the range of input sizes, whereas
         * "no candidate fits" means the true growth is outside the candidate
         * set entirely and the exponent estimate should be consulted.
         */
        public String getClassification() {
            FitResult best = getBest();
            if (best == null) {
                return "no candidate fits (nothing could be fitted)";
            }
            String verdict = getVerdict();
            if (VERDICT_NO_FIT.equals(verdict)) {
                StringBuilder sb = new StringBuilder();
                sb.append("no candidate fits (best ").append(best.getComplexityClass());
                sb.append(String.format(" at nMSE %.3e, above the %.1e ceiling",
                    best.getNormalisedMse(), MAX_ACCEPTABLE_NMSE));
                if (!Double.isNaN(estimatedExponent)) {
                    sb.append(String.format("; estimated exponent p=%.3f", estimatedExponent));
                }
                sb.append(")");
                return sb.toString();
            }
            if (VERDICT_INCONCLUSIVE.equals(verdict)) {
                return "inconclusive (" + best.getComplexityClass()
                       + " vs " + (getRunnerUp() == null ? "?" : getRunnerUp().getComplexityClass()) + ")";
            }
            return best.getComplexityClass();
        }

        /** Human-readable remark about how the result was arrived at. */
        public String getNote() { return note; }
    }

    /**
     * Fit all candidate complexity classes to the supplied samples.
     *
     * @param samples at least {@link #MIN_SAMPLES} observations, each with n &gt; 0
     * @return ranked fit report, best fit first
     * @throws IllegalArgumentException if the samples are unusable
     */
    public static FitReport fit(List<Sample> samples) {
        if (samples == null) {
            throw new IllegalArgumentException("samples must not be null");
        }
        if (samples.size() < MIN_SAMPLES) {
            throw new IllegalArgumentException(
                "complexity fitting requires at least " + MIN_SAMPLES
                + " input sizes, got " + samples.size()
                + " -- a single measurement cannot determine a growth rate");
        }

        int m = samples.size();
        double[] n = new double[m];
        double[] cost = new double[m];
        Set<Integer> distinctSizes = new HashSet<Integer>();

        for (int i = 0; i < m; i++) {
            Sample s = samples.get(i);
            if (s.getN() <= 0) {
                throw new IllegalArgumentException(
                    "input size must be positive, got n=" + s.getN()
                    + " (log n is undefined at n<=0)");
            }
            if (s.getCost() < 0) {
                throw new IllegalArgumentException(
                    "cost must not be negative, got " + s.getCost() + " at n=" + s.getN());
            }
            n[i] = s.getN();
            cost[i] = s.getCost();
            distinctSizes.add(Integer.valueOf(s.getN()));
        }

        if (distinctSizes.size() < MIN_SAMPLES) {
            throw new IllegalArgumentException(
                "complexity fitting requires at least " + MIN_SAMPLES
                + " DISTINCT input sizes, got " + distinctSizes.size());
        }

        // Checked before the all-identical case below, which would otherwise
        // swallow it: an all-zero cost vector is technically constant, but it
        // means the benchmark never executed rather than that it is O(1).
        if (allZero(cost)) {
            throw new IllegalArgumentException(
                "all measured costs are zero -- the benchmark did not execute");
        }

        // Explicit handling of the genuinely-constant case.  Every cost being
        // identical is a direct observation, not something to infer from a
        // curve fit -- and with a flat observed vector several models can
        // produce indistinguishable normalised error.
        if (allIdentical(cost)) {
            List<FitResult> ranked = new ArrayList<FitResult>();
            ranked.add(new FitResult(CLASS_CONSTANT, cost[0], 0.0, 1.0));
            for (int k = 1; k < CANDIDATES.length; k++) {
                // Non-constant models cannot reproduce a flat curve exactly.
                double[] basis = basisVector(CANDIDATES[k], n);
                double nmse = normalisedMse(basis, cost);
                double c = scalingConstant(basis, cost);
                ranked.add(new FitResult(CANDIDATES[k], c, nmse, rSquared(basis, cost, c)));
            }
            Collections.sort(ranked, byNormalisedMse());
            // Force O(1) to the front: it is exact by observation.
            promoteConstant(ranked);
            ranked.get(0).setConfidence(1.0);
            double[] flatExponent = estimateExponent(n, cost);
            return new FitReport(ranked, true,
                "All measured costs were identical across " + distinctSizes.size()
                + " input sizes; classified O(1) by direct observation, not by curve fit.",
                flatExponent[0], flatExponent[1]);
        }

        List<FitResult> ranked = new ArrayList<FitResult>();
        for (int k = 0; k < CANDIDATES.length; k++) {
            double[] basis = basisVector(CANDIDATES[k], n);
            if (l2Norm(basis) == 0.0) {
                // Degenerate basis (e.g. log n where every n == 1); the model
                // cannot be fitted at all, so rank it last rather than
                // silently treating it as a perfect fit.
                ranked.add(new FitResult(CANDIDATES[k], 0.0,
                    Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY));
                continue;
            }
            double c = scalingConstant(basis, cost);
            double nmse = normalisedMse(basis, cost);
            double r2 = rSquared(basis, cost, c);
            ranked.add(new FitResult(CANDIDATES[k], c, nmse, r2));
        }

        Collections.sort(ranked, byNormalisedMse());

        // Confidence is the relative separation between the best and second
        // best fit.  Two classes that explain the data equally well give a
        // confidence near zero, which the caller must report as inconclusive.
        double confidence = 0.0;
        if (ranked.size() >= 2) {
            double mse1 = ranked.get(0).getNormalisedMse();
            double mse2 = ranked.get(1).getNormalisedMse();
            if (Double.isInfinite(mse2)) {
                confidence = 1.0;
            } else if (mse2 > 0.0) {
                confidence = (mse2 - mse1) / mse2;
            } else {
                // Both fits are exact; the data cannot separate them.
                confidence = 0.0;
            }
        }
        if (confidence < 0.0) confidence = 0.0;
        if (confidence > 1.0) confidence = 1.0;
        ranked.get(0).setConfidence(confidence);

        String note = "Fitted " + CANDIDATES.length + " candidate classes to "
            + distinctSizes.size() + " input sizes by least squares on the "
            + "scaling constant, ranked by L2-normalised MSE.";

        double[] exponent = estimateExponent(n, cost);
        return new FitReport(ranked, false, note, exponent[0], exponent[1]);
    }

    /**
     * Convenience overload for callers holding parallel arrays.
     *
     * @param sizes input sizes
     * @param costs measured cost at each corresponding input size
     * @return ranked fit report
     */
    public static FitReport fit(int[] sizes, long[] costs) {
        if (sizes == null || costs == null || sizes.length != costs.length) {
            throw new IllegalArgumentException("sizes and costs must be non-null and equal length");
        }
        List<Sample> samples = new ArrayList<Sample>();
        for (int i = 0; i < sizes.length; i++) {
            samples.add(new Sample(sizes[i], costs[i]));
        }
        return fit(samples);
    }

    // ==================== Model evaluation ====================

    /**
     * Evaluate a candidate model f(n) at every sampled input size.
     * Logarithms are base 2; the choice of base only rescales c and does not
     * affect the ranking.
     */
    private static double[] basisVector(String complexityClass, double[] n) {
        double[] f = new double[n.length];
        for (int i = 0; i < n.length; i++) {
            double x = n[i];
            if (CLASS_CONSTANT.equals(complexityClass)) {
                f[i] = 1.0;
            } else if (CLASS_LOG.equals(complexityClass)) {
                f[i] = log2(x);
            } else if (CLASS_LINEAR.equals(complexityClass)) {
                f[i] = x;
            } else if (CLASS_LINEARITHMIC.equals(complexityClass)) {
                f[i] = x * log2(x);
            } else if (CLASS_QUADRATIC.equals(complexityClass)) {
                f[i] = x * x;
            } else if (CLASS_CUBIC.equals(complexityClass)) {
                f[i] = x * x * x;
            } else {
                throw new IllegalArgumentException("unknown complexity class: " + complexityClass);
            }
        }
        return f;
    }

    /**
     * Base-2 logarithm, guarded against non-positive input.  Callers already
     * reject n &lt;= 0; this is a second line of defence so a bad sample can
     * never produce NaN or -Infinity inside the fit.
     */
    private static double log2(double x) {
        if (x <= 0.0) {
            return 0.0;
        }
        return Math.log(x) / Math.log(2.0);
    }

    /**
     * Least-squares scaling constant minimising sum((c*f_i - cost_i)^2).
     */
    private static double scalingConstant(double[] f, double[] cost) {
        double num = 0.0;
        double den = 0.0;
        for (int i = 0; i < f.length; i++) {
            num += f[i] * cost[i];
            den += f[i] * f[i];
        }
        if (den == 0.0) {
            return 0.0;
        }
        return num / den;
    }

    /**
     * Mean squared error between the unit-normalised model curve and the
     * unit-normalised observed curve.  Dividing each vector by its own L2 norm
     * removes magnitude entirely, so classes of very different growth rate are
     * compared on shape alone and are directly comparable to one another.
     */
    private static double normalisedMse(double[] f, double[] cost) {
        double fNorm = l2Norm(f);
        double costNorm = l2Norm(cost);
        if (fNorm == 0.0 || costNorm == 0.0) {
            return Double.POSITIVE_INFINITY;
        }
        double sum = 0.0;
        for (int i = 0; i < f.length; i++) {
            double d = (f[i] / fNorm) - (cost[i] / costNorm);
            sum += d * d;
        }
        return sum / f.length;
    }

    /**
     * Coefficient of determination of the raw fit c*f(n) against the observed
     * costs.  Reported alongside the normalised MSE because readers expect it,
     * but the ranking is done on the normalised MSE.
     */
    private static double rSquared(double[] f, double[] cost, double c) {
        double mean = 0.0;
        for (int i = 0; i < cost.length; i++) {
            mean += cost[i];
        }
        mean /= cost.length;

        double ssRes = 0.0;
        double ssTot = 0.0;
        for (int i = 0; i < cost.length; i++) {
            double residual = c * f[i] - cost[i];
            ssRes += residual * residual;
            double deviation = cost[i] - mean;
            ssTot += deviation * deviation;
        }
        if (ssTot == 0.0) {
            return (ssRes == 0.0) ? 1.0 : 0.0;
        }
        return 1.0 - (ssRes / ssTot);
    }

    /**
     * Estimate the exponent p in cost ~ k * n^p by ordinary least squares on
     * log(cost) against log(n).  Taking logs turns the power law into a
     * straight line, log y = log k + p * log n, whose slope is p.
     *
     * This is reported as a DIAGNOSTIC beside the candidate-set fit, never as
     * a replacement for it.  Its strength is that it is open-ended: it can
     * report p = 1.5 for data that lies between O(n) and O(n^2), which no
     * closed candidate set can express, so it exposes exactly the misfits the
     * ranked fit cannot describe.
     *
     * Its weakness is that a power law is the wrong model for several
     * candidates.  O(n) and O(n log n) both give p near 1 and cannot be told
     * apart this way -- the log n factor shows up only as a slow drift in the
     * slope.  O(log n) is not a power law at all.  That is precisely why this
     * supplements the candidate fit rather than replacing it.
     *
     * @param n input sizes, all positive
     * @param cost measured costs
     * @return a two-element array {exponent, rSquaredOfLogLogFit}, both NaN if
     *         any cost is zero (log is undefined) or the sizes do not vary
     */
    private static double[] estimateExponent(double[] n, double[] cost) {
        int m = n.length;
        for (int i = 0; i < m; i++) {
            if (cost[i] <= 0.0 || n[i] <= 0.0) {
                return new double[] {Double.NaN, Double.NaN};
            }
        }

        double[] lx = new double[m];
        double[] ly = new double[m];
        double meanX = 0.0;
        double meanY = 0.0;
        for (int i = 0; i < m; i++) {
            lx[i] = Math.log(n[i]);
            ly[i] = Math.log(cost[i]);
            meanX += lx[i];
            meanY += ly[i];
        }
        meanX /= m;
        meanY /= m;

        double sxy = 0.0;
        double sxx = 0.0;
        for (int i = 0; i < m; i++) {
            sxy += (lx[i] - meanX) * (ly[i] - meanY);
            sxx += (lx[i] - meanX) * (lx[i] - meanX);
        }
        if (sxx == 0.0) {
            // Every input size identical; the slope is undefined.  Rejected
            // earlier by the distinct-size check, guarded here as well.
            return new double[] {Double.NaN, Double.NaN};
        }

        double slope = sxy / sxx;
        double intercept = meanY - slope * meanX;

        // R^2 of the straight-line fit in log-log space.
        double ssRes = 0.0;
        double ssTot = 0.0;
        for (int i = 0; i < m; i++) {
            double predicted = intercept + slope * lx[i];
            ssRes += (ly[i] - predicted) * (ly[i] - predicted);
            ssTot += (ly[i] - meanY) * (ly[i] - meanY);
        }
        // A perfectly flat cost curve has zero variance in log y; the slope is
        // then exactly 0 and the fit is exact, so report R^2 = 1.
        double r2 = (ssTot == 0.0) ? 1.0 : 1.0 - (ssRes / ssTot);

        return new double[] {slope, r2};
    }

    private static double l2Norm(double[] v) {
        double sum = 0.0;
        for (int i = 0; i < v.length; i++) {
            sum += v[i] * v[i];
        }
        return Math.sqrt(sum);
    }

    private static boolean allIdentical(double[] v) {
        for (int i = 1; i < v.length; i++) {
            if (v[i] != v[0]) {
                return false;
            }
        }
        return true;
    }

    private static boolean allZero(double[] v) {
        for (int i = 0; i < v.length; i++) {
            if (v[i] != 0.0) {
                return false;
            }
        }
        return true;
    }

    private static Comparator<FitResult> byNormalisedMse() {
        return new Comparator<FitResult>() {
            public int compare(FitResult a, FitResult b) {
                return Double.compare(a.getNormalisedMse(), b.getNormalisedMse());
            }
        };
    }

    /** Move the O(1) entry to the front of the ranking, preserving the rest. */
    private static void promoteConstant(List<FitResult> ranked) {
        for (int i = 0; i < ranked.size(); i++) {
            if (CLASS_CONSTANT.equals(ranked.get(i).getComplexityClass())) {
                FitResult constant = ranked.remove(i);
                ranked.add(0, constant);
                return;
            }
        }
    }

    /**
     * Render a ranked fit report as a fixed-width table.
     *
     * @param report the report to format
     * @return formatted multi-line table
     */
    public static String formatRanking(FitReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-6s %-12s %14s %14s %10s %10s%n",
            "Rank", "Class", "Constant c", "Norm. MSE", "R^2", "Conf."));
        sb.append(dashes(72)).append("\n");
        List<FitResult> ranked = report.getRanked();
        for (int i = 0; i < ranked.size(); i++) {
            FitResult r = ranked.get(i);
            String mse = Double.isInfinite(r.getNormalisedMse())
                ? "n/a" : String.format("%14.6e", r.getNormalisedMse());
            String r2 = Double.isInfinite(r.getRSquared())
                ? "n/a" : String.format("%10.4f", r.getRSquared());
            sb.append(String.format("%-6d %-12s %14.4f %s %s %10s%n",
                i + 1, r.getComplexityClass(), r.getScalingConstant(),
                Double.isInfinite(r.getNormalisedMse()) ? String.format("%14s", mse) : mse,
                Double.isInfinite(r.getRSquared()) ? String.format("%10s", r2) : r2,
                i == 0 ? String.format("%.3f", r.getConfidence()) : "-"));
        }
        sb.append(dashes(72)).append("\n");
        sb.append(formatDiagnostics(report));
        return sb.toString();
    }

    /**
     * Render the absolute fit-quality check and the open-ended exponent
     * estimate.  Both are reported whatever the verdict, because they are most
     * informative exactly when the classification is unsatisfying.
     *
     * @param report the report to describe
     * @return formatted multi-line diagnostics
     */
    public static String formatDiagnostics(FitReport report) {
        StringBuilder sb = new StringBuilder();
        FitResult best = report.getBest();

        if (best != null && !Double.isInfinite(best.getNormalisedMse())) {
            sb.append(String.format("Absolute fit check: best nMSE %.3e vs ceiling %.1e -> %s%n",
                best.getNormalisedMse(), MAX_ACCEPTABLE_NMSE,
                report.isNoCandidateFit() ? "NO CANDIDATE FITS" : "pass"));
        }

        double p = report.getEstimatedExponent();
        if (Double.isNaN(p)) {
            sb.append("Estimated exponent: unavailable (a measured cost was zero)\n");
        } else {
            sb.append(String.format(
                "Estimated exponent (log-log regression, diagnostic only): "
                + "p = %.3f  (R^2 %.4f)%n", p, report.getExponentRSquared()));
            sb.append("  cost ~ n^p, fitted without a candidate menu, so it can indicate\n");
            sb.append("  growth that lies between two candidate classes. A log-log R^2 of\n");
            sb.append("  1 means the growth really is a power law; below 1 means it is not\n");
            sb.append("  (logarithmic and n log n growth both show up as a drifting slope,\n");
            sb.append("  so p alone is a weak discriminator among those).\n");
        }
        return sb.toString();
    }

    /**
     * Repeat a dash character; kept as a helper so the class stays free of
     * dependencies on newer String APIs.
     */
    private static String dashes(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append('-');
        }
        return sb.toString();
    }
}
