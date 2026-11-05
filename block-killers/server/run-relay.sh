#!/bin/bash
# Online multiplayer relay for Zombie Killer. Needs Java 11+ on a PC/server reachable from the internet.
# Usage: server/run-relay.sh [port]      (default 47800; open this TCP port in your firewall/router)
# Then in the game: Multiplayer -> Relay server -> enter  <this machine's public IP or domain>:<port>
set -e
cd "$(dirname "$0")/.."
mkdir -p build/relay
javac -d build/relay app/src/main/java/com/blockkillers/core/net/Frames.java app/src/main/java/com/blockkillers/core/net/RelayServer.java
exec java -cp build/relay com.blockkillers.core.net.RelayServer "${1:-47800}"
