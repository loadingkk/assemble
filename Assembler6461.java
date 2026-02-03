import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * CSCI 6461 Project 0 Assembler (Two-pass)
 *
 * Outputs:
 *  1) Load file:   <octal_addr(6)> <octal_word(6)>
 *  2) Listing file:<octal_addr(6)> <octal_word(6)> <original line>
 *
 * Supported directives:
 *  - LOC <decimal>
 *  - Data <decimal | label>
 * Supported labels:
 *  - "Label:" at line start (may be followed by instruction/directive)
 *
 * Instruction encodings follow the ISA document formats.
 */
public class Assembler6461 {

    // ========== Models ==========
    static class ParsedLine {
        final int lineNo;
        final String original;
        final String codePart; // without comments, trimmed
        final String label;    // may be null
        final String op;       // may be null (blank/comment only)
        final List<String> operands; // already split by ',' and trimmed
        final boolean generatesWord; // Data or instruction (not LOC)
        int location = -1; // assigned in pass1 if generatesWord

        ParsedLine(int lineNo, String original, String codePart,
                   String label, String op, List<String> operands, boolean generatesWord) {
            this.lineNo = lineNo;
            this.original = original;
            this.codePart = codePart;
            this.label = label;
            this.op = op;
            this.operands = operands;
            this.generatesWord = generatesWord;
        }
    }

    // ========== Opcode Tables (octal in spec; store as int value) ==========
    // Load/Store
    static final Map<String, Integer> OPC = new HashMap<>();
    static {
        // Misc
        OPC.put("HLT",  00);
        OPC.put("TRAP", 030);

        // Load/Store
        OPC.put("LDR",  01);
        OPC.put("STR",  02);
        OPC.put("LDA",  03);
        OPC.put("LDX",  041);
        OPC.put("STX",  042);

        // Transfer
        OPC.put("JZ",   010);
        OPC.put("JNE",  011);
        OPC.put("JCC",  012);
        OPC.put("JMA",  013);
        OPC.put("JSR",  014);
        OPC.put("RFS",  015);
        OPC.put("SOB",  016);
        OPC.put("JGE",  017);

        // Arithmetic/Logical (memory/immediate)
        OPC.put("AMR",  04);
        OPC.put("SMR",  05);
        OPC.put("AIR",  06);
        OPC.put("SIR",  07);

        // Reg-to-reg
        OPC.put("MLT",  070);
        OPC.put("DVD",  071);
        OPC.put("TRR",  072);
        OPC.put("AND",  073);
        OPC.put("ORR",  074);
        OPC.put("NOT",  075);

        // Shift/Rotate
        OPC.put("SRC",  031);
        OPC.put("RRC",  032);

        // I/O
        OPC.put("IN",   061);
        OPC.put("OUT",  062);
        OPC.put("CHK",  063);

        // Floating/Vector (optional for assembler; safe to support)
        OPC.put("FADD", 033);
        OPC.put("FSUB", 034);
        OPC.put("VADD", 035);
        OPC.put("VSUB", 036);
        OPC.put("CNVRT",037);
        OPC.put("LDFR", 050);
        OPC.put("STFR", 051);
    }

    // ========== Entry ==========
    public static void main(String[] args) throws Exception {
        Path in = Paths.get("source.src");
        Path loadOut = Paths.get("output.load");
        Path listOut = Paths.get("output.lst");

        List<String> lines = Files.readAllLines(in);

        // Parse all lines
        List<ParsedLine> parsed = parseLines(lines);

        // Pass 1: build symbol table + assign locations
        Map<String, Integer> symtab = new HashMap<>();
        pass1AssignLocations(parsed, symtab);

        // Pass 2: generate words + write outputs
        pass2Generate(parsed, symtab, loadOut, listOut);

        System.out.println("Done.");
        System.out.println("Load file:   " + loadOut.toAbsolutePath());
        System.out.println("Listing file:" + listOut.toAbsolutePath());
    }

    // ========== Parsing ==========
    static List<ParsedLine> parseLines(List<String> lines) {
        List<ParsedLine> out = new ArrayList<>();
        Pattern labelPat = Pattern.compile("^\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*:\\s*(.*)$");

        for (int i = 0; i < lines.size(); i++) {
            String original = lines.get(i);

            // Separate comment
            String code = original;
            int semi = code.indexOf(';');
            if (semi >= 0) code = code.substring(0, semi);
            code = code.trim();

            if (code.isEmpty()) {
                out.add(new ParsedLine(i + 1, original, "", null, null, List.of(), false));
                continue;
            }

            String label = null;
            String rest = code;

            Matcher m = labelPat.matcher(code);
            if (m.matches()) {
                label = m.group(1);
                rest = m.group(2).trim();
            }

            if (rest.isEmpty()) {
                // label-only line
                out.add(new ParsedLine(i + 1, original, code, label, null, List.of(), false));
                continue;
            }

            // Tokenize op and operands
            String[] parts = rest.split("\\s+", 2);
            String op = parts[0].trim().toUpperCase(Locale.ROOT);
            String ops = (parts.length > 1) ? parts[1].trim() : "";

            List<String> operands = new ArrayList<>();
            if (!ops.isEmpty()) {
                // Split by comma; keep simple
                for (String tok : ops.split(",")) {
                    String t = tok.trim();
                    if (!t.isEmpty()) operands.add(t);
                }
            }

            boolean generatesWord = !(op.equals("LOC")); // LOC does not allocate
            out.add(new ParsedLine(i + 1, original, code, label, op, operands, generatesWord));
        }
        return out;
    }

    // ========== Pass 1 ==========
    static void pass1AssignLocations(List<ParsedLine> parsed, Map<String, Integer> symtab) {
        int loc = 0;

        for (ParsedLine pl : parsed) {
            // If label exists, record current loc (before processing op)
            if (pl.label != null) {
                if (symtab.containsKey(pl.label)) {
                    throw new IllegalArgumentException("Duplicate label '" + pl.label + "' at line " + pl.lineNo);
                }
                symtab.put(pl.label, loc);
            }

            if (pl.op == null) continue;

            if (pl.op.equals("LOC")) {
                if (pl.operands.size() != 1) {
                    throw new IllegalArgumentException("LOC expects 1 operand at line " + pl.lineNo);
                }
                int newLoc = parseDecimal(pl.operands.get(0), pl.lineNo);
                if (newLoc < 0) throw new IllegalArgumentException("LOC must be >= 0 at line " + pl.lineNo);
                loc = newLoc;
                continue;
            }

            if (pl.generatesWord) {
                pl.location = loc;
                loc += 1;
            }
        }
    }

    // ========== Pass 2 ==========
    static void pass2Generate(List<ParsedLine> parsed,
                              Map<String, Integer> symtab,
                              Path loadOut, Path listOut) throws IOException {
        try (BufferedWriter load = Files.newBufferedWriter(loadOut);
             BufferedWriter lst = Files.newBufferedWriter(listOut)) {

            for (ParsedLine pl : parsed) {
                if (!pl.generatesWord || pl.op == null) continue;

                int word;
                if (pl.op.equalsIgnoreCase("DATA")) {
                    word = encodeData(pl, symtab);
                } else {
                    word = encodeInstruction(pl, symtab);
                }

                int addr = pl.location;
                String addrOct = fmt6(addr);
                String wordOct = fmt6(word & 0xFFFF);

                // Load file: only non-blank generated lines
                load.write(addrOct + " " + wordOct);
                load.newLine();

                // Listing file: include original line
                lst.write(addrOct + " " + wordOct + " " + pl.original);
                lst.newLine();
            }
        }
    }

    // ========== Encoders ==========
    static int encodeData(ParsedLine pl, Map<String, Integer> symtab) {
        if (pl.operands.size() != 1) {
            throw new IllegalArgumentException("Data expects 1 operand at line " + pl.lineNo);
        }
        String v = pl.operands.get(0);
        int value;
        if (isNumber(v)) {
            value = parseDecimal(v, pl.lineNo);
        } else {
            Integer a = symtab.get(v);
            if (a == null) throw new IllegalArgumentException("Unknown label '" + v + "' at line " + pl.lineNo);
            value = a;
        }
        return value & 0xFFFF;
    }

    static int encodeInstruction(ParsedLine pl, Map<String, Integer> symtab) {
        Integer opcodeOct = OPC.get(pl.op.toUpperCase(Locale.ROOT));
        if (opcodeOct == null) {
            throw new IllegalArgumentException("Unknown opcode '" + pl.op + "' at line " + pl.lineNo);
        }
        int op6 = opcodeOct & 0b111111; // op is 6 bits

        switch (pl.op.toUpperCase(Locale.ROOT)) {
            // ---- Misc ----
            case "HLT":
                requireOperands(pl, 0);
                return packLS(op6, 0, 0, 0, 0);

            case "TRAP": {
                requireOperands(pl, 1);
                int code = parseDecimal(pl.operands.get(0), pl.lineNo);
                if (code < 0 || code > 15) throw new IllegalArgumentException("TRAP code must be 0..15 at line " + pl.lineNo);
                return packLS(op6, 0, 0, 0, code);
            }

            // ---- Load/Store: r,x,address[,I] ----
            case "LDR":
            case "STR":
            case "LDA":
            case "AMR":
            case "SMR":
            case "JZ":
            case "JNE":
            case "SOB":
            case "JGE":
            case "FADD":
            case "FSUB": {
                // For FADD/FSUB, r is fr (0..1) but still fits in 2 bits
                ParsedLS ls = parseLS(pl, symtab, 3, true);
                return packLS(op6, ls.r, ls.ix, ls.i, ls.addr5);
            }

            // ---- LDX/STX: x,address[,I] ; encoded: R=0, IX=x ----
            case "LDX":
            case "STX":
            case "JMA":
            case "JSR":
            case "LDFR":
            case "STFR": {
                ParsedLS2 ls2 = parseXAddr(pl, symtab, 2, true);
                // R ignored (0), IX = x
                return packLS(op6, 0, ls2.x, ls2.i, ls2.addr5);
            }

            // ---- JCC: cc,x,address[,I] (cc in R field, x in IX) ----
            case "JCC": {
                ParsedLS ls = parseLS(pl, symtab, 3, true);
                // Here "r" is cc
                return packLS(op6, ls.r, ls.ix, ls.i, ls.addr5);
            }

            // ---- RFS: immed (address field) ----
            case "RFS": {
                requireOperands(pl, 1);
                int imm = parseDecimal(pl.operands.get(0), pl.lineNo);
                if (imm < 0 || imm > 31) throw new IllegalArgumentException("RFS immed must be 0..31 at line " + pl.lineNo);
                return packLS(op6, 0, 0, 0, imm);
            }

            // ---- AIR/SIR: r,immed (IX/I ignored) ----
            case "AIR":
            case "SIR": {
                requireOperands(pl, 2);
                int r = parseReg(pl.operands.get(0), pl.lineNo);
                int imm = parseDecimal(pl.operands.get(1), pl.lineNo);
                if (imm < 0 || imm > 31) throw new IllegalArgumentException(pl.op + " immed must be 0..31 at line " + pl.lineNo);
                return packLS(op6, r, 0, 0, imm);
            }

            // ---- Reg-to-reg: rx,ry (NOT: rx) ----
            case "MLT":
            case "DVD":
            case "TRR":
            case "AND":
            case "ORR": {
                requireOperands(pl, 2);
                int rx = parseReg(pl.operands.get(0), pl.lineNo);
                int ry = parseReg(pl.operands.get(1), pl.lineNo);
                return packRR(op6, rx, ry);
            }
            case "NOT": {
                requireOperands(pl, 1);
                int rx = parseReg(pl.operands.get(0), pl.lineNo);
                return packRR(op6, rx, 0);
            }

            // ---- Shift/Rotate: r,count,L/R,A/L ----
            // Accept forms:
            //   SRC r,count,LR,AL   (LR:0 right,1 left; AL:0 arith,1 logical)
            //   RRC r,count,LR,AL   (AL should be 1 logically, but we still encode)
            case "SRC":
            case "RRC": {
                requireOperands(pl, 4);
                int r = parseReg(pl.operands.get(0), pl.lineNo);
                int count = parseDecimal(pl.operands.get(1), pl.lineNo);
                int lr = parse01(pl.operands.get(2), pl.lineNo);
                int al = parse01(pl.operands.get(3), pl.lineNo);
                if (count < 0 || count > 15) throw new IllegalArgumentException(pl.op + " count must be 0..15 at line " + pl.lineNo);
                return packShiftRotate(op6, r, al, lr, count);
            }

            // ---- I/O: r,devid ----
            case "IN":
            case "OUT":
            case "CHK": {
                requireOperands(pl, 2);
                int r = parseReg(pl.operands.get(0), pl.lineNo);
                int devid = parseDecimal(pl.operands.get(1), pl.lineNo);
                if (devid < 0 || devid > 31) throw new IllegalArgumentException(pl.op + " devid must be 0..31 at line " + pl.lineNo);
                return packIO(op6, r, devid);
            }

            // ---- Vector / CNVRT (optional) ----
            // We'll treat VADD/VSUB/CNVRT same as LS format: r,x,address[,I]
            case "VADD":
            case "VSUB":
            case "CNVRT": {
                ParsedLS ls = parseLS(pl, symtab, 3, true);
                return packLS(op6, ls.r, ls.ix, ls.i, ls.addr5);
            }

            default:
                throw new IllegalArgumentException("Opcode recognized but encoder not implemented: " + pl.op + " at line " + pl.lineNo);
        }
    }

    // LS format: opcode(6) R(2) IX(2) I(1) Address(5)
    static int packLS(int op6, int r2, int ix2, int i1, int addr5) {
        return ((op6 & 0x3F) << 10)
                | ((r2 & 0x3) << 8)
                | ((ix2 & 0x3) << 6)
                | ((i1 & 0x1) << 5)
                | (addr5 & 0x1F);
    }

    // RR format: opcode(6) Rx(2) Ry(2) zeros(6)
    static int packRR(int op6, int rx2, int ry2) {
        return ((op6 & 0x3F) << 10)
                | ((rx2 & 0x3) << 8)
                | ((ry2 & 0x3) << 6);
    }

    // Shift/Rotate:
    // opcode(6) R(2) A/L(1) L/R(1) Count(4) zeros(2)
    static int packShiftRotate(int op6, int r2, int al1, int lr1, int count4) {
        return ((op6 & 0x3F) << 10)
                | ((r2 & 0x3) << 8)
                | ((al1 & 0x1) << 7)
                | ((lr1 & 0x1) << 6)
                | ((count4 & 0xF) << 2);
    }

    // IO:
    // opcode(6) R(2) zeros(3) devid(5)
    static int packIO(int op6, int r2, int devid5) {
        return ((op6 & 0x3F) << 10)
                | ((r2 & 0x3) << 8)
                | (devid5 & 0x1F);
    }

    // ========== Helpers for parsing operands ==========
    static class ParsedLS {
        int r, ix, i, addr5;
        ParsedLS(int r, int ix, int i, int addr5) { this.r=r; this.ix=ix; this.i=i; this.addr5=addr5; }
    }
    static class ParsedLS2 {
        int x, i, addr5;
        ParsedLS2(int x, int i, int addr5) { this.x=x; this.i=i; this.addr5=addr5; }
    }

    // Parse r,x,address[,I] or cc,x,address[,I]
    static ParsedLS parseLS(ParsedLine pl, Map<String, Integer> symtab, int minOps, boolean allowIndirect) {
        if (pl.operands.size() < minOps || pl.operands.size() > (allowIndirect ? minOps + 1 : minOps)) {
            throw new IllegalArgumentException(pl.op + " expects " + minOps + (allowIndirect ? " or "+(minOps+1) : "")
                    + " operands at line " + pl.lineNo);
        }
        int r = parseRegOrCC(pl.operands.get(0), pl.lineNo, pl.op);
        int ix = parseIX(pl.operands.get(1), pl.lineNo);
        int addr5 = parseAddr5(pl.operands.get(2), symtab, pl.lineNo);

        int i = 0;
        if (pl.operands.size() == minOps + 1) {
            i = parseIndirectFlag(pl.operands.get(3), pl.lineNo);
        }
        return new ParsedLS(r, ix, i, addr5);
    }

    // Parse x,address[,I] where x=1..3 stored in IX field
    static ParsedLS2 parseXAddr(ParsedLine pl, Map<String, Integer> symtab, int minOps, boolean allowIndirect) {
        if (pl.operands.size() < minOps || pl.operands.size() > (allowIndirect ? minOps + 1 : minOps)) {
            throw new IllegalArgumentException(pl.op + " expects " + minOps + (allowIndirect ? " or "+(minOps+1) : "")
                    + " operands at line " + pl.lineNo);
        }
        int x = parseIX(pl.operands.get(0), pl.lineNo);
        if (x == 0) throw new IllegalArgumentException(pl.op + " x must be 1..3 at line " + pl.lineNo);

        int addr5 = parseAddr5(pl.operands.get(1), symtab, pl.lineNo);

        int i = 0;
        if (pl.operands.size() == minOps + 1) {
            i = parseIndirectFlag(pl.operands.get(2), pl.lineNo);
        }
        return new ParsedLS2(x, i, addr5);
    }

    static int parseReg(String s, int lineNo) {
        int r = parseDecimal(s, lineNo);
        if (r < 0 || r > 3) throw new IllegalArgumentException("Register must be 0..3 at line " + lineNo);
        return r;
    }

    static int parseRegOrCC(String s, int lineNo, String op) {
        // For JCC, the first operand is cc (0..3). For others it's reg.
        int v = parseDecimal(s, lineNo);
        if (v < 0 || v > 3) throw new IllegalArgumentException(op + " first operand must be 0..3 at line " + lineNo);
        return v;
    }

    static int parseIX(String s, int lineNo) {
        int ix = parseDecimal(s, lineNo);
        if (ix < 0 || ix > 3) throw new IllegalArgumentException("IX must be 0..3 at line " + lineNo);
        return ix;
    }

    static int parseAddr5(String s, Map<String, Integer> symtab, int lineNo) {
        int v;
        if (isNumber(s)) {
            v = parseDecimal(s, lineNo);
        } else {
            Integer a = symtab.get(s);
            if (a == null) throw new IllegalArgumentException("Unknown label '" + s + "' at line " + lineNo);
            v = a;
        }
        if (v < 0 || v > 31) {
            throw new IllegalArgumentException("Address field must be 0..31 (5 bits) at line " + lineNo
                    + " but got " + v + ". Use indexing + base address scheme if you need full memory.");
        }
        return v;
    }

    static int parseIndirectFlag(String s, int lineNo) {
        // Accept "1" or "I" (case-insensitive)
        if (s.equalsIgnoreCase("I")) return 1;
        int v = parseDecimal(s, lineNo);
        if (v != 0 && v != 1) throw new IllegalArgumentException("Indirect flag must be 0/1 or I at line " + lineNo);
        return v;
    }

    static int parse01(String s, int lineNo) {
        int v = parseDecimal(s, lineNo);
        if (v != 0 && v != 1) throw new IllegalArgumentException("Expected 0 or 1 at line " + lineNo);
        return v;
    }

    static void requireOperands(ParsedLine pl, int n) {
        if (pl.operands.size() != n) {
            throw new IllegalArgumentException(pl.op + " expects " + n + " operands at line " + pl.lineNo);
        }
    }

    // ========== Formatting & Numeric ==========
    static String fmt6(int v) {
        // always 6-digit octal like examples: 000006 003412
        return String.format("%06o", v & 0xFFFF);
    }

    static boolean isNumber(String s) {
        return s.matches("[-+]?\\d+");
    }

    static int parseDecimal(String s, int lineNo) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Expected decimal integer but got '" + s + "' at line " + lineNo);
        }
    }
}
