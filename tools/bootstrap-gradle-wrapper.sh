#!/usr/bin/env bash
set -euo pipefail

if ! command -v gradle >/dev/null 2>&1; then
  echo "A system Gradle installation is required once to generate the wrapper." >&2
  echo "Install Gradle, then rerun this script." >&2
  exit 1
fi

gradle wrapper --gradle-version=9.5.1 --distribution-type=bin

echo "Generated Gradle wrapper. Commit gradlew, gradlew.bat, gradle/wrapper/gradle-wrapper.jar, and gradle-wrapper.properties."
