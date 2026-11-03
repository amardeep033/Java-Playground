#!/usr/bin/env bash
set -euo pipefail

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required to start supporting infrastructure." >&2
  exit 1
fi
docker info --format '{{.ServerVersion}}' >/dev/null
BROKER_NAME=event-relay-rabbitmq

# Stop the previous project broker; its persistent volume is retained.
if docker container inspect "$BROKER_NAME" >/dev/null 2>&1; then
  echo "Stopping previous RabbitMQ instance…"
  docker stop --time 10 "$BROKER_NAME" >/dev/null
fi
if [[ "${1:-}" == "--stop" ]]; then
  echo "RabbitMQ stopped."
  exit 0
fi

if docker container inspect "$BROKER_NAME" >/dev/null 2>&1; then
  docker start "$BROKER_NAME" >/dev/null
else
  docker run -d \
    --name "$BROKER_NAME" \
    --hostname "$BROKER_NAME" \
    --restart unless-stopped \
    -p 127.0.0.1:5672:5672 \
    -p 127.0.0.1:15672:15672 \
    -e "RABBITMQ_DEFAULT_USER=${RABBITMQ_USERNAME:-guest}" \
    -e "RABBITMQ_DEFAULT_PASS=${RABBITMQ_PASSWORD:-guest}" \
    --mount type=volume,source=event-relay-rabbitmq-data,target=/var/lib/rabbitmq \
    rabbitmq:4-management >/dev/null
fi

echo "Waiting for RabbitMQ…"
for (( attempt = 0; attempt < 30; attempt++ )); do
  if docker exec "$BROKER_NAME" rabbitmq-diagnostics -q ping >/dev/null 2>&1; then
    echo "RabbitMQ ready: localhost:5672; management: http://localhost:15672"
    if command -v pg_isready >/dev/null 2>&1; then
      if ! pg_isready -h "${PGHOST:-localhost}" -p "${PGPORT:-5432}"; then
        echo "Start your existing PostgreSQL server before running Service A or B." >&2
        exit 1
      fi
    fi
    exit 0
  fi
  sleep 1
done
echo "RabbitMQ did not become ready. Inspect: docker logs $BROKER_NAME" >&2
exit 1
