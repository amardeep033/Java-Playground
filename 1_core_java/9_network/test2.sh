#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

mvn -q compile dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
CP="target/classes:$(cat target/classpath.txt)"
PORT="${1:-8281}"

java -cp "$CP" S03RestServer "$PORT" > /tmp/rest-server.out &
SERVER_PID=$!
trap 'kill "$SERVER_PID" 2>/dev/null || true' EXIT
sleep 1
OUTPUT="$(java -cp "$CP" S04RestClient "$PORT")"
kill "$SERVER_PID" 2>/dev/null || true
wait "$SERVER_PID" 2>/dev/null || true

[[ "$OUTPUT" == *'POST: 201 {"saved":true}'* ]]
[[ "$OUTPUT" == *"GET: 200 Amar"* ]]
[[ "$OUTPUT" == *"ASYNC GET: Amar"* ]]
[[ "$OUTPUT" == *"URLConnection GET: Amar"* ]]
echo "REST check passed"
