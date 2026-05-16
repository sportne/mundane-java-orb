#!/usr/bin/env bash
set -euo pipefail

TARGET_REPO="${1:-local-maven-repo}"
mkdir -p "${TARGET_REPO}"

if [ ! -x ./gradlew ]; then
  echo "Gradle wrapper is not available. Run ./tools/bootstrap-gradle-wrapper.sh first." >&2
  exit 1
fi

./gradlew --refresh-dependencies help >/dev/null
./gradlew publishToMavenLocal

cat > "${TARGET_REPO}/README.txt" <<EOF
Offline repository preparation placeholder.

A production implementation should copy resolved Gradle plugin and dependency
artifacts into this Maven-layout repository and generate a manifest. This script
currently validates dependency resolution and local publication only.
EOF

echo "Prepared placeholder offline repository at ${TARGET_REPO}."
