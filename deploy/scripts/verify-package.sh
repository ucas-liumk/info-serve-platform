#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PACKAGE_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
# shellcheck source=update-common.sh
source "$SCRIPT_DIR/update-common.sh"

[[ -f "$PACKAGE_ROOT/MANIFEST.sha256" ]] || { info_serve_die 'package manifest is missing'; exit 1; }
[[ -f "$PACKAGE_ROOT/RELEASE-METADATA.env" ]] || { info_serve_die 'release metadata is missing'; exit 1; }
[[ -f "$PACKAGE_ROOT/BASELINE-RECEIPT.env" ]] || { info_serve_die 'baseline receipt is missing'; exit 1; }
verify_canonical_manifest "$PACKAGE_ROOT" "$PACKAGE_ROOT/MANIFEST.sha256"
printf 'Package contents verified: %s\n' "$PACKAGE_ROOT"
