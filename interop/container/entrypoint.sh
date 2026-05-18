#!/usr/bin/env bash
set -euo pipefail

command="${1:-help}"

case "${command}" in
  help|--help|-h)
    echo "Interop peer container command contract."
    echo "Commands: metadata, client, server, naming, health, report."
    ;;
  metadata)
    echo "peer=${INTEROP_PEER:-unknown}"
    echo "stage=g6-830-report-harness"
    ;;
  client|server|naming|report)
    mkdir -p /interop/logs /interop/iors /interop/reports
    echo "peer=${INTEROP_PEER:-unknown}"
    echo "role=${INTEROP_ROLE:-${command}}"
    echo "scenario=${INTEROP_SCENARIO:-manual}"
    echo "stage=g6-830-report-harness"
    if [[ "${command}" == "server" || "${command}" == "naming" ]]; then
      echo "IOR:scaffold:${INTEROP_PEER:-unknown}:${INTEROP_SCENARIO:-manual}:${command}" \
        >"/interop/iors/${INTEROP_SCENARIO:-manual}-${command}.ior"
    fi
    ;;
  health)
    echo "ready: g6-830 container command contract active"
    ;;
  *)
    echo "Unknown scaffold command: ${command}" >&2
    exit 64
    ;;
esac
