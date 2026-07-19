# ==============================================================================
# Bubble Sort -- expected complexity O(n^2)
# ==============================================================================
#
# EMPIRICAL BENCHMARK CONVENTION
# ------------------------------
# Parameterised for mars.simulator.EmpiricalComplexityRunner.  The runner
# patches the data segment after assembly and before simulation:
#
#   size    .word   input size n, overwritten by the runner.
#   target  .word   unused by this benchmark; present so that every benchmark
#                   shares one data-segment layout.
#   arr     .space  data buffer, 1024 words.  The runner fills the first n
#                   words with a strictly DESCENDING sequence, which is the
#                   worst case for bubble sort and makes the instruction count
#                   a deterministic function of n.
#
# This implementation deliberately has no "swapped" early-exit flag, so the
# inner loop always runs its full (n-1) iterations and the cost is exactly
# quadratic regardless of the data.  The descending fill additionally forces
# the swap branch on every comparison.
#
# The array is filled from the host rather than by a setup loop here; see
# linear_search.asm for the full statement of the convention and the reason.
# ==============================================================================

.data
    size:   .word 64            # patched by the runner
    target: .word -1            # unused by this benchmark
    .align 2
    arr:    .space 4096         # 1024 words, filled descending by the runner

.text
.globl main

main:
    la   $s0, arr           # $s0 = base address of array
    lw   $s1, size          # $s1 = n
    addi $s1, $s1, -1       # $s1 = n - 1
    li   $t0, 0             # $t0 = i = outer loop counter

outer_loop:
    bge  $t0, $s1, done     # while i < n-1
    li   $t1, 0             # $t1 = j = inner loop counter

inner_loop:
    bge  $t1, $s1, outer_next   # while j < n-1
    sll  $t2, $t1, 2        # byte offset = j * 4
    add  $t2, $s0, $t2      # address of arr[j]
    lw   $t3, 0($t2)        # $t3 = arr[j]
    lw   $t4, 4($t2)        # $t4 = arr[j+1]
    ble  $t3, $t4, inner_next   # already in order, no swap
    sw   $t4, 0($t2)        # arr[j]   = arr[j+1]
    sw   $t3, 4($t2)        # arr[j+1] = arr[j]

inner_next:
    addi $t1, $t1, 1        # j++
    j    inner_loop

outer_next:
    addi $t0, $t0, 1        # i++
    j    outer_loop

done:
    li $v0, 10
    syscall
