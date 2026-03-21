package cisc.sim.cache;

import cisc.sim.memory.Memory;
import cisc.sim.memory.MemoryFault;

public class Cache {

    private static final int CACHE_SIZE = 16;

    private static class Line {
        boolean valid;
        int tag;
        short data;
    }

    private final Line[] lines;
    private final Memory memory;

    public Cache(Memory memory) {
        this.memory = memory;
        lines = new Line[CACHE_SIZE];
        for (int i = 0; i < CACHE_SIZE; i++) {
            lines[i] = new Line();
        }
    }

    private int index(int addr) {
        return addr % CACHE_SIZE;
    }

    private int tag(int addr) {
        return addr / CACHE_SIZE;
    }

    public short read(int addr) throws MemoryFault {
        int i = index(addr);
        int t = tag(addr);

        Line line = lines[i];

        if (line.valid && line.tag == t) {
            System.out.println("Cache HIT @" + addr);
            return line.data;
        }

        System.out.println("Cache MISS @" + addr);

        // Use your memory cycle model
        memory.cycle1_setAddress(addr, false);
        short val = memory.cycle2_read();

        line.valid = true;
        line.tag = t;
        line.data = val;

        return val;
    }

    public void write(int addr, short value) throws MemoryFault {
        int i = index(addr);
        int t = tag(addr);

        // write-through
        memory.cycle1_setAddress(addr, true);
        memory.cycle2_write(value);

        Line line = lines[i];
        line.valid = true;
        line.tag = t;
        line.data = value;

        System.out.println("Cache WRITE @" + addr);
    }
}