# Running Guide – MiniPython Compiler

This document explains how to build, run, and test the MiniPython compiler.

---

## Prerequisites

Make sure you have:

- Java JDK 11 or higher
- Bash shell (Linux / macOS / WSL on Windows)
- SableCC (optional – downloaded automatically by script)

Check Java installation:

```bash
java -version
javac -version
```

---

## 1. Generate the Parser

Before compiling, generate the parser from the grammar:

```bash
chmod +x scripts/generate_parser.sh
./scripts/generate_parser.sh
```

This will generate parser classes inside `src/minipython/`.

---

## 2. Compile the Compiler

After generating the parser:

```bash
chmod +x scripts/compile.sh
./scripts/compile.sh
```

Compiled `.class` files will appear in the `build/` directory.

---

## 3. Run the Test Suite

To run all test programs:

```bash
chmod +x scripts/run.sh
./scripts/run.sh
```

This will:

- Execute all test files in `tests/`
- Report pass/fail statistics

---

## Running a Single Test File

You can manually test a specific file:

```bash
java -cp build ParserTest path/to/file.py
```

Example:

```bash
java -cp build ParserTest tests/example.py
```

---

## Common Issues

### `generate_parser.sh` fails

- Ensure Java is installed
- Ensure `grammar/minipython.grammar` exists
- Install wget or curl

### Compilation fails

- Ensure parser has been generated
- Ensure Java version is 11+
- Check for syntax errors in grammar or Java files

### Tests do not run

- Ensure compilation succeeded
- Ensure test files exist in `tests/`

---

## Expected Output

After running tests, you should see something similar to:

```
Tests passed: 12 / 12
```