# Bubble Sort - Inefficient O(n²)
# Sorts array in ascending order

.data
    arr:    .word 5, 2, 8, 1, 9, 3, 7
    size:   .word 7

.text
.globl main

main:
    # Initialize
    la $s0, arr          # $s0 = array address
    lw $s1, size         # $s1 = array size
    addi $s1, $s1, -1    # n-1
    li $t0, 0            # outer loop counter
    
outer_loop:
    bge $t0, $s1, done   # if i >= n-1, done
    li $t1, 0            # inner loop counter
    
inner_loop:
    bge $t1, $s1, outer_next
    
    # Load arr[j] and arr[j+1]
    mul $t2, $t1, 4      # j*4
    add $t2, $s0, $t2    # address of arr[j]
    lw $t3, 0($t2)       # arr[j]
    lw $t4, 4($t2)       # arr[j+1]
    
    # if arr[j] <= arr[j+1], skip swap
    ble $t3, $t4, inner_next
    
    # Swap arr[j] and arr[j+1]
    sw $t4, 0($t2)       # arr[j] = arr[j+1]
    sw $t3, 4($t2)       # arr[j+1] = arr[j]
    
inner_next:
    addi $t1, $t1, 1
    j inner_loop
    
outer_next:
    addi $t0, $t0, 1
    j outer_loop
    
done:
    # Exit
    li $v0, 10
    syscall
