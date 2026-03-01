# Part I GUI Instructions

This GUI simulates a simple front panel for the Part I machine.

## Basic Flow

1. Press `IPL` to preload the Part I demo program.
2. Use `Step` to execute one instruction at a time.
3. Use `Run` to continue until `HLT`.
4. Use `Halt` to pause the machine.

## Front Panel Input

- `Octal Input` is the current front-panel value.
- The 16-bit `BINARY` switches represent the same value in binary.
- Whatever is currently shown in the input fields is treated as the active input value.

## Manual Register Load

To load a value into a register:

1. Enter a value in `Octal Input` (example: `000123`).
2. Click the `LD` button next to the target register (example: `R0`).
3. Click `Load`.

Result: the selected register receives the current front-panel value.

## Manual Memory Store

To write a register value into memory:

1. Load a value into a register (example: `R0`).
2. Set `MAR` to the target address using the same `LD` + `Load` process.
3. Click the `LD` button next to the register you want to store.
4. Click `Store`.

Result: `MEM[MAR]` becomes the selected register value.

## Load+ / Store+

- `Load+`: performs `Load`, then increments `MAR`.
- `Store+`: performs `Store`, then increments `MAR`.

These are useful for repeated front-panel operations across consecutive addresses.

## Display Notes

- `PC` and `MAR` are shown as 4-digit octal values (12-bit registers).
- General registers, index registers, `IR`, `MBR`, and memory words are shown as 6-digit octal values (16-bit words).
- `CC` and `MFR` are shown as 4-bit binary values.
- `MEM[MAR]` shows the contents of the memory location currently addressed by `MAR`.
