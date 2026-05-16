#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
exec "${ROOT_DIR}/interop/bin/interop-peer" health "$@" jboss-openjdk-orb
