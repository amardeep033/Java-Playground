#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
COMPONENT_DIR="$PROJECT_DIR/UI"
PORT="${UI_PORT:-5173}"
LABEL="UI"

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
if ! command -v npm >/dev/null 2>&1; then
  echo "npm is required to start the UI." >&2
  exit 1
fi
if ! command -v node >/dev/null 2>&1 || ! node -e '
  const [major, minor] = process.versions.node.split(".").map(Number);
  process.exit(major > 22 || (major === 22 && minor >= 12) ? 0 : 1);
'; then
  echo "Starting UI with a temporary Node 22 runtime…"
  exec npx --yes --package=node@22 -- bash "$PROJECT_DIR/run0.sh" "$@"
fi
if [[ ! -d node_modules ]]; then
  npm ci
fi
exec npm run dev -- --port "$PORT" "$@"
