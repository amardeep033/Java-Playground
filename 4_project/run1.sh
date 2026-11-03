#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
COMPONENT_DIR="$PROJECT_DIR/service-a"
PORT="${SERVER_PORT:-8081}"
LABEL="Service A"

# Stop the previous project instance before starting a replacement.
stop_previous() {
  if ! command -v lsof >/dev/null 2>&1; then
    echo "lsof is required to find the process listening on port $PORT." >&2
    return 1
  fi
  if [[ ! "$PORT" =~ ^[0-9]+$ ]] || (( PORT < 1 || PORT > 65535 )); then
    echo "Invalid port: $PORT" >&2
    return 1
  fi

  belongs_to_project() {
    local pid="$1" process_dir command_line
    process_dir="$(readlink -f "/proc/$pid/cwd" 2>/dev/null || true)"
    command_line="$(tr '\0' ' ' < "/proc/$pid/cmdline" 2>/dev/null || true)"
    [[ "$process_dir" == "$COMPONENT_DIR" || "$process_dir" == "$COMPONENT_DIR/"* || "$command_line" == *"$COMPONENT_DIR/"* ]]
  }

  mapfile -t PIDS < <(lsof -nP -t -iTCP:"$PORT" -sTCP:LISTEN | sort -u || true)
  if (( ${#PIDS[@]} == 0 )); then
    echo "$LABEL is not running on port $PORT."
    return 0
  fi

  # Validate all listeners before stopping any of them.
  for pid in "${PIDS[@]}"; do
    if ! belongs_to_project "$pid"; then
      echo "Port $PORT is used by PID $pid outside $COMPONENT_DIR; refusing to stop it." >&2
      return 1
    fi
  done

  for pid in "${PIDS[@]}"; do
    echo "Stopping $LABEL (PID $pid, port $PORT)…"
    kill -TERM "$pid" 2>/dev/null || true
  done
  for (( attempt = 0; attempt < 100; attempt++ )); do
    running=false
    for pid in "${PIDS[@]}"; do
      if kill -0 "$pid" 2>/dev/null && belongs_to_project "$pid"; then
        running=true
      fi
    done
    if [[ "$running" == false ]]; then
      echo "$LABEL stopped."
      return 0
    fi
    sleep 0.1
  done

  for pid in "${PIDS[@]}"; do
    if kill -0 "$pid" 2>/dev/null && belongs_to_project "$pid"; then
      echo "Force-stopping $LABEL (PID $pid) after 10 seconds."
      kill -KILL "$pid" 2>/dev/null || true
    fi
  done
}

stop_previous
if [[ "${1:-}" == "--stop" ]]; then
  exit 0
fi

cd "$COMPONENT_DIR"
if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven and Java 21 are required to start Service A." >&2
  exit 1
fi
exec mvn spring-boot:run "$@"
