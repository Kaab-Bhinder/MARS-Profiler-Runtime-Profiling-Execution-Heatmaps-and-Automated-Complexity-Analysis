# Simple test program for heatmap visualization
# This program has multiple execution paths to create visible heatmap

.data
    arr:  .word 1, 2, 3, 4, 5

.text
.globl main

main:
    # Initialize counter
    li $t0, 0          # Line 12: initialization (low freq)
    li $t1, 5          # Line 13: loop counter
    
loop:
    # Loop body - executed multiple times (should be red/orange)
    addi $t0, $t0, 1   # Line 18: increment (HIGH freq - red)
    beq $t1, $zero, end # Line 19: check condition (HIGH freq)
    addi $t1, $t1, -1  # Line 20: decrement (HIGH freq)
    j loop             # Line 21: jump back (HIGH freq - red)
    
end:
    # Program end - executed once (low freq - green)
    li $v0, 10         # Line 25: exit syscall setup (low freq)
    syscall            # Line 26: exit (low freq)
