# Empirical Complexity Inference in MARS

This document describes how the complexity classifier in this fork of MARS
works, why each choice was made, and where the method breaks down. It is
written to be adapted into the Methodology section of a paper.

---

## 1. What changed and why

The previous classifier inferred a complexity class from the execution count of
the hottest source line in a **single** run, using fixed thresholds:

```
hot line executed > 1000 times  ->  O(n^3)
                  >  100 times  ->  O(n^2)
                  >   10 times  ->  O(n)
                  otherwise     ->  O(1)
```

This is not a measurement of complexity. Complexity is a statement about how
cost *grows* with input size, and a single measurement contains no growth
information at all. The rule is unsound in both directions:

- A constant-time routine with a large constant factor (a fixed 5000-iteration
  loop) is classified O(n^3), because magnitude is being read as growth.
- A quadratic routine on a small input (n = 5, 25 inner iterations) is
  classified O(n), because the same confusion runs the other way.

No choice of thresholds repairs this, because the thresholds are attempting to
recover a two-dimensional property (cost versus size) from a one-dimensional
observation. The classifier has been replaced by multi-size measurement and
curve fitting, which is the standard empirical approach.

---

## 2. Cost metric: dynamic instruction count

The cost recorded for each run is the **total number of instructions executed
by the simulator**, counted by `ProfilerService` at the fetch-execute boundary
in `Simulator.java`.

Wall-clock time is deliberately not used. This is a methodological advantage of
working inside a simulator rather than on real hardware:

| Property | Instruction count | Wall-clock time |
|---|---|---|
| Repeatable on the same machine | exactly | no (variance from cache, scheduler, frequency scaling) |
| Reproducible on another machine | exactly | no |
| Needs repeated trials and averaging | no | yes |
| Needs warm-up / outlier rejection | no | yes |
| Confounded by JIT, OS noise, thermal state | no | yes |

Because the metric is exact, each (n, cost) point is a single deterministic
observation rather than a sample from a noisy distribution. There is no error
bar to report and no averaging step to justify. Re-running the full experiment
produces byte-identical output; this was verified by diffing two consecutive
runs.

The cost of that choice is that instruction count is a *proxy* for time: it
weights every instruction equally and ignores memory hierarchy and pipeline
effects. For inferring an asymptotic class this is acceptable, because the
class depends on how the operation count scales, not on the per-operation
constant. It would not be acceptable for predicting real runtime. (`ProfilerService`
does also maintain a cycle-count model with per-instruction latencies; that
model would give a different constant factor but the same complexity class,
since it is a fixed positive weighting of the same instruction stream.)

---

## 3. Measurement procedure

`EmpiricalComplexityRunner` executes each benchmark headlessly at each input
size in the sequence **n = 10, 20, 40, 80, 160, 320** (configurable). Sizes
double so that the ratio cost(2n)/cost(n) is directly interpretable: it tends to
1 for O(1), 2 for O(n), and 4 for O(n^2).

For each size the runner:

1. Assembles the program afresh (which clears the data segment and symbol table).
2. Resets the register file and both coprocessors.
3. Resolves the data-segment labels `size`, `target` and `arr` through the
   symbol table, and writes the input size and array contents directly into
   simulated memory.
4. Resets `ProfilerService` and `ExecutionHeatmap`.
5. Runs to termination and reads the total instruction count.

### Parameterisation without editing the benchmark

Benchmarks are not edited between runs. Each declares a fixed data-segment
layout, documented in a header comment in every `.asm` file:

```asm
size:   .word 64      # patched by the runner with n
target: .word -1      # patched by the runner with an absent search key
arr:    .space 4096   # 1024 words, filled by the runner
```

**The array is filled from the host, not by a setup loop inside the benchmark.**
This matters more than it first appears. An in-assembly fill loop costs O(n)
instructions and would be counted by the profiler. For bubble sort that is
harmless (it is swamped by the O(n^2) body), but for binary search it would
*dominate*: the measured cost would be O(n) + O(log n) = O(n), and the benchmark
would be classified O(n) no matter how correct the fitter was. Filling from Java
keeps the measured count equal to the cost of the algorithm under test alone.

### Forcing the worst case

The runner writes a search key that is absent from the array. This makes the
cost at each size the deterministic worst case rather than a data-dependent
value: linear search scans all n elements, and binary search descends to a leaf
in exactly floor(log2 n) + 1 probes — as does the recursive version, whose call
depth is therefore also floor(log2 n) + 1. Bubble sort is filled in descending order,
its worst case, and its implementation has no early-exit flag, so its inner loop
always runs the full (n-1) iterations.

---

## 4. The fitting method

Let the observations be (n_i, y_i) for i = 1..m, where y_i is the instruction
count at size n_i. The candidate model set is

> O(1), O(log n), O(n), O(n log n), O(n^2), O(n^3)

For each candidate f:

**Step 1 — basis.** Evaluate f_i = f(n_i). Logarithms are base 2; the base only
rescales the fitted constant and cannot change the ranking.

**Step 2 — least-squares scaling constant.** Fit the single free parameter c
minimising the residual sum of squares:

> minimise  S(c) = sum_i (c·f_i − y_i)^2

Setting dS/dc = 0 gives the closed form

> **c = ( sum_i f_i·y_i ) / ( sum_i f_i^2 )**

**Step 3 — normalised mean squared error.** Normalise both the model vector and
the observation vector to unit L2 length, then take the mean squared error
between them:

> **nMSE(f) = (1/m) · sum_i ( f_i/‖f‖ − y_i/‖y‖ )^2**

The candidate with the smallest nMSE is the classification.

### Why normalise

Raw MSE cannot be compared across candidate classes. The residuals of a model
fitted to data of magnitude 10^6 are numerically far larger than those of a
model fitted to data of magnitude 10^2, whatever the quality of either fit. A
ranking on raw MSE therefore systematically favours the slower-growing classes —
it penalises a class for the size of the numbers involved rather than for
fitting worse. Normalisation removes magnitude entirely and leaves only the
shape of the growth curve, which is exactly the property a complexity class
describes.

A useful consequence: since the predicted vector is c·f, and

> (c·f)/‖c·f‖ = f/‖f‖   for any c > 0

the scaling constant **cancels out of the nMSE**. The fit criterion reduces to
the angle between the observed cost vector and the model basis vector, and is
scale-free by construction. The constant c is still computed and reported,
because it is the per-element cost of the algorithm and is of independent
interest — for example c ≈ 10.95 for bubble sort means roughly 11 instructions
per comparison in the inner loop.

R² is reported alongside, computed on the raw (un-normalised) fit. Because the
model has no intercept term, R² can be negative for a badly matched class; this
is expected and is not clamped. Ranking is done on nMSE, not R².

### Two independent acceptance checks

A fit is only reported as a classification if it passes **both** a relative and
an absolute check. These catch different failures and are reported differently,
because they call for different responses.

**Relative check — confidence.** The separation between the best and
second-best fit:

> **confidence = (nMSE_2 − nMSE_1) / nMSE_2**,  clamped to [0, 1]

If the top two candidates are within 10% of each other (confidence < 0.10) the
result is **inconclusive**, naming both candidates. This is a statement about
the data, not a failure of the tool: the sampled range of input sizes does not
distinguish the two classes, and the correct response is to widen the range.

**Absolute check — goodness-of-fit ceiling.** Confidence measures separation
*between candidates*, so it says nothing about whether either candidate is any
good. A curve matching no candidate at all can still separate cleanly from the
runner-up and be reported with high confidence. Only an absolute check catches
that, so the best candidate's normalised MSE must also satisfy

> **nMSE_1 ≤ 5.0 × 10⁻⁴**

Above the ceiling the verdict is **no candidate fits**, and the classification
is withheld.

*Derivation of the ceiling.* It is fixed by two measured anchors, both taken
before the recursive benchmark existed:

| anchor | nMSE |
|---|---:|
| worst correctly-classified fit (binary_search) | 1.6554e-04 |
| closest misfit that must be rejected (n^1.5 data) | 1.5973e-03 |

These differ by roughly one order of magnitude, so the threshold is placed at
their **geometric mean** — the midpoint in log space, the natural centre for a
quantity spanning orders of magnitude:

> sqrt(1.6554e-04 × 1.5973e-03) = 5.142e-04

rounded down to 5.0e-04, the conservative direction, since erring low flags a
borderline result rather than silently accepting it. That leaves a factor of
3.02 above the worst fit that must be accepted and 3.19 below the misfit that
must be rejected.

The recursive benchmark, added *after* the ceiling was fixed, landed at nMSE
1.7206e-04 and passed without adjustment — a small out-of-sample check on the
value.

*The separation is not clean in general*, and the ceiling should be read as a
guard against gross misfits rather than as a decision boundary. Data generated
as n^1.3 fits O(n log n) at nMSE 2.2606e-04 — a real misfit scoring **better**
than the correctly-classified binary_search, and so passing the check. No single
threshold can separate those two cases, because over a 32× range of n the curves
genuinely are that similar. The exponent estimate below is the diagnostic that
exposes such cases.

### Exponent estimation (diagnostic)

Alongside the candidate fit, the exponent p in cost ~ k·n^p is estimated
directly by ordinary least squares on log(cost) against log(n) — taking logs
turns the power law into the straight line log y = log k + p·log n, whose slope
is p. The log-log R² is reported with it.

This is **diagnostic only and never overrides the classification.** Its value is
that it is open-ended: for n^1.5 data it recovers p = 1.498 (R² = 1.0000), which
no member of a closed candidate set can express. A p sitting well away from 0,
1, 2 or 3 is a signal that the true growth lies between the candidates.

It cannot replace the candidate fit, because a power law is the wrong model for
several candidates and the exponent alone does not name a class. Measured over
n = 10..320:

| growth | p | log-log R² | |
|---|---:|---:|---|
| O(n) | 1.000 | 1.000000 | a true power law |
| O(n log n) | 1.262 | 0.999222 | not a power law |
| O(log n) | 0.228 | 0.9865 | not a power law |

The log n factor never appears as a distinct exponent; it appears as a slowly
**drifting slope**, since d(log(n log n))/d(log n) = 1 + 1/ln n, falling from
1.43 at n = 10 to 1.17 at n = 320. Regression averages that drift into a single
number near 1.26, which is not a recognisable landmark and does not by itself
say "there is a log factor here". The gap from O(n) is only 0.26 and is
range-dependent, so p is a weak discriminator between those two. The tell is
instead the **R²**: a genuine power law fits the log-log line exactly, and
anything less indicates growth that is not of the form n^p at all.

The two methods answer different questions — the candidate fit names a class
from a fixed menu, the exponent measures growth without a menu — and are
reported together.

### Explicit handling of degenerate cases

The fitter refuses or specially handles inputs it cannot honestly fit:

| Condition | Behaviour |
|---|---|
| Fewer than 4 samples | Rejected. Below four points the ranking is not meaningfully constrained. |
| Fewer than 4 *distinct* sizes | Rejected. Repeating a size adds no growth information. |
| n ≤ 0 | Rejected; log n is undefined. |
| All costs zero | Rejected — the benchmark did not execute. Checked *before* the constant case, which would otherwise report it as O(1). |
| All costs identical (non-zero) | Classified **O(1) by direct observation**, with confidence 1.0, and flagged as such. Not routed through the curve fit, since a flat observation vector makes several models numerically indistinguishable. |
| Degenerate basis (e.g. log n where every n = 1) | That candidate is ranked last with nMSE = ∞ rather than treated as a perfect fit. |

---

## 5. Structural hint versus empirical fit

Static loop-nesting detection is retained but **demoted**. It is reported as a
separate "structural hint" and never as the classification.

The hint is computed by real static analysis of the source: a loop is identified
by a **back edge**, a branch or jump whose target label is defined earlier in
the file. Back edges sharing a target label are merged into one loop (a loop
body with several `continue` paths is one loop, not several nested ones), and
the nesting depth is the largest number of loop intervals covering any single
line. Depth d is mapped to O(n^d).

`jal` and `jalr` are deliberately **excluded** from the back-edge set. They are
calls, not loop edges: control leaves the procedure and returns through `jr`, so
a `jal` whose target happens to sit earlier in the file is not a loop. Including
it produced a false positive on any subroutine defined above its call site —
straight-line code consisting of `helper: … jr $ra` followed by `main: jal
helper` was reported as nesting depth 1, implying O(n), for a program containing
no loop at all. Excluding `jal` is also why recursion is invisible to this
analyzer, which is the point of the recursive benchmark below.

That depth-to-class mapping assumes every loop iterates proportionally to n,
which is often false. The hint counts loops, not iterations. It is an upper
bound on the loop-driven part of the cost and nothing more.

**When the hint and the fit disagree, both are reported and the disagreement is
flagged** — in the text report, the CSV (`agreement` column), and the GUI. The
disagreement is informative rather than a defect. In this suite:

| Benchmark | Structural hint | Empirical fit | |
|---|---|---|---|
| heatmap_test | O(n) | O(1) | disagree — loop bound is a literal constant, not n |
| binary_search | O(n) | O(log n) | disagree — loop halves its range per iteration |
| **recursive_binary_search** | **O(1)** | **O(log n)** | **disagree — no loop exists to find; the recursion is invisible** |
| linear_search | O(n) | O(n) | agree |
| bubble_sort | O(n^2) | O(n^2) | agree |

Three of five disagree, and each is a case where purely syntactic analysis is
*necessarily* wrong:

- **heatmap_test** — a loop exists, but its trip count is a literal constant, so
  loop-counting over-states the growth.
- **binary_search** — a loop exists, but its trip count is logarithmic rather
  than proportional to n, so loop-counting again over-states it.
- **recursive_binary_search** — the most severe case, and the reason it was
  added. It is the *same algorithm* as binary_search expressed with `jal`/`jr`
  and a stack frame instead of a loop. There is no back edge anywhere in the
  file, so the analyzer reports nesting depth 0 and hints **O(1)** — maximally
  wrong, since the cost genuinely grows. The empirical fit nonetheless returns
  O(log n) at nMSE 1.72e-04 with confidence 0.984.

The recursive case is the sharpest available demonstration that the empirical
method is doing the work: two programs with identical asymptotic behaviour
(iterative and recursive binary search, both O(log n), differing only by a
constant factor of roughly 1.7 in the fitted c — 13.45 versus 22.46
instructions per level, the difference being per-call frame overhead) receive
*opposite* structural hints, O(n) and O(1), while the measurement gets both
right.

---

## 6. Results

Full experiment, 233-source-file build, all four benchmarks:

### Measured growth (dynamic instruction count)

| n | heatmap_test | binary_search | recursive_binary_search | linear_search | bubble_sort |
|---:|---:|---:|---:|---:|---:|
| 10 | 26 | 49 | 82 | 92 | 973 |
| 20 | 26 | 61 | 102 | 172 | 4,133 |
| 40 | 26 | 73 | 122 | 332 | 17,053 |
| 80 | 26 | 85 | 142 | 652 | 69,293 |
| 160 | 26 | 97 | 162 | 1,292 | 279,373 |
| 320 | 26 | 109 | 182 | 2,572 | 1,121,933 |

Note the signatures visible in the raw numbers before any fitting: heatmap_test
is exactly flat; binary_search rises by a constant **+12 instructions per
doubling** of n and the recursive version by a constant **+20**, both the
definition of logarithmic growth; linear_search approaches a doubling ratio of
2; bubble_sort approaches 4.

### Classification

| benchmark | predicted | expected | nMSE | R² | conf. | exponent p | verdict | match |
|---|---|---|---:|---:|---:|---:|---|---|
| heatmap_test | O(1) | O(1) | 0.0000e+00 | 1.0000 | 1.000 | 0.000 | classified | yes |
| binary_search | O(log n) | O(log n) | 1.6554e-04 | 0.9843 | 0.984 | 0.228 | classified | yes |
| recursive_binary_search | O(log n) | O(log n) | 1.7206e-04 | 0.9836 | 0.984 | 0.228 | classified | yes |
| linear_search | O(n) | O(n) | 8.3786e-06 | 0.9999 | 0.994 | 0.964 | classified | yes |
| bubble_sort | O(n^2) | O(n^2) | 2.6281e-07 | 1.0000 | 1.000 | 2.032 | classified | yes |

**5 of 5 classified as expected**, all with confidence well above the 0.10
threshold and all comfortably under the 5.0e-04 goodness-of-fit ceiling. No
constants were tuned to obtain this result. The expected class is recorded for
reporting only; it is never passed to the fitter and cannot influence a
classification.

The exponent column behaves exactly as §4 predicts: accurate and interpretable
for the power-law cases (0.000, 0.964, 2.032), uninformative for the logarithmic
ones (0.228). That is why it supplements rather than replaces the candidate fit.

The two binary searches are worth reading together. They are the same algorithm,
one iterative and one recursive, and the fit assigns both O(log n) while
separating their constants: c = 13.45 versus 22.46 instructions per level. The
1.7× difference is the per-call stack-frame overhead, which is exactly what the
scaling constant is supposed to capture — a constant factor, not a change of
class.

### Rejection of an out-of-set growth curve

Constructed data following n^1.5, a growth rate no candidate class can express:

| best candidate | nMSE | confidence | verdict | exponent p (R²) |
|---|---:|---:|---|---:|
| O(n log n) | 1.5973e-03 | 0.260 | **no-candidate-fits** | 1.498 (1.0000) |

The confidence of 0.260 sits well **above** the 0.10 inconclusive threshold, so
the relative check alone would have accepted this and reported O(n log n) as a
classification — which is what the previous version of this tool did. Only the
absolute ceiling rejects it. The exponent estimate then identifies what the
growth actually is, recovering p = 1.498 against a true 1.5 with an exact
log-log fit. This case motivated both additions and is covered by a regression
test.

### Reproducibility

Two consecutive full runs produce byte-identical console output and
byte-identical CSV files.

Raw data is exported to `complexity_samples.csv` (benchmark, n,
instructionCount) and `complexity_fits.csv` (benchmark, predictedClass,
trueClass, normalisedMse, rSquared, confidence, verdict, inconclusive,
noCandidateFit, estimatedExponent, exponentRSquared, structuralHint, agreement).

---

## 7. Known limitations

These are real and should be stated in the paper rather than discovered by a
reviewer.

1. **The candidate set is closed — partially addressed.** The set is still
   closed, but a fit that matches no candidate is now detected rather than
   silently reported. Two mechanisms were added (§4): an absolute
   goodness-of-fit ceiling that returns a *no-candidate-fits* verdict distinct
   from *inconclusive*, and an open-ended log-log exponent estimate that
   describes growth the candidate set cannot express. The n^1.5 case that
   previously classified as O(n log n) with confidence 0.26 is now rejected, and
   its exponent recovered as 1.498.

   What remains: the ceiling catches gross misfits, not marginal ones. Data
   generated as n^1.3 fits O(n log n) at nMSE 2.2606e-04 and passes, despite
   scoring better than the correctly-classified binary_search at 1.6554e-04. No
   threshold separates those two, because over a 32× range of n the curves
   genuinely are that similar. Widening the size range is the only real remedy.
   The exponent estimate is also weak precisely where the candidate set is
   strong: it cannot distinguish O(n) from O(n log n) (p = 1.000 versus 1.262,
   a small and range-dependent gap) and is uninformative for logarithmic growth.
   A principled fix would fit an explicit two-parameter model a + c·f(n) and
   compare candidates by an information criterion, which would also address
   limitation 2.

2. **The model has no intercept term.** The fit is c·f(n), not a + c·f(n). Fixed
   setup cost is therefore absorbed into c rather than separated out, which
   biases the result toward the lower class when the constant term dominates
   over the sampled range. Constructed data of the form y = 1000 + 10n sampled
   over n = 10..16 is classified O(1), correctly reflecting that the data really
   is nearly flat there but not reflecting the underlying model. Widening the
   size range mitigates this; the benchmarks here keep prologue cost small
   (roughly 8 instructions) relative to the signal.

3. **Six sizes over a 32× range is a narrow window.** Adjacent classes such as
   O(n) and O(n log n) differ by a factor of only log n, and separating them
   reliably requires either a wider range or more points. The 0.994 confidence
   for linear_search reflects a clean separation here, but that should not be
   assumed to generalise.

4. **Worst case only.** The runner forces the worst case by construction. It
   does not measure average-case behaviour, which would require a distribution
   over inputs and would reintroduce the need for repeated trials.

5. **Single-parameter inputs.** Complexity is inferred with respect to one
   scalar input size. Algorithms whose cost depends on two independent
   parameters (graph algorithms in |V| and |E|, for instance) are outside the
   current model.

6. **Instruction count is not time.** As noted in §2, the metric ignores the
   memory hierarchy and pipeline behaviour. It supports claims about asymptotic
   class, not about real-world runtime.

7. **Pseudo-instruction expansion affects the constant.** MARS expands pseudo-
   instructions (`la`, `li`, `bge`, `move`) into multiple real instructions, so
   the fitted constant c depends on the assembler's expansion choices. The
   complexity class does not, since expansion applies a bounded factor.

8. **The structural hint has no interprocedural analysis — now demonstrated.**
   It examines back edges within a single source file and does not follow
   `jal`/`jr` call structure, so recursion is not detected at all. This is no
   longer a prediction: `recursive_binary_search.asm` reports nesting depth 0
   and hints **O(1)** while its measured growth is O(log n) (§5). It is the most
   severe of the three disagreement cases in the suite.

   This is deliberately **not repaired**. Two reasons. First, the hint is
   already explicitly subordinate to the fit, so a wrong hint costs nothing as
   long as the disagreement is surfaced — which it is, in the report, the CSV
   and the GUI. Second, detecting a recursive call is not the hard part;
   detecting the recursion *depth* is, and that is what determines the cost. A
   call-graph analysis would tell you `bsearch` calls itself, which implies
   unbounded cost, not O(log n) — you would still need to prove the range halves
   each call. That is a termination-and-bounds argument, not a syntactic one,
   and it is exactly the question measurement answers directly.

   A related false positive was found and fixed while adding this benchmark:
   `jal` was originally treated as a back edge, so any subroutine defined above
   its call site was counted as a loop, and straight-line code with one function
   call was hinted O(n). `jal` and `jalr` are now excluded as calls rather than
   loop edges (§5). The four pre-existing benchmarks contain no `jal` and their
   hints are unchanged.

9. **The exponent diagnostic assumes a power law.** It is fitted by least
   squares on log-transformed data, which is not equivalent to least squares on
   the original scale: taking logs weights proportional rather than absolute
   errors, so small-n points carry more influence than they would otherwise.
   For clean simulator data this is immaterial, but it means the reported p is
   not directly comparable to the scaling constant c from the candidate fit, and
   the two should not be mixed in a single argument.

---

## 8. Reproducing

```bash
# compile (encoding flag required: the tree mixes UTF-8 and legacy sources)
find . -name "*.java" -not -path "./docs/*" > sources.txt
javac -encoding UTF-8 -d classes @sources.txt

# run the full experiment; writes complexity_samples.csv and complexity_fits.csv
java -cp classes:. mars.simulator.ComplexityExperiment . .
```

Interactively, the same sweep is available from the MARS GUI under
**Tools → Resource Profiler → Complexity**, which reports the fitted class, the
confidence, the full ranked candidate list, the structural hint, and any
disagreement. Low-confidence results are displayed as "INCONCLUSIVE" rather than
being presented as a classification.

Relevant source:

| File | Role |
|---|---|
| `mars/simulator/ComplexityFitter.java` | curve fitting, normalisation, confidence, absolute ceiling, exponent estimate, degenerate cases |
| `mars/simulator/EmpiricalComplexityRunner.java` | headless multi-size execution, input patching, CSV export |
| `mars/simulator/ComplexityExperiment.java` | command-line driver for the full experiment |
| `mars/simulator/AlgorithmComplexityAnalyzer.java` | reporting, static structural hint, disagreement flagging |
| `mars/tools/ResourceProfiler.java` | GUI Complexity tab |
