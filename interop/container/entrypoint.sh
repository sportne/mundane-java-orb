#!/usr/bin/env bash
set -euo pipefail

command="${1:-help}"

run_peer_command() {
  local role="$1"
  local role_upper="${role^^}"
  local role_command_env="INTEROP_PEER_${role_upper}_COMMAND"
  local command_value="${!role_command_env:-${INTEROP_PEER_COMMAND:-}}"
  if [[ -n "${command_value}" ]]; then
    exec /usr/bin/env bash -lc "${command_value}"
  fi
  local command_path="/interop/peer/${role}.sh"
  if [[ -x "${command_path}" ]]; then
    exec "${command_path}"
  fi
  echo "Missing real peer ${role} command; set ${role_command_env} or provide ${command_path}" >&2
  exit 66
}

case "${command}" in
  help|--help|-h)
    echo "Interop peer container command contract."
    echo "Commands: metadata, client, server, naming, health, report."
    ;;
  metadata)
    echo "peer=${INTEROP_PEER:-unknown}"
    echo "stage=g10-110-real-peer-harness"
    ;;
  client|server|naming|report)
    mkdir -p /interop/logs /interop/iors /interop/reports
    if [[ ! -f /interop/scenario.idl && "${command}" != "report" ]]; then
      echo "Missing mounted scenario IDL: /interop/scenario.idl" >&2
      exit 66
    fi
    if [[ ! -d /interop/artifacts ]]; then
      echo "Missing mounted approved artifact cache: /interop/artifacts" >&2
      exit 66
    fi
    {
      echo "peer=${INTEROP_PEER:-unknown}"
      echo "role=${INTEROP_ROLE:-${command}}"
      echo "scenario=${INTEROP_SCENARIO:-manual}"
      echo "idl=${INTEROP_IDL:-unknown}"
      echo "stage=g10-110-real-peer-harness"
    } >"/interop/logs/${INTEROP_SCENARIO:-manual}-${command}.harness.log"
    if [[ -n "${INTEROP_PEER_COMMAND:-}" ]]; then
      exec ${INTEROP_PEER_COMMAND}
    fi
    if [[ "${command}" == "server" || "${command}" == "naming" ]]; then
      : >"/interop/iors/${INTEROP_SCENARIO:-manual}-${command}.ior"
    fi
    run_peer_command "${command}"
    ;;
  health)
    run_peer_command health
    ;;
  *)
    echo "Unknown interop peer command: ${command}" >&2
    exit 64
    ;;
esac
