# MiniPython Compiler

A custom compiler for a subset of Python (MiniPython) built using SableCC.

This project implements the core phases of a compiler including:

- Lexer & Parser generation from a formal grammar
- Abstract Syntax Tree (AST) construction
- Semantic analysis and type checking
- Test suite with  example programs
- Build and run scripts

This repository was developed as part of the *Compilers* course and is suitable as a demonstration of language processing, AST design, and static analysis skills.

---

##  Project Overview

The compiler supports:

- Parsing MiniPython code based on a custom grammar
- Semantic analysis including type checking and scope validation
- Error reporting for invalid test files
- Modular design with clear separation between generated and handwritten components

---

## Project Structure

```

minipython-compiler/
│
├── src/
│ ├── ASTPrinter.java
│ ├── DeclarationVisitor.java
│ ├── TypeCheckerVisitor.java
│ ├── VariableDeclarationVisitor.java
│ ├── ASTTest1.java
│ ├── LexerTest1.java
│ └── ParserTest.java
│
├── grammar/
│ └── minipython.grammar
│
├── tests/
│
├── scripts/
│ ├── generate_parser.sh
│ ├── compile.sh
│ ├── run.sh
│ └── sablecc.bat
│
├── .gitignore
├── LICENSE
├── README.md
└── RUNNING.md

```
---

##  Key Concepts Demonstrated

This project showcases:

 Formal grammar design with SableCC  
 Lexer and parser generation  
 AST generation and visitor pattern  
 Semantic analysis and type checking  
 Batch testing framework  
 Shell script automation of build/test pipeline

---

##  Development & Build Tools

- Java (JDK 11 or higher)
- SableCC (version 3.7)
- Bash / Shell scripting

---

##  Dependencies

External tools required:

| Tool         | Version      |
|--------------|--------------|
| Java JDK     | 11+          |
| SableCC      | 3.7          |
| Bash Shell   | Any POSIX-compatible |

(Note: The repository contains a placeholder for `sablecc.jar`, but it is recommended to install it manually or via the build scripts.)

---

##  Usage Summary

This compiler can:

- Generate a parser from a grammar file
- Compile source code using Java
- Run test files and report pass/fail
- Distinguish valid from invalid MiniPython programs

For detailed instructions, see `RUNNING.md`.

---

##  License

This project is licensed under the **MIT License** — see the `LICENSE` file for details.

---

##  Authors

- Anastasios Nektarios Loukas
- Panagiotis Papageorgiou
- Konstantina Karapetsa
- Foteini Sotiropoulou
