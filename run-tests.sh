#!/usr/bin/env bash
# ============================================================
# DRS-Enhanced - JUnit Test Suite Runner (macOS / Linux)
#
# Run from a terminal:  ./run-tests.sh
# Expected outcome: all tests green - 244 run / 0 failures with a database
# (MySQL, or H2 via -Ptest-h2); 219 run / 7 skipped with no database, where
# the 7 skips are the DB-gated spec classes (skips, never failures)
#
# Author: Fabian Roberto Guzman (12287570)
# Unit:   COIT20258 Software Engineering - Assessment 3
# ============================================================
set -euo pipefail

cd "$(dirname "$0")"

echo "============================================================"
echo " DRS-Enhanced - JUnit Test Suite"
echo " Fabian Roberto Guzman (12287570) - COIT20258 Assessment 3"
echo "============================================================"
echo

if ! command -v mvn >/dev/null 2>&1; then
    echo "[ERROR] Maven is not installed or not on PATH."
    echo "Install via your package manager (e.g. brew install maven, apt install maven)."
    exit 1
fi

echo "[INFO] Running the JUnit test suite..."
echo "[INFO] 40 *Spec.java classes; 244 executions with a database (or -Ptest-h2),"
echo "[INFO] 219 run / 7 skipped without one (DB-gated classes skip, never fail)"
echo
mvn clean test

echo
echo "============================================================"
echo " Test run complete."
echo "============================================================"
