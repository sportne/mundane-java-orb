#!/usr/bin/env bash
set -euo pipefail

exec java ${INTEROP_JAVA_OPTS:-} -cp '/interop/peer/classes:/interop/peer/lib/*' \
  PeerSmoke "${INTEROP_ROLE:-${1:-report}}"
