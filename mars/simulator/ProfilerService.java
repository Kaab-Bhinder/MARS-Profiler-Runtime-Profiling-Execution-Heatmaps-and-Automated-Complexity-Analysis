package mars.simulator;

import java.util.*;

/*
Copyright (c) 2003-2025, Pete Sanderson and Kenneth Vollmar

Developed by Pete Sanderson (psanderson@otterbein.edu)
and Kenneth Vollmar (kenvollmar@missouristate.edu)

Permission is hereby granted, free of charge, to any person obtaining 
a copy of this software and associated documentation files (the 
"Software"), to deal in the Software without restriction, including 
without limitation the rights to use, copy, modify, merge, publish, 
distribute, sublicense, and/or sell copies of the Software, and to 
permit persons to whom the Software is furnished to do so, subject 
to the following conditions:

The above copyright notice and this permission notice shall be 
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

(MIT license, http://www.opensource.org/licenses/mit-license.html)
*/

/**
 * Singleton service to profile MIPS program execution.
 * Tracks instruction execution frequency and other metrics.
 * 
 * @author Pete Sanderson and Ken Vollmar
 * @version 2025
 */
public class ProfilerService {
    
    private static ProfilerService instance = null;
    
    // Instruction profiling - tracks execution frequency of each instruction
    private HashMap<String, Integer> instructionCounts;
    
    // Register profiling - tracks read/write frequency per register
    private HashMap<Integer, Integer> registerReads;
    private HashMap<Integer, Integer> registerWrites;
    
    // Memory profiling - tracks read/write frequency per memory address
    private HashMap<Integer, Integer> memoryReads;
    private HashMap<Integer, Integer> memoryWrites;
    
    // Cycle/timing tracking - MIPS instruction latencies
    private HashMap<String, Integer> instructionCycles;  // Instruction mnemonic -> cycles
    private long totalCycles;
    private long totalMemoryCycles;  // Extra cycles for memory operations
    
    // State control
    private boolean isRecording;
    private int totalInstructions;
    private int totalRegisterReads;
    private int totalRegisterWrites;
    private int totalMemoryReads;
    private int totalMemoryWrites;
    
    /**
     * Private constructor - Singleton pattern
     */
    private ProfilerService() {
        instructionCounts = new HashMap<String, Integer>();
        instructionCycles = new HashMap<String, Integer>();
        registerReads = new HashMap<Integer, Integer>();
        registerWrites = new HashMap<Integer, Integer>();
        memoryReads = new HashMap<Integer, Integer>();
        memoryWrites = new HashMap<Integer, Integer>();
        isRecording = true;
        totalInstructions = 0;
        totalRegisterReads = 0;
        totalRegisterWrites = 0;
        totalMemoryReads = 0;
        totalMemoryWrites = 0;
        totalCycles = 0;
        totalMemoryCycles = 0;
        
        // Initialize MIPS instruction cycle counts (typical latencies)
        // Based on MIPS architecture specifications
        initializeCycleCounts();
    }
    
    /**
     * Initialize cycle counts for all MIPS instructions based on typical latencies
     */
    private void initializeCycleCounts() {
        // Arithmetic operations (1 cycle)
        String[] arithOps = {"add", "addi", "addiu", "addu", "sub", "subu", "and", "andi", 
                            "or", "ori", "xor", "xori", "nor", "sll", "sra", "srl"};
        for (String op : arithOps) {
            instructionCycles.put(op, 1);
        }
        
        // Multiply/Divide (1 cycle start, but results have latency - tracked separately)
        instructionCycles.put("mult", 1);
        instructionCycles.put("multu", 1);
        instructionCycles.put("div", 1);
        instructionCycles.put("divu", 1);
        
        // Move from special (1 cycle)
        instructionCycles.put("mfhi", 1);
        instructionCycles.put("mflo", 1);
        
        // Move to special (1 cycle)
        instructionCycles.put("mthi", 1);
        instructionCycles.put("mtlo", 1);
        
        // Set on less than (1 cycle)
        instructionCycles.put("slt", 1);
        instructionCycles.put("slti", 1);
        instructionCycles.put("sltiu", 1);
        instructionCycles.put("sltu", 1);
        
        // Load operations (3 cycles - memory access latency)
        instructionCycles.put("lw", 3);
        instructionCycles.put("lb", 3);
        instructionCycles.put("lbu", 3);
        instructionCycles.put("lh", 3);
        instructionCycles.put("lhu", 3);
        instructionCycles.put("lwu", 3);
        
        // Store operations (1 cycle)
        instructionCycles.put("sw", 1);
        instructionCycles.put("sb", 1);
        instructionCycles.put("sh", 1);
        
        // Branch operations (1 cycle, but may cause pipeline stalls)
        instructionCycles.put("beq", 1);
        instructionCycles.put("bne", 1);
        instructionCycles.put("blez", 1);
        instructionCycles.put("bgtz", 1);
        instructionCycles.put("bltz", 1);
        instructionCycles.put("bgez", 1);
        
        // Jump operations (1 cycle)
        instructionCycles.put("j", 1);
        instructionCycles.put("jr", 1);
        instructionCycles.put("jal", 1);
        instructionCycles.put("jalr", 1);
        
        // Default cycle count for unknown instructions
        instructionCycles.put("default", 1);
    }
    
    /**
     * Get singleton instance of ProfilerService
     * @return the ProfilerService instance
     */
    public static ProfilerService getInstance() {
        if (instance == null) {
            instance = new ProfilerService();
        }
        return instance;
    }
    
    /**
     * Reset all profiling counters. Called at the start of each simulation.
     */
    public void reset() {
        instructionCounts.clear();
        registerReads.clear();
        registerWrites.clear();
        memoryReads.clear();
        memoryWrites.clear();
        totalInstructions = 0;
        totalRegisterReads = 0;
        totalRegisterWrites = 0;
        totalMemoryReads = 0;
        totalMemoryWrites = 0;
        totalCycles = 0;
        totalMemoryCycles = 0;
        isRecording = true;
    }
    
    /**
     * Record execution of an instruction.
     * @param mnemonic the instruction mnemonic (e.g., "add", "lw", "beq")
     */
    public void recordInstruction(String mnemonic) {
        if (!isRecording) {
            return;
        }
        
        if (mnemonic == null) {
            return;
        }
        
        // Increment count for this instruction mnemonic
        if (instructionCounts.containsKey(mnemonic)) {
            instructionCounts.put(mnemonic, instructionCounts.get(mnemonic) + 1);
        } else {
            instructionCounts.put(mnemonic, 1);
        }
        
        totalInstructions++;
        
        // Add cycles for this instruction
        int cycles = instructionCycles.getOrDefault(mnemonic, 1);
        totalCycles += cycles;
    }
    
    /**
     * Record a register read operation.
     * @param registerNumber the register number (0-31 for general registers, 32=PC, 33=HI, 34=LO)
     */
    public void recordRegisterRead(int registerNumber) {
        if (!isRecording) {
            return;
        }
        
        if (registerReads.containsKey(registerNumber)) {
            registerReads.put(registerNumber, registerReads.get(registerNumber) + 1);
        } else {
            registerReads.put(registerNumber, 1);
        }
        
        totalRegisterReads++;
    }
    
    /**
     * Record a register write operation.
     * @param registerNumber the register number (0-31 for general registers, 33=HI, 34=LO)
     */
    public void recordRegisterWrite(int registerNumber) {
        if (!isRecording) {
            return;
        }
        
        // Don't count writes to $zero (register 0) since they have no effect
        if (registerNumber == 0) {
            return;
        }
        
        if (registerWrites.containsKey(registerNumber)) {
            registerWrites.put(registerNumber, registerWrites.get(registerNumber) + 1);
        } else {
            registerWrites.put(registerNumber, 1);
        }
        
        totalRegisterWrites++;
    }
    
    /**
     * Record a memory read operation.
     * @param address the memory address being read
     */
    public void recordMemoryRead(int address) {
        if (!isRecording) {
            return;
        }
        
        if (memoryReads.containsKey(address)) {
            memoryReads.put(address, memoryReads.get(address) + 1);
        } else {
            memoryReads.put(address, 1);
        }
        
        totalMemoryReads++;
    }
    
    /**
     * Record a memory write operation.
     * @param address the memory address being written
     */
    public void recordMemoryWrite(int address) {
        if (!isRecording) {
            return;
        }
        
        if (memoryWrites.containsKey(address)) {
            memoryWrites.put(address, memoryWrites.get(address) + 1);
        } else {
            memoryWrites.put(address, 1);
        }
        
        totalMemoryWrites++;
    }
    
    /**
     * Start recording profiling data
     */
    public void startRecording() {
        isRecording = true;
    }
    
    /**
     * Stop recording profiling data
     */
    public void stopRecording() {
        isRecording = false;
    }
    
    /**
     * Get the HashMap of instruction counts
     * @return HashMap mapping instruction mnemonic to execution count
     */
    public HashMap<String, Integer> getInstructionCounts() {
        return new HashMap<String, Integer>(instructionCounts);
    }
    
    /**
     * Get the HashMap of register read counts
     * @return HashMap mapping register number to read count
     */
    public HashMap<Integer, Integer> getRegisterReads() {
        return new HashMap<Integer, Integer>(registerReads);
    }
    
    /**
     * Get the HashMap of register write counts
     * @return HashMap mapping register number to write count
     */
    public HashMap<Integer, Integer> getRegisterWrites() {
        return new HashMap<Integer, Integer>(registerWrites);
    }
    
    /**
     * Get total number of instructions executed
     * @return total instruction count
     */
    public int getTotalInstructions() {
        return totalInstructions;
    }
    
    /**
     * Get total number of register reads
     * @return total register read count
     */
    public int getTotalRegisterReads() {
        return totalRegisterReads;
    }
    
    /**
     * Get total number of register writes
     * @return total register write count
     */
    public int getTotalRegisterWrites() {
        return totalRegisterWrites;
    }
    
    /**
     * Get the HashMap of memory read counts
     * @return HashMap mapping memory address to read count
     */
    public HashMap<Integer, Integer> getMemoryReads() {
        return new HashMap<Integer, Integer>(memoryReads);
    }
    
    /**
     * Get the HashMap of memory write counts
     * @return HashMap mapping memory address to write count
     */
    public HashMap<Integer, Integer> getMemoryWrites() {
        return new HashMap<Integer, Integer>(memoryWrites);
    }
    
    /**
     * Get total number of memory reads
     * @return total memory read count
     */
    public int getTotalMemoryReads() {
        return totalMemoryReads;
    }
    
    /**
     * Get total number of memory writes
     * @return total memory write count
     */
    public int getTotalMemoryWrites() {
        return totalMemoryWrites;
    }
    
    /**
     * Get total number of clock cycles executed
     * @return total cycle count
     */
    public long getTotalCycles() {
        return totalCycles;
    }
    
    /**
     * Get cycles per instruction (average)
     * @return average CPI (cycles per instruction)
     */
    public double getCyclesPerInstruction() {
        if (totalInstructions == 0) {
            return 0.0;
        }
        return (double) totalCycles / totalInstructions;
    }
    
    /**
     * Get cycles for a specific instruction mnemonic
     * @param mnemonic instruction mnemonic
     * @return cycles for that instruction type
     */
    public int getCyclesForInstruction(String mnemonic) {
        return instructionCycles.getOrDefault(mnemonic, 1);
    }
    
    /**
     * Record memory access latency (cache miss, etc)
     * @param additionalCycles extra cycles for memory operation
     */
    public void recordMemoryLatency(int additionalCycles) {
        if (!isRecording) {
            return;
        }
        totalMemoryCycles += additionalCycles;
        totalCycles += additionalCycles;
    }
    
    /**
     * Get register name from register number
     * @param regNum register number
     * @return register name string
     */
    private String getRegisterName(int regNum) {
        if (regNum == 32) return "PC";
        if (regNum == 33) return "HI";
        if (regNum == 34) return "LO";
        if (regNum >= 0 && regNum <= 31) {
            String[] regNames = {"$zero", "$at", "$v0", "$v1", "$a0", "$a1", "$a2", "$a3",
                                 "$t0", "$t1", "$t2", "$t3", "$t4", "$t5", "$t6", "$t7",
                                 "$s0", "$s1", "$s2", "$s3", "$s4", "$s5", "$s6", "$s7",
                                 "$t8", "$t9", "$k0", "$k1", "$gp", "$sp", "$fp", "$ra"};
            return regNames[regNum];
        }
        return "REG" + regNum;
    }
    
    /**
     * Generate a formatted text report of instruction profiling
     * @return String containing the formatted report
     */
    public String getInstructionReport() {
        StringBuilder report = new StringBuilder();
        
        report.append("\n");
        report.append("========================================\n");
        report.append("  MIPS INSTRUCTION PROFILE\n");
        report.append("========================================\n");
        
        if (totalInstructions == 0) {
            report.append("No instructions executed.\n");
            report.append("========================================\n");
            return report.toString();
        }
        
        report.append(String.format("Total Instructions Executed: %d\n\n", totalInstructions));
        
        // Sort instructions by frequency (descending)
        List<Map.Entry<String, Integer>> sortedList = 
            new ArrayList<Map.Entry<String, Integer>>(instructionCounts.entrySet());
        sortedList.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        // Print instruction frequencies
        report.append(String.format("%-15s %10s %10s\n", "Instruction", "Count", "Percent"));
        report.append("-".repeat(37) + "\n");
        
        for (Map.Entry<String, Integer> entry : sortedList) {
            String instruction = entry.getKey();
            int count = entry.getValue();
            double percent = (count * 100.0) / totalInstructions;
            report.append(String.format("%-15s %10d %9.2f%%\n", instruction, count, percent));
        }
        
        report.append("========================================\n");
        
        return report.toString();
    }
    
    /**
     * Print instruction report to standard output
     */
    public void printInstructionReport() {
        System.out.print(getInstructionReport());
    }
    
    /**
     * Generate a formatted text report of register profiling
     * @return String containing the formatted register report
     */
    public String getRegisterReport() {
        StringBuilder report = new StringBuilder();
        
        report.append("\n");
        report.append("========================================\n");
        report.append("  MIPS REGISTER PROFILE\n");
        report.append("========================================\n");
        
        if (totalRegisterReads == 0 && totalRegisterWrites == 0) {
            report.append("No register accesses recorded.\n");
            report.append("========================================\n");
            return report.toString();
        }
        
        report.append(String.format("Total Register Reads: %d\n", totalRegisterReads));
        report.append(String.format("Total Register Writes: %d\n", totalRegisterWrites));
        report.append(String.format("Total Accesses: %d\n\n", totalRegisterReads + totalRegisterWrites));
        
        // Create combined access map for sorting
        HashMap<Integer, Integer> combinedAccess = new HashMap<Integer, Integer>();
        for (Integer reg : registerReads.keySet()) {
            combinedAccess.put(reg, registerReads.get(reg));
        }
        for (Integer reg : registerWrites.keySet()) {
            int combined = combinedAccess.getOrDefault(reg, 0) + registerWrites.get(reg);
            combinedAccess.put(reg, combined);
        }
        
        // Sort by total accesses (descending)
        List<Map.Entry<Integer, Integer>> sortedList = 
            new ArrayList<Map.Entry<Integer, Integer>>(combinedAccess.entrySet());
        sortedList.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        // Print register statistics
        report.append(String.format("%-10s %10s %10s %10s\n", "Register", "Reads", "Writes", "Total"));
        report.append("-".repeat(42) + "\n");
        
        for (Map.Entry<Integer, Integer> entry : sortedList) {
            int regNum = entry.getKey();
            String regName = getRegisterName(regNum);
            int reads = registerReads.getOrDefault(regNum, 0);
            int writes = registerWrites.getOrDefault(regNum, 0);
            int total = reads + writes;
            
            report.append(String.format("%-10s %10d %10d %10d\n", regName, reads, writes, total));
        }
        
        report.append("========================================\n");
        
        return report.toString();
    }
    
    /**
     * Print register report to standard output
     */
    public void printRegisterReport() {
        System.out.print(getRegisterReport());
    }
    
    /**
     * Generate a formatted text report of memory profiling
     * @return String containing the formatted memory report
     */
    public String getMemoryReport() {
        StringBuilder report = new StringBuilder();
        
        report.append("\n");
        report.append("========================================\n");
        report.append("  MIPS MEMORY ACCESS PROFILE\n");
        report.append("========================================\n");
        
        if (totalMemoryReads == 0 && totalMemoryWrites == 0) {
            report.append("No memory accesses recorded.\n");
            report.append("========================================\n");
            return report.toString();
        }
        
        report.append(String.format("Total Memory Reads: %d\n", totalMemoryReads));
        report.append(String.format("Total Memory Writes: %d\n", totalMemoryWrites));
        report.append(String.format("Total Accesses: %d\n", totalMemoryReads + totalMemoryWrites));
        report.append(String.format("Unique Addresses: %d\n\n", 
            getUniqueMemoryAddresses()));
        
        // Create combined access map for sorting
        HashMap<Integer, Integer> combinedAccess = new HashMap<Integer, Integer>();
        for (Integer addr : memoryReads.keySet()) {
            combinedAccess.put(addr, memoryReads.get(addr));
        }
        for (Integer addr : memoryWrites.keySet()) {
            int combined = combinedAccess.getOrDefault(addr, 0) + memoryWrites.get(addr);
            combinedAccess.put(addr, combined);
        }
        
        // Sort by total accesses (descending)
        List<Map.Entry<Integer, Integer>> sortedList = 
            new ArrayList<Map.Entry<Integer, Integer>>(combinedAccess.entrySet());
        sortedList.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        // Print top 50 memory addresses (limit output for readability)
        report.append(String.format("%-12s %10s %10s %10s\n", "Address", "Reads", "Writes", "Total"));
        report.append("-".repeat(44) + "\n");
        
        int printed = 0;
        for (Map.Entry<Integer, Integer> entry : sortedList) {
            if (printed >= 50) {
                report.append(String.format("... and %d more addresses\n", sortedList.size() - 50));
                break;
            }
            
            int addr = entry.getKey();
            int reads = memoryReads.getOrDefault(addr, 0);
            int writes = memoryWrites.getOrDefault(addr, 0);
            int total = reads + writes;
            
            // Format address as hex
            String hexAddr = String.format("0x%08X", addr);
            report.append(String.format("%-12s %10d %10d %10d\n", hexAddr, reads, writes, total));
            printed++;
        }
        
        report.append("========================================\n");
        
        return report.toString();
    }
    
    /**
     * Print memory report to standard output
     */
    public void printMemoryReport() {
        System.out.print(getMemoryReport());
    }
    
    /**
     * Get count of unique memory addresses accessed
     * @return number of unique addresses
     */
    private int getUniqueMemoryAddresses() {
        HashSet<Integer> uniqueAddrs = new HashSet<Integer>(memoryReads.keySet());
        uniqueAddrs.addAll(memoryWrites.keySet());
        return uniqueAddrs.size();
    }
    
    /**
     * Get total register operations (reads + writes)
     * @return total register read and write count
     */
    public int getTotalRegisterOperations() {
        return totalRegisterReads + totalRegisterWrites;
    }
    
    /**
     * Get total memory operations (reads + writes)
     * @return total memory read and write count
     */
    public int getTotalMemoryOperations() {
        return totalMemoryReads + totalMemoryWrites;
    }
}
