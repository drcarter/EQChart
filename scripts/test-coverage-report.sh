#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

MODULES=(
  "EQChart"
  "EQChart-common"
  "EQChart-compose"
)

usage() {
  cat <<'EOF'
Usage:
  scripts/test-coverage-report.sh
  scripts/test-coverage-report.sh --help

Description:
  Reads JaCoCo XML reports from the library modules and prints:
  - a combined coverage table across all modules
  - a per-module coverage table for each module

Expected report paths:
  EQChart/build/reports/jacoco/jacocoDebugUnitTestReport/jacocoDebugUnitTestReport.xml
  EQChart-common/build/reports/jacoco/jacocoDebugUnitTestReport/jacocoDebugUnitTestReport.xml
  EQChart-compose/build/reports/jacoco/jacocoDebugUnitTestReport/jacocoDebugUnitTestReport.xml

Reported metrics:
  INSTRUCTION, BRANCH, LINE, COMPLEXITY, METHOD, CLASS

Notes:
  - If any XML report is missing, the script builds the missing JaCoCo reports first.
EOF
}

log() {
  printf '[coverage] %s\n' "$*"
}

err() {
  printf '[coverage][error] %s\n' "$*" >&2
}

if [[ $# -gt 0 ]]; then
  case "$1" in
    -h|--help)
      usage
      exit 0
      ;;
    *)
      err "Unknown option: $1"
      usage
      exit 1
      ;;
  esac
fi

if ! command -v python3 >/dev/null 2>&1; then
  err "python3 is required."
  exit 1
fi

module_args=()
missing_modules=()
for module in "${MODULES[@]}"; do
  report_path="${PROJECT_ROOT}/${module}/build/reports/jacoco/jacocoDebugUnitTestReport/jacocoDebugUnitTestReport.xml"
  if [[ ! -f "${report_path}" ]]; then
    missing_modules+=("${module}")
  fi
done

if [[ ${#missing_modules[@]} -gt 0 ]]; then
  if [[ ! -x "${PROJECT_ROOT}/gradlew" ]]; then
    err "Missing reports and gradlew is not available at ${PROJECT_ROOT}/gradlew"
    exit 1
  fi

  gradle_tasks=()
  for module in "${missing_modules[@]}"; do
    gradle_tasks+=(":${module}:jacocoDebugUnitTestReport")
  done

  log "Missing JaCoCo XML for ${#missing_modules[@]} module(s): ${missing_modules[*]}"
  log "Building missing JaCoCo reports..."
  (
    cd "${PROJECT_ROOT}"
    ./gradlew "${gradle_tasks[@]}"
  )
fi

for module in "${MODULES[@]}"; do
  report_path="${PROJECT_ROOT}/${module}/build/reports/jacoco/jacocoDebugUnitTestReport/jacocoDebugUnitTestReport.xml"
  if [[ ! -f "${report_path}" ]]; then
    err "JaCoCo XML is still missing for ${module}: ${report_path}"
    exit 1
  fi
  module_args+=("${module}=${report_path}")
done

log "Reading JaCoCo XML reports for ${#MODULES[@]} modules"

python3 - "${module_args[@]}" <<'PY'
import sys
import xml.etree.ElementTree as ET

METRICS = ["INSTRUCTION", "BRANCH", "LINE", "COMPLEXITY", "METHOD", "CLASS"]
HEADERS = ["Metric", "Coverage", "Covered", "Missed", "Total"]


def parse_module(argument: str):
    module, path = argument.split("=", 1)
    root = ET.parse(path).getroot()
    counters = {
        counter.attrib["type"]: (
            int(counter.attrib["missed"]),
            int(counter.attrib["covered"]),
        )
        for counter in root.findall("counter")
    }
    rows = []
    for metric in METRICS:
        missed, covered = counters.get(metric, (0, 0))
        total = missed + covered
        ratio = 0.0 if total == 0 else (covered / total) * 100.0
        rows.append((metric, f"{ratio:.2f}%", covered, missed, total))
    return module, rows


def format_table(rows):
    table_rows = [HEADERS] + [
        [metric, coverage, str(covered), str(missed), str(total)]
        for metric, coverage, covered, missed, total in rows
    ]
    widths = [max(len(row[index]) for row in table_rows) for index in range(len(HEADERS))]

    def fmt(row):
        cells = []
        for index, cell in enumerate(row):
            if index in (1, 2, 3, 4):
                cells.append(cell.rjust(widths[index]))
            else:
                cells.append(cell.ljust(widths[index]))
        return "| " + " | ".join(cells) + " |"

    border = "+-" + "-+-".join("-" * width for width in widths) + "-+"
    return "\n".join(
        [border, fmt(table_rows[0]), border] +
        [fmt(row) for row in table_rows[1:]] +
        [border]
    )


module_results = [parse_module(argument) for argument in sys.argv[1:]]

combined_counts = {metric: [0, 0] for metric in METRICS}
for _, rows in module_results:
    for metric, _, covered, missed, _ in rows:
        combined_counts[metric][0] += missed
        combined_counts[metric][1] += covered

combined_rows = []
for metric in METRICS:
    missed, covered = combined_counts[metric]
    total = missed + covered
    ratio = 0.0 if total == 0 else (covered / total) * 100.0
    combined_rows.append((metric, f"{ratio:.2f}%", covered, missed, total))

print("=== Combined Coverage ===")
print(format_table(combined_rows))

for module, rows in module_results:
    print()
    print(f"=== {module} ===")
    print(format_table(rows))
PY
