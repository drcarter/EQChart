#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PORT="${EQCHART_REFERENCE_DOCS_PORT:-8123}"
HOST="${EQCHART_REFERENCE_DOCS_HOST:-127.0.0.1}"
PID_FILE="/tmp/eqchart-reference-docs-${PORT}.pid"
LOG_FILE="/tmp/eqchart-reference-docs-${PORT}.log"
URL="http://${HOST}:${PORT}/docs/reference/index.html"

is_running() {
  if [[ -f "${PID_FILE}" ]]; then
    local pid
    pid="$(cat "${PID_FILE}")"
    if kill -0 "${pid}" >/dev/null 2>&1; then
      return 0
    fi
    rm -f "${PID_FILE}"
  fi

  if lsof -iTCP:"${PORT}" -sTCP:LISTEN >/dev/null 2>&1; then
    return 0
  fi

  return 1
}

start_server() {
  if is_running; then
    return 0
  fi

  cd "${ROOT_DIR}"
  nohup python3 -m http.server "${PORT}" --bind "${HOST}" >"${LOG_FILE}" 2>&1 &
  echo $! > "${PID_FILE}"

  for _ in $(seq 1 50); do
    if lsof -iTCP:"${PORT}" -sTCP:LISTEN >/dev/null 2>&1; then
      return 0
    fi
    sleep 0.1
  done

  echo "Failed to start preview server on ${HOST}:${PORT}" >&2
  exit 1
}

open_browser() {
  if command -v open >/dev/null 2>&1; then
    open "${URL}"
  elif command -v xdg-open >/dev/null 2>&1; then
    xdg-open "${URL}"
  else
    echo "Open this URL manually: ${URL}"
  fi
}

stop_server() {
  if [[ -f "${PID_FILE}" ]]; then
    local pid
    pid="$(cat "${PID_FILE}")"
    if kill -0 "${pid}" >/dev/null 2>&1; then
      kill "${pid}" >/dev/null 2>&1 || true
    fi
    rm -f "${PID_FILE}"
  fi
}

case "${1:-open}" in
  open)
    start_server
    echo "Reference docs preview: ${URL}"
    open_browser
    ;;
  stop)
    stop_server
    echo "Stopped reference docs preview server on port ${PORT}"
    ;;
  *)
    echo "Usage: $0 [open|stop]" >&2
    exit 1
    ;;
esac
