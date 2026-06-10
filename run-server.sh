#!/usr/bin/env bash
# ============================================================
# DRS-Enhanced - Server Launcher (macOS / Linux)
#
# Run from a terminal:  ./run-server.sh
# Requires: JDK 17+ and Maven 3.6+ on PATH, and a running MySQL 8
# server. Connection settings come from db.properties (edit
# db.user / db.password to match your install) or from the
# DB_URL / DB_USER / DB_PASSWORD environment variables, which
# take precedence.
#
# The server applies the database schema and seed, creates the
# default admin and the demo accounts (see README), and listens
# on port 5599. Ctrl+C stops it. Start clients with run-app.sh.
#
# Author: Fabian Roberto Guzman (12287570)
# Unit:   COIT20258 Software Engineering - Assessment 3
# ============================================================
set -euo pipefail

cd "$(dirname "$0")"

echo "============================================================"
echo " DRS-Enhanced - Server"
echo " Fabian Roberto Guzman (12287570) - COIT20258 Assessment 3"
echo "============================================================"
echo

if ! command -v mvn >/dev/null 2>&1; then
    echo "[ERROR] Maven is not installed or not on PATH."
    echo "Install via your package manager (e.g. brew install maven, apt install maven)."
    exit 1
fi

echo "[INFO] Starting the DRS server (port 5599)..."
echo "[INFO] Keep this terminal open while clients are connected; Ctrl+C stops it."
echo
exec mvn -q compile exec:java -Dexec.mainClass=edu.cqu.drs.server.DrsServerLauncher
