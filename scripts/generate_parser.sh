#!/bin/bash

set -e

echo "========================================"
echo "   MiniPython Parser Generator"
echo "========================================"
echo ""

# Check Java
if ! command -v java &> /dev/null; then
    echo "Error: Java not found. Install JDK 11+."
    exit 1
fi

# Check grammar file
if [ ! -f "grammar/minipython.grammar" ]; then
    echo "Error: grammar/minipython.grammar not found."
    exit 1
fi

# Ensure lib directory exists
mkdir -p lib

# Download SableCC if missing
if [ ! -f "lib/sablecc.jar" ]; then
    echo "SableCC not found. Downloading..."

    if command -v wget &> /dev/null; then
        wget -q https://www.sablecc.org/download/sablecc-3.7.zip
    elif command -v curl &> /dev/null; then
        curl -L -o sablecc-3.7.zip https://www.sablecc.org/download/sablecc-3.7.zip
    else
        echo "Error: Install wget or curl."
        exit 1
    fi

    unzip -q sablecc-3.7.zip
    cp sablecc-3.7/lib/sablecc.jar lib/
    rm -rf sablecc-3.7 sablecc-3.7.zip

    echo "SableCC downloaded successfully."
fi

# Remove old generated code
rm -rf src/minipython src/parser src/node src/analysis src/lexer 2>/dev/null || true

echo "Generating parser..."
java -jar lib/sablecc.jar grammar/minipython.grammar -d src/

echo ""
echo "Parser generation completed."
echo ""