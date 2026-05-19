# Linear Search - More Efficient O(n)
# Finds maximum element in array (simpler, faster operation)

.data
    arr:    .word 5, 2, 8, 1, 9, 3, 7
    size:   .word 7

.text
.globl main

main:
    # Initialize
    la $s0, arr          # $s0 = array address
    lw $s1, size         # $s1 = array size
    li $s2, 0            # counter = 0
    lw $s3, 0($s0)       # max = arr[0]
    
loop:
    bge $s2, $s1, done   # if counter >= size, done
    
    # Load current element
    mul $t0, $s2, 4      # counter*4
    add $t0, $s0, $t0    # address
    lw $t1, 0($t0)       # current element
    
    # if current <= max, skip
    ble $t1, $s3, next
    
    # max = current
    move $s3, $t1
    
next:
    addi $s2, $s2, 1
    j loop
    
done:
    # Exit
    li $v0, 10
    syscall
