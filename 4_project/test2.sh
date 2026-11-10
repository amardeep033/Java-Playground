#!/usr/bin/env bash
set -euo pipefail

# Edit N and restart, or run: ./test.sh 3
N=0                    # Fail N times per delivery, then succeed; 0 = always succeed
PORT="${WEBHOOK_TEST_PORT:-9091}"

PROJECT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PID_FILE="$PROJECT_DIR/.local/test-webhook.pid"
MARKER="event-relay-test-webhook:$PROJECT_DIR/test.sh"

# Stop only the receiver previously started by this script.
if [[ -f "$PID_FILE" ]]; then
  read -r previous_pid < "$PID_FILE" || true
  if [[ "${previous_pid:-}" =~ ^[0-9]+$ ]] && [[ -r "/proc/$previous_pid/cmdline" ]]; then
    command_line="$(tr '\0' '\n' < "/proc/$previous_pid/cmdline")"
    if [[ "$command_line" == *"$MARKER"* ]]; then
      kill -TERM "$previous_pid" 2>/dev/null || true
      for (( attempt = 0; attempt < 50; attempt++ )); do
        [[ ! -e "$PID_FILE" ]] && break
        sleep 0.1
      done
      if [[ -e "$PID_FILE" ]]; then
        echo "Previous receiver has not stopped. Try again shortly." >&2
        exit 1
      fi
    fi
  fi
  rm -f -- "$PID_FILE"
fi

[[ "${1:-}" == "--stop" ]] && exit 0
N="${1:-$N}"
if (( $# > 1 )) || [[ ! "$N" =~ ^(0|[1-9][0-9]*)$ ]]; then
  echo "Usage: ./test.sh [N] | ./test.sh --stop" >&2
  exit 1
fi
mkdir -p -- "$PROJECT_DIR/.local"

exec python3 -u - "$N" "$PORT" "$PID_FILE" "$MARKER" <<'PY'
import json
import os
from pathlib import Path
import signal
import sys
import threading
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

failures, port = map(int, sys.argv[1:3])
pid_file = Path(sys.argv[3])
counts = {}
lock = threading.Lock()

class Receiver(BaseHTTPRequestHandler):
    def setup(self):
        super().setup()
        self.connection.settimeout(10)

    def log_message(self, *_):
        pass  # Never log cookies, signing headers or credentials.

    def respond(self, status, body):
        encoded = json.dumps(body).encode()
        self.send_response(status)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Content-Length', str(len(encoded)))
        self.end_headers()
        try:
            self.wfile.write(encoded)
        except (BrokenPipeError, ConnectionResetError):
            pass

    def do_GET(self):
        if self.path == '/health':
            self.respond(200, {'status': 'up', 'n': failures})
        else:
            self.respond(404, {'error': 'Use POST /webhook or GET /health'})

    def do_POST(self):
        if self.path != '/webhook':
            self.respond(404, {'error': 'Use POST /webhook'})
            return
        try:
            length = int(self.headers.get('Content-Length', '0'))
            if not 0 <= length <= 2_000_000:
                self.respond(413, {'error': 'Payload exceeds 2 MB'})
                return
            payload = json.loads(self.rfile.read(length)) if length else {}
        except (ValueError, UnicodeDecodeError):
            self.respond(400, {'error': 'Invalid JSON or Content-Length'})
            return
        payload = payload if isinstance(payload, dict) else {}
        delivery_id = self.headers.get('webhook-id') or self.headers.get('Idempotency-Key') or str(payload.get('id') or 'manual')
        with lock:
            count = counts.get(delivery_id, 0) + 1
            counts[delivery_id] = count
        status = 503 if count <= failures else 200
        result = {
            'status': 'success' if status == 200 else 'failed',
            'deliveryId': delivery_id,
            'receivedAttempt': count,
            'failuresBeforeSuccess': failures,
        }
        print(json.dumps({'time': datetime.now(timezone.utc).isoformat(), 'httpStatus': status, **result}), flush=True)
        self.respond(status, result)

if not 1 <= port <= 65535:
    sys.exit('PORT must be between 1 and 65535')
try:
    server = ThreadingHTTPServer(('127.0.0.1', port), Receiver)
except OSError as error:
    sys.exit(f'Cannot start receiver on port {port}: {error}')
pid_file.write_text(str(os.getpid()) + '\n')
signal.signal(signal.SIGTERM, lambda *_: sys.exit(0))
print(f'Webhook URL: http://127.0.0.1:{port}/webhook', flush=True)
print('Always HTTP 200' if failures == 0 else f'HTTP 503 for first {failures} requests per delivery; HTTP 200 afterward', flush=True)
print('Counters reset on restart. Ctrl+C or ./test.sh --stop to stop.', flush=True)
try:
    server.serve_forever()
except KeyboardInterrupt:
    pass
finally:
    server.server_close()
    if pid_file.exists() and pid_file.read_text().strip() == str(os.getpid()):
        pid_file.unlink()
PY
