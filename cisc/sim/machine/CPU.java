package cisc.sim.machine;

import cisc.sim.memory.Memory;
import cisc.sim.memory.MemoryFault;

public class CPU {
    private final Registers regs = new Registers();
    private final Memory memory;

    public CPU(Memory memory) {
        this.memory = memory;
    }

    public Registers R() { return regs; }

    public void reset() {
        regs.reset();
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
}
