#!/usr/bin/env bash
set -euo pipefail

if [ "$(sudo docker inspect -f '{{.State.Running}}' rabbitmq-learning 2>/dev/null)" != "true" ]; then
  sudo docker start rabbitmq-learning 2>/dev/null || \
  sudo docker run -d --rm --name rabbitmq-learning \
    -p 127.0.0.1:5672:5672 -p 127.0.0.1:15672:15672 \
    -e RABBITMQ_DEFAULT_USER=learner \
    -e RABBITMQ_DEFAULT_PASS=learner \
    rabbitmq:4-management
fi

cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
port="${SERVER_PORT:-8080}"
mapfile -t pids < <(lsof -t -iTCP:"$port" -sTCP:LISTEN || true)
if [ "${#pids[@]}" -gt 0 ]; then
  echo "Stopping process on port $port..."
  kill "${pids[@]}" 2>/dev/null || true
  # A suspended process (Ctrl+Z) must resume to handle the termination signal.
  kill -CONT "${pids[@]}" 2>/dev/null || true
  # Spring may need a few seconds to finish requests and release the port.
  for attempt in {1..70}; do
    if ! lsof -t -iTCP:"$port" -sTCP:LISTEN >/dev/null; then
      break
    fi
    sleep 0.5
  done
fi
if lsof -t -iTCP:"$port" -sTCP:LISTEN >/dev/null; then
  echo "Port $port is still occupied. Stop its listener before restarting." >&2
  exit 1
fi
exec mvn spring-boot:run "$@"
