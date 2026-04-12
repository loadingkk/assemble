package cisc.sim.cache;

import cisc.sim.memory.Memory;
import cisc.sim.memory.MemoryFault;

public class Cache {

    public static final int CACHE_SIZE = 16;

    private static class Line {
        boolean valid;
        int tag;
        short data;
    }

    private final Line[] lines;
    private final Memory memory;
    private int fifoVictim = 0;
    private int hitCount = 0;
    private int missCount = 0;

    public Cache(Memory memory) {
        this.memory = memory;
        lines = new Line[CACHE_SIZE];
        for (int i = 0; i < CACHE_SIZE; i++) {
            lines[i] = new Line();
        }
    }

    public void reset() {
        for (Line line : lines) {
            line.valid = false;
            line.tag = 0;
            line.data = 0;
        }
        fifoVictim = 0;
        hitCount = 0;
        missCount = 0;
    }

    public short read(int addr) throws MemoryFault {
        int address = addr & 0xFFF;
        int hitIdx = findLine(address);
        if (hitIdx >= 0) {
            hitCount++;
            return lines[hitIdx].data;
        }

        missCount++;

        // Use your memory cycle model
        memory.cycle1_setAddress(address, false);
        short val = memory.cycle2_read();
        fillLine(address, val);

        return val;
    }

    public void write(int addr, short value) throws MemoryFault {
        int address = addr & 0xFFF;

        // write-through
        memory.cycle1_setAddress(address, true);
        memory.cycle2_write(value);

        int hitIdx = findLine(address);
        if (hitIdx >= 0) {
            hitCount++;
            lines[hitIdx].data = value;
            return;
        }
        missCount++;
        fillLine(address, value);
    }

    public int getHitCount() {
        return hitCount;
    }

    public int getMissCount() {
        return missCount;
    }

    public CacheLine[] snapshot() {
        CacheLine[] out = new CacheLine[CACHE_SIZE];
        for (int i = 0; i < CACHE_SIZE; i++) {
            Line line = lines[i];
            out[i] = new CacheLine(line.valid, line.tag, line.data);
        }
        return out;
    }

    private int findLine(int address) {
        for (int i = 0; i < CACHE_SIZE; i++) {
            Line line = lines[i];
            if (line.valid && line.tag == address) {
                return i;
            }
        }
        return -1;
    }

    private int firstInvalidLine() {
        for (int i = 0; i < CACHE_SIZE; i++) {
            if (!lines[i].valid) {
                return i;
            }
        }
        return -1;
    }

    private void fillLine(int address, short value) {
        int invalidIdx = firstInvalidLine();
        int target = (invalidIdx >= 0) ? invalidIdx : fifoVictim;
        Line line = lines[target];
        line.valid = true;
        line.tag = address;
        line.data = value;
        if (invalidIdx < 0) {
            fifoVictim = (fifoVictim + 1) % CACHE_SIZE;
        }
    }
}
