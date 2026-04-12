# Part III GUI and Program 2 Instructions

## Build and Launch

If you are using the packaged submission folder, `cd` into the folder:

`Team 8 Machine Simulator Part III CSCI6461Section12Spring2026Apr122026`

Then run the packaged simulator jar:

```bash
java -jar CSCI6461-Part3-Simulator.jar
```

If you are using the full project repository root instead, build from source with:

```bash
rm -rf build
mkdir build
javac -d build cisc/sim/*.java cisc/sim/machine/*.java cisc/sim/memory/*.java cisc/sim/cache/*.java
java -cp build cisc.sim.SimGuiMain
```

If you want to build from source inside the packaged submission folder, use:

```bash
rm -rf build
mkdir build
javac -d build "Part III"/cisc/sim/*.java "Part III"/cisc/sim/machine/*.java "Part III"/cisc/sim/memory/*.java "Part III"/cisc/sim/cache/*.java
java -cp build cisc.sim.SimGuiMain
```

## Files

If running from the packaged submission folder:

- Program file: `Part III/programs/part3/program2/output.load`
- Test paragraph file: `Part III/programs/part3/program2/paragraph.txt`

If running from the repository root:

- Program file: `programs/part3/program2/output.load`
- Test paragraph file: `programs/part3/program2/paragraph.txt`

## Program 2 Demo Flow

1. Build and launch the simulator GUI.
2. In the `Program File` panel, set:
   - packaged submission: `Part III/programs/part3/program2/output.load`
   - repository root: `programs/part3/program2/output.load`
3. Press `Load .load`.
4. In the `Printer` panel, switch render mode to `Text`.
5. In the `Card Reader File` panel, set:
   - packaged submission: `Part III/programs/part3/program2/paragraph.txt`
   - repository root: `programs/part3/program2/paragraph.txt`
6. Press `Load File`.
7. In the `Console Input` panel, type an uppercase query word from the paragraph:
   - example: `STARS`
8. Press `Send`.
9. Press `Run`.

Expected log lines before execution:

- `Program loaded: ...output.load`
- `PC set to 0040`
- `Card reader loaded ... chars from ...paragraph.txt`
- `Console text queued > STARS`

## Expected Output

The printer panel should show the six sentences reconstructed from the file, followed by the search result line:

```text
PRINTER > BLUE BIRDS FLY HIGH TODAY.
PRINTER > TALL TREES HOLD QUIET NESTS.
PRINTER > SOFT RAIN FALLS OVER BRIGHT FIELDS.
PRINTER > WARM WINDS MOVE ACROSS THE DUNES.
PRINTER > STARS GLOW OVER THE LAKE TONIGHT.
PRINTER > TEAMS BUILD THE FINAL DEMO TOGETHER.
PRINTER > STARS 5 1
```

## Notes

- Console text input is automatically uppercased for Program 2.
- Card-reader file loading preserves punctuation and line breaks so Program 2 can detect sentence boundaries dynamically.
- The bundled paragraph file intentionally uses different word counts per sentence to validate dynamic parsing.
- If the printer shows numbers instead of text, switch the printer panel back to `Text` mode before running Program 2.
