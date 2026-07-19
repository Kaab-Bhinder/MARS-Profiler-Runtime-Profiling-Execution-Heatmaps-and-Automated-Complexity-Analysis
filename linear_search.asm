# ==============================================================================
# Linear Search -- expected complexity O(n)
# ==============================================================================
#
# EMPIRICAL BENCHMARK CONVENTION
# ------------------------------
# This program is parameterised so that mars.simulator.EmpiricalComplexityRunner
# can execute it at a series of input sizes WITHOUT editing this file.  The
# runner patches the data segment after assembly and before simulation:
#
#   size    .word   input size n.  Overwritten by the runner with the n for
#                   this run.  The value below is only a standalone default.
#   target  .word   the key to search for.  The runner writes a key that is
#                   NOT present in the array, which forces the worst case and
#                   makes the instruction count a deterministic function of n.
#   arr     .space  data buffer, 1024 words.  The runner fills the first n
#                   words itself, from Java, via Memory.setWord.
#
# Why the runner fills the array rather than a setup loop in this file:
# an in-assembly fill loop would itself cost O(n) instructions and would be
# counted by the profiler, swamping the signal of any algorithm cheaper than
# linear.  Filling from the host keeps the measured instruction count equal to
# the cost of the algorithm under test alone.
#
# The largest supported n is 1024 (the capacity of arr).
# ==============================================================================

.data
    size:   .word 64            # patched by the runner
    target: .word -1            # patched by the runner (absent key => worst case)
    .align 2
    arr:    .space 4096         # 1024 words, filled by the runner

.text
.globl main

main:
    la   $s0, arr           # $s0 = base address of array
    lw   $s1, size          # $s1 = n
    lw   $s2, target        # $s2 = search key
    li   $t0, 0             # $t0 = i = 0
    li   $v1, -1            # $v1 = result index, -1 = not found

loop:
    bge  $t0, $s1, done     # while i < n
    sll  $t1, $t0, 2        # byte offset = i * 4
    add  $t1, $s0, $t1      # address of arr[i]
    lw   $t2, 0($t1)        # $t2 = arr[i]
    beq  $t2, $s2, found    # if arr[i] == key, stop
    addi $t0, $t0, 1        # i++
    j    loop

found:
    move $v1, $t0           # record the index we found it at

done:
    li $v0, 10
    syscall
