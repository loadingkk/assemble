# CSCI 6461 Computer Systems Architecture Final Project

- Group 8: Zhentao Fan and Aarifah Ullah
- Prof. Morris Lancaster

**Objective: Design a CISC computer**

<p> Repository: https://github.com/loadingkk/assemble </p>

## Part 0: Assembler

<p>The assembler is found under: ./cisc/assembler</p>

### Overall Design

<p> The assembler takes an input source program and generates two output documents: a listing file for humans to read and any errors and a load file for the simulator. The load file contains octal addresses with the octal word per line. The assu=embler is also two-pass which separates building the symbol table and outputting machine words. Two-pass assemblers are the typical choice over one-pass assemblers.</p>

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

<p> The basic machine architecture consists of the CPU, memory, and registers. Machine.java includes CPU and memory objects from CPU.java and Memory.java respectively. You can gain access to the machine using: </p>

```
Machine machine = new Machine();
machine.resetAll(); // Initialize memory to 0

CPU cpu = machine.cpu()
Memory memory = machine.memory()
Registers r = cpu.R()
```

<p>Additionally you can read and write into registers.</p>

#### Simple Memory

### Notes and Documentation

<p>You can test the basic machine without the UI using SimMain.java which shows how CPU, Memory and Registers all come together to form the basic machine. </p>

** Usage **

<p> To test the Basic Machine Architecture & Simple Memory without Load/Store & UI: </p>

```
rm -rf build
mkdir build
javac -d build cisc/**/*.java
java -cp build cisc.sim.SimMain
```

<p>API</p>

Machine
- resetAll()
- cpu()
- memory()

CPU
- fetch()
- readWord(int addr)
- writeWord(int addr, short value)
- Registers R()

Registers
- getR(int r) / setR(int r, short val)
- getX(int x) / setX(int x, short val)
- getPC() / setPC(int val)
- getIR(), getMAR(), getMBR(), getCC(), getMFR()

Memory
- peek(int addr) ---> for UI only
- poke(int addr, short val) ----> for loader only


## Part 2: Memory and Cache Design

<p>TODO</p>

### Overall Design

### Notes and Documentation

## Part 3: Execute all Instructions

<p>TODO</p>

### Overall Design

### Notes and Documentation

## Part 4: Floating Point and Vector Operations OR Enhanced Scheduling

<p>TODO</p>

### Overall Design

### Notes and Documentation