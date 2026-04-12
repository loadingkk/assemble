package cisc.sim;

import cisc.sim.machine.CPU;
import cisc.sim.machine.Machine;
import cisc.sim.machine.Registers;
import cisc.sim.memory.Memory;
import cisc.sim.memory.MemoryFault;
import java.util.List;

public class SimMain {

    public static void main(String[] args) {
        try {
            System.out.println("=== CSCI 6461 Part 1/2/3 Test ===");

            Machine machine = new Machine();
            machine.resetAll();

            CPU cpu = machine.cpu();
            Memory mem = machine.memory();
            Registers r = cpu.R();

            // Reset checks
            assertEquals(020, mem.peek(0) & 0xFFFF, "mem[0] trap table base after reset");
            assertEquals(0006, mem.peek(1) & 0xFFFF, "mem[1] fault handler after reset");
            assertEquals(0, mem.peek(10), "mem[10] after reset");
            assertEquals(0, mem.peek(2047), "mem[2047] after reset");

            // --- EA checks ---
            r.setX(1, (short) 5);
            mem.poke(17, (short) 23); // used by indirect lookup

            assertEquals(12, cpu.computeEA(0, 0, 12), "EA direct");
            assertEquals(17, cpu.computeEA(1, 0, 12), "EA indexed");
            assertEquals(23, cpu.computeEA(1, 1, 12), "EA indexed+indirect");

            // --- Program for LDR/STR/LDA/LDX/STX + HLT ---
            // base=0010 (avoid reserved addresses 0..5)
            int base = 010;
            mem.poke(base, encodeLS(03, 0, 0, 0, 020));
            mem.poke(base + 1, encodeLS(02, 0, 0, 0, 021));
            mem.poke(base + 2, encodeLS(01, 1, 0, 0, 021));
            mem.poke(base + 3, encodeLS(041, 0, 1, 0, 022));
            mem.poke(base + 4, encodeLS(042, 0, 1, 0, 023));
            mem.poke(base + 5, (short) 0);

            mem.poke(022, (short) 000077);
            r.setX(1, (short) 0);
            r.setPC(base);

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

            // --- Illegal opcode sets MFR, saves PC, and vectors to fault handler ---
            machine.resetAll();
            mem = machine.memory();
            cpu = machine.cpu();
            r = cpu.R();
            mem.poke(base, encodeLS(077, 0, 0, 0, 0)); // unsupported opcode
            r.setPC(base);
            System.out.println(cpu.executeLoadStoreStep());
            assertEquals(0b0100, r.getMFR(), "MFR for illegal opcode");
            assertEquals(0006, r.getPC(), "PC vectored to fault handler");
            assertEquals(0011, mem.peek(4) & 0xFFFF, "fault saved PC at reserved location 4");
            System.out.println(cpu.executeLoadStoreStep()); // HLT at fault handler
            if (!cpu.isHalted()) {
                throw new RuntimeException("CPU should halt after executing the fault handler HLT");
            }

            // --- TRAP saves PC and vectors through trap table ---
            machine.resetAll();
            mem = machine.memory();
            cpu = machine.cpu();
            r = cpu.R();
            mem.poke(020, (short) 030);                  // trap code 0 -> handler at 0030
            mem.poke(030, (short) 0);                    // HLT handler
            mem.poke(base, encodeLS(030, 0, 0, 0, 0));   // TRAP 0
            r.setPC(base);
            System.out.println(cpu.executeLoadStoreStep());
            assertEquals(0011, mem.peek(2) & 0xFFFF, "trap saved PC at reserved location 2");
            assertEquals(0030, r.getPC(), "TRAP vectored to handler");
            System.out.println(cpu.executeLoadStoreStep());
            if (!cpu.isHalted()) {
                throw new RuntimeException("CPU should halt after trap handler HLT");
            }

            // --- Part II IO quick test (CHK/IN/OUT) ---
            machine.resetAll();
            mem = machine.memory();
            cpu = machine.cpu();
            r = cpu.R();
            cpu.pushKeyboardInput((short) 123);
            mem.poke(base, encodeLS(063, 1, 0, 0, 0)); // CHK R1, devid=0
            mem.poke(base + 1, encodeLS(061, 0, 0, 0, 0)); // IN R0, devid=0
            mem.poke(base + 2, encodeLS(062, 0, 0, 0, 1)); // OUT R0, devid=1
            mem.poke(base + 3, (short) 0); // HLT
            r.setPC(base);
            System.out.println(cpu.executeLoadStoreStep()); // CHK
            System.out.println(cpu.executeLoadStoreStep()); // IN
            System.out.println(cpu.executeLoadStoreStep()); // OUT
            System.out.println(cpu.executeLoadStoreStep()); // HLT
            assertEquals(1, r.getR(1) & 0xFFFF, "CHK keyboard ready status");
            assertEquals(123, r.getR(0) & 0xFFFF, "IN value loaded");
            String printerOut = cpu.drainPrinterOutput().trim();
            if (!"123".equals(printerOut)) {
                throw new RuntimeException("OUT printer value mismatch, got=" + printerOut);
            }

            // --- Part III card reader device quick test ---
            machine.resetAll();
            mem = machine.memory();
            cpu = machine.cpu();
            r = cpu.R();
            cpu.pushCardReaderText("AB", true);
            mem.poke(base, encodeLS(063, 1, 0, 0, 2)); // CHK R1, devid=2
            mem.poke(base + 1, encodeLS(061, 0, 0, 0, 2)); // IN R0, devid=2
            mem.poke(base + 2, encodeLS(061, 2, 0, 0, 2)); // IN R2, devid=2
            mem.poke(base + 3, (short) 0);
            r.setPC(base);
            System.out.println(cpu.executeLoadStoreStep());
            System.out.println(cpu.executeLoadStoreStep());
            System.out.println(cpu.executeLoadStoreStep());
            System.out.println(cpu.executeLoadStoreStep());
            assertEquals(1, r.getR(1) & 0xFFFF, "CHK card reader ready status");
            assertEquals('A', r.getR(0) & 0xFFFF, "First card reader character");
            assertEquals('B', r.getR(2) & 0xFFFF, "Second card reader character");

            // --- Part III Program 2 built-in trap service test ---
            machine.resetAll();
            mem = machine.memory();
            cpu = machine.cpu();
            r = cpu.R();
            cpu.pushCardReaderText(
                "Blue birds fly high today.\n"
                    + "Tall trees hold quiet nests.\n"
                    + "Soft rain falls over bright fields.\n"
                    + "Warm winds move across the dunes.\n"
                    + "Stars glow over the lake tonight.\n"
                    + "Teams build the final demo together.\n",
                true
            );
            cpu.pushKeyboardText("STARS", true);
            mem.poke(base, encodeLS(030, 0, 0, 0, 4));      // TRAP 4 = load paragraph
            mem.poke(base + 1, encodeLS(030, 0, 0, 0, 5));  // TRAP 5 = read query
            mem.poke(base + 2, encodeLS(030, 0, 0, 0, 6));  // TRAP 6 = search
            mem.poke(base + 3, (short) 0);
            r.setPC(base);
            while (!cpu.isHalted()) {
                System.out.println(cpu.executeLoadStoreStep());
            }
            String program2Printer = printerValuesToText(cpu.drainPrinterValues());
            if (!program2Printer.contains("STARS 5 1")) {
                throw new RuntimeException("Program 2 trap flow output mismatch, got=" + program2Printer);
            }

            // --- Part II cache quick test (repeat read should hit) ---
            machine.resetAll();
            mem = machine.memory();
            cpu = machine.cpu();
            r = cpu.R();
            mem.poke(020, (short) 000777);
            mem.poke(base, encodeLS(01, 0, 0, 0, 020)); // LDR R0,0,20
            mem.poke(base + 1, encodeLS(01, 1, 0, 0, 020)); // LDR R1,0,20
            mem.poke(base + 2, (short) 0);                  // HLT
            r.setPC(base);
            System.out.println(cpu.executeLoadStoreStep());
            int hitsAfterFirstRead = cpu.getCacheHitCount();
            System.out.println(cpu.executeLoadStoreStep());
            int hitsAfterSecondRead = cpu.getCacheHitCount();
            if (hitsAfterSecondRead <= hitsAfterFirstRead) {
                throw new RuntimeException("Expected cache hit on repeated read.");
            }

            System.out.println("\nPart 1/2/3 simulator smoke tests passed.");

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

    private static String printerValuesToText(List<Short> values) {
        StringBuilder out = new StringBuilder();
        for (short value : values) {
            int normalized = value & 0xFFFF;
            if (normalized >= 32 && normalized <= 126 || normalized == '\n') {
                out.append((char) normalized);
            } else {
                out.append('<').append(normalized).append('>');
            }
        }
        return out.toString();
    }
}
