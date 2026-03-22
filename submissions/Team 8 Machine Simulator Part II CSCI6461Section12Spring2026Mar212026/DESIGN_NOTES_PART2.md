# Part II Design Notes

## 1. Scope

Part II implementation in this project covers:
1. Cache design and integration with CPU memory path.
2. Extended simulator execution support needed by Program 1 (`CHK`, `IN`, `OUT`).
3. Program 1 implementation and machine-code generation.
4. GUI extensions for program loading, console input, printer output, and cache visibility.

## 2. Cache Design

### 2.1 Structure
- Cache is implemented in `cisc/sim/cache/Cache.java`.
- Number of lines: 16.
- Line fields:
  - `valid`
  - `tag` (stored as effective memory address)
  - `data` (16-bit word)
- Public line snapshot type: `cisc/sim/cache/CacheLine.java`.

### 2.2 Mapping and Replacement
- Mapping policy: fully associative (any memory address can be stored in any cache line).
- Replacement policy: FIFO (first in, first out) when cache is full.
- Fill behavior:
  - Use first invalid line if available.
  - Otherwise evict by FIFO pointer.

### 2.3 Write Strategy
- Write-through policy:
  - All writes update memory immediately.
  - Cache line is updated if present; otherwise inserted via fill path.

### 2.4 Statistics
- Cache tracks cumulative counters:
  - `hitCount`
  - `missCount`
- Snapshot API for GUI:
  - `getHitCount()`
  - `getMissCount()`
  - `snapshot()`

## 3. CPU Integration

### 3.1 Memory Access Path
CPU (`cisc/sim/machine/CPU.java`) routes these operations through cache:
- Instruction fetch (`fetch`)
- Data read (`readWord`)
- Data write (`writeWord`)

This keeps cache visible in both instruction and data traffic.

### 3.2 Added Device I/O Instructions
Program 1 requires basic I/O instruction execution:
- `IN` (opcode 061)
- `OUT` (opcode 062)
- `CHK` (opcode 063)

Device model in CPU:
- Device 0: console keyboard queue (`keyboardQueue`)
- Device 1: console printer buffer (`printerOutput`)

CPU helper APIs exposed to GUI:
- `pushKeyboardInput(short value)`
- `keyboardQueueDepth()`
- `peekKeyboardInput()`
- `drainPrinterOutput()`

## 4. Program 1 Design

### 4.1 Files
- Source: `programs/part2/program1/source.src`
- Load file: `programs/part2/program1/output.load`
- Listing file: `programs/part2/program1/output.lst`

### 4.2 Behavior
Program 1 performs:
1. Read target integer from keyboard device.
2. Read 20 candidate integers.
3. Echo candidates to printer.
4. Compute closest candidate to target (minimum absolute difference).
5. Print target and closest value.
6. Halt.

### 4.3 Addressing Strategy
Because instruction address field is 5-bit:
- Data block is placed at high memory (`LOC 64`, octal `0100`).
- `X1` holds base pointer to the data block.
- Data fields are accessed using indexed addressing with small offsets.

## 5. GUI Extension Design

GUI entry: `cisc/sim/SimGuiMain.java`

Implemented extensions:
1. Program file loader (`Load .load`) that resets machine, loads words, sets `PC`.
2. Console input queueing to CPU keyboard device.
3. Printer output flushing from CPU printer buffer (`PRINTER > ...`).
4. Cache content panel:
   - hit/miss stats
   - valid cache lines
   - recent memory access trace
5. Scrollable cache panel for stable display on smaller window sizes.

## 6. Validation

### 6.1 Build
```bash
javac -d build cisc/sim/*.java cisc/sim/machine/*.java cisc/sim/memory/*.java cisc/sim/cache/*.java
```

### 6.2 Smoke Test
```bash
java -cp build cisc.sim.SimMain
```
Expected final message:
- `Part 1/2 simulator smoke tests passed.`

### 6.3 GUI Demo
1. Launch GUI.
2. Load Program 1 `.load` file.
3. Queue one target value and 20 candidates.
4. Run program.
5. Verify printer output ends with target and closest value.

## 7. Deliverables Summary

Part II-relevant deliverables in this repository:
- Simulator source under `cisc/sim/` (with cache + UI extensions).
- Assembler source under `cisc/assembler/`.
- Program 1 source and machine code under `programs/part2/program1/`.
- User instruction document: `GUI_AND_PROGRAM1_INSTRUCTIONS.md`.
- These design notes: `DESIGN_NOTES_PART2.md`.
