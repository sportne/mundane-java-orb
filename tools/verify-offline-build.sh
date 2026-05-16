#!/usr/bin/env bash
set -euo pipefail

OFFLINE_REPO="${1:-local-maven-repo}"

if [ ! -x ./gradlew ]; then
  echo "Gradle wrapper is not executable. Run from the repository root." >&2
  exit 1
fi

./gradlew --offline -Pcorba.offlineRepo="${OFFLINE_REPO}" clean qualityGate
