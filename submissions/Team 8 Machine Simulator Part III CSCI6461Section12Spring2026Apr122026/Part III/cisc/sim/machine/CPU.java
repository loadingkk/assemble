package cisc.sim.machine;

import cisc.sim.memory.Memory;
import cisc.sim.memory.MemoryFault;
import cisc.sim.cache.Cache;
import cisc.sim.cache.CacheLine;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class CPU {
    private static final int OP_HLT = 0; // Stops machine
    private static final int OP_TRAP = 030;

    // Load/Store
    private static final int OP_LDR = 1;
    private static final int OP_STR = 2;
    private static final int OP_LDA = 3;
    private static final int OP_LDX = 041;
    private static final int OP_STX = 042;

    // Arithmetic
    private static final int OP_AMR = 04;
    private static final int OP_SMR = 05;
    private static final int OP_AIR = 06;
    private static final int OP_SIR = 07;

    // Transfer
    private static final int OP_JZ = 010;
    private static final int OP_JNE = 011;
    private static final int OP_JCC = 012;
    private static final int OP_JMA = 013;
    private static final int OP_JSR = 014;
    private static final int OP_RFS = 015;
    private static final int OP_SOB = 016;
    private static final int OP_JGE = 017;

    // Register ops
    private static final int OP_MLT = 070;
    private static final int OP_DVD = 071;
    private static final int OP_TRR = 072;
    private static final int OP_AND = 073;
    private static final int OP_ORR = 074;
    private static final int OP_NOT = 075;

    // Shift/Rotate
    private static final int OP_SRC = 031;
    private static final int OP_RRC = 032;
    // I/O
    private static final int OP_IN = 061;
    private static final int OP_OUT = 062;
    private static final int OP_CHK = 063;

    private static final int RESERVED_LIMIT = 6;
    private static final int DEFAULT_HANDLER_ADDR = 6;
    private static final int DEFAULT_TRAP_TABLE_BASE = 020;

    private static final int MFR_RESERVED_ADDRESS = 0b0001; // Illegal memory address to reserved locations
    private static final int MFR_ILLEGAL_TRAP = 0b0010; // Illegal TRAP code
    private static final int MFR_ILLEGAL_OPCODE = 0b0100; // Illegal operation code
    private static final int MFR_MEMORY_OVERFLOW = 0b1000; // Illegal memory address beyond memory installed

    private static final int CC_OVERFLOW = 0b0001;
    private static final int CC_UNDERFLOW = 0b0010;
    private static final int CC_DIVZERO = 0b0100;
    private static final int CC_EQUAL = 0b1000;

    private static final int DEV_KEYBOARD = 0;
    private static final int DEV_PRINTER = 1;
    private static final int DEV_CARD_READER = 2;

    private static final int TRAP_PROGRAM2_LOAD = 4;
    private static final int TRAP_PROGRAM2_QUERY = 5;
    private static final int TRAP_PROGRAM2_SEARCH = 6;
    private static final int PROGRAM2_QUERY_BASE = 0240;

    private static final class Program2Word {
        final String word;
        final int sentenceNumber;
        final int wordNumber;

        Program2Word(String word, int sentenceNumber, int wordNumber) {
            this.word = word;
            this.sentenceNumber = sentenceNumber;
            this.wordNumber = wordNumber;
        }
    }

    private static final class FaultHalt extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private final Registers regs = new Registers();
    private final Memory memory;
    private final Cache cache;
    private final Deque<Short> keyboardQueue = new ArrayDeque<>();
    private final Deque<Short> cardReaderQueue = new ArrayDeque<>();
    private final Deque<Short> printerQueue = new ArrayDeque<>();
    private final List<Program2Word> program2Words = new ArrayList<>();
    private String program2Paragraph = "";
    private String program2Query = "";
    private boolean halted = false;

    public CPU(Memory memory) {
        this.memory = memory;
        this.cache = new Cache(memory);
        initializeSystemMemory();
    }

    public Registers R() {
        return regs;
    }

    public boolean isHalted() {
        return halted;
    }

    public void halt() {
        halted = true;
    }

    private void initializeSystemMemory() {
        try {
            memory.poke(0, (short) DEFAULT_TRAP_TABLE_BASE);
            memory.poke(1, (short) DEFAULT_HANDLER_ADDR);
            memory.poke(2, (short) 0);
            memory.poke(4, (short) 0);
            memory.poke(DEFAULT_HANDLER_ADDR, (short) (OP_HLT << 10));
            for (int i = 0; i < 16; i++) {
                memory.poke(DEFAULT_TRAP_TABLE_BASE + i, (short) DEFAULT_HANDLER_ADDR);
            }
        } catch (MemoryFault mf) {
            System.out.println("Unexpected memory fault during CPU initialization: " + mf.getMessage());
        }
    }

    private boolean isReservedAddress(int addr) {
        return addr >= 0 && addr < RESERVED_LIMIT;
    }

    private void raiseMachineFault(int faultCode, String message) {
        triggerMachineFault(faultCode, message);
        throw new FaultHalt();
    }

    private void triggerMachineFault(int faultCode, String message) {
        regs.setMFR(faultCode);

        try {
            systemWriteWord(4, (short) regs.getPC());
            int newPC = systemReadWord(1) & 0xFFF;
            regs.setPC(newPC);
        } catch (MemoryFault mf) {
            halted = true;
        }

        System.out.println("Machine fault: " + message);
    }

    public void reset() {
        regs.reset();
        cache.reset();
        keyboardQueue.clear();
        cardReaderQueue.clear();
        printerQueue.clear();
        program2Words.clear();
        program2Paragraph = "";
        program2Query = "";
        halted = false;
        initializeSystemMemory();
    }

    private boolean isValidOpcode(int opcode) {
        return switch(opcode) {
            case OP_HLT, OP_LDR, OP_STR, OP_LDA, OP_LDX, OP_STX,
                 OP_AMR, OP_SMR, OP_AIR, OP_SIR,
                 OP_JZ, OP_JNE, OP_JCC, OP_JMA, OP_JSR, OP_RFS, OP_SOB, OP_JGE,
                 OP_MLT, OP_DVD, OP_TRR, OP_AND, OP_ORR, OP_NOT,
                 OP_SRC, OP_RRC,
                 OP_IN, OP_OUT, OP_CHK,
                 OP_TRAP -> true;
            default -> false;
        };
    }

    /** Fetch next instruction: MAR <- PC; MBR <- mem[MAR]; IR <- MBR; PC++ */
    public void fetch() throws MemoryFault {
        if (isReservedAddress(regs.getPC())) {
            raiseMachineFault(
                MFR_RESERVED_ADDRESS,
                "Illegal instruction fetch from reserved memory: " + regs.getPC()
            );
        }
        regs.setMAR(regs.getPC());
        regs.setMBR(cache.read(regs.getMAR()));
        regs.setIR(regs.getMBR());
        regs.setPC(regs.getPC() + 1);
    }

    public short readWord(int addr) throws MemoryFault {
        if (isReservedAddress(addr)) {
            raiseMachineFault(
                MFR_RESERVED_ADDRESS,
                "Illegal read from reserved memory address: " + addr
            );
        }
        return systemReadWord(addr);
    }

    public void writeWord(int addr, short value) throws MemoryFault {
        if (isReservedAddress(addr)) {
            raiseMachineFault(
                MFR_RESERVED_ADDRESS,
                "Illegal write to reserved memory address: " + addr
            );
        }
        systemWriteWord(addr, value);
    }

    private short systemReadWord(int addr) throws MemoryFault {
        regs.setMAR(addr);
        short val = cache.read(regs.getMAR());
        regs.setMBR(val);
        return val;
    }

    private void systemWriteWord(int addr, short value) throws MemoryFault {
        regs.setMAR(addr);
        regs.setMBR(value);
        cache.write(regs.getMAR(), regs.getMBR());
    }

    /** Effective address for load/store format. */
    public int computeEA(int ix, int iBit, int addr5) throws MemoryFault {
        int ea = addr5 & 0x1F;
        if (ix >= 1 && ix <= 3) {
            ea = (ea + (regs.getX(ix) & 0xFFFF)) & 0xFFF;
        }
        if (iBit == 1) {
            ea = readWord(ea) & 0xFFF;
        }
        return ea;
    }

    /**
     * Execute one instruction for Part I subset.
     * Flow: fetch -> decode -> execute (HLT/LDR/STR/LDA/LDX/STX)
     */
    public String executeLoadStoreStep() throws MemoryFault {
        if (halted) {
            return "HALTED";
        }

        try {
            fetch();
            int ir = regs.getIR() & 0xFFFF;
            int opcode = (ir >>> 10) & 0x3F;
            int r = (ir >>> 8) & 0x3;
            int ix = (ir >>> 6) & 0x3;
            int iBit = (ir >>> 5) & 0x1;
            int addr5 = ir & 0x1F;

            if (!isValidOpcode(opcode)) {
                triggerMachineFault(MFR_ILLEGAL_OPCODE, "Invalid opcode: " + opcode);
                return "FAULT";
            }

            return switch (opcode) {

                case OP_HLT -> {
                    halted = true;
                    yield "HLT";
                }

                case OP_TRAP -> {
                    if (!executeBuiltInTrap(addr5)) {
                        executeTrap(addr5);
                    }
                    yield "TRAP";
                }

                // =====================
                // LOAD/STORE (USE CACHE)
                // =====================
                case OP_LDR -> {
                    int ea = computeEA(ix, iBit, addr5);
                    short value = readWord(ea);
                    regs.setR(r, value);
                    yield "LDR";
                }

                case OP_STR -> {
                    int ea = computeEA(ix, iBit, addr5);
                    writeWord(ea, regs.getR(r));
                    yield "STR";
                }

                case OP_LDA -> {
                    int ea = computeEA(ix, iBit, addr5);
                    regs.setR(r, (short) ea);
                    yield "LDA";
                }

                case OP_LDX -> {
                    if (ix == 0) {
                        triggerMachineFault(MFR_ILLEGAL_OPCODE, "Invalid LDX IX=0");
                        yield "FAULT";
                    }
                    int ea = computeEAForXOp(iBit, addr5);
                    short value = readWord(ea);
                    regs.setX(ix, value);
                    yield "LDX";
                }
                case OP_STX -> {
                    if (ix == 0) {
                        triggerMachineFault(MFR_ILLEGAL_OPCODE, "Invalid STX IX=0");
                        yield "FAULT";
                    }
                    int ea = computeEAForXOp(iBit, addr5);
                    short value = regs.getX(ix);
                    writeWord(ea, value);
                    yield "STX";
                }

                // =====================
                // ARITHMETIC
                // =====================
                case OP_AMR -> {
                    int ea = computeEA(ix, iBit, addr5);
                    regs.setR(r, addWithCc(regs.getR(r), readWord(ea)));
                    yield "AMR";
                }

                case OP_SMR -> {
                    int ea = computeEA(ix, iBit, addr5);
                    regs.setR(r, subtractWithCc(regs.getR(r), readWord(ea)));
                    yield "SMR";
                }

                case OP_AIR -> {
                    regs.setR(r, addWithCc(regs.getR(r), (short) addr5));
                    yield "AIR";
                }

                case OP_SIR -> {
                    regs.setR(r, subtractWithCc(regs.getR(r), (short) addr5));
                    yield "SIR";
                }

                // =====================
                // TRANSFER
                // =====================
                case OP_JZ -> {
                    if (regs.getR(r) == 0) {
                        regs.setPC(computeEA(ix, iBit, addr5));
                    }
                    yield "JZ";
                }

                case OP_JNE -> {
                    if (regs.getR(r) != 0) {
                        regs.setPC(computeEA(ix, iBit, addr5));
                    }
                    yield "JNE";
                }

                case OP_JCC -> {
                    int cc = r; // cc replaces r
                    if ((regs.getCC() & (1 << cc)) != 0) {
                        regs.setPC(computeEA(ix, iBit, addr5));
                    }
                    yield "JCC";
                }

                case OP_JMA -> {
                    regs.setPC(computeEA(ix, iBit, addr5));
                    yield "JMA";
                }

                case OP_JSR -> {
                    regs.setR(3, (short) regs.getPC()); // save return
                    regs.setPC(computeEA(ix, iBit, addr5));
                    yield "JSR";
                }

                case OP_RFS -> {
                    regs.setR(0, (short) addr5); // return code
                    regs.setPC(regs.getR(3));
                    yield "RFS";
                }

                case OP_SOB -> {
                    short val = (short) (regs.getR(r) - 1);
                    regs.setR(r, val);
                    if (val > 0) {
                        regs.setPC(computeEA(ix, iBit, addr5));
                    }
                    yield "SOB";
                }

                case OP_JGE -> {
                    if (regs.getR(r) >= 0) {
                        regs.setPC(computeEA(ix, iBit, addr5));
                    }
                    yield "JGE";
                }

                // =====================
                // REGISTER OPS
                // =====================
                case OP_MLT -> {
                    int rx = r;
                    int ry = ix;
                    if ((rx & 0x1) != 0 || (ry & 0x1) != 0) {
                        raiseMachineFault(MFR_ILLEGAL_OPCODE, "MLT requires rx, ry to be 0 or 2");
                    }

                    long result = (long) regs.getR(rx) * (long) regs.getR(ry);

                    regs.setR(rx, (short) ((result >> 16) & 0xFFFF));
                    regs.setR(rx + 1, (short) (result & 0xFFFF));

                    yield "MLT";
                }

                case OP_DVD -> {
                    int rx = r;
                    int ry = ix;
                    if ((rx & 0x1) != 0 || (ry & 0x1) != 0) {
                        raiseMachineFault(MFR_ILLEGAL_OPCODE, "DVD requires rx, ry to be 0 or 2");
                    }

                    short divisor = regs.getR(ry);

                    if (divisor == 0) {
                        regs.setCC(regs.getCC() | CC_DIVZERO);
                        yield "DVD DIVZERO";
                    }

                    short quotient = (short) (regs.getR(rx) / divisor);
                    short remainder = (short) (regs.getR(rx) % divisor);

                    regs.setR(rx, quotient);
                    regs.setR(rx + 1, remainder);

                    yield "DVD";
                }

                case OP_TRR -> {
                    int rx = r;
                    int ry = ix;

                    if (regs.getR(rx) == regs.getR(ry)) {
                        regs.setCC(regs.getCC() | CC_EQUAL);
                    } else {
                        regs.setCC(regs.getCC() & ~CC_EQUAL);
                    }
                    yield "TRR";
                }

                case OP_AND -> {
                    int rx = r;
                    int ry = ix;
                    regs.setR(rx, (short) (regs.getR(rx) & regs.getR(ry)));
                    yield "AND";
                }

                case OP_ORR -> {
                    int rx = r;
                    int ry = ix;
                    regs.setR(rx, (short) (regs.getR(rx) | regs.getR(ry)));
                    yield "ORR";
                }

                case OP_NOT -> {
                    regs.setR(r, (short) (~regs.getR(r)));
                    yield "NOT";
                }

                case OP_SRC -> {
                    int count = (ir >> 2) & 0xF;
                    int lr = (ir >> 6) & 0x1; // 1 = left, 0 = right
                    int al = (ir >> 7) & 0x1; // 1 = logical, 0 = arithmetic

                    if (count == 0) {
                        yield "SRC (no-op)";
                    }

                    int val = regs.getR(r);

                    for (int i = 0; i < count; i++) {
                        if (lr == 1) {
                            val = (val << 1) & 0xFFFF;
                        } else {
                            if (al == 1) {
                                val = (val & 0xFFFF) >>> 1;
                            } else {
                                val = (short) val;
                                val >>= 1;
                            }
                        }
                    }

                    regs.setR(r, (short) val);
                    yield "SRC";
                }

                case OP_RRC -> {
                    int count = (ir >> 2) & 0xF;
                    int lr = (ir >> 6) & 0x1; // 1 = left, 0 = right

                    if (count == 0) {
                        yield "RRC (no-op)";
                    }

                    int val = regs.getR(r) & 0xFFFF;

                    for (int i = 0; i < count; i++) {
                        if (lr == 1) {
                            val = ((val << 1) | (val >>> 15)) & 0xFFFF;
                        } else {
                            val = ((val >>> 1) | ((val & 0x1) << 15)) & 0xFFFF;
                        }
                    }

                    regs.setR(r, (short) val);
                    yield "RRC";
                }
                case OP_IN -> {
                    regs.setR(r, readDevice(addr5));
                    yield "IN";
                }
                case OP_OUT -> {
                    writeDevice(addr5, regs.getR(r));
                    yield "OUT";
                }
                case OP_CHK -> {
                    regs.setR(r, checkDevice(addr5));
                    yield "CHK";
                }

                default -> {
                    triggerMachineFault(MFR_ILLEGAL_OPCODE, "Illegal opcode: " + opcode);
                    yield "Illegal opcode";
                }
            };
        } catch (FaultHalt fh) {
            return "FAULT";
        }
        catch (MemoryFault mf) {
            triggerMachineFault(MFR_MEMORY_OVERFLOW, "Memory address beyond memory installed: " + mf.getMessage());
            return "FAULT";
        }
    }

    // LDX/STX use X field as destination/source register id, not as EA index.
    private int computeEAForXOp(int iBit, int addr5) throws MemoryFault {
        int ea = addr5 & 0x1F;
        if (iBit == 1) {
            ea = readWord(ea) & 0xFFF;
        }
        return ea;
    }

    private void executeTrap(int trapCode) throws MemoryFault {
        if (trapCode < 0 || trapCode > 15) {
            raiseMachineFault(MFR_ILLEGAL_TRAP, "Invalid TRAP code: " + trapCode);
        }
        systemWriteWord(2, (short) regs.getPC());
        int trapTableBase = systemReadWord(0) & 0xFFF;
        int tableEntryAddr = trapTableBase + trapCode;
        if (tableEntryAddr < 0 || tableEntryAddr >= Memory.SIZE) {
            raiseMachineFault(MFR_MEMORY_OVERFLOW, "Trap table address out of range: " + tableEntryAddr);
        }
        int handlerAddr = systemReadWord(tableEntryAddr) & 0xFFF;
        regs.setPC(handlerAddr);
    }

    private boolean executeBuiltInTrap(int trapCode) throws MemoryFault {
        if (trapCode == TRAP_PROGRAM2_LOAD) {
            systemWriteWord(2, (short) regs.getPC());
            loadProgram2Paragraph();
            return true;
        }
        if (trapCode == TRAP_PROGRAM2_QUERY) {
            systemWriteWord(2, (short) regs.getPC());
            loadProgram2Query();
            return true;
        }
        if (trapCode == TRAP_PROGRAM2_SEARCH) {
            systemWriteWord(2, (short) regs.getPC());
            searchProgram2Word();
            return true;
        }
        return false;
    }

    private void loadProgram2Paragraph() throws MemoryFault {
        program2Words.clear();
        program2Paragraph = "";

        StringBuilder raw = new StringBuilder();
        StringBuilder currentWord = new StringBuilder();
        int sentenceNumber = 1;
        int wordNumber = 0;
        boolean lastWasSentenceTerminator = false;

        while (true) {
            short value = readDevice(DEV_CARD_READER);
            if (value == 0) {
                break;
            }
            char ch = Character.toUpperCase((char) (value & 0xFF));
            raw.append(ch);
            emitPrinterChar(ch);

            if (isProgram2WordChar(ch)) {
                currentWord.append(ch);
                lastWasSentenceTerminator = false;
                continue;
            }

            if (currentWord.length() > 0) {
                wordNumber++;
                program2Words.add(new Program2Word(currentWord.toString(), sentenceNumber, wordNumber));
                currentWord.setLength(0);
            }

            if (isSentenceTerminator(ch)) {
                sentenceNumber++;
                wordNumber = 0;
                lastWasSentenceTerminator = true;
            } else if (!Character.isWhitespace(ch)) {
                lastWasSentenceTerminator = false;
            }
        }

        if (currentWord.length() > 0) {
            wordNumber++;
            program2Words.add(new Program2Word(currentWord.toString(), sentenceNumber, wordNumber));
        }

        if (raw.length() > 0 && !lastWasSentenceTerminator) {
            emitPrinterChar('\n');
        }
        program2Paragraph = raw.toString();
        regs.setR(0, (short) program2Words.size());
    }

    private void loadProgram2Query() throws MemoryFault {
        int ptr = PROGRAM2_QUERY_BASE;
        StringBuilder query = new StringBuilder();
        while (true) {
            short value = readDevice(DEV_KEYBOARD);
            if (value == 0 || value == '\n' || value == '\r') {
                systemWriteWord(ptr, (short) 0);
                break;
            }
            char ch = Character.toUpperCase((char) (value & 0xFF));
            if (!isProgram2WordChar(ch)) {
                continue;
            }
            query.append(ch);
            systemWriteWord(ptr++, (short) ch);
        }
        program2Query = query.toString();
        regs.setR(1, (short) PROGRAM2_QUERY_BASE);
    }

    private void searchProgram2Word() throws MemoryFault {
        int foundSentence = 0;
        int foundWord = 0;

        for (Program2Word entry : program2Words) {
            if (entry.word.equals(program2Query)) {
                foundSentence = entry.sentenceNumber;
                foundWord = entry.wordNumber;
                break;
            }
        }

        regs.setR(0, (short) foundSentence);
        regs.setR(1, (short) foundWord);
        if (!program2Query.isEmpty()) {
            emitPrinterText(program2Query);
        }
        if (foundSentence > 0) {
            emitPrinterChar(' ');
            emitPrinterNumber(foundSentence);
            emitPrinterChar(' ');
            emitPrinterNumber(foundWord);
        } else {
            emitPrinterText(" NOT FOUND");
        }
        emitPrinterChar('\n');
    }

    private void emitPrinterChar(int value) {
        printerQueue.addLast((short) value);
    }

    private void emitPrinterText(String text) {
        for (int i = 0; i < text.length(); i++) {
            emitPrinterChar(text.charAt(i));
        }
    }

    private void emitPrinterNumber(int value) {
        emitPrinterText(Integer.toString(value));
    }

    private boolean isProgram2WordChar(char ch) {
        return Character.isLetterOrDigit(ch);
    }

    private boolean isSentenceTerminator(char ch) {
        return ch == '.' || ch == '!' || ch == '?';
    }

    private short addWithCc(short left, short right) {
        clearArithmeticCcBits();
        int result = left + right;
        if (result > Short.MAX_VALUE) {
            regs.setCC(regs.getCC() | CC_OVERFLOW);
        } else if (result < Short.MIN_VALUE) {
            regs.setCC(regs.getCC() | CC_UNDERFLOW);
        }
        return (short) result;
    }

    private short subtractWithCc(short left, short right) {
        clearArithmeticCcBits();
        int result = left - right;
        if (result > Short.MAX_VALUE) {
            regs.setCC(regs.getCC() | CC_OVERFLOW);
        } else if (result < Short.MIN_VALUE) {
            regs.setCC(regs.getCC() | CC_UNDERFLOW);
        }
        return (short) result;
    }

    private void clearArithmeticCcBits() {
        regs.setCC(regs.getCC() & ~(CC_OVERFLOW | CC_UNDERFLOW));
    }

    public void pushKeyboardInput(short value) {
        keyboardQueue.addLast(value);
    }

    public void pushKeyboardText(String text, boolean appendNewline) {
        for (int i = 0; i < text.length(); i++) {
            keyboardQueue.addLast((short) text.charAt(i));
        }
        if (appendNewline) {
            keyboardQueue.addLast((short) '\n');
        }
    }

    public int keyboardQueueDepth() {
        return keyboardQueue.size();
    }

    public Short peekKeyboardInput() {
        return keyboardQueue.peekFirst();
    }

    public void pushCardReaderInput(short value) {
        cardReaderQueue.addLast(value);
    }

    public void pushCardReaderText(String text, boolean appendZeroTerminator) {
        for (int i = 0; i < text.length(); i++) {
            cardReaderQueue.addLast((short) text.charAt(i));
        }
        if (appendZeroTerminator) {
            cardReaderQueue.addLast((short) 0);
        }
    }

    public int cardReaderQueueDepth() {
        return cardReaderQueue.size();
    }

    public Short peekCardReaderInput() {
        return cardReaderQueue.peekFirst();
    }

    public List<Short> drainPrinterValues() {
        List<Short> values = new ArrayList<>(printerQueue.size());
        while (!printerQueue.isEmpty()) {
            values.add(printerQueue.removeFirst());
        }
        return values;
    }

    public String drainPrinterOutput() {
        StringBuilder out = new StringBuilder();
        for (short value : drainPrinterValues()) {
            out.append(value).append('\n');
        }
        return out.toString();
    }

    public int getCacheHitCount() {
        return cache.getHitCount();
    }

    public int getCacheMissCount() {
        return cache.getMissCount();
    }

    public CacheLine[] getCacheSnapshot() {
        return cache.snapshot();
    }

    private short readDevice(int devid) {
        if (devid == DEV_KEYBOARD) {
            Short value = keyboardQueue.pollFirst();
            return value == null ? 0 : value;
        }
        if (devid == DEV_CARD_READER) {
            Short value = cardReaderQueue.pollFirst();
            return value == null ? 0 : value;
        }
        return 0;
    }

    private short checkDevice(int devid) {
        if (devid == DEV_KEYBOARD) {
            return (short) (keyboardQueue.isEmpty() ? 0 : 1);
        }
        if (devid == DEV_CARD_READER) {
            return (short) (cardReaderQueue.isEmpty() ? 0 : 1);
        }
        if (devid == DEV_PRINTER) {
            return 1;
        }
        return 0;
    }

    private void writeDevice(int devid, short value) {
        if (devid == DEV_PRINTER) {
            printerQueue.addLast(value);
        }
    }
}
