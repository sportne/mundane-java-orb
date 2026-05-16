#!/usr/bin/env bash
set -euo pipefail

OFFLINE_REPO="${1:-local-maven-repo}"

if [ ! -x ./gradlew ]; then
  echo "Gradle wrapper is not available. Run ./tools/bootstrap-gradle-wrapper.sh first." >&2
  exit 1
fi

./gradlew --offline -PcorbaOfflineRepo="${OFFLINE_REPO}" clean check
