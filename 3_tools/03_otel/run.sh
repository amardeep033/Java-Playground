#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"

for tool in java mvn curl tar sha256sum ss lsof; do
  command -v "$tool" >/dev/null || { echo "Missing required tool: $tool" >&2; exit 1; }
done

# On exit, clean up the app and Jaeger started by this invocation.
app_pid=''
jaeger_pid=''
download_dir=''
cleanup() {
  trap - EXIT INT TERM
  for pid in "$app_pid" "$jaeger_pid"; do
    if [[ -n "$pid" ]]; then
      kill "$pid" 2>/dev/null || true
    fi
  done
  for pid in "$app_pid" "$jaeger_pid"; do
    if [[ -n "$pid" ]]; then
      wait "$pid" 2>/dev/null || true
    fi
  done
  if [[ -n "$download_dir" ]]; then rm -rf -- "$download_dir"; fi
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

app_port="${SERVER_PORT:-8080}"
if [[ ! "$app_port" =~ ^[0-9]{1,5}$ ]] || (( 10#$app_port < 1 || 10#$app_port > 65535 )); then
  echo 'SERVER_PORT must be between 1 and 65535.' >&2
  exit 1
fi
app_port=$((10#$app_port))
case "$app_port" in
  4317|4318|16686)
    echo "SERVER_PORT=$app_port conflicts with a Jaeger port. Choose another app port." >&2
    exit 1
    ;;
esac
mapfile -t app_listeners < <(lsof -nP -t -iTCP:"$app_port" -sTCP:LISTEN | sort -u || true)
if (( ${#app_listeners[@]} > 0 )); then
  echo "Stopping process(es) on app port $app_port: ${app_listeners[*]}"
  if ! kill "${app_listeners[@]}"; then
    echo "Could not stop the listener on port $app_port. Check process ownership." >&2
    exit 1
  fi
  # Allow suspended processes to handle the termination signal.
  kill -CONT "${app_listeners[@]}" 2>/dev/null || true
  for attempt in {1..40}; do
    if [[ -z "$(ss -H -ltn "sport = :$app_port")" ]]; then break; fi
    sleep 0.25
  done
fi
for port in "$app_port" 4317 4318 16686; do
  if [[ -n "$(ss -H -ltn "sport = :$port")" ]]; then
    echo "Port $port is occupied. Stop that service first (or use SERVER_PORT for the app)." >&2
    exit 1
  fi
done

# If the native binary is missing, install the pinned version locally. No Docker or sudo.
mkdir -p .tools .run
if [[ ! -x .tools/jaeger ]]; then
  case "$(uname -s)-$(uname -m)" in
    Linux-x86_64) arch=amd64 ;;
    Linux-aarch64) arch=arm64 ;;
    *) echo 'Automatic Jaeger installation supports Linux amd64/arm64.' >&2; exit 1 ;;
  esac
  version=2.22.0
  archive="jaeger-${version}-linux-${arch}"
  base="https://github.com/jaegertracing/jaeger/releases/download/v${version}"
  download_dir=$(mktemp -d)
  echo "Installing Jaeger $version into .tools/..."
  curl -4 -fsSL --connect-timeout 15 --max-time 180 --retry 2 "$base/$archive.tar.gz" -o "$download_dir/$archive.tar.gz"
  curl -4 -fsSL --connect-timeout 15 --max-time 30 --retry 2 "$base/$archive.sha256sum.txt" -o "$download_dir/checksums"
  tar -xzf "$download_dir/$archive.tar.gz" -C "$download_dir"
  (cd "$download_dir"; sha256sum --check checksums)
  install -m 755 "$download_dir/$archive/jaeger" .tools/jaeger
fi

.tools/jaeger validate --config config/jaeger.yaml
mvn -f app/pom.xml -Dmaven.repo.local="$PWD/.tools/m2" -q clean package

.tools/jaeger --config config/jaeger.yaml > .run/jaeger.log 2>&1 &
jaeger_pid=$!
SERVER_PORT="$app_port" java -jar app/target/otel-demo-0.0.1-SNAPSHOT.jar > .run/app.log 2>&1 &
app_pid=$!

ready=false
for attempt in {1..60}; do
  if ! kill -0 "$app_pid" 2>/dev/null || ! kill -0 "$jaeger_pid" 2>/dev/null; then break; fi
  if curl -fsS --connect-timeout 1 --max-time 2 http://localhost:16686/ >/dev/null 2>&1 &&
     curl -fsS --connect-timeout 1 --max-time 2 "http://localhost:$app_port/actuator/health" >/dev/null 2>&1; then
    ready=true
    break
  fi
  sleep 1
done
if [[ "$ready" != true ]]; then
  tail -40 .run/jaeger.log .run/app.log
  exit 1
fi

echo "App:     http://localhost:$app_port/order"
echo 'Jaeger:  http://localhost:16686'
echo "Metrics: http://localhost:$app_port/actuator/prometheus"
echo 'Logs:    .run/app.log | .run/otel-logs.log | .run/jaeger.log'
echo 'Running. Press Ctrl+C to stop both the app and Jaeger.'
wait -n "$app_pid" "$jaeger_pid"
