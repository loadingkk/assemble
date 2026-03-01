package cisc.sim.machine;

import cisc.sim.memory.Memory;
import cisc.sim.memory.MemoryFault;

public class CPU {
    private static final int OP_HLT = 0;
    private static final int OP_LDR = 1;
    private static final int OP_STR = 2;
    private static final int OP_LDA = 3;
    private static final int OP_LDX = 041;
    private static final int OP_STX = 042;

    private static final int MFR_ILLEGAL_OPCODE = 0b0100;

    private final Registers regs = new Registers();
    private final Memory memory;
    private boolean halted = false;

    public CPU(Memory memory) {
        this.memory = memory;
    }

    public Registers R() { return regs; }
    public boolean isHalted() { return halted; }
    public void halt() { halted = true; }

    public void reset() {
        regs.reset();
        halted = false;
    }

    /** Fetch next instruction: MAR <- PC; MBR <- mem[MAR]; IR <- MBR; PC++ */
    public void fetch() throws MemoryFault {
        regs.setMAR(regs.getPC());
        // memory cycle 1: latch address, read mode
        memory.cycle1_setAddress(regs.getMAR(), false);
        // memory cycle 2: read to MBR
        regs.setMBR(memory.cycle2_read());
        // move into IR
        regs.setIR(regs.getMBR());
        // increment PC (12-bit)
        regs.setPC(regs.getPC() + 1);
    }

    // Helpers your teammate will use in execute stage:
    public short readWord(int addr) throws MemoryFault {
        regs.setMAR(addr);
        memory.cycle1_setAddress(regs.getMAR(), false);
        short val = memory.cycle2_read();
        regs.setMBR(val);
        return val;
    }

    public void writeWord(int addr, short value) throws MemoryFault {
        regs.setMAR(addr);
        regs.setMBR(value);
        memory.cycle1_setAddress(regs.getMAR(), true);
        memory.cycle2_write(regs.getMBR());
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

        fetch();
        int ir = regs.getIR() & 0xFFFF;
        int opcode = (ir >>> 10) & 0x3F;
        int r = (ir >>> 8) & 0x3;
        int ix = (ir >>> 6) & 0x3;
        int iBit = (ir >>> 5) & 0x1;
        int addr5 = ir & 0x1F;

        return switch (opcode) {
            case OP_HLT -> {
                halted = true;
                yield "HLT";
            }
            case OP_LDR -> {
                int ea = computeEA(ix, iBit, addr5);
                short value = readWord(ea);
                regs.setR(r, value);
                yield String.format("LDR R%d <- M[%04o] (%06o)", r, ea, value & 0xFFFF);
            }
            case OP_STR -> {
                int ea = computeEA(ix, iBit, addr5);
                short value = regs.getR(r);
                writeWord(ea, value);
                yield String.format("STR M[%04o] <- R%d (%06o)", ea, r, value & 0xFFFF);
            }
            case OP_LDA -> {
                int ea = computeEA(ix, iBit, addr5);
                regs.setR(r, (short) (ea & 0xFFFF));
                yield String.format("LDA R%d <- EA(%04o)", r, ea);
            }
            case OP_LDX -> {
                if (ix == 0) {
                    regs.setMFR(MFR_ILLEGAL_OPCODE);
                    halted = true;
                    yield "LDX invalid IX=0";
                }
                int ea = computeEAForXOp(iBit, addr5);
                short value = readWord(ea);
                regs.setX(ix, value);
                yield String.format("LDX X%d <- M[%04o] (%06o)", ix, ea, value & 0xFFFF);
            }
            case OP_STX -> {
                if (ix == 0) {
                    regs.setMFR(MFR_ILLEGAL_OPCODE);
                    halted = true;
                    yield "STX invalid IX=0";
                }
                int ea = computeEAForXOp(iBit, addr5);
                short value = regs.getX(ix);
                writeWord(ea, value);
                yield String.format("STX M[%04o] <- X%d (%06o)", ea, ix, value & 0xFFFF);
            }
            default -> {
                regs.setMFR(MFR_ILLEGAL_OPCODE);
                halted = true;
                yield String.format("Illegal opcode %02o", opcode);
            }
        };
    }

    // LDX/STX use X field as destination/source register id, not as EA index.
    private int computeEAForXOp(int iBit, int addr5) throws MemoryFault {
        int ea = addr5 & 0x1F;
        if (iBit == 1) {
            ea = readWord(ea) & 0xFFF;
        }
        return ea;
    }
}
