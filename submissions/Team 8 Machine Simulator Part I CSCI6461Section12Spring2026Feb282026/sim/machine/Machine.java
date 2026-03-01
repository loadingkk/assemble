package cisc.sim.machine;

import cisc.sim.memory.Memory;

public class Machine {
    private final Memory memory = new Memory();
    private final CPU cpu = new CPU(memory);

    public Memory memory() { return memory; }
    public CPU cpu() { return cpu; }

    public void resetAll() {
        memory.resetToZero();
        cpu.reset();
    }
}