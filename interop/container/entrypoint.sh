#!/usr/bin/env bash
set -euo pipefail

command="${1:-help}"

case "${command}" in
  help|--help|-h)
    echo "Interop peer container scaffold."
    echo "Real ORB launch commands are intentionally not implemented in G4."
    ;;
  metadata)
    echo "peer=${INTEROP_PEER:-unknown}"
    echo "stage=g4-script-scaffold"
    ;;
  health)
    echo "not-ready: real peer health checks require later approved peer launch work"
    exit 2
    ;;
  *)
    echo "Unknown scaffold command: ${command}" >&2
    exit 64
    ;;
esac
