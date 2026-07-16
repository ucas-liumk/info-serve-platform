#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=update-common.sh
source "$SCRIPT_DIR/update-common.sh"

TARGET=''
OUTPUT=''

while [[ $# -gt 0 ]]; do
  case "$1" in
    --target) TARGET="${2:-}"; shift 2 ;;
    --output) OUTPUT="${2:-}"; shift 2 ;;
    *) info_serve_die "unknown argument: $1"; exit 2 ;;
  esac
done

[[ -n "$TARGET" && -n "$OUTPUT" ]] || { info_serve_die 'usage: collect-baseline-receipt.sh --target PATH --output FILE'; exit 2; }
collect_baseline_receipt "$TARGET" "$OUTPUT"
printf 'Baseline receipt written: %s\n' "$OUTPUT"
