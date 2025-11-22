#!/bin/bash

JAR_NAME="autofx.jar"
LOG_FILE="nohup.log"

function start_app() {
  if pgrep -f "$JAR_NAME" > /dev/null; then
    echo "Application is already running."
    return 1
  fi

  echo "Starting application..."
  nohup API_KEY=${API_KEY} API_SECRET=${API_SECRET} java -jar "$JAR_NAME" > "$LOG_FILE" 2>&1 &

  echo -n "Waiting for application to start"
  for i in {1..10}; do
    sleep 1
    if pgrep -f "$JAR_NAME" > /dev/null; then
      echo
      echo "Application started successfully."
      return 0
    fi
    echo -n "."
  done

  echo
  echo "Failed to start application."
  return 1
}

function stop_app() {
  PID=$(pgrep -f "$JAR_NAME")
  if [ -z "$PID" ]; then
    echo "Application is not running."
    return 1
  fi

  echo "Stopping application (PID: $PID)..."
  kill "$PID"

  echo -n "Waiting for process to stop"
  while kill -0 "$PID" 2> /dev/null; do
    echo -n "."
    sleep 1
  done

  echo
  echo "Application stopped."
  return 0
}

function restart_app() {
  stop_app
  sleep 2
  start_app
}

case "$1" in
  start)
    start_app
    ;;
  stop)
    stop_app
    ;;
  restart)
    restart_app
    ;;
  *)
    echo "Usage: $0 {start|stop|restart}"
    exit 1
    ;;
esac
