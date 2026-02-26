#!/bin/bash

set -e

echo "========================================"
echo "   MiniPython Compiler Build"
echo "========================================"
echo ""

# Check javac
if ! command -v javac &> /dev/null; then
    echo "Error: javac not found. Install JDK 11+."
    exit 1
fi

# Generate parser if not present
if [ ! -d "src/minipython" ]; then
    echo "Generated parser not found. Running generator..."
    ./scripts/generate_parser.sh
fi

# Clean previous build
rm -rf build
mkdir -p build

echo "Compiling Java sources..."
javac -g -d build $(find src -name "*.java")

echo ""
echo "Compilation successful."
echo "Class files located in /build"
echo ""