# Part III Design Notes

## 1. Scope

Part III in this repository focuses on:

1. Correct machine-fault handling through `MFR`.
2. Proper `TRAP` save/vector behavior.
3. File-backed Program 2 execution through the GUI card-reader flow.
4. Program 2 source, machine code, and test data.

## 2. CPU / Fault Handling

`cisc/sim/machine/CPU.java` now enforces the Part III execution rules:

- Reserved memory (`0` to `5`) cannot be accessed by user programs.
- Machine faults save the current `PC` to reserved location `4`.
- Machine faults vector to the handler address stored at reserved location `1`.
- `TRAP` saves the current `PC` to reserved location `2`.
- `TRAP` vectors through the trap-table base stored at reserved location `0`.

Default reset state:

- `memory[0] = 020` (octal trap-table base)
- `memory[1] = 006` (octal machine-fault handler)
- `memory[6] = HLT`
- trap table entries default to `006`

## 3. Part III Device Flow

The simulator now supports:

- Device `0`: console keyboard queue
- Device `1`: console printer
- Device `2`: card reader queue

GUI support added for Part III:

- Console text input queues uppercase characters plus newline for Program 2.
- Card-reader file loading preserves uppercase paragraph characters, punctuation, and line breaks for Program 2.
- Printer panel supports `Numeric` and `Text` render modes.

## 4. Program 2 Design

Program 2 is located at:

- `programs/part3/program2/source.src`
- `programs/part3/program2/output.load`
- `programs/part3/program2/output.lst`
- `programs/part3/program2/paragraph.txt`

The program uses three built-in trap services:

1. `TRAP 4`: load the paragraph from the card-reader file, print it, and build searchable sentence/word metadata.
2. `TRAP 5`: read the user query word from the keyboard queue.
3. `TRAP 6`: search the stored words and print `WORD SENTENCE WORD`.

Program 2 parsing is dynamic:

- Sentence boundaries are detected by `.`, `!`, and `?`.
- Word boundaries are detected from actual non-alphanumeric separators.
- Sentence lengths are not fixed.
- The search result reports the real sentence number and word number from the paragraph.

For the bundled test data, the expected successful search example is:

- query word: `STARS`
- output result line: `STARS 5 1`

## 5. Validation

Build and run smoke tests:

```bash
rm -rf build
mkdir build
javac -d build cisc/sim/*.java cisc/sim/machine/*.java cisc/sim/memory/*.java cisc/sim/cache/*.java
java -cp build cisc.sim.SimMain
```

Expected final line:

- `Part 1/2/3 simulator smoke tests passed.`
