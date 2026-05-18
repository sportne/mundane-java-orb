#!/usr/bin/env bash
set -euo pipefail

OFFLINE_REPO="${1:-local-maven-repo}"

if [ ! -x ./gradlew ]; then
  echo "Gradle wrapper is not executable. Run from the repository root." >&2
  exit 1
fi

if [ ! -d "${OFFLINE_REPO}" ]; then
  echo "Offline repository does not exist: ${OFFLINE_REPO}" >&2
  exit 1
fi

OFFLINE_REPO_ABS="$(cd "${OFFLINE_REPO}" && pwd)"
if [ ! -f "${OFFLINE_REPO_ABS}/MANIFEST.sha256" ]; then
  echo "Offline repository manifest is missing: ${OFFLINE_REPO_ABS}/MANIFEST.sha256" >&2
  exit 1
fi
if ! (cd "${OFFLINE_REPO_ABS}" && sha256sum --check MANIFEST.sha256 >/dev/null); then
  echo "Offline repository manifest checksum validation failed: ${OFFLINE_REPO_ABS}/MANIFEST.sha256" >&2
  exit 1
fi

SOURCE_GRADLE_HOME="${GRADLE_USER_HOME:-${HOME}/.gradle}"
ISOLATED_GRADLE_HOME="$(mktemp -d "${TMPDIR:-/tmp}/mjo-offline-gradle-home.XXXXXX")"
STABLE_OFFLINE_REPO="${ISOLATED_GRADLE_HOME}/offline-repo"
trap 'rm -rf "${ISOLATED_GRADLE_HOME}"' EXIT

for gradle_cache in wrapper caches jdks; do
  if [ -d "${SOURCE_GRADLE_HOME}/${gradle_cache}" ]; then
    cp -R "${SOURCE_GRADLE_HOME}/${gradle_cache}" "${ISOLATED_GRADLE_HOME}/"
  fi
done
cp -R "${OFFLINE_REPO_ABS}" "${STABLE_OFFLINE_REPO}"

GRADLE_USER_HOME="${ISOLATED_GRADLE_HOME}" \
  ./gradlew \
  --offline \
  -Dorg.gradle.dependency.verification=strict \
  -Pcorba.offlineRepo="${STABLE_OFFLINE_REPO}" \
  clean \
  qualityGate
