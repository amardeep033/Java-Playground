#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

mvn -q compile dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
CP="target/classes:$(cat target/classpath.txt)"
PORT="${1:-9292}"

java -cp "$CP" S05GrpcServer "$PORT" > /tmp/grpc-server.out &
SERVER_PID=$!
trap 'kill "$SERVER_PID" 2>/dev/null || true' EXIT
sleep 1
OUTPUT="$(java -cp "$CP" S06GrpcClient "$PORT")"
kill "$SERVER_PID" 2>/dev/null || true
wait "$SERVER_PID" 2>/dev/null || true

[[ "$OUTPUT" == *"gRPC addUser saved: true"* ]]
[[ "$OUTPUT" == *"gRPC getUser: 1 Amar"* ]]
echo "gRPC check passed"
