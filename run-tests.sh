#!/usr/bin/env bash
# ============================================================
# DRS-Initial - JUnit Test Suite Runner (macOS / Linux)
#
# Run from a terminal:  ./run-tests.sh
# Expected outcome: Tests run: 95, Failures: 0, Errors: 0
#
# Author: Fabian Roberto Guzman (12287570)
# Unit:   COIT20258 Software Engineering - Assessment 2
# ============================================================
set -euo pipefail

cd "$(dirname "$0")"

echo "============================================================"
echo " DRS-Initial - JUnit Test Suite"
echo " Fabian Roberto Guzman (12287570) - COIT20258 Assessment 2"
echo "============================================================"
echo

if ! command -v mvn >/dev/null 2>&1; then
    echo "[ERROR] Maven is not installed or not on PATH."
    echo "Install via your package manager (e.g. brew install maven, apt install maven)."
    exit 1
fi

echo "[INFO] Running the JUnit test suite..."
echo "[INFO] 14 *Spec.java classes, 83 @Test + 2 @ParameterizedTest = 95 executions"
echo
mvn clean test

echo
echo "============================================================"
echo " Test run complete."
echo "============================================================"
