#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

PACKAGE_NAMES_DEFAULT="eqchart-common,eqchart,eqchart-compose"
PACKAGE_NAMES="${PACKAGE_NAMES_DEFAULT}"
DATE_OVERRIDE=""
OWNER_OVERRIDE=""
REPO_OVERRIDE=""
RESOLVE_ONLY=0

usage() {
  cat <<'EOF'
Usage:
  scripts/publish-github-packages.sh [options]

Options:
  --date YYYY.MM.DD       Override base date (default: Asia/Seoul today)
  --owner OWNER           Override GitHub owner (default: parsed from origin remote)
  --repo REPO             Override GitHub repo (default: parsed from origin remote)
  --packages a,b,c        Comma-separated package names (default: eqchart-common,eqchart,eqchart-compose)
  --resolve-only          Print resolved version only (no publish)
  -h, --help              Show this help
EOF
}

log() {
  printf '[publish] %s\n' "$*"
}

err() {
  printf '[publish][error] %s\n' "$*" >&2
}

trim() {
  local value="$1"
  value="${value#"${value%%[![:space:]]*}"}"
  value="${value%"${value##*[![:space:]]}"}"
  printf '%s' "$value"
}

parse_owner_repo_from_remote() {
  local remote_url path owner repo
  remote_url="$(git -C "${PROJECT_ROOT}" remote get-url origin 2>/dev/null || true)"
  if [[ -z "${remote_url}" ]]; then
    return 1
  fi

  case "${remote_url}" in
    git@github.com:*)
      path="${remote_url#git@github.com:}"
      ;;
    https://github.com/*)
      path="${remote_url#https://github.com/}"
      ;;
    ssh://git@github.com/*)
      path="${remote_url#ssh://git@github.com/}"
      ;;
    *)
      return 1
      ;;
  esac

  path="${path#/}"
  path="${path%/}"
  path="${path%.git}"

  if [[ "${path}" != */* ]]; then
    return 1
  fi

  owner="${path%%/*}"
  repo="${path#*/}"
  if [[ -z "${owner}" || -z "${repo}" || "${repo}" == */* ]]; then
    return 1
  fi

  printf '%s %s\n' "${owner}" "${repo}"
}

parse_owner_repo_from_gh_context() {
  local name_with_owner owner repo
  name_with_owner="$(gh repo view --json nameWithOwner --jq '.nameWithOwner' 2>/dev/null || true)"
  if [[ -z "${name_with_owner}" || "${name_with_owner}" != */* ]]; then
    return 1
  fi

  owner="${name_with_owner%%/*}"
  repo="${name_with_owner#*/}"
  if [[ -z "${owner}" || -z "${repo}" ]]; then
    return 1
  fi

  printf '%s %s\n' "${owner}" "${repo}"
}

fetch_versions_for_package() {
  local owner="$1"
  local package_name="$2"
  local versions

  versions="$(gh api --paginate "/users/${owner}/packages/maven/${package_name}/versions?per_page=100" --jq '.[].name' 2>/dev/null || true)"
  if [[ -n "${versions}" ]]; then
    printf '%s\n' "${versions}"
    return
  fi

  versions="$(gh api --paginate "/orgs/${owner}/packages/maven/${package_name}/versions?per_page=100" --jq '.[].name' 2>/dev/null || true)"
  if [[ -n "${versions}" ]]; then
    printf '%s\n' "${versions}"
  fi
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --date)
      DATE_OVERRIDE="${2:-}"
      shift 2
      ;;
    --owner)
      OWNER_OVERRIDE="${2:-}"
      shift 2
      ;;
    --repo)
      REPO_OVERRIDE="${2:-}"
      shift 2
      ;;
    --packages)
      PACKAGE_NAMES="${2:-}"
      shift 2
      ;;
    --resolve-only)
      RESOLVE_ONLY=1
      shift
      ;;
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
done

if ! command -v gh >/dev/null 2>&1; then
  err "gh CLI is required."
  exit 1
fi

if [[ ! -x "${PROJECT_ROOT}/gradlew" ]]; then
  err "gradlew not found in project root: ${PROJECT_ROOT}"
  exit 1
fi

if ! gh auth status >/dev/null 2>&1; then
  err "gh authentication is required. Run: gh auth login"
  exit 1
fi

owner=""
repo=""
if parsed="$(parse_owner_repo_from_remote)"; then
  owner="$(awk '{print $1}' <<< "${parsed}")"
  repo="$(awk '{print $2}' <<< "${parsed}")"
elif parsed="$(parse_owner_repo_from_gh_context)"; then
  owner="$(awk '{print $1}' <<< "${parsed}")"
  repo="$(awk '{print $2}' <<< "${parsed}")"
fi

if [[ -n "${OWNER_OVERRIDE}" ]]; then
  owner="${OWNER_OVERRIDE}"
fi
if [[ -n "${REPO_OVERRIDE}" ]]; then
  repo="${REPO_OVERRIDE}"
fi

if [[ -z "${owner}" || -z "${repo}" ]]; then
  err "Could not resolve GitHub owner/repo. Use --owner and --repo."
  exit 1
fi

today="${DATE_OVERRIDE}"
if [[ -z "${today}" ]]; then
  today="$(TZ=Asia/Seoul date +%Y.%m.%d)"
fi

if [[ ! "${today}" =~ ^[0-9]{4}\.[0-9]{2}\.[0-9]{2}$ ]]; then
  err "Date must be YYYY.MM.DD format."
  exit 1
fi

IFS=',' read -r -a raw_packages <<< "${PACKAGE_NAMES}"
packages=()
for raw in "${raw_packages[@]}"; do
  package_name="$(trim "${raw}")"
  if [[ -n "${package_name}" ]]; then
    packages+=("${package_name}")
  fi
done

if [[ ${#packages[@]} -eq 0 ]]; then
  err "No package names provided."
  exit 1
fi

log "Owner/Repo: ${owner}/${repo}"
log "Packages: ${packages[*]}"
log "Base date: ${today}"

max_increment=-1
for package_name in "${packages[@]}"; do
  versions="$(fetch_versions_for_package "${owner}" "${package_name}")"
  if [[ -z "${versions}" ]]; then
    log "No existing versions found for ${package_name}"
    continue
  fi

  while IFS= read -r version; do
    [[ -z "${version}" ]] && continue
    if [[ "${version}" == "${today}" ]]; then
      if (( max_increment < 0 )); then
        max_increment=0
      fi
      continue
    fi
    if [[ "${version}" =~ ^${today}\.([0-9]+)$ ]]; then
      increment="${BASH_REMATCH[1]}"
      if (( increment > max_increment )); then
        max_increment="${increment}"
      fi
    fi
  done <<< "${versions}"
done

publish_increment=0
resolved_version="${today}"
if (( max_increment >= 0 )); then
  publish_increment=$((max_increment + 1))
  resolved_version="${today}.${publish_increment}"
fi

log "Resolved version: ${resolved_version}"

if (( RESOLVE_ONLY == 1 )); then
  exit 0
fi

publish_cmd=(
  ./gradlew
  publish
  "-PpublishDate=${today}"
)

if (( publish_increment > 0 )); then
  publish_cmd+=("-PpublishIncrement=${publish_increment}")
fi

log "Executing: ${publish_cmd[*]}"
(cd "${PROJECT_ROOT}" && "${publish_cmd[@]}")
