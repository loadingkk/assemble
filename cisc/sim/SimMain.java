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

            // Machine wrapper:
            Machine machine = new Machine();
            machine.resetAll();

            CPU cpu = machine.cpu();
            Memory mem = machine.memory();
            Registers r = cpu.R();

            // Confirm memory reset (spot check a few addresses)
            assertEquals(0, mem.peek(0), "mem[0] after reset");
            assertEquals(0, mem.peek(10), "mem[10] after reset");
            assertEquals(0, mem.peek(2047), "mem[2047] after reset");

            // Load a couple of fake 16-bit words as "instructions"
            // (These don't need to be valid ISA yet; fetch just moves words into IR.)
            mem.poke(0, (short) 0x1234);
            mem.poke(1, (short) 0xBEEF);

            // Set PC to 0 and fetch twice
            r.setPC(0);
            dumpState("Before fetch #1", r);

            cpu.fetch();
            dumpState("After fetch #1", r);
            assertEquals(1, r.getPC(), "PC after fetch #1");
            assertEquals((short) 0x1234, r.getIR(), "IR after fetch #1");

            cpu.fetch();
            dumpState("After fetch #2", r);
            assertEquals(2, r.getPC(), "PC after fetch #2");
            assertEquals((short) 0xBEEF, r.getIR(), "IR after fetch #2");

            System.out.println("\n Test passed.");

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

    private static String toHex(short v) {
        return String.format("0x%04X", v & 0xFFFF);
    }
}
