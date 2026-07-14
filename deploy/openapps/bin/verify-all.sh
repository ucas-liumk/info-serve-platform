#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
FAILED=0

while IFS= read -r app_code; do
  if "$ROOT_DIR/bin/appctl" "$app_code" config; then
    echo "PASS $app_code"
  else
    echo "FAIL $app_code" >&2
    FAILED=1
  fi
done < <("$ROOT_DIR/bin/appctl" list)

exit "$FAILED"
