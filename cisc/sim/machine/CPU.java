package cisc.sim.machine;

import cisc.sim.memory.Memory;
import cisc.sim.memory.MemoryFault;
import cisc.sim.cache.Cache;
import cisc.sim.cache.CacheLine;
import java.util.ArrayDeque;
import java.util.Deque;

public class CPU {
    private static final int OP_HLT = 0; // Stops machine
    private static final int OP_TRAP = 030;

    // Load/Store
    private static final int OP_LDR = 1;
    private static final int OP_STR = 2;
    private static final int OP_LDA = 3;
    private static final int OP_LDX = 041;
    private static final int OP_STX = 042;

    // Arithmetic
    private static final int OP_AMR = 04;
    private static final int OP_SMR = 05;
    private static final int OP_AIR = 06;
    private static final int OP_SIR = 07;

    // Transfer
    private static final int OP_JZ = 010;
    private static final int OP_JNE = 011;
    private static final int OP_JCC = 012;
    private static final int OP_JMA = 013;
    private static final int OP_JSR = 014;
    private static final int OP_RFS = 015;
    private static final int OP_SOB = 016;
    private static final int OP_JGE = 017;

    // Register ops
    private static final int OP_MLT = 070;
    private static final int OP_DVD = 071;
    private static final int OP_TRR = 072;
    private static final int OP_AND = 073;
    private static final int OP_ORR = 074;
    private static final int OP_NOT = 075;

    // Shift/Rotate
    private static final int OP_SRC = 031;
    private static final int OP_RRC = 032;
    // I/O
    private static final int OP_IN = 061;
    private static final int OP_OUT = 062;
    private static final int OP_CHK = 063;

    private static final int MFR_ILLEGAL_OPCODE = 0b0100;
    private static final int DEV_KEYBOARD = 0;
    private static final int DEV_PRINTER = 1;

    private final Registers regs = new Registers();
    private final Memory memory;
    private final Cache cache;
    private final Deque<Short> keyboardQueue = new ArrayDeque<>();
    private final StringBuilder printerOutput = new StringBuilder();
    private boolean halted = false;

    public CPU(Memory memory) {
        this.memory = memory;
        this.cache = new Cache(memory);
    }

    public Registers R() {
        return regs;
    }

    public boolean isHalted() {
        return halted;
    }

    public void halt() {
        halted = true;
    }

    private void triggerMachineFault(int faultCode, String message) {
        regs.setMFR(faultCode);
        halted = true;
        System.out.println("Machine fault: " + message);
    }

    public void reset() {
        regs.reset();
        cache.reset();
        keyboardQueue.clear();
        printerOutput.setLength(0);
        halted = false;
    }

    private boolean isValidOpcode(int opcode) {
        return switch(opcode) {
            case OP_HLT, OP_LDR, OP_STR, OP_LDA, OP_LDX, OP_STX,
                 OP_AMR, OP_SMR, OP_AIR, OP_SIR,
                 OP_JZ, OP_JNE, OP_JCC, OP_JMA, OP_JSR, OP_RFS, OP_SOB, OP_JGE,
                 OP_MLT, OP_DVD, OP_TRR, OP_AND, OP_ORR, OP_NOT,
                 OP_SRC, OP_RRC,
                 OP_IN, OP_OUT, OP_CHK,
                 OP_TRAP -> true;
            default -> false;
        };
    }

    /** Fetch next instruction: MAR <- PC; MBR <- mem[MAR]; IR <- MBR; PC++ */
    public void fetch() throws MemoryFault {
        regs.setMAR(regs.getPC());
        regs.setMBR(cache.read(regs.getMAR()));
        // move into IR
        regs.setIR(regs.getMBR());
        // increment PC (12-bit)
        regs.setPC(regs.getPC() + 1);
    }

    public short readWord(int addr) throws MemoryFault {
        regs.setMAR(addr);
        short val = cache.read(regs.getMAR());
        regs.setMBR(val);
        return val;
    }

    public void writeWord(int addr, short value) throws MemoryFault {
        regs.setMAR(addr);
        regs.setMBR(value);
        cache.write(regs.getMAR(), regs.getMBR());
    }

    /** Effective address for load/store format. */
    public int computeEA(int ix, int iBit, int addr5) throws MemoryFault {
        int ea = addr5 & 0x1F;
        if (ix >= 1 && ix <= 3) {
            ea = (ea + (regs.getX(ix) & 0xFFFF)) & 0xFFF;
        }
        if (iBit == 1) {
            ea = readWord(ea) & 0xFFF;
        }
        return ea;
    }

    /**
     * Execute one instruction for Part I subset.
     * Flow: fetch -> decode -> execute (HLT/LDR/STR/LDA/LDX/STX)
     */
    public String executeLoadStoreStep() throws MemoryFault {
        if (halted) {
            return "HALTED";
        }

        try {

            fetch();
            int ir = regs.getIR() & 0xFFFF;
            int opcode = (ir >>> 10) & 0x3F;
            int r = (ir >>> 8) & 0x3;
            int ix = (ir >>> 6) & 0x3;
            int iBit = (ir >>> 5) & 0x1;
            int addr5 = ir & 0x1F;

            if (!isValidOpcode(opcode)) {
                triggerMachineFault(MFR_ILLEGAL_OPCODE, "Invalid opcode: " + opcode);
                return "FAULT";
            }

            return switch (opcode) {

                case OP_HLT -> {
                    halted = true;
                    yield "HLT";
                }

                case OP_TRAP -> {
                    int trapCode = addr5;

                    if (trapCode < 0 || trapCode > 15) {
                        triggerMachineFault(MFR_ILLEGAL_OPCODE, "Invalid TRAP code: " + trapCode);
                        yield "FAULT";
                    }

                    System.out.println("TRAP executed with code: " + trapCode); // No interrupts
                    yield "TRAP";
                }

                // =====================
                // LOAD/STORE (USE CACHE)
                // =====================
                case OP_LDR -> {
                    int ea = computeEA(ix, iBit, addr5);
                    short value = cache.read(ea);
                    regs.setR(r, value);
                    yield "LDR";
                }

                case OP_STR -> {
                    int ea = computeEA(ix, iBit, addr5);
                    cache.write(ea, regs.getR(r));
                    yield "STR";
                }

                case OP_LDA -> {
                    int ea = computeEA(ix, iBit, addr5);
                    regs.setR(r, (short) ea);
                    yield "LDA";
                }

                case OP_LDX -> {
                    if (ix == 0) {
                        triggerMachineFault(MFR_ILLEGAL_OPCODE, "Invalid LDX IX=0");
                        yield "FAULT";
                    }
                    int ea = computeEAForXOp(iBit, addr5);
                    short value = cache.read(ea);
                    regs.setX(ix, value);
                    yield "LDX";
                }
                case OP_STX -> {
                    if (ix == 0) {
                        triggerMachineFault(MFR_ILLEGAL_OPCODE, "Invalid STX IX=0");
                        yield "FAULT";
                    }
                    int ea = computeEAForXOp(iBit, addr5);
                    short value = regs.getX(ix);
                    writeWord(ea, value);
                    yield "STX";
                }

                // =====================
                // ARITHMETIC
                // =====================
                case OP_AMR -> {
                    int ea = computeEA(ix, iBit, addr5);
                    short res = (short) (regs.getR(r) + cache.read(ea));
                    regs.setR(r, res);
                    yield "AMR";
                }

                case OP_SMR -> {
                    int ea = computeEA(ix, iBit, addr5);
                    short res = (short) (regs.getR(r) - cache.read(ea));
                    regs.setR(r, res);
                    yield "SMR";
                }

                case OP_AIR -> {
                    regs.setR(r, (short) (regs.getR(r) + addr5));
                    yield "AIR";
                }

                case OP_SIR -> {
                    regs.setR(r, (short) (regs.getR(r) - addr5));
                    yield "SIR";
                }

                // =====================
                // TRANSFER
                // =====================
                case OP_JZ -> {
                    if (regs.getR(r) == 0) {
                        regs.setPC(computeEA(ix, iBit, addr5));
                    }
                    yield "JZ";
                }

                case OP_JNE -> {
                    if (regs.getR(r) != 0) {
                        regs.setPC(computeEA(ix, iBit, addr5));
                    }
                    yield "JNE";
                }

                case OP_JCC -> {
                    int cc = r; // cc replaces r
                    if ((regs.getCC() & (1 << cc)) != 0) {
                        regs.setPC(computeEA(ix, iBit, addr5));
                    }
                    yield "JCC";
                }

                case OP_JMA -> {
                    regs.setPC(computeEA(ix, iBit, addr5));
                    yield "JMA";
                }

                case OP_JSR -> {
                    regs.setR(3, (short) regs.getPC()); // save return
                    regs.setPC(computeEA(ix, iBit, addr5));
                    yield "JSR";
                }

                case OP_RFS -> {
                    regs.setR(0, (short) addr5); // return code
                    regs.setPC(regs.getR(3));
                    yield "RFS";
                }

                case OP_SOB -> {
                    short val = (short) (regs.getR(r) - 1);
                    regs.setR(r, val);
                    if (val > 0) {
                        regs.setPC(computeEA(ix, iBit, addr5));
                    }
                    yield "SOB";
                }

                case OP_JGE -> {
                    if (regs.getR(r) >= 0) {
                        regs.setPC(computeEA(ix, iBit, addr5));
                    }
                    yield "JGE";
                }

                // =====================
                // REGISTER OPS
                // =====================
                case OP_MLT -> {
                    int rx = r;
                    int ry = ix;

                    long result = (long) regs.getR(rx) * (long) regs.getR(ry);

                    regs.setR(rx, (short) ((result >> 16) & 0xFFFF));
                    regs.setR(rx + 1, (short) (result & 0xFFFF));

                    yield "MLT";
                }

                case OP_DVD -> {
                    int rx = r;
                    int ry = ix;

                    short divisor = regs.getR(ry);

                    if (divisor == 0) {
                        regs.setCC(regs.getCC() | (1 << 3)); // DIVZERO
                        yield "DVD DIVZERO";
                    }

                    short quotient = (short) (regs.getR(rx) / divisor);
                    short remainder = (short) (regs.getR(rx) % divisor);

                    regs.setR(rx, quotient);
                    regs.setR(rx + 1, remainder);

                    yield "DVD";
                }

                case OP_TRR -> {
                    int rx = r;
                    int ry = ix;

                    if (regs.getR(rx) == regs.getR(ry)) {
                        regs.setCC(regs.getCC() | (1 << 2));
                    }
                    yield "TRR";
                }

                case OP_AND -> {
                    int rx = r;
                    int ry = ix;
                    regs.setR(rx, (short) (regs.getR(rx) & regs.getR(ry)));
                    yield "AND";
                }

                case OP_ORR -> {
                    int rx = r;
                    int ry = ix;
                    regs.setR(rx, (short) (regs.getR(rx) | regs.getR(ry)));
                    yield "ORR";
                }

                case OP_NOT -> {
                    regs.setR(r, (short) (~regs.getR(r)));
                    yield "NOT";
                }

                case OP_SRC -> {
                    int count = ir & 0xF;
                    int lr = (ir >> 4) & 0x1; // 1 = left, 0 = right
                    int al = (ir >> 5) & 0x1; // 1 = logical, 0 = arithmetic

                    if (count == 0)
                        yield "SRC (no-op)";

                    int val = regs.getR(r) & 0xFFFF;

                    for (int i = 0; i < count; i++) {
                        if (lr == 1) {
                            // LEFT SHIFT (same for logical & arithmetic)
                            val = (val << 1) & 0xFFFF;
                        } else {
                            // RIGHT SHIFT
                            if (al == 1) {
                                // LOGICAL → fill with 0
                                val = (val >>> 1);
                            } else {
                                // ARITHMETIC → preserve sign
                                val = (val >> 1);
                            }
                        }
                    }

                    regs.setR(r, (short) val);
                    yield "SRC";
                }

                case OP_RRC -> {
                    int count = ir & 0xF;
                    int lr = (ir >> 4) & 0x1; // 1 = left, 0 = right

                    if (count == 0)
                        yield "RRC (no-op)";

                    int val = regs.getR(r) & 0xFFFF;

                    for (int i = 0; i < count; i++) {
                        if (lr == 1) {
                            // ROTATE LEFT
                            val = ((val << 1) | (val >>> 15)) & 0xFFFF;
                        } else {
                            // ROTATE RIGHT
                            val = ((val >>> 1) | (val << 15)) & 0xFFFF;
                        }
                    }

                    regs.setR(r, (short) val);
                    yield "RRC";
                }
                case OP_IN -> {
                    regs.setR(r, readDevice(addr5));
                    yield "IN";
                }
                case OP_OUT -> {
                    writeDevice(addr5, regs.getR(r));
                    yield "OUT";
                }
                case OP_CHK -> {
                    regs.setR(r, checkDevice(addr5));
                    yield "CHK";
                }

                default -> {
                    triggerMachineFault(MFR_ILLEGAL_OPCODE, "Illegal opcode: " + opcode);
                    yield "Illegal opcode";
                }
            };


        } 
        catch (MemoryFault mf) {
            triggerMachineFault(MFR_ILLEGAL_OPCODE, "Memory fault: " + mf.getMessage());
            return "FAULT";
        }

    }

    // LDX/STX use X field as destination/source register id, not as EA index.
    private int computeEAForXOp(int iBit, int addr5) throws MemoryFault {
        int ea = addr5 & 0x1F;
        if (iBit == 1) {
            ea = readWord(ea) & 0xFFF;
        }
        return ea;
    }

    public void pushKeyboardInput(short value) {
        keyboardQueue.addLast(value);
    }

    public int keyboardQueueDepth() {
        return keyboardQueue.size();
    }

    public Short peekKeyboardInput() {
        return keyboardQueue.peekFirst();
    }

    public String drainPrinterOutput() {
        String out = printerOutput.toString();
        printerOutput.setLength(0);
        return out;
    }

    public int getCacheHitCount() {
        return cache.getHitCount();
    }

    public int getCacheMissCount() {
        return cache.getMissCount();
    }

    public CacheLine[] getCacheSnapshot() {
        return cache.snapshot();
    }

    private short readDevice(int devid) {
        if (devid == DEV_KEYBOARD) {
            Short value = keyboardQueue.pollFirst();
            return value == null ? 0 : value;
        }
        return 0;
    }

    private short checkDevice(int devid) {
        if (devid == DEV_KEYBOARD) {
            return (short) (keyboardQueue.isEmpty() ? 0 : 1);
        }
        if (devid == DEV_PRINTER) {
            return 1;
        }
        return 0;
    }

    private void writeDevice(int devid, short value) {
        if (devid == DEV_PRINTER) {
            printerOutput.append(value).append('\n');
        }
    }
}
