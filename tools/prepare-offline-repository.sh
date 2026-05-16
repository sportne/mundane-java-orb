#!/usr/bin/env bash
set -euo pipefail

TARGET_REPO="${1:-local-maven-repo}"
mkdir -p "${TARGET_REPO}"

if [ ! -x ./gradlew ]; then
  echo "Gradle wrapper is not executable. Run from the repository root." >&2
  exit 1
fi

./gradlew --refresh-dependencies help >/dev/null
./gradlew publishToMavenLocal

GRADLE_CACHE="${GRADLE_USER_HOME:-${HOME}/.gradle}/caches/modules-2/files-2.1"
if [ ! -d "${GRADLE_CACHE}" ]; then
  echo "Gradle module cache not found at ${GRADLE_CACHE}" >&2
  exit 1
fi

find "${GRADLE_CACHE}" -mindepth 5 -maxdepth 5 -type f \
  \( -name '*.jar' -o -name '*.pom' -o -name '*.module' \) |
while IFS= read -r artifact; do
  relative="${artifact#${GRADLE_CACHE}/}"
  group="${relative%%/*}"
  remainder="${relative#*/}"
  module="${remainder%%/*}"
  remainder="${remainder#*/}"
  version="${remainder%%/*}"
  file_name="${artifact##*/}"
  target_dir="${TARGET_REPO}/${group//.//}/${module}/${version}"
  target_file="${target_dir}/${file_name}"
  mkdir -p "${target_dir}"
  if [ ! -e "${target_file}" ]; then
    cp "${artifact}" "${target_file}"
  fi
done

cat > "${TARGET_REPO}/README.txt" <<EOF
Offline repository staging area for mundane-java-orb.

This Maven-layout directory was populated from the local Gradle dependency cache.
Review and approve contents before using it as a controlled offline repository.

Run:

  ./gradlew --offline -Pcorba.offlineRepo=${TARGET_REPO} clean qualityGate
EOF

echo "Prepared offline repository staging area at ${TARGET_REPO}."
