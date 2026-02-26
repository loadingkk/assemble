package cisc.sim.memory;

public class Memory {
    public static final int SIZE = 2048; // words
    private final short[] mem = new short[SIZE];

    private int latchedAddr = 0;
    private Pending pending = Pending.NONE;

    private enum Pending { NONE, READ, WRITE }

    public void resetToZero() {
        for (int i = 0; i < SIZE; i++) mem[i] = 0;
        latchedAddr = 0;
        pending = Pending.NONE;
    }

    /** Cycle 1: latch address from MAR and declare next operation */
    public void cycle1_setAddress(int mar12, boolean isWrite) throws MemoryFault {
        int addr = mar12 & 0xFFF;
        if (addr < 0 || addr >= SIZE) throw new MemoryFault("Address out of range: " + addr);
        latchedAddr = addr;
        pending = isWrite ? Pending.WRITE : Pending.READ;
    }

    /** Cycle 2 for READ: returns memory[latchedAddr] to load into MBR */
    public short cycle2_read() throws MemoryFault {
        if (pending != Pending.READ) throw new MemoryFault("No pending READ");
        pending = Pending.NONE;
        return mem[latchedAddr];
    }

    /** Cycle 2 for WRITE: writes MBR value into memory[latchedAddr] */
    public void cycle2_write(short mbr) throws MemoryFault {
        if (pending != Pending.WRITE) throw new MemoryFault("No pending WRITE");
        mem[latchedAddr] = mbr;
        pending = Pending.NONE;
    }

    /** Debugging / UI memory view (NOT part of timing model) */
    public short peek(int address) throws MemoryFault {
        if (address < 0 || address >= SIZE) throw new MemoryFault("Address out of range: " + address);
        return mem[address];
    }

    /** Debugging / loader convenience (bypasses timing model) */
    public void poke(int address, short value) throws MemoryFault {
        if (address < 0 || address >= SIZE) throw new MemoryFault("Address out of range: " + address);
        mem[address] = value;
    }
}