#!/usr/bin/env bash
set -euo pipefail

PID=$(lsof -t -i :8080 || true)

if [ -n "$PID" ]; then
  kill "$PID"
fi

mvn spring-boot:run
