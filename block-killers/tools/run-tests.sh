#!/bin/bash
# Compiles the platform-independent core + desktop harness and runs the automated gameplay test-suite.
# Usage: tools/run-tests.sh [tests|screens|assets]
set -e
cd "$(dirname "$0")/.."
OUT="${BK_BUILD_DIR:-build}/desktop"
mkdir -p "$OUT"
javac -encoding UTF-8 --release 11 -d "$OUT" app/src/main/java/com/blockkillers/core/*.java desktop/src/com/blockkillers/desktop/*.java
case "${1:-tests}" in
  tests)   java -Djava.awt.headless=true -cp "$OUT" com.blockkillers.desktop.GameTests ;;
  screens) java -Djava.awt.headless=true -cp "$OUT" com.blockkillers.desktop.Screens "${2:-docs/screenshots}" ;;
  assets)  java -Djava.awt.headless=true -cp "$OUT" com.blockkillers.desktop.AssetGen app/src/main/res ;;
esac
