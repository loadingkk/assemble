# GUI and Program 1 Instructions

This document explains:
1. How to use the simulator GUI.
2. How to run Part 2 Program 1 correctly.

## 0. Directory/File Map for Packaging

When packaging JAR + project files together, use this quick map:

- Assembler:
  - Source: `cisc/assembler/Assembler6461.java`
  - JAR: `cisc/assembler/Assembler6461.jar`
  - Typical assembler input/output (in same folder when running assembler):
    - `source.src`
    - `output.load`
    - `output.lst`

- Simulator:
  - Sources: `cisc/sim/` (including `machine/`, `memory/`, `cache/`)
  - Main GUI entry: `cisc/sim/SimGuiMain.java`
  - Main console test entry: `cisc/sim/SimMain.java`
  - Existing simulator JAR in repo root: `CSCI6461-Part2-Simulator.jar`

- Program 1 (Part 2):
  - Source: `programs/part2/program1/source.src`
  - Machine code files:
    - `programs/part2/program1/output.load`
    - `programs/part2/program1/output.lst`

## 1. Build and Launch

run simulator 
java -jar CSCI6461-Part2-Simulator.jar

Run these commands from your project root directory (the folder that contains `cisc/` and `programs/`):
`<PROJECT_ROOT>`

```bash
rm -rf build
mkdir build
javac -d build cisc/sim/*.java cisc/sim/machine/*.java cisc/sim/memory/*.java cisc/sim/cache/*.java
java -cp build cisc.sim.SimGuiMain
```

## 2. GUI Panel Guide

### Registers
- Shows GPR/IXR/control registers.
- Use `LD` + `Load` to manually place front-panel input into a selected register.

### Binary / Octal Input
- 16-bit toggle row + text input fields.
- `Octal Input` accepts octal values for front-panel load/store actions.

### Controls
- `Step`: execute one instruction.
- `Run`: execute continuously until `HLT` (or safety step limit).
- `Halt`: stop execution.
- `IPL`: reset and load the built-in Part I demo program.
- `Load/Load+/Store/Store+`: manual front-panel operations.

### Program File
- Used to load external `.load` files into memory.
- `Load .load` resets machine state, loads words, and sets `PC` to the first loaded address.

### Cache Content
- `Stats`: cache hit (`H`) and miss (`M`) counters.
- `L0..L15`: cache lines currently holding valid entries.
- Additional rows such as `LOAD/EXEC/WRITE` show recent memory access trace.

### Printer
- Shows trace logs and printer device output.
- Program output lines are prefixed with `PRINTER >`.

### Console Input
- Enter numeric values and click `Send`.
- Values are queued into the keyboard device buffer used by `IN`/`CHK`.

## 3. Program 1 Purpose

Program 1 (Part 2) performs:
1. Read one target integer from keyboard device.
2. Read 20 candidate integers from keyboard device.
3. Echo each candidate to printer.
4. Compute and print:
   - target value
   - closest candidate value to target
5. Halt.

## 4. Program 1 Run Steps (Required Order)

### Step A: Load Program 1 machine code
In GUI, in `Program File`, paste this **absolute path**:

`<PROJECT_ROOT>/programs/part2/program1/output.load`

Then click `Load .load`.

Expected log lines include:
- `Program loaded: ... words from .../programs/part2/program1/output.load`
- `PC set to 0000`

### Step B: Queue inputs in exact sequence
Click `Send` after each value.

1. First value = target.
2. Next 20 values = candidates.

Total values to send: **21**.

### Step C: Execute
Click `Run` once.

Expected completion log:
- `Run: halted after steps=..., last=HLT`

### Step D: Verify output
In `Printer`, you should see:
1. 20 echoed candidate values.
2. Final two lines:
   - target value
   - closest value

## 5. Example Test

Input order:
- Target: `56`
- 20 candidates: `1, 12, 23, 34, 45, 48, 55, 59, 64, 73, 89, 99, 102, 119, -10, 230, 91, 86, 5, 8`

Expected final two printer values:
- `56`
- `55`

## 6. Common Issues

1. `File not found` when loading program:
- Use an absolute path based on your own local `<PROJECT_ROOT>`.

2. Program keeps waiting at `CHK`:
- Queue missing inputs in `Console Input`.

3. Program does not restart after finish:
- Reload the `.load` file, then queue a new set of 21 inputs.

4. Cache panel seems crowded:
- Use mouse wheel to scroll in `Cache Content`.
