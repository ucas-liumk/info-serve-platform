#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PACKAGE_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
# shellcheck source=update-common.sh
source "$SCRIPT_DIR/update-common.sh"

TARGET=''
TEST_FAIL_AFTER_SWITCH=0
BACKUP_DIR=''
OPERATION_ID="apply-$(date -u +%Y%m%dT%H%M%SZ)-$$"
ROLLBACK_IN_PROGRESS=0
LOCK_ACQUIRED=0
OLD_DIST=''

while [[ $# -gt 0 ]]; do
  case "$1" in
    --target) TARGET="${2:-}"; shift 2 ;;
    --test-fail-after-switch) TEST_FAIL_AFTER_SWITCH=1; shift ;;
    *) info_serve_die "unknown argument: $1"; exit 2 ;;
  esac
done

[[ -n "$TARGET" ]] || { info_serve_die 'usage: apply-update.sh --target PATH [--test-fail-after-switch]'; exit 2; }
validate_target_root "$TARGET"

LOG_DIR="$TARGET/.update-logs"
mkdir -p "$LOG_DIR"
LOG_FILE="$LOG_DIR/$OPERATION_ID.log"
exec > >(tee -a "$LOG_FILE") 2>&1

cleanup_apply() {
  if [[ "$LOCK_ACQUIRED" == 1 ]]; then
    release_update_lock || true
  fi
}

handle_apply_error() {
  local rc="$?"
  trap - ERR
  if [[ "$ROLLBACK_IN_PROGRESS" == 0 && -n "$BACKUP_DIR" && -f "$BACKUP_DIR/BACKUP_COMPLETE" ]]; then
    ROLLBACK_IN_PROGRESS=1
    printf 'Upgrade failed with rc=%s; starting one-time automatic rollback.\n' "$rc"
    if restore_from_backup "$TARGET" "$BACKUP_DIR" "$OPERATION_ID-auto"; then
      printf 'Automatic rollback completed.\n'
    else
      printf 'Automatic rollback failed; preserve %s and %s for manual recovery.\n' "$BACKUP_DIR" "$LOG_FILE" >&2
      exit 70
    fi
  fi
  exit "$rc"
}

trap cleanup_apply EXIT
trap handle_apply_error ERR

"$SCRIPT_DIR/verify-package.sh"
load_release_metadata "$PACKAGE_ROOT/RELEASE-METADATA.env"

[[ "$(tr -d '[:space:]' <"$TARGET/VERSION")" == "$REQUIRED_BASE_VERSION" ]] || info_serve_die "target version is not $REQUIRED_BASE_VERSION"

CURRENT_RECEIPT="$(mktemp "${TMPDIR:-/tmp}/info-serve-current-receipt.XXXXXX")"
collect_baseline_receipt "$TARGET" "$CURRENT_RECEIPT"
if ! cmp -s "$PACKAGE_ROOT/BASELINE-RECEIPT.env" "$CURRENT_RECEIPT"; then
  diff -u "$PACKAGE_ROOT/BASELINE-RECEIPT.env" "$CURRENT_RECEIPT" || true
  rm -f "$CURRENT_RECEIPT"
  info_serve_die 'target baseline receipt does not match this single-snapshot package'
fi
rm -f "$CURRENT_RECEIPT"

if [[ "$TEST_FAIL_AFTER_SWITCH" == 1 && "${INFO_SERVE_ALLOW_FAILURE_INJECTION:-0}" != 1 ]]; then
  info_serve_die 'failure injection requires INFO_SERVE_ALLOW_FAILURE_INJECTION=1'
fi

acquire_update_lock "$TARGET"
LOCK_ACQUIRED=1

# Close the preflight/write race after acquiring the lock.
CURRENT_RECEIPT="$(mktemp "${TMPDIR:-/tmp}/info-serve-current-receipt.XXXXXX")"
collect_baseline_receipt "$TARGET" "$CURRENT_RECEIPT"
cmp -s "$PACKAGE_ROOT/BASELINE-RECEIPT.env" "$CURRENT_RECEIPT" || info_serve_die 'target changed after preflight'
rm -f "$CURRENT_RECEIPT"

BACKUP_DIR="$TARGET/.update-backups/${PACKAGE_VERSION}-$(date -u +%Y%m%dT%H%M%SZ)-${SOURCE_SHORT_COMMIT}-$$"
[[ ! -e "$BACKUP_DIR" ]] || info_serve_die "backup already exists: $BACKUP_DIR"
mkdir -p "$BACKUP_DIR/payload/plus-ui"
cp -a "$TARGET/plus-ui/dist" "$BACKUP_DIR/payload/plus-ui/dist"
cp -a "$TARGET/VERSION" "$BACKUP_DIR/payload/VERSION"
if [[ -f "$TARGET/.release-info" ]]; then
  cp -a "$TARGET/.release-info" "$BACKUP_DIR/payload/.release-info"
  printf 'RELEASE_INFO_PRESENT=1\n' >"$BACKUP_DIR/BACKUP-METADATA.env"
else
  printf 'RELEASE_INFO_PRESENT=0\n' >"$BACKUP_DIR/BACKUP-METADATA.env"
fi
canonical_manifest "$BACKUP_DIR/payload" "$BACKUP_DIR/PAYLOAD-MANIFEST.sha256z"
verify_canonical_manifest "$BACKUP_DIR/payload" "$BACKUP_DIR/PAYLOAD-MANIFEST.sha256z"
printf 'complete\n' >"$BACKUP_DIR/.BACKUP_COMPLETE.tmp"
mv "$BACKUP_DIR/.BACKUP_COMPLETE.tmp" "$BACKUP_DIR/BACKUP_COMPLETE"

STAGE="$TARGET/plus-ui/.dist-update-stage-$OPERATION_ID"
OLD_DIST="$TARGET/plus-ui/.dist-old-$OPERATION_ID"
[[ ! -e "$STAGE" && ! -e "$OLD_DIST" ]] || info_serve_die 'update staging path already exists'
cp -a "$PACKAGE_ROOT/payload/plus-ui/dist" "$STAGE"
[[ "$(canonical_tree_digest "$STAGE")" == "$DIST_MANIFEST_SHA256" ]] || info_serve_die 'staged dist fingerprint mismatch'
mv "$TARGET/plus-ui/dist" "$OLD_DIST"
mv "$STAGE" "$TARGET/plus-ui/dist"
printf '%s\n' "$PACKAGE_VERSION" >"$TARGET/.VERSION.update-$OPERATION_ID"
mv "$TARGET/.VERSION.update-$OPERATION_ID" "$TARGET/VERSION"
{
  printf 'VERSION=%s\n' "$PACKAGE_VERSION"
  printf 'SOURCE_TAG=%s\n' "$SOURCE_TAG"
  printf 'SOURCE_COMMIT=%s\n' "$SOURCE_COMMIT"
  printf 'BASELINE_ID=%s\n' "$REQUIRED_BASELINE_ID"
  printf 'APPLIED_AT_UTC=%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
} >"$TARGET/.release-info.update-$OPERATION_ID"
mv "$TARGET/.release-info.update-$OPERATION_ID" "$TARGET/.release-info"

OLD_CONTAINER_ID="$(docker inspect --format '{{.Id}}' infosys-ruoyi-cloud-plus-web 2>/dev/null || true)"
recreate_nginx_web "$TARGET"
NEW_CONTAINER_ID="$(docker inspect --format '{{.Id}}' infosys-ruoyi-cloud-plus-web)"
NEW_STARTED_AT="$(docker inspect --format '{{.State.StartedAt}}' infosys-ruoyi-cloud-plus-web)"
printf 'nginx-web old=%s new=%s started=%s\n' "$OLD_CONTAINER_ID" "$NEW_CONTAINER_ID" "$NEW_STARTED_AT"

if [[ "$TEST_FAIL_AFTER_SWITCH" == 1 ]]; then
  printf 'Failure injection enabled after nginx recreate.\n'
  false
fi

official_health_check
rm -rf "$OLD_DIST"
OLD_DIST=''
printf 'Update completed: %s -> %s\n' "$REQUIRED_BASE_VERSION" "$PACKAGE_VERSION"
printf 'Backup retained: %s\n' "$BACKUP_DIR"
