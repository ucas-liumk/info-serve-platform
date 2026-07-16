#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=update-common.sh
source "$SCRIPT_DIR/update-common.sh"

TARGET=''
BACKUP='latest'
LOCK_ACQUIRED=0
OPERATION_ID="rollback-$(date -u +%Y%m%dT%H%M%SZ)-$$"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --target) TARGET="${2:-}"; shift 2 ;;
    --backup) BACKUP="${2:-}"; shift 2 ;;
    *) info_serve_die "unknown argument: $1"; exit 2 ;;
  esac
done

[[ -n "$TARGET" ]] || { info_serve_die 'usage: rollback-update.sh --target PATH [--backup latest|PATH]'; exit 2; }
validate_target_root "$TARGET"

LOG_DIR="$TARGET/.update-logs"
mkdir -p "$LOG_DIR"
LOG_FILE="$LOG_DIR/$OPERATION_ID.log"
exec > >(tee -a "$LOG_FILE") 2>&1
trap '[[ "$LOCK_ACQUIRED" == 1 ]] && release_update_lock || true' EXIT

if [[ "$BACKUP" == latest ]]; then
  BACKUP=''
  while IFS= read -r candidate; do
    if [[ -f "$candidate/BACKUP_COMPLETE" ]] && verify_backup_payload "$candidate" >/dev/null 2>&1; then
      BACKUP="$candidate"
      break
    fi
  done < <(find "$TARGET/.update-backups" -mindepth 1 -maxdepth 1 -type d -name '0.3.6-*' -print 2>/dev/null | LC_ALL=C sort -r)
  [[ -n "$BACKUP" ]] || info_serve_die 'no complete backup found'
else
  [[ "$BACKUP" == /* ]] || BACKUP="$TARGET/.update-backups/$BACKUP"
fi

verify_backup_payload "$BACKUP"
acquire_update_lock "$TARGET"
LOCK_ACQUIRED=1
restore_from_backup "$TARGET" "$BACKUP" "$OPERATION_ID"
printf 'Rollback completed from: %s\n' "$BACKUP"
