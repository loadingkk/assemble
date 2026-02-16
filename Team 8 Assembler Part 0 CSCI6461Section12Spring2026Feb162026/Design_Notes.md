# Design Notes — Assembler6461

## Overview
This assembler is a two-pass assembler for the CSCI 6461 ISA. It reads a single input file, builds a symbol table in pass 1, and emits machine code in pass 2. The implementation is contained in a single Java file: [Assembler6461.java](Assembler6461.java).

## Inputs and Outputs
- **Input**: `source.src` (assembly source)
- **Outputs**:
  - `output.load`: machine words only (octal address + octal word)
  - `output.lst`: listing file (octal address + octal word + original source line)

## Supported Directives
- `LOC <decimal>`: set the location counter (does not allocate a word).
- `DATA <decimal | label>`: emit a word containing a constant or label address.

## Labels
- Labels are defined as `Label:` at the start of a line.
- Labels may appear on a line by themselves or before an instruction/directive.
- The assembler rejects duplicate labels.

## Pass 1 (Symbol Table + Locations)
- Scans the file, removes comments, and tokenizes each line.
- Assigns a location counter to any line that generates a word.
- Records label addresses in the symbol table.
- `LOC` updates the location counter and can bind a label to the new location.

## Pass 2 (Code Generation)
- Encodes `DATA` into a 16-bit word.
- Encodes instructions using the ISA formats:
  - **LS format**: opcode(6) R(2) IX(2) I(1) Address(5)
  - **RR format**: opcode(6) Rx(2) Ry(2) zeros(6)
  - **Shift/Rotate**: opcode(6) R(2) A/L(1) L/R(1) Count(4) zeros(2)
  - **I/O**: opcode(6) R(2) zeros(3) devid(5)
- Writes `output.load` and `output.lst` line-by-line.

## Addressing and Limits
- The address field is 5 bits (0–31). Larger memory references must use indexing + base address schemes.
- Registers are limited to 0–3 (2 bits).
- Immediate fields (like `AIR`, `SIR`, `RFS`, and I/O device IDs) are range-checked.

## Error Handling
- The assembler throws clear errors for:
  - Unknown opcodes
  - Wrong operand counts
  - Duplicate or unknown labels
  - Out-of-range fields

## Notes
- Comments start with `;` and are ignored.
- All numeric operands are parsed as decimal integers.
