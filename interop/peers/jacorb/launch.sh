#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
DRY_RUN=()
if [[ "${1:-}" == "--dry-run" ]]; then
  DRY_RUN=(--dry-run)
  shift
fi

exec "${ROOT_DIR}/interop/bin/interop-peer" launch "${DRY_RUN[@]}" jacorb "$@"
