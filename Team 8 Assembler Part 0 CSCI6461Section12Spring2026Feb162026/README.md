# CSCI 6461 Computer Systems Architecture Final Project

- Group 8: Zhentao Fan and Aarifah Ullah
- Prof. Morris Lancaster

**Objective: Design a CISC computer**

## Part 0: Assembler

Repository: https://github.com/loadingkk/assemble

### Overall Design

<p> The assembler takes an input source program and generates two output documents: a listing file for humans to read and any errors and a load file for the simulator. The load file contains octal addresses with the octal word per line. The assu=embler is also two-pass which separates building the symbol table and outputting machine words. Two-pass assemblers are the typical choice over one-pass assemblers.</p>

### Notes and Documentation

<p>*What's included as part of submission:* <p>

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