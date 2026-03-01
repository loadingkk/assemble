# Part I Design Notes

## Overview

Part I implements the basic CSCI 6461 machine architecture, simple memory, the initial operator-style GUI, and the Load/Store instruction subset required for the first deliverable.

## Machine Structure

The simulator is organized into three main parts:

- `Machine`: wraps the CPU and memory into one object.
- `CPU`: manages registers, fetch, effective address calculation, and Part I instruction execution.
- `Memory`: implements a 2048-word memory with a 2-cycle access model.

This keeps the GUI separate from the machine logic. The GUI reads machine state and triggers actions, while the CPU and memory classes handle execution.

## Registers

Implemented registers:

- `R0` to `R3` (16-bit general purpose registers)
- `X1` to `X3` (16-bit index registers)
- `PC` (12-bit)
- `MAR` (12-bit)
- `MBR` (16-bit)
- `IR` (16-bit)
- `CC` (4-bit)
- `MFR` (4-bit)

Register widths are masked in the `Registers` class to match the ISA.

## Memory Model

Memory is word-addressable and contains 2048 words.

The memory model uses two phases:

1. Cycle 1: latch the address from `MAR`
2. Cycle 2: perform the read or write through `MBR`

For execution, the simulator uses `CPU.readWord()` and `CPU.writeWord()` so the access follows the timing model.

## Part I Instruction Support

Implemented instruction subset:

- `HLT`
- `LDR`
- `STR`
- `LDA`
- `LDX`
- `STX`

The CPU decodes the 16-bit instruction word into:

- opcode
- register field
- index field
- indirect bit
- address field

Effective address calculation supports:

- direct addressing
- indexed addressing
- indirect addressing

## GUI Design

The GUI is implemented in `SimGuiMain` and acts as a front panel:

- register display and manual register selection
- binary and octal input
- `Load`, `Load+`, `Store`, `Store+`
- `Run`, `Step`, `Halt`, `IPL`
- `MEM[MAR]` display
- right-side observation panel for the Part I demo program and key memory locations

The GUI is meant to support both:

- manual front-panel interaction
- instruction-by-instruction observation during execution

## IPL Demo Program

Pressing `IPL` resets the machine and preloads a short demo program beginning at octal address `0010`.

This program is used to demonstrate:

- basic Load/Store behavior
- indexed addressing
- indirect addressing
- step-by-step execution through the GUI

## Testing

`SimMain` provides a simple Part I test harness that checks:

- memory reset
- effective address calculation
- Load/Store instruction execution
- halt behavior
- illegal opcode handling through `MFR`
