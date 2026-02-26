#!/bin/bash

set -e

echo "========================================"
echo "   MiniPython Test Suite"
echo "========================================"
echo ""

# Ensure compiled
if [ ! -d "build" ]; then
    echo "Build not found. Running compilation..."
    ./scripts/compile.sh
fi

TOTAL=0
PASSED=0

run_test() {
    FILE=$1
    TYPE=$2

    echo "Testing: $FILE"

    if java -cp build ParserTest "$FILE" > output.tmp 2>&1; then
        RESULT=0
    else
        RESULT=1
    fi

    if [ "$TYPE" = "valid" ]; then
        if [ $RESULT -eq 0 ]; then
            echo "  PASS"
            ((PASSED++))
        else
            echo "  FAIL"
        fi
    else
        if [ $RESULT -ne 0 ]; then
            echo "  PASS"
            ((PASSED++))
        else
            echo "  FAIL"
        fi
    fi

    ((TOTAL++))
    echo ""
}

# Run valid tests
if [ -d "tests/valid" ]; then
    for f in tests/valid/*.py; do
        [ -e "$f" ] || continue
        run_test "$f" "valid"
    done
fi

# Run invalid tests
if [ -d "tests/invalid" ]; then
    for f in tests/invalid/*.py; do
        [ -e "$f" ] || continue
        run_test "$f" "invalid"
    done
fi

rm -f output.tmp

echo "========================================"
echo "Tests passed: $PASSED / $TOTAL"
echo "========================================"
echo ""
