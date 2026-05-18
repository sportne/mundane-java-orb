#!/usr/bin/env bash
set -euo pipefail

TARGET_REPO="${1:-local-maven-repo}"
TARGET_PARENT="$(dirname "${TARGET_REPO}")"
TARGET_NAME="$(basename "${TARGET_REPO}")"
mkdir -p "${TARGET_PARENT}"
TARGET_PARENT_ABS="$(cd "${TARGET_PARENT}" && pwd)"
TARGET_REPO_ABS="${TARGET_PARENT_ABS}/${TARGET_NAME}"
PROJECT_ROOT="$(pwd)"

if [ ! -x ./gradlew ]; then
  echo "Gradle wrapper is not executable. Run from the repository root." >&2
  exit 1
fi

if [ "${TARGET_REPO_ABS}" = "/" ] || [ "${TARGET_REPO_ABS}" = "${PROJECT_ROOT}" ]; then
  echo "Refusing to overwrite unsafe offline repository path: ${TARGET_REPO_ABS}" >&2
  exit 1
fi

rm -rf "${TARGET_REPO_ABS}"
mkdir -p "${TARGET_REPO_ABS}"

./gradlew --refresh-dependencies help >/dev/null
./gradlew stageReleasePublications validatePublicationDryRun

GRADLE_CACHE="${GRADLE_USER_HOME:-${HOME}/.gradle}/caches/modules-2/files-2.1"
if [ ! -d "${GRADLE_CACHE}" ]; then
  echo "Gradle module cache not found at ${GRADLE_CACHE}" >&2
  exit 1
fi

STAGING_REPO="build/staging-repository"
if [ ! -d "${STAGING_REPO}" ]; then
  echo "Publication staging repository not found at ${STAGING_REPO}" >&2
  exit 1
fi

find "${GRADLE_CACHE}" -mindepth 5 -maxdepth 5 -type f \
  \( -name '*.jar' -o -name '*.pom' -o -name '*.module' \) | sort |
while IFS= read -r artifact; do
  relative="${artifact#${GRADLE_CACHE}/}"
  group="${relative%%/*}"
  remainder="${relative#*/}"
  module="${remainder%%/*}"
  remainder="${remainder#*/}"
  version="${remainder%%/*}"
  file_name="${artifact##*/}"
  target_dir="${TARGET_REPO_ABS}/${group//.//}/${module}/${version}"
  target_file="${target_dir}/${file_name}"
  mkdir -p "${target_dir}"
  if [ ! -e "${target_file}" ]; then
    cp "${artifact}" "${target_file}"
  fi
done

find "${STAGING_REPO}" -type f | sort |
while IFS= read -r artifact; do
  relative="${artifact#${STAGING_REPO}/}"
  target_file="${TARGET_REPO_ABS}/${relative}"
  mkdir -p "$(dirname "${target_file}")"
  cp "${artifact}" "${target_file}"
done

MANIFEST="${TARGET_REPO_ABS}/MANIFEST.sha256"
find "${TARGET_REPO_ABS}" -type f \
  ! -name 'MANIFEST.sha256' \
  ! -name 'README.txt' | sort |
while IFS= read -r artifact; do
  checksum="$(sha256sum "${artifact}" | cut -d ' ' -f 1)"
  relative="${artifact#${TARGET_REPO_ABS}/}"
  printf '%s  %s\n' "${checksum}" "${relative}"
done > "${MANIFEST}"

cat > "${TARGET_REPO_ABS}/README.txt" <<EOF
Offline repository staging area for mundane-java-orb.

This Maven-layout directory was populated from the local Gradle dependency cache
and the project's build/staging-repository publication dry run.

Review MANIFEST.sha256 and approve contents before using it as a controlled
offline repository.

Run:

  ./gradlew --offline -Pcorba.offlineRepo=${TARGET_REPO_ABS} clean qualityGate
EOF

echo "Prepared offline repository staging area at ${TARGET_REPO_ABS}."
