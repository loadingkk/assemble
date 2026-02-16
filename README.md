# CSCI 6461 Computer Systems Architecture Final Project

- **Group 8**: Zhentao Fan and Aarifah Ullah
- Prof. Morris Lancaster

<p>Objective: Design a CISC computer</p>

## Part 0: Assembler

### Overall Design

<p> The assembler takes an input source program and generates two output documents: a listing file for humans to read and any errors and a load file for the simulator. The load file contains octal addresses with the octal word per line. The assu=embler is also two-pass which separates building the symbol table and outputting machine words. Two-pass assemblers are the typical choice over one-pass assemblers.</p>

### Notes and Documentation

<p>*What's included as part of submission:* <p>

- Assembler source code that generates JAR file: *Assembler6461.java*
- JAR file already built: *Assembler6461.jar*
- A test input: *source.src*
- Example List and Load outputs: *output.lst* and *output.load*
- IntelliJ artifacts: ./.idea & ./META-INF & ./out directories

**Usage**

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

<p>TODO</p>

### Overall Design

### Notes and Documentation

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