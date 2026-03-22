#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PORT="${EQCHART_REFERENCE_DOCS_PORT:-8123}"
HOST="${EQCHART_REFERENCE_DOCS_HOST:-127.0.0.1}"
PID_FILE="/tmp/eqchart-reference-docs-${PORT}.pid"
LOG_FILE="/tmp/eqchart-reference-docs-${PORT}.log"
INDEX_FILE="${ROOT_DIR}/docs/reference/index.html"
NO_OPEN="${EQCHART_REFERENCE_DOCS_NO_OPEN:-0}"
URL="http://${HOST}:${PORT}/docs/reference/index.html"

listener_pids() {
  lsof -tiTCP:"${PORT}" -sTCP:LISTEN 2>/dev/null || true
}

url_is_ready() {
  curl --silent --fail --output /dev/null "${URL}"
}

is_running() {
  if [[ -f "${PID_FILE}" ]]; then
    local pid
    pid="$(cat "${PID_FILE}")"
    if kill -0 "${pid}" >/dev/null 2>&1 && url_is_ready; then
      return 0
    fi
    rm -f "${PID_FILE}"
  fi

  if lsof -iTCP:"${PORT}" -sTCP:LISTEN >/dev/null 2>&1 && url_is_ready; then
    return 0
  fi

  return 1
}

ensure_reference_docs() {
  if [[ -f "${INDEX_FILE}" ]]; then
    return 0
  fi

  echo "Reference docs not found at ${INDEX_FILE}. Building them first..."
  (
    cd "${ROOT_DIR}"
    ./gradlew referenceDocs
  )

  if [[ ! -f "${INDEX_FILE}" ]]; then
    echo "Reference docs build finished, but ${INDEX_FILE} is still missing." >&2
    exit 1
  fi
}

start_server() {
  if is_running; then
    return 0
  fi

  stop_server
  nohup python3 -m http.server "${PORT}" --bind "${HOST}" --directory "${ROOT_DIR}" >"${LOG_FILE}" 2>&1 &
  echo $! > "${PID_FILE}"

  for _ in $(seq 1 50); do
    if lsof -iTCP:"${PORT}" -sTCP:LISTEN >/dev/null 2>&1 && url_is_ready; then
      return 0
    fi
    sleep 0.1
  done

  echo "Failed to start a healthy preview server on ${HOST}:${PORT}" >&2
  exit 1
}

open_browser() {
  if [[ "${NO_OPEN}" == "1" ]]; then
    echo "Skipping browser launch because EQCHART_REFERENCE_DOCS_NO_OPEN=1"
    return 0
  fi

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

  local pid
  for pid in $(listener_pids); do
    kill "${pid}" >/dev/null 2>&1 || true
  done
}

case "${1:-open}" in
  open)
    ensure_reference_docs
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
