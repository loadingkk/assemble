package cisc.sim;

import cisc.sim.machine.CPU;
import cisc.sim.machine.Machine;
import cisc.sim.machine.Registers;
import cisc.sim.memory.Memory;
import cisc.sim.memory.MemoryFault;

public class SimMain {

    public static void main(String[] args) {
        try {
            System.out.println("=== CSCI 6461 Part 1 Test ===");

            Machine machine = new Machine();
            machine.resetAll();

            CPU cpu = machine.cpu();
            Memory mem = machine.memory();
            Registers r = cpu.R();

            // Reset checks
            assertEquals(0, mem.peek(0), "mem[0] after reset");
            assertEquals(0, mem.peek(10), "mem[10] after reset");
            assertEquals(0, mem.peek(2047), "mem[2047] after reset");

            // --- EA checks ---
            r.setX(1, (short) 5);
            mem.poke(17, (short) 23); // used by indirect lookup

            assertEquals(12, cpu.computeEA(0, 0, 12), "EA direct");
            assertEquals(17, cpu.computeEA(1, 0, 12), "EA indexed");
            assertEquals(23, cpu.computeEA(1, 1, 12), "EA indexed+indirect");

            // --- Program for LDR/STR/LDA/LDX/STX + HLT ---
            // base=0:
            // 0 LDA R0,0,20
            // 1 STR R0,0,21
            // 2 LDR R1,0,21
            // 3 LDX X1,22
            // 4 STX X1,23
            // 5 HLT
            mem.poke(0, encodeLS(03, 0, 0, 0, 020));
            mem.poke(1, encodeLS(02, 0, 0, 0, 021));
            mem.poke(2, encodeLS(01, 1, 0, 0, 021));
            mem.poke(3, encodeLS(041, 0, 1, 0, 022));
            mem.poke(4, encodeLS(042, 0, 1, 0, 023));
            mem.poke(5, (short) 0);

            mem.poke(022, (short) 000077);
            r.setX(1, (short) 0);
            r.setPC(0);

            dumpState("Before execution", r);
            System.out.println(cpu.executeLoadStoreStep()); // LDA
            System.out.println(cpu.executeLoadStoreStep()); // STR
            System.out.println(cpu.executeLoadStoreStep()); // LDR
            System.out.println(cpu.executeLoadStoreStep()); // LDX
            System.out.println(cpu.executeLoadStoreStep()); // STX
            System.out.println(cpu.executeLoadStoreStep()); // HLT
            dumpState("After execution", r);

            assertEquals(020, r.getR(0) & 0xFFFF, "R0 after LDA");
            assertEquals(020, mem.peek(021) & 0xFFFF, "mem[021] after STR");
            assertEquals(020, r.getR(1) & 0xFFFF, "R1 after LDR");
            assertEquals(000077, r.getX(1) & 0xFFFF, "X1 after LDX");
            assertEquals(000077, mem.peek(023) & 0xFFFF, "mem[023] after STX");
            if (!cpu.isHalted()) {
                throw new RuntimeException("CPU should be halted after HLT");
            }

            // --- Illegal opcode sets MFR and halts ---
            machine.resetAll();
            mem = machine.memory();
            cpu = machine.cpu();
            r = cpu.R();
            mem.poke(0, encodeLS(077, 0, 0, 0, 0)); // unsupported opcode
            r.setPC(0);
            System.out.println(cpu.executeLoadStoreStep());
            assertEquals(0b0100, r.getMFR(), "MFR for illegal opcode");
            if (!cpu.isHalted()) {
                throw new RuntimeException("CPU should halt on illegal opcode");
            }

            System.out.println("\nPart 1 test passed.");

        } catch (MemoryFault mf) {
            System.err.println("\n MemoryFault: " + mf.getMessage());
            mf.printStackTrace();
        } catch (RuntimeException re) {
            System.err.println("\n Test failed: " + re.getMessage());
            re.printStackTrace();
        } catch (Exception e) {
            System.err.println("\n Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void dumpState(String label, Registers r) {
        System.out.println("\n--- " + label + " ---");
        System.out.printf("PC=%04o  MAR=%04o  IR=%04o  MBR=%04o  CC=%01o  MFR=%01o%n",
                r.getPC(),
                r.getMAR(),
                (r.getIR() & 0xFFFF),
                (r.getMBR() & 0xFFFF),
                r.getCC(),
                r.getMFR()
        );
    }

    private static void assertEquals(int expected, int actual, String msg) {
        if (expected != actual) {
            throw new RuntimeException(msg + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertEquals(short expected, short actual, String msg) {
        if (expected != actual) {
            throw new RuntimeException(msg + " expected=" + toHex(expected) + " actual=" + toHex(actual));
        }
    }

    private static void assertEquals(int expected, short actual, String msg) {
        int act = actual & 0xFFFF;
        if (expected != act) {
            throw new RuntimeException(msg + " expected=" + expected + " actual=" + act);
        }
    }

    private static short encodeLS(int opcodeOct, int r, int ix, int iBit, int addrOct) {
        int word = ((opcodeOct & 0x3F) << 10)
                | ((r & 0x3) << 8)
                | ((ix & 0x3) << 6)
                | ((iBit & 0x1) << 5)
                | (addrOct & 0x1F);
        return (short) (word & 0xFFFF);
    }

    private static String toHex(short v) {
        return String.format("0x%04X", v & 0xFFFF);
    }
}
