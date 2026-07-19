# ==============================================================================
# Binary Search -- expected complexity O(log n)
# ==============================================================================
#
# EMPIRICAL BENCHMARK CONVENTION
# ------------------------------
# Parameterised for mars.simulator.EmpiricalComplexityRunner.  The runner
# patches the data segment after assembly and before simulation:
#
#   size    .word   input size n, overwritten by the runner.
#   target  .word   the key to search for.  The runner writes a key that is NOT
#                   present, which forces the search to descend to a leaf and
#                   makes the probe count exactly floor(log2 n) + 1.
#   arr     .space  data buffer, 1024 words.  The runner fills the first n
#                   words with a strictly ASCENDING sequence -- binary search
#                   requires sorted input, and the runner is responsible for
#                   supplying it.
#
# The array is filled from the host rather than by a setup loop here because an
# in-assembly fill loop costs O(n) instructions, which would completely swamp
# the O(log n) signal this benchmark exists to measure.
#
# See linear_search.asm for the full statement of the convention.
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
    li   $t0, 0             # $t0 = lo = 0
    addi $t1, $s1, -1       # $t1 = hi = n - 1
    li   $v1, -1            # $v1 = result index, -1 = not found

loop:
    bgt  $t0, $t1, done     # while lo <= hi
    add  $t2, $t0, $t1      # lo + hi
    sra  $t2, $t2, 1        # $t2 = mid = (lo + hi) / 2
    sll  $t3, $t2, 2        # byte offset = mid * 4
    add  $t3, $s0, $t3      # address of arr[mid]
    lw   $t4, 0($t3)        # $t4 = arr[mid]
    beq  $t4, $s2, found    # if arr[mid] == key, stop
    blt  $t4, $s2, go_right # if arr[mid] < key, search upper half
    addi $t1, $t2, -1       # else hi = mid - 1
    j    loop

go_right:
    addi $t0, $t2, 1        # lo = mid + 1
    j    loop

found:
    move $v1, $t2           # record the index we found it at

done:
    li $v0, 10
    syscall
