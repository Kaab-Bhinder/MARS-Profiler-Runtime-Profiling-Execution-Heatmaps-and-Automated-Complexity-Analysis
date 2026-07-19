# ==============================================================================
# Recursive Binary Search -- expected complexity O(log n)
# ==============================================================================
#
# EMPIRICAL BENCHMARK CONVENTION
# ------------------------------
# Parameterised for mars.simulator.EmpiricalComplexityRunner.  The runner
# patches the data segment after assembly and before simulation:
#
#   size    .word   input size n, overwritten by the runner.
#   target  .word   the key to search for.  The runner writes a key that is NOT
#                   present, which forces the recursion to descend to a leaf and
#                   makes the call depth exactly floor(log2 n) + 1.
#   arr     .space  data buffer, 1024 words.  The runner fills the first n
#                   words with a strictly ASCENDING sequence -- binary search
#                   requires sorted input, and the runner is responsible for
#                   supplying it.
#
# The array is filled from the host rather than by a setup loop here because an
# in-assembly fill loop costs O(n) instructions, which would completely swamp
# the O(log n) signal this benchmark exists to measure.  See linear_search.asm
# for the full statement of the convention.
#
# WHY THIS BENCHMARK EXISTS
# -------------------------
# It is the same algorithm as binary_search.asm, but expressed with jal/jr and
# a real stack frame instead of a loop.  That difference is the whole point.
#
# The static structural analyzer in AlgorithmComplexityAnalyzer detects loops by
# finding BACK EDGES -- branches whose target label appears earlier in the file.
# A recursive function has no back edge: control leaves through jal and returns
# through jr, neither of which the analyzer follows.  So the structural hint for
# this program is nesting depth 0, implying O(1), which is as wrong as a
# structural hint can be.
#
# The empirical fit, which measures actual growth, should still report O(log n).
# The resulting disagreement is the intended result, not a defect: it is the
# clearest demonstration that syntactic loop counting cannot substitute for
# measurement.
#
# Calling convention used below:
#   $s0  base address of arr   (set up by main, not modified by the callee)
#   $s2  search key            (set up by main, not modified by the callee)
#   $a0  lo index              (argument)
#   $a1  hi index              (argument)
#   $v0  return value: index of the key, or -1 if absent
# ==============================================================================

.data
    size:   .word 64            # patched by the runner
    target: .word -1            # patched by the runner (absent key => full depth)
    .align 2
    arr:    .space 4096         # 1024 words, filled ascending by the runner

.text
.globl main

main:
    la   $s0, arr           # $s0 = base address of array
    lw   $s1, size          # $s1 = n
    lw   $s2, target        # $s2 = search key
    li   $a0, 0             # lo = 0
    addi $a1, $s1, -1       # hi = n - 1
    jal  bsearch            # recursive entry
    move $v1, $v0           # $v1 = result index, -1 = not found
    li $v0, 10
    syscall

# ------------------------------------------------------------------------------
# bsearch(lo = $a0, hi = $a1) -> $v0
# Recursive.  Each call establishes a two-word frame holding the return address
# and the incoming lo, so the frame grows to depth floor(log2 n) + 1.
# ------------------------------------------------------------------------------
bsearch:
    addi $sp, $sp, -8       # allocate frame
    sw   $ra, 4($sp)        # save return address
    sw   $a0, 0($sp)        # save incoming lo

    bgt  $a0, $a1, bs_absent    # base case: empty range, key is not present

    add  $t0, $a0, $a1
    sra  $t0, $t0, 1        # $t0 = mid = (lo + hi) / 2
    sll  $t1, $t0, 2        # byte offset = mid * 4
    add  $t1, $s0, $t1      # address of arr[mid]
    lw   $t2, 0($t1)        # $t2 = arr[mid]

    beq  $t2, $s2, bs_hit       # arr[mid] == key, done
    blt  $t2, $s2, bs_right     # arr[mid] <  key, recurse on upper half

    addi $a1, $t0, -1       # else recurse on lower half: hi = mid - 1
    jal  bsearch
    j    bs_return          # $v0 already holds the recursive result

bs_right:
    addi $a0, $t0, 1        # lo = mid + 1
    jal  bsearch
    j    bs_return

bs_hit:
    move $v0, $t0           # return the index we found it at
    j    bs_return

bs_absent:
    li   $v0, -1            # return "not found"

bs_return:
    lw   $a0, 0($sp)        # restore incoming lo
    lw   $ra, 4($sp)        # restore return address
    addi $sp, $sp, 8        # release frame
    jr   $ra
