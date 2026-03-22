# CSCI 6461 Computer Systems Architecture Final Project

- Group 8: Zhentao Fan and Aarifah Ullah
- Prof. Morris Lancaster

**Objective: Design a CISC computer**

<p> Repository: https://github.com/loadingkk/assemble </p>

## Part 0: Assembler

<p>The assembler is found under: ./cisc/assembler</p>

### Overall Design

<p> The assembler takes an input source program and generates two output documents: a listing file for humans to read and any errors and a load file for the simulator. The load file contains octal addresses with the octal word per line. The assembler is also two-pass which separates building the symbol table and outputting machine words. Two-pass assemblers are the typical choice over one-pass assemblers.</p>

### Notes and Documentation

*What's included as part of submission:*

- Assembler6461.java *Assembler source code that generates JAR file*
- Assembler6461.jar *Already compiled JAR file*
- source.src *A test input*
- output.lst & output.load *Generated List and Load outputs*
- IntelliJ artifacts: 
    - ./.idea
    - ./META-INF
    - ./out
    - p.iml
    - p.jar

**Usage**
<p>The assembler expects that all input test files are called source.src and will always deliver the list and load files as output.lst and output.load respectively. The source file must be in the same path as the JAR and the list and load will appear in the same directory too. Place Assembler6461.jar and source.src in the same folder; running the JAR generates output.lst and output.load in that folder.</p>

<p>Run the assembler:</p>

```
java -jar Assembler6461.jar
```

<p>Rebuild JAR file:</p>

```
rm -rf build
mkdir build
javac -d build Assembler6461.java
jar cfe Assembler6461.jar Assembler6461 -C build .
```

## Part 1: Basic Machine

<p>The basic machine and UI is found under: ./cisc/sim</p>

### Overall Design

#### Basic Machine Architecture

<p> The basic machine architecture consists of the CPU, memory, and registers. These are treated as packages and classes that get incorporated together. Machine.java includes CPU and memory objects from CPU.java and Memory.java respectively. Memory and all registers start at 0 upon initialization, You can gain access to the machine using: </p>

```
Machine machine = new Machine();
machine.resetAll(); // Initialize memory to 0

CPU cpu = machine.cpu()
Memory memory = machine.memory()
Registers r = cpu.R()
```
*Registers*

Implemented Registers:

- GPR0–GPR3: 16-bit general purpose registers
- IXR1–IXR3: 16-bit index registers (no X0; IX field=0 means “no indexing”)
- PC: 12 bits (next instruction address)
- CC: 4 bits condition codes
- IR: 16 bits
- MAR: 12 bits
- MBR: 16 bits
- MFR: 4 bits

<p>Registers can be read and written to: </p>

```
short value = r.getR(0)  // get Register R0
r.setR(2, (short) 1212); // set Register R2

short xVal = r.getX(2)   // getRegister X1
r.setPC(100);            // PC in 12-bit mask
```

#### Simple Memory

<p>. Memory is word-addressable. There are 2048 words and each word size is 16-bits. There are 2 cycles for memory. In cycle 1, memory takes an address from MAR. In cycle 2, there is a data transfer, a read/write operation. Either memory reads into MBR and stores to memory[MAR] or writes from MBR and loads memory[MAR] to MBR.</p>

### Notes and Documentation

*What's included as part of submission:*

- ./cisc/sim *Directory for UI, Machine and Memory*
- ./cisc/sim/SimMain.java *Part I tests for architecture, EA, and Load/Store execution*
- ./cisc/sim/SimGuiMain.java *Operator console style GUI for Part I*

<p>You can test the basic machine without the UI using SimMain.java which shows how CPU, Memory and Registers all come together to form the basic machine. </p>

*Usage*

<p> To test the Basic Machine Architecture + Load/Store execution (Part I): </p>

```
rm -rf build
mkdir build
javac -d build cisc/**/*.java
java -cp build cisc.sim.SimMain
```

<p> To run the Part I GUI: </p>

```
rm -rf build
mkdir build
javac -d build cisc/**/*.java
java -cp build cisc.sim.SimGuiMain
```

<p>Part I GUI usage:</p>

- Start the GUI and press `IPL` to preload the short Part I demo program into memory. The demo is not loaded until `IPL` is pressed.
- The demo program is loaded beginning at octal address `0010` and includes `LDA`, `STR`, `LDR`, `LDX`, `STX`, plus one indexed `LDR`, one indirect `LDR`, and a final `HLT`.
- The preloaded instruction sequence is:
  - `0010: LDA R0,0,22`
  - `0011: STR R0,0,23`
  - `0012: LDR R1,0,23`
  - `0013: LDX X1,24`
  - `0014: STX X1,25`
  - `0015: LDR R2,1,20`
  - `0016: LDR R3,0,30,1`
  - `0017: HLT`
- The right-side panel includes cache view, printer output, and console input controls used by later parts; the Part I demo can still be stepped/run from the same controls.
- Press `Step` to execute one instruction at a time. Each step fetches the next instruction from `PC`, executes it, updates the register display, and prints a trace line to the `Printer` panel.
- Press `Run` to continue stepping until `HLT` is reached or the machine is halted.
- Press `Halt` to pause the machine immediately. Press `IPL` again to reset and reload the demo program.
- The `MEM[MAR]` display shows the contents of the memory location currently addressed by `MAR`.
- The `Load`, `Load+`, `Store`, and `Store+` buttons simulate front-panel register loading and storing:
  - Select a target register by clicking its `LD` button.
  - Enter a value in the `Octal Input` or toggle the 16-bit binary switches.
  - `Load` copies the current front-panel word into the selected register.
  - `Load+` does the same and then increments `MAR`.
  - `Store` writes the selected register value to memory at `MAR`.
  - `Store+` writes the selected register value to memory at `MAR` and then increments `MAR`.
- `Program File` can load an external `.load` file (used in Part II/III flows); Part I itself still uses the built-in IPL demo program.

*Basic Machine & Memory API*

Machine
- resetAll()
- cpu()
- memory()

CPU
- fetch()
- readWord(int addr)
- writeWord(int addr, short value)
- computeEA(int ix, int iBit, int addr5)
- executeLoadStoreStep()
- isHalted()
- halt()
- Registers R()

Registers
- getR(int r) / setR(int r, short val)
- getX(int x) / setX(int x, short val)
- getPC() / setPC(int val)
- getIR(), getMAR(), getMBR(), getCC(), getMFR()

Memory
- peek(int addr) ---> for UI only
- poke(int addr, short val) ----> for loader only

*Note* Instruction execution must use CPU.readWord() and CPU.writeWord() instead of Memory.peek() and Memory.poke() in order to maintain the MAR/MBR timing model and 2-cycle memory properties.

## Part 2: Memory and Cache Design

<p>Part 2 in this repository includes cache implementation, extended instruction execution (including basic I/O), and Program 1 + UI integration support.</p>

### Overall Design

#### Program 1

- Program 1 source file path: `./programs/part2/program1/source.src`
- Program 1 machine code files:
  - `./programs/part2/program1/output.load`
  - `./programs/part2/program1/output.lst`
- File roles:
  - `source.src`: assembly source code authored by the team.
  - `output.load`: machine-code load file used by simulator execution.
  - `output.lst`: assembler listing file (address + machine word + source line) for verification/debugging.
- Recommended workflow:
  1. Run assembler on `source.src` to generate `output.load` and `output.lst`.
  2. Start simulator GUI and load `output.load` from Program File.
  3. Queue inputs and run Program 1.
- GUI test input order for current Program 1 build:
  - queue the target integer first
  - then queue 20 candidate integers
- Current Program 1 behavior:
  - Uses `CHK/IN/OUT` device flow to read integer inputs and print to console printer.
  - Tracks closest value to a target across 20 input integers.
  - Prints the target and final closest value at program end.
- The source assembles successfully with the current assembler and can be loaded through the GUI `.load` workflow.

#### Cache + UI Extension

Part 2 now includes a cache layer and GUI integration:

- Cache implementation (`./cisc/sim/cache`):
  - 16-line fully-associative unified cache.
  - FIFO replacement policy.
  - Write-through behavior.
  - Cache hit/miss counters and line snapshot support for GUI.

- CPU-memory path:
  - Instruction fetch and `readWord`/`writeWord` go through cache.
  - Added I/O instruction handling for `IN`, `OUT`, `CHK`.
  - Basic console keyboard/printer device model is provided in CPU.

`SimGuiMain` extension:

- Program file panel:
  - Added real `.load` file loader.
  - Loads octal `<address word>` entries into memory.
  - Sets `PC` to first loaded address.
- Console input panel:
  - Inputs are queued into CPU keyboard device buffer.
  - Queue depth/head are shown in UI.
  - `IN` consumes queued values; `CHK` reflects keyboard readiness.
- Printer panel:
  - Added `Clear` action for operator workflow.
  - `OUT` output is displayed in the printer area (`PRINTER > ...`).
- Cache content panel:
  - Shows cache hit/miss stats from CPU/cache.
  - Shows valid cache lines (line id, tag/address, data) and recent memory access trace.

### Notes and Documentation

- Part 2 UI implementation file: `./cisc/sim/SimGuiMain.java`
- Program 1 source file: `./programs/part2/program1/source.src`
- Part 2 cache implementation files:
  - `./cisc/sim/cache/Cache.java`
  - `./cisc/sim/cache/CacheLine.java`
- Detailed GUI + Program 1 usage guide:
  - `./GUI_AND_PROGRAM1_INSTRUCTIONS.md`

### Part 2 Test Flow

1. Build simulator classes:
   - `javac -d build cisc/sim/*.java cisc/sim/machine/*.java cisc/sim/memory/*.java cisc/sim/cache/*.java`
2. Run smoke tests:
   - `java -cp build cisc.sim.SimMain`
   - Expected final line includes: `Part 1/2 simulator smoke tests passed.`
3. Run GUI:
   - `java -cp build cisc.sim.SimGuiMain`
4. Program 1 GUI demo:
   - Load `./programs/part2/program1/output.load`
   - Queue one target integer first, then queue 20 candidate integers
   - Press `Run`
   - Expected behavior:
     - The 20 candidate integers are printed
     - Final two printed values are `target` and `closest value`
     - Run ends with `last=HLT`

## Part 3: Execute all Instructions

<p>TODO</p>

### Overall Design

### Notes and Documentation

## Part 4: Floating Point and Vector Operations OR Enhanced Scheduling

<p>TODO</p>

### Overall Design

### Notes and Documentation
