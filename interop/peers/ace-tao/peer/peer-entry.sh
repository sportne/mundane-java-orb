#!/usr/bin/env bash
set -euo pipefail

role="${1:-${INTEROP_ROLE:-manual}}"
scenario="${INTEROP_SCENARIO:-manual}"
reports_dir="${INTEROP_REPORTS_DIR:-/interop/reports}"
iors_dir="${INTEROP_IORS_DIR:-/interop/iors}"
mkdir -p "${reports_dir}" "${iors_dir}" /interop/logs

write_peer_report() {
  local status="$1"
  local classification="$2"
  local message="$3"
  local report="${reports_dir}/${scenario}-${role}.ace-tao.json"
  cat >"${report}" <<JSON
{
  "peer": "ace-tao",
  "role": "${role}",
  "scenario": "${scenario}",
  "status": "${status}",
  "classification": "${classification}",
  "message": "${message}"
}
JSON
  printf '%s\n' "${report}"
}

case "${scenario}" in
  basic-idl|object-reference|giop|iiop|naming|rmi-iiop|health|report|manual)
    ;;
  *)
    write_peer_report "failed" "unsupported-scenario" \
      "ACE/TAO peer glue has no command mapping for this scenario"
    exit 67
    ;;
esac

if [[ "${role}" == "health" && "${scenario}" == "health" ]]; then
  write_peer_report "failed" "missing-prerequisite" \
    "Standalone ACE/TAO health requires a running scenario server; use run-scenario for live readiness"
  exit 66
fi

case "${role}" in
  client|health)
    exec /interop/peer/bin/ace_tao_peer "${role}"
    ;;
  server)
    exec /interop/peer/bin/ace_tao_peer server -ORBEndpoint iiop://0.0.0.0:2809
    ;;
  naming)
    ior_file="${iors_dir}/${scenario}-naming.ior"
    if command -v tao_cosnaming >/dev/null 2>&1; then
      exec tao_cosnaming -ORBEndpoint iiop://0.0.0.0:2809 -o "${ior_file}"
    fi
    write_peer_report "failed" "missing-prerequisite" \
      "ACE/TAO Naming Service executable tao_cosnaming is not available in the peer image"
    exit 66
    ;;
  report)
    write_peer_report "passed" "peer-report" \
      "ACE/TAO peer command contract is installed; live compatibility evidence is recorded by the outer harness"
    ;;
  *)
    printf 'unknown ACE/TAO peer role: %s\n' "${role}" >&2
    exit 64
    ;;
esac
