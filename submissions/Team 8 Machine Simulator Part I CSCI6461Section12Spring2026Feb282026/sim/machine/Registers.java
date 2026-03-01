package cisc.sim.machine;


public class Registers {
    // 16-bit registers
    private short[] gpr = new short[4];     // R0..R3
    private short[] ixr = new short[4];     // use ixr[1..3], ixr[0] unused

    private short ir;   // 16
    private short mbr;  // 16

    // “smaller” registers stored in int for easy masking
    private int pc;     // 12
    private int mar;    // 12
    private int cc;     // 4
    private int mfr;    // 4

    public void reset() {
        for (int i=0;i<4;i++) gpr[i]=0;
        for (int i=0;i<4;i++) ixr[i]=0;
        ir = 0; mbr = 0;
        pc = 0; mar = 0; cc = 0; mfr = 0;
    }

    // --- GPR ---
    public short getR(int r) { return gpr[r]; }
    public void setR(int r, short v) { gpr[r] = v; }

    // --- IXR (1..3) ---
    public short getX(int x) { return ixr[x]; }
    public void setX(int x, short v) { ixr[x] = v; }

    // --- PC (12-bit) ---
    public int getPC() { return pc; }
    public void setPC(int v) { pc = v & 0xFFF; }

    // --- MAR (12-bit) ---
    public int getMAR() { return mar; }
    public void setMAR(int v) { mar = v & 0xFFF; }

    // --- IR/MBR ---
    public short getIR() { return ir; }
    public void setIR(short v) { ir = v; }
    public short getMBR() { return mbr; }
    public void setMBR(short v) { mbr = v; }

    // --- CC/MFR ---
    public int getCC() { return cc; }
    public void setCC(int v) { cc = v & 0xF; }

    public int getMFR() { return mfr; }
    public void setMFR(int v) { mfr = v & 0xF; }
}