package cisc.sim.cache;

public class CacheLine {
    public final boolean valid;
    public final int tag;
    public final short data;

    public CacheLine(boolean valid, int tag, short data) {
        this.valid = valid;
        this.tag = tag;
        this.data = data;
    }
}
