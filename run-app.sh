#!/usr/bin/env bash
# ============================================================
# DRS-Initial - JavaFX Prototype Launcher (macOS / Linux)
#
# Run from a terminal:  ./run-app.sh
# Requires: JDK 17+ and Maven 3.6+ on PATH.
#
# Author: Fabian Roberto Guzman (12287570)
# Unit:   COIT20258 Software Engineering - Assessment 2
# ============================================================
set -euo pipefail

cd "$(dirname "$0")"

echo "============================================================"
echo " DRS-Initial - Disaster Response System"
echo " Fabian Roberto Guzman (12287570) - COIT20258 Assessment 2"
echo "============================================================"
echo

if ! command -v mvn >/dev/null 2>&1; then
    echo "[ERROR] Maven is not installed or not on PATH."
    echo "Install via your package manager (e.g. brew install maven, apt install maven)."
    exit 1
fi

echo "[INFO] Launching the JavaFX application..."
echo "[INFO] (Close the application window to exit.)"
echo
exec mvn -q javafx:run
