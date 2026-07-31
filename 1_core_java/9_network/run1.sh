#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

mvn -q compile dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
CP="target/classes:$(cat target/classpath.txt)"
PORT="${1:-9190}"

java -cp "$CP" S01SocketServer "$PORT" &
SERVER_PID=$!
sleep 1
java -cp "$CP" S02SocketClient "$PORT"
wait "$SERVER_PID"
