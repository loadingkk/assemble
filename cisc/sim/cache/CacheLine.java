package cisc.sim.cache;

public class CacheLine {
    public boolean valid;
    public int tag;
    public int data;

    public CacheLine() {
        valid = false;
        tag = -1;
        data = 0;
    }
}