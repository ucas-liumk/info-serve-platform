#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
# shellcheck source=update-common.sh
source "$SCRIPT_DIR/update-common.sh"

BASELINE_RECEIPT=''
OUTPUT_DIR="$REPO_ROOT/releases"
RELEASE_BRANCH_BASE_COMMIT='3f8b19d4bb87dcf02b36058a73be7991204350a8'
REQUIRED_BASE_COMMIT='f632f960b5d3fd1737b658e45fe43b15dd319e19'

while [[ $# -gt 0 ]]; do
  case "$1" in
    --baseline-receipt) BASELINE_RECEIPT="${2:-}"; shift 2 ;;
    --output-dir) OUTPUT_DIR="${2:-}"; shift 2 ;;
    *) info_serve_die "unknown argument: $1"; exit 2 ;;
  esac
done

[[ -f "$BASELINE_RECEIPT" ]] || { info_serve_die 'usage: package-update.sh --baseline-receipt FILE [--output-dir DIR]'; exit 2; }
[[ "$(tr -d '[:space:]' <"$REPO_ROOT/VERSION")" == 0.3.6 ]] || { info_serve_die 'VERSION must be 0.3.6'; exit 1; }
[[ -z "$(git -C "$REPO_ROOT" status --porcelain)" ]] || { info_serve_die 'git worktree must be clean'; exit 1; }

SOURCE_COMMIT="$(git -C "$REPO_ROOT" rev-parse HEAD)"
SOURCE_SHORT_COMMIT="$(git -C "$REPO_ROOT" rev-parse --short=8 HEAD)"
SOURCE_TAG="$(git -C "$REPO_ROOT" describe --tags --exact-match HEAD)"
[[ "$SOURCE_TAG" == v0.3.6 || "$SOURCE_TAG" == v0.3.6-rc.* ]] || { info_serve_die 'HEAD must have v0.3.6 or v0.3.6-rc.N tag'; exit 1; }
git -C "$REPO_ROOT" merge-base --is-ancestor "$RELEASE_BRANCH_BASE_COMMIT" HEAD
[[ "$(git -C "$REPO_ROOT" merge-base "$RELEASE_BRANCH_BASE_COMMIT" HEAD)" == "$RELEASE_BRANCH_BASE_COMMIT" ]] || { info_serve_die 'release branch base mismatch'; exit 1; }

while IFS= read -r changed; do
  case "$changed" in
    plus-ui/*|docs/*|VERSION|deploy/scripts/package-update.sh|deploy/scripts/apply-update.sh|deploy/scripts/rollback-update.sh|deploy/scripts/update-common.sh|deploy/scripts/verify-package.sh|deploy/scripts/collect-baseline-receipt.sh|deploy/tests/update-package-test.sh|deploy/update/README.md|deploy/updates/MANIFEST.md) ;;
    *) info_serve_die "file outside release allowlist: $changed"; exit 1 ;;
  esac
done < <(git -C "$REPO_ROOT" diff --name-only "$REQUIRED_BASE_COMMIT..HEAD")

if [[ "${INFO_SERVE_PACKAGE_SKIP_BUILD:-0}" != 1 ]]; then
  (
    cd "$REPO_ROOT/plus-ui"
    rm -rf dist
    npm ci
    npm run test
    mapfile -t lint_files < <(git -C "$REPO_ROOT" diff --name-only --diff-filter=ACMR "$REQUIRED_BASE_COMMIT..HEAD" -- 'plus-ui/src/**/*.ts' 'plus-ui/src/**/*.vue' | sed 's#^plus-ui/##')
    if [[ "${#lint_files[@]}" -gt 0 ]]; then
      npm run lint:eslint -- "${lint_files[@]}"
    fi
    npm run design:audit
    npm run build:prod
  )
else
  [[ "${INFO_SERVE_TEST_MODE:-0}" == 1 ]] || { info_serve_die 'skip-build is test-only'; exit 1; }
fi

[[ -f "$REPO_ROOT/plus-ui/dist/index.html" ]] || { info_serve_die 'production dist was not created'; exit 1; }
[[ -z "$(git -C "$REPO_ROOT" status --porcelain)" ]] || { info_serve_die 'build changed tracked files'; exit 1; }

grep -qx 'RECEIPT_SCHEMA_VERSION=1' "$BASELINE_RECEIPT" || { info_serve_die 'invalid baseline receipt schema'; exit 1; }
REQUIRED_BASE_DIST_MANIFEST_SHA256="$(awk -F= '$1=="DIST_MANIFEST_SHA256" {print $2}' "$BASELINE_RECEIPT")"
[[ "$REQUIRED_BASE_DIST_MANIFEST_SHA256" =~ ^[0-9a-f]{64}$ ]] || { info_serve_die 'invalid baseline dist fingerprint'; exit 1; }

mkdir -p "$OUTPUT_DIR"
ASSEMBLY_PARENT="$(mktemp -d "$OUTPUT_DIR/.0.3.6-assembly.XXXXXX")"
PACKAGE_ROOT="$ASSEMBLY_PARENT/info-serve-update-0.3.6"
mkdir -p "$PACKAGE_ROOT/payload/plus-ui" "$PACKAGE_ROOT/scripts"
trap 'rm -rf "$ASSEMBLY_PARENT"' EXIT

cp -a "$REPO_ROOT/plus-ui/dist" "$PACKAGE_ROOT/payload/plus-ui/dist"
cp "$BASELINE_RECEIPT" "$PACKAGE_ROOT/BASELINE-RECEIPT.env"
cp "$REPO_ROOT/deploy/update/README.md" "$PACKAGE_ROOT/README.md"
for script in apply-update.sh rollback-update.sh update-common.sh verify-package.sh collect-baseline-receipt.sh; do
  cp "$REPO_ROOT/deploy/scripts/$script" "$PACKAGE_ROOT/scripts/$script"
done
chmod 0755 "$PACKAGE_ROOT/scripts/"*.sh

DIST_MANIFEST_SHA256="$(canonical_tree_digest "$PACKAGE_ROOT/payload/plus-ui/dist")"
PACKAGE_LOCK_SHA256="$(sha256_file "$REPO_ROOT/plus-ui/package-lock.json")"
{
  printf 'PACKAGE_VERSION=0.3.6\n'
  printf 'REQUIRED_BASE_VERSION=0.3.5\n'
  printf 'REQUIRED_BASE_COMMIT=%s\n' "$REQUIRED_BASE_COMMIT"
  printf 'REQUIRED_BASELINE_ID=utm-kylin-20260716-pre-0.3.6\n'
  printf 'REQUIRED_BASE_DIST_MANIFEST_SHA256=%s\n' "$REQUIRED_BASE_DIST_MANIFEST_SHA256"
  printf 'SOURCE_TAG=%s\n' "$SOURCE_TAG"
  printf 'SOURCE_COMMIT=%s\n' "$SOURCE_COMMIT"
  printf 'SOURCE_SHORT_COMMIT=%s\n' "$SOURCE_SHORT_COMMIT"
  printf 'RELEASE_BRANCH_BASE_COMMIT=%s\n' "$RELEASE_BRANCH_BASE_COMMIT"
  printf 'NODE_VERSION=%s\n' "$(node --version)"
  printf 'NPM_VERSION=%s\n' "$(npm --version)"
  printf 'PACKAGE_LOCK_SHA256=%s\n' "$PACKAGE_LOCK_SHA256"
  printf 'DIST_MANIFEST_SHA256=%s\n' "$DIST_MANIFEST_SHA256"
  printf 'BUILD_ARCH=arm64\n'
  printf 'BUILD_TIME_UTC=%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
} >"$PACKAGE_ROOT/RELEASE-METADATA.env"

MANIFEST_TEMP="$ASSEMBLY_PARENT/MANIFEST.sha256"
canonical_manifest "$PACKAGE_ROOT" "$MANIFEST_TEMP"
mv "$MANIFEST_TEMP" "$PACKAGE_ROOT/MANIFEST.sha256"
verify_canonical_manifest "$PACKAGE_ROOT" "$PACKAGE_ROOT/MANIFEST.sha256"

ARCHIVE="$OUTPUT_DIR/info-serve-update-0.3.6-${SOURCE_SHORT_COMMIT}-arm64.tar.gz"
tar -C "$ASSEMBLY_PARENT" -czf "$ARCHIVE" "$(basename "$PACKAGE_ROOT")"
(cd "$OUTPUT_DIR" && sha256sum "$(basename "$ARCHIVE")" >"$(basename "$ARCHIVE").sha256")
printf 'Archive: %s\n' "$ARCHIVE"
printf 'SHA256: %s\n' "$(sha256_file "$ARCHIVE")"
