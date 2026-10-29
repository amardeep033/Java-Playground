#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
export PATH="$HOME/.local/bin:$PATH"

port="${REDIS_PORT:-6379}"
if ! redis-cli -p "$port" ping 2>/dev/null | grep -qx PONG; then
  mkdir -p .redis
  redis-server --bind 127.0.0.1 --port "$port" --daemonize yes \
    --dir "$PWD/.redis" --logfile "$PWD/.redis/redis.log" \
    --pidfile "$PWD/.redis/redis.pid" --appendonly yes
fi

app_port="${SERVER_PORT:-8080}"
port_in_use() {
  [[ -n "$(ss -H -ltn "sport = :$1")" ]]
}
mapfile -t pids < <(lsof -t -iTCP:"$app_port" -sTCP:LISTEN || true)
if [[ "${#pids[@]}" -gt 0 ]]; then
  echo "Stopping process on port $app_port..."
  kill "${pids[@]}" 2>/dev/null || true
  # Resume suspended processes so they can handle the termination signal.
  kill -CONT "${pids[@]}" 2>/dev/null || true
  for attempt in {1..60}; do
    if ! port_in_use "$app_port"; then
      break
    fi
    sleep 0.5
  done
fi
# lsof may hide listeners owned by another process/user; ss still shows the socket.
while port_in_use "$app_port"; do
  echo "Port $app_port is occupied; trying the next port..."
  app_port=$((app_port + 1))
  if (( app_port > 65535 )); then
    echo "No available HTTP port found." >&2
    exit 1
  fi
done
export SERVER_PORT="$app_port"
echo "Starting app at http://localhost:$SERVER_PORT (use this port in curl commands)"

exec mvn -f app/pom.xml clean spring-boot:run "$@"
