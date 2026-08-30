PID=$(lsof -t -i :8080)

if [ -n "$PID" ]; then
  kill "$PID"
fi

mvn spring-boot:run
