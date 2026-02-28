package cisc.sim;

import cisc.sim.machine.CPU;
import cisc.sim.machine.Machine;
import cisc.sim.machine.Registers;
import cisc.sim.memory.Memory;
import cisc.sim.memory.MemoryFault;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class SimGuiMain {
    private static final int WORD_BITS = 16;
    private static final int DEMO_BASE_OCT = 010;
    private static final Color BG = new Color(173, 206, 223);

    private final Machine machine = new Machine();
    private final CPU cpu = machine.cpu();
    private final Memory memory = machine.memory();
    private final Registers regs = cpu.R();

    private final Map<String, JTextField> registerFields = new LinkedHashMap<>();
    private final JToggleButton[] bitSwitches = new JToggleButton[WORD_BITS];

    private JTextField binaryField;
    private JTextField octalField;
    private JTextField marMemoryField;
    private final JTextField[][] cacheCells = new JTextField[17][3];
    private JTextArea printerArea;
    private JTextField consoleInputField;
    private JTextField programFileField;

    private String selectedRegister = "R0";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SimGuiMain().createAndShow());
    }

    private void createAndShow() {
        setSystemLookAndFeel();

        JFrame frame = new JFrame("CSCI 6461 Machine Simulator - Part I");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1380, 860));

        JPanel root = new JPanel(new BorderLayout(16, 16));
        root.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        root.setBackground(BG);

        JLabel title = new JLabel("CSCI 6461 Machine Simulator", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD | Font.ITALIC, 34));
        title.setForeground(new Color(58, 58, 65));
        root.add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(12, 0));
        center.setOpaque(false);
        center.add(buildLeftPanel(), BorderLayout.CENTER);
        center.add(buildRightPanel(), BorderLayout.EAST);

        root.add(center, BorderLayout.CENTER);
        frame.setContentPane(root);

        machine.resetAll();
        setWordValue(0);
        refreshUiFromMachine();
        appendPrinter("GUI ready. Press IPL to preload the Part I demo program.");

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel buildLeftPanel() {
        JPanel left = new JPanel(new GridBagLayout());
        left.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.gridx = 0;

        gbc.gridy = 0;
        left.add(buildRegistersPanel(), gbc);

        gbc.gridy = 1;
        left.add(buildBinaryControlRow(), gbc);

        gbc.gridy = 2;
        left.add(buildProgramFilePanel(), gbc);

        gbc.gridy = 3;
        gbc.weighty = 1;
        left.add(Box.createVerticalGlue(), gbc);
        return left;
    }

    private JPanel buildRightPanel() {
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setPreferredSize(new Dimension(520, 720));
        right.add(buildCachePanel());
        right.add(Box.createVerticalStrut(8));
        right.add(buildPrinterPanel());
        right.add(Box.createVerticalStrut(8));
        right.add(buildConsolePanel());
        return right;
    }

    private JPanel buildRegistersPanel() {
        JPanel panel = borderedPanel("Registers");
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = baseGbc();

        gbc.gridy = 0;
        gbc.gridx = 0;
        panel.add(new JLabel("GPR"), gbc);
        gbc.gridx = 1;
        panel.add(new JLabel("IXR"), gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("Control"), gbc);

        for (int i = 0; i < 4; i++) {
            gbc.gridy = i + 1;
            gbc.gridx = 0;
            panel.add(registerRow("R" + i), gbc);

            gbc.gridx = 1;
            if (i == 0) {
                panel.add(Box.createHorizontalStrut(120), gbc);
            } else {
                panel.add(registerRow("X" + i), gbc);
            }
        }

        String[] ctrlRegs = {"PC", "MAR", "MBR", "IR", "CC", "MFR"};
        for (int i = 0; i < ctrlRegs.length; i++) {
            gbc.gridy = i + 1;
            gbc.gridx = 2;
            panel.add(registerRow(ctrlRegs[i]), gbc);
        }

        gbc.gridy = 7;
        gbc.gridx = 2;
        panel.add(buildMarDisplayRow(), gbc);
        return panel;
    }

    private JPanel buildBinaryControlRow() {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;

        gbc.gridx = 0;
        gbc.weightx = 0.60;
        row.add(buildBinaryPanel(), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.40;
        gbc.insets = new Insets(0, 10, 0, 0);
        row.add(buildControlPanel(), gbc);
        return row;
    }

    private JPanel buildBinaryPanel() {
        JPanel panel = borderedPanel("Binary / Octal Input");
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = baseGbc();
        gbc.weightx = 1;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("BINARY"), gbc);

        gbc.gridy = 1;
        panel.add(buildBitSwitchPanel(), gbc);

        gbc.gridy = 2;
        binaryField = new JTextField(18);
        panel.add(binaryField, gbc);

        gbc.gridy = 3;
        panel.add(new JLabel("OCTAL INPUT"), gbc);

        gbc.gridy = 4;
        JPanel octalRow = new JPanel(new BorderLayout(6, 0));
        octalRow.setOpaque(false);
        octalField = new JTextField("0", 8);
        octalRow.add(octalField, BorderLayout.CENTER);
        panel.add(octalRow, gbc);

        binaryField.addActionListener(e -> applyBinaryInput(binaryField.getText()));
        octalField.addActionListener(e -> applyOctalInput(octalField.getText()));
        octalField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                applyOctalInput(octalField.getText());
            }
        });

        return panel;
    }

    private JPanel buildBitSwitchPanel() {
        JPanel bits = new JPanel(new GridLayout(1, WORD_BITS, 2, 2));
        bits.setOpaque(false);
        for (int i = 0; i < WORD_BITS; i++) {
            JToggleButton bit = new JToggleButton("0");
            bit.setMargin(new Insets(1, 1, 1, 1));
            bit.setPreferredSize(new Dimension(28, 28));
            bit.addActionListener(e -> {
                bit.setText(bit.isSelected() ? "1" : "0");
                syncWordFieldsFromToggles();
            });
            bitSwitches[i] = bit;
            bits.add(bit);
        }
        return bits;
    }

    private JPanel buildControlPanel() {
        JPanel panel = borderedPanel("Controls");
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = baseGbc();

        String[] left = {"Load", "Load+", "Store", "Store+"};
        String[] right = {"Run", "Step", "Halt", "IPL"};

        for (int i = 0; i < left.length; i++) {
            final String lAction = left[i];
            final String rAction = right[i];
            gbc.gridy = i;
            gbc.gridx = 0;
            JButton lButton = new JButton(lAction);
            lButton.addActionListener(e -> handleControl(lAction));
            panel.add(lButton, gbc);

            gbc.gridx = 1;
            JButton rButton = new JButton(rAction);
            rButton.addActionListener(e -> handleControl(rAction));
            panel.add(rButton, gbc);
        }
        return panel;
    }

    private JPanel buildProgramFilePanel() {
        JPanel panel = borderedPanel("Program File");
        panel.setLayout(new BorderLayout(8, 0));
        programFileField = new JTextField();
        JButton setBtn = new JButton("Set");
        setBtn.addActionListener(e -> appendPrinter("Program file set: " + programFileField.getText()));
        panel.add(programFileField, BorderLayout.CENTER);
        panel.add(setBtn, BorderLayout.EAST);
        return panel;
    }

    private JPanel buildTextPanel(String title, boolean cachePanel) {
        JPanel panel = borderedPanel(title);
        panel.setLayout(new BorderLayout());
        JTextArea area = new JTextArea(cachePanel ? 12 : 8, 26);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        panel.add(new JScrollPane(area), BorderLayout.CENTER);
        printerArea = area;
        printerArea.setEditable(false);
        return panel;
    }

    private JPanel buildPrinterPanel() {
        JPanel panel = buildTextPanel("Printer", false);
        panel.setPreferredSize(new Dimension(520, 250));
        return panel;
    }

    private JPanel buildCachePanel() {
        JPanel panel = borderedPanel("Cache Content");
        panel.setLayout(new BorderLayout());
        panel.setPreferredSize(new Dimension(520, 460));

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        String[] headers = {"View", "Addr", "Value"};
        for (int col = 0; col < headers.length; col++) {
            gbc.gridx = col;
            gbc.gridy = 0;
            JLabel header = new JLabel(headers[col], SwingConstants.CENTER);
            header.setFont(new Font("SansSerif", Font.BOLD, 12));
            grid.add(header, gbc);
        }

        for (int row = 0; row < cacheCells.length; row++) {
            for (int col = 0; col < cacheCells[row].length; col++) {
                gbc.gridx = col;
                gbc.gridy = row + 1;
                JTextField field = new JTextField();
                field.setEditable(false);
                field.setHorizontalAlignment(SwingConstants.CENTER);
                field.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                if (col == 0) {
                    field.setPreferredSize(new Dimension(86, 26));
                } else if (col == 1) {
                    field.setPreferredSize(new Dimension(76, 26));
                } else {
                    field.setPreferredSize(new Dimension(112, 26));
                }
                cacheCells[row][col] = field;
                grid.add(field, gbc);
            }
        }

        panel.add(grid, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildConsolePanel() {
        JPanel panel = borderedPanel("Console Input");
        panel.setLayout(new BorderLayout(6, 0));
        panel.setPreferredSize(new Dimension(520, 90));
        consoleInputField = new JTextField();
        JButton sendBtn = new JButton("Send");
        sendBtn.addActionListener(e -> {
            appendPrinter("Console Input > " + consoleInputField.getText());
            consoleInputField.setText("");
        });
        consoleInputField.addActionListener(sendBtn.getActionListeners()[0]);
        panel.add(consoleInputField, BorderLayout.CENTER);
        panel.add(sendBtn, BorderLayout.EAST);
        return panel;
    }

    private JPanel registerRow(String name) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        JLabel label = new JLabel(name, SwingConstants.RIGHT);
        label.setPreferredSize(new Dimension(34, 28));
        JTextField field = new JTextField(7);
        field.setPreferredSize(new Dimension(98, 28));
        JButton ldBtn = new JButton("LD");
        ldBtn.setMargin(new Insets(1, 6, 1, 6));
        ldBtn.addActionListener(e -> {
            selectedRegister = name;
            appendPrinter("Selected register: " + name);
        });
        registerFields.put(name, field);
        row.add(label, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        row.add(ldBtn, BorderLayout.EAST);
        return row;
    }

    private JPanel buildMarDisplayRow() {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        JLabel label = new JLabel("MEM[MAR]", SwingConstants.RIGHT);
        label.setPreferredSize(new Dimension(90, 28));
        marMemoryField = new JTextField(10);
        marMemoryField.setEditable(false);
        row.add(label, BorderLayout.WEST);
        row.add(marMemoryField, BorderLayout.CENTER);
        return row;
    }

    private void handleControl(String action) {
        try {
            switch (action) {
                case "IPL" -> {
                    iplLoadDemoProgram();
                    refreshUiFromMachine();
                    setWordValue(0);
                    appendPrinter("IPL: demo load/store program loaded at 0010(octal).");
                }
                case "Load", "Load+" -> {
                    syncInputBeforeAction();
                    int value = parseCurrentWordValue();
                    loadSwitchValueToRegister(selectedRegister, value);
                    if ("Load+".equals(action)) {
                        regs.setMAR(regs.getMAR() + 1);
                    }
                    refreshUiFromMachine();
                    appendPrinter(action + ": " + selectedRegister + " <- " + formatOctal(value, 6));
                }
                case "Store", "Store+" -> {
                    int mar = regs.getMAR();
                    short value = readSelectedRegister();
                    cpu.writeWord(mar, value);
                    if ("Store+".equals(action)) {
                        regs.setMAR(regs.getMAR() + 1);
                    }
                    refreshUiFromMachine();
                    appendPrinter(action + ": MEM[" + formatOctal(mar, 4) + "] <- " + selectedRegister);
                }
                case "Step" -> {
                    pullUiRegistersToMachine();
                    String trace = cpu.executeLoadStoreStep();
                    refreshUiFromMachine();
                    appendPrinter("Step: " + trace);
                }
                case "Run" -> {
                    pullUiRegistersToMachine();
                    String last = "none";
                    int steps = 0;
                    while (!cpu.isHalted() && steps < 200) {
                        last = cpu.executeLoadStoreStep();
                        steps++;
                    }
                    refreshUiFromMachine();
                    appendPrinter("Run: steps=" + steps + ", last=" + last);
                }
                case "Halt" -> {
                    cpu.halt();
                    refreshUiFromMachine();
                    appendPrinter("Halt: machine paused.");
                }
                default -> appendPrinter("Unknown action: " + action);
            }
        } catch (MemoryFault mf) {
            appendPrinter("MemoryFault: " + mf.getMessage());
        }
    }

    private void loadSwitchValueToRegister(String regName, int value) {
        short v = (short) (value & 0xFFFF);
        if (regName.startsWith("R")) {
            regs.setR(Character.getNumericValue(regName.charAt(1)), v);
            return;
        }
        if (regName.startsWith("X")) {
            int idx = Character.getNumericValue(regName.charAt(1));
            if (idx >= 1 && idx <= 3) {
                regs.setX(idx, v);
            }
            return;
        }
        switch (regName) {
            case "PC" -> regs.setPC(value);
            case "MAR" -> regs.setMAR(value);
            case "MBR" -> regs.setMBR(v);
            case "IR" -> regs.setIR(v);
            case "CC" -> regs.setCC(value);
            case "MFR" -> regs.setMFR(value);
            default -> {
            }
        }
    }

    private short readSelectedRegister() {
        if (selectedRegister.startsWith("R")) {
            return regs.getR(Character.getNumericValue(selectedRegister.charAt(1)));
        }
        if (selectedRegister.startsWith("X")) {
            int idx = Character.getNumericValue(selectedRegister.charAt(1));
            if (idx >= 1 && idx <= 3) {
                return regs.getX(idx);
            }
        }
        return switch (selectedRegister) {
            case "PC" -> (short) regs.getPC();
            case "MAR" -> (short) regs.getMAR();
            case "MBR" -> regs.getMBR();
            case "IR" -> regs.getIR();
            case "CC" -> (short) regs.getCC();
            case "MFR" -> (short) regs.getMFR();
            default -> 0;
        };
    }

    private void refreshUiFromMachine() {
        for (int i = 0; i < 4; i++) {
            setRegisterText("R" + i, regs.getR(i) & 0xFFFF);
        }
        for (int i = 1; i <= 3; i++) {
            setRegisterText("X" + i, regs.getX(i) & 0xFFFF);
        }
        setRegisterText("PC", regs.getPC());
        setRegisterText("MAR", regs.getMAR());
        setRegisterText("MBR", regs.getMBR() & 0xFFFF);
        setRegisterText("IR", regs.getIR() & 0xFFFF);
        setRegisterText("CC", regs.getCC());
        setRegisterText("MFR", regs.getMFR());
        try {
            marMemoryField.setText(formatOctal(memory.peek(regs.getMAR()) & 0xFFFF, 6));
        } catch (MemoryFault mf) {
            marMemoryField.setText("FAULT");
        }
        refreshCacheView();
    }

    private void setRegisterText(String regName, int value) {
        JTextField field = registerFields.get(regName);
        if (field == null) {
            return;
        }
        if ("PC".equals(regName) || "MAR".equals(regName)) {
            field.setText(formatOctal(value, 4));
        } else if ("CC".equals(regName) || "MFR".equals(regName)) {
            field.setText(formatBinary(value & 0xF).substring(12));
        } else {
            field.setText(formatOctal(value, 6));
        }
    }

    private void pullUiRegistersToMachine() {
        for (int i = 0; i < 4; i++) {
            regs.setR(i, (short) parseRegisterValue("R" + i));
        }
        for (int i = 1; i <= 3; i++) {
            regs.setX(i, (short) parseRegisterValue("X" + i));
        }
        regs.setPC(parseRegisterValue("PC"));
        regs.setMAR(parseRegisterValue("MAR"));
        regs.setMBR((short) parseRegisterValue("MBR"));
        regs.setIR((short) parseRegisterValue("IR"));
        regs.setCC(parseRegisterValue("CC"));
        regs.setMFR(parseRegisterValue("MFR"));
    }

    private int parseRegisterValue(String regName) {
        JTextField field = registerFields.get(regName);
        if (field == null) {
            return 0;
        }
        String txt = field.getText().trim();
        if (txt.isEmpty()) {
            return 0;
        }
        try {
            if ("CC".equals(regName) || "MFR".equals(regName)) {
                return Integer.parseInt(txt, 2);
            }
            return Integer.parseInt(txt, 8);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void syncInputBeforeAction() {
        String oct = octalField.getText().trim();
        if (oct.matches("[0-7]{1,6}")) {
            applyOctalInput(oct);
            return;
        }
        String bin = binaryField.getText().trim();
        if (bin.matches("[01]{1,16}")) {
            applyBinaryInput(bin);
        }
    }

    private void applyBinaryInput(String text) {
        String cleaned = text.trim();
        if (!cleaned.matches("[01]{1,16}")) {
            appendPrinter("Binary input must be 1..16 bits.");
            return;
        }
        int value = Integer.parseInt(cleaned, 2);
        setWordValue(value);
    }

    private void applyOctalInput(String text) {
        String cleaned = text.trim();
        if (!cleaned.matches("[0-7]{1,6}")) {
            appendPrinter("Octal input must be 1..6 digits.");
            return;
        }
        int value = Integer.parseInt(cleaned, 8);
        setWordValue(value);
    }

    private void syncWordFieldsFromToggles() {
        int value = 0;
        for (JToggleButton bit : bitSwitches) {
            value = (value << 1) | (bit.isSelected() ? 1 : 0);
        }
        binaryField.setText(formatBinary(value));
        octalField.setText(formatOctal(value, 6));
    }

    private void setWordValue(int value) {
        int masked = value & 0xFFFF;
        for (int i = 0; i < WORD_BITS; i++) {
            boolean on = ((masked >> (WORD_BITS - 1 - i)) & 1) == 1;
            bitSwitches[i].setSelected(on);
            bitSwitches[i].setText(on ? "1" : "0");
        }
        binaryField.setText(formatBinary(masked));
        octalField.setText(formatOctal(masked, 6));
    }

    private int parseCurrentWordValue() {
        try {
            return Integer.parseInt(binaryField.getText(), 2);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void refreshCacheView() {
        if (cacheCells[0][0] == null) {
            return;
        }
        String[] labels = {
            "Boot+0", "Boot+1", "Boot+2", "Boot+3", "Boot+4", "Boot+5", "Boot+6", "Boot+7",
            "Data20", "Data22", "Data23", "Data24", "Data25", "Data27", "Ptr30",
            "PC", "MAR"
        };
        int[] addresses = {
            DEMO_BASE_OCT,
            DEMO_BASE_OCT + 1,
            DEMO_BASE_OCT + 2,
            DEMO_BASE_OCT + 3,
            DEMO_BASE_OCT + 4,
            DEMO_BASE_OCT + 5,
            DEMO_BASE_OCT + 6,
            DEMO_BASE_OCT + 7,
            020,
            022,
            023,
            024,
            025,
            027,
            030,
            regs.getPC() & 0xFFF,
            regs.getMAR() & 0xFFF
        };
        try {
            for (int row = 0; row < cacheCells.length; row++) {
                int addr = addresses[row] % Memory.SIZE;
                cacheCells[row][0].setText(labels[row]);
                cacheCells[row][1].setText(formatOctal(addr, 4));
                cacheCells[row][2].setText(formatOctal(memory.peek(addr) & 0xFFFF, 6));
            }
        } catch (MemoryFault mf) {
            for (JTextField[] row : cacheCells) {
                for (JTextField cell : row) {
                    if (cell != null) {
                        cell.setText("FAULT");
                    }
                }
            }
        }
    }

    private void iplLoadDemoProgram() {
        machine.resetAll();
        regs.setPC(DEMO_BASE_OCT);
        regs.setMAR(0);
        regs.setCC(0);
        regs.setMFR(0);
        selectedRegister = "R0";

        try {
            int b = DEMO_BASE_OCT;
            memory.poke(b, encodeLS(03, 0, 0, 0, 022));      // LDA R0,0,22
            memory.poke(b + 1, encodeLS(02, 0, 0, 0, 023));  // STR R0,0,23
            memory.poke(b + 2, encodeLS(01, 1, 0, 0, 023));  // LDR R1,0,23
            memory.poke(b + 3, encodeLS(041, 0, 1, 0, 024)); // LDX X1,24
            memory.poke(b + 4, encodeLS(042, 0, 1, 0, 025)); // STX X1,25
            memory.poke(b + 5, encodeLS(01, 2, 1, 0, 020));  // LDR R2,1,20
            memory.poke(b + 6, encodeLS(01, 3, 0, 1, 030));  // LDR R3,0,30,1
            memory.poke(b + 7, (short) 0);                   // HLT

            memory.poke(020, (short) 000444);
            memory.poke(022, (short) 000555);
            memory.poke(023, (short) 000000);
            memory.poke(024, (short) 000002); // X1 <- 2 for indexed mode
            memory.poke(025, (short) 000000);
            memory.poke(027, (short) 000654);
            memory.poke(030, (short) 000027); // indirect pointer -> 027
        } catch (MemoryFault mf) {
            appendPrinter("IPL load fault: " + mf.getMessage());
        }
    }

    private short encodeLS(int opcodeOct, int r, int ix, int iBit, int addrOct) {
        int word = ((opcodeOct & 0x3F) << 10)
            | ((r & 0x3) << 8)
            | ((ix & 0x3) << 6)
            | ((iBit & 0x1) << 5)
            | (addrOct & 0x1F);
        return (short) (word & 0xFFFF);
    }

    private void appendPrinter(String msg) {
        if (printerArea == null) {
            return;
        }
        printerArea.append(msg + "\n");
        printerArea.setCaretPosition(printerArea.getDocument().getLength());
    }

    private static String formatBinary(int value) {
        String b = Integer.toBinaryString(value & 0xFFFF);
        return "0".repeat(Math.max(0, WORD_BITS - b.length())) + b;
    }

    private static String formatOctal(int value, int width) {
        String o = Integer.toOctalString(value & 0xFFFF);
        return "0".repeat(Math.max(0, width - o.length())) + o;
    }

    private static GridBagConstraints baseGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        return gbc;
    }

    private static JPanel borderedPanel(String title) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(title));
        return panel;
    }

    private static void setSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Default look and feel fallback.
        }
    }
}
