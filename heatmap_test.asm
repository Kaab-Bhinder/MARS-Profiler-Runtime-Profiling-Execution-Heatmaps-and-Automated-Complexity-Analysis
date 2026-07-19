# ==============================================================================
# Fixed-Iteration Loop -- expected complexity O(1)
# ==============================================================================
#
# EMPIRICAL BENCHMARK CONVENTION
# ------------------------------
# This program follows the data-segment layout used by
# mars.simulator.EmpiricalComplexityRunner (size / target / arr), but it
# DELIBERATELY IGNORES the patched size.  The loop below always runs a fixed
# number of iterations, so its dynamic instruction count is identical at every
# input size.
#
# That makes it the control case for the empirical classifier: a correct
# classifier must report O(1) here at every n.  The previous
# threshold-on-magnitude classifier could not, because it inferred the
# complexity class from how large the execution count happened to be rather
# than from how it grew.
#
# It also doubles as the heatmap demonstration program: the loop body lines are
# executed many times over and render hot, while the prologue and epilogue
# render cold.
# ==============================================================================

.data
    size:   .word 64            # patched by the runner, but intentionally unused
    target: .word -1            # unused by this benchmark
    .align 2
    arr:    .word 1, 2, 3, 4, 5

.text
.globl main

main:
    li $t0, 0               # accumulator
    li $t1, 5               # fixed iteration count, independent of size

loop:
    addi $t0, $t0, 1        # loop body (hot)
    beq  $t1, $zero, end    # exit test (hot)
    addi $t1, $t1, -1       # decrement (hot)
    j    loop               # back edge (hot)

end:
    li $v0, 10              # exit syscall setup (cold)
    syscall                 # exit (cold)
