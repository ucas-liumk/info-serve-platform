#!/usr/bin/env bash
set -Eeuo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMMON="$REPO_ROOT/deploy/scripts/update-common.sh"
TEST_ROOT="$(mktemp -d "${TMPDIR:-/tmp}/info-serve-update-test.XXXXXX")"
trap 'rm -rf "$TEST_ROOT"' EXIT

fail() {
  printf 'FAIL: %s\n' "$*" >&2
  exit 1
}

assert_eq() {
  [[ "$1" == "$2" ]] || fail "expected '$2', got '$1'"
}

FAKE_BIN="$TEST_ROOT/bin"
TARGET="$TEST_ROOT/target"
PACKAGE="$TEST_ROOT/package/info-serve-update-0.3.6"
EVENT_LOG="$TEST_ROOT/events.log"
mkdir -p "$FAKE_BIN" "$TARGET/plus-ui/dist" "$TARGET/deploy/compose/services" "$PACKAGE/scripts" "$PACKAGE/payload/plus-ui/dist"
printf '0.3.5\n' >"$TARGET/VERSION"
printf '<html>old</html>\n' >"$TARGET/plus-ui/dist/index.html"
printf 'x\n' >"$TARGET/deploy/.env"
printf 'services: {}\n' >"$TARGET/deploy/docker-compose.yml"
for file in portal-kernel.yml portal-appcenter.yml portal-forum.yml portal-requiredknowledge.yml portal-resources.yml; do
  printf 'services: %s\n' "$file" >"$TARGET/deploy/compose/services/$file"
done
printf '<html>new</html>\n' >"$PACKAGE/payload/plus-ui/dist/index.html"
cp "$REPO_ROOT/deploy/scripts/"{apply-update.sh,rollback-update.sh,update-common.sh,verify-package.sh,collect-baseline-receipt.sh} "$PACKAGE/scripts/"
chmod 0755 "$PACKAGE/scripts/"*.sh

cat >"$FAKE_BIN/docker" <<'FAKE_DOCKER'
#!/usr/bin/env bash
set -e
if [[ "$1" == inspect ]]; then
  format="$3"
  name="$4"
  case "$format" in
    '{{.Image}}')
      suffix="$(printf '%s' "$name${FAKE_IMAGE_MUTATE:-}" | sha256sum | awk '{print $1}')"
      printf 'sha256:%s\n' "$suffix"
      ;;
    '{{.Id}}') printf 'container-id-%s\n' "$name" ;;
    '{{.State.StartedAt}}') printf '2026-07-16T00:00:00Z\n' ;;
    '{{.State.Running}}') printf 'true\n' ;;
    *) exit 2 ;;
  esac
elif [[ "$1" == exec ]]; then
  printf '%s\n' \
    'table.portal_module=portal_module' \
    'table.info_resource_note=info_resource_note' \
    'table.info_resource_view_record=info_resource_view_record' \
    'table.info_resource_category_link=info_resource_category_link' \
    "column.info_resource_category.parent_id=bigint:YES${FAKE_DB_MUTATE:-}"
elif [[ "$1" == compose ]]; then
  printf 'compose-recreate\n' >>"$EVENT_LOG"
else
  exit 2
fi
FAKE_DOCKER

cat >"$FAKE_BIN/curl" <<'FAKE_CURL'
#!/usr/bin/env bash
set -e
args="$*"
if [[ "$args" == *'/v1/auth/login'* ]]; then
  printf '{"accessToken":"test-token"}\n'
elif [[ "$args" == *'/v1/cs/configs'* ]]; then
  printf 'nacos-config-%s-%s\n' "$args" "${FAKE_NACOS_MUTATE:-}"
else
  output=''
  url="${!#}"
  previous=''
  for arg in "$@"; do
    if [[ "$previous" == -o ]]; then output="$arg"; fi
    previous="$arg"
  done
  if [[ "$output" == /dev/null ]]; then
    printf '200'
  elif [[ "$url" == *'/prod-api/auth/code' ]]; then
    printf '{"code":200,"msg":"%s","data":{"captchaEnabled":false,"uuid":"%s","img":null}}\n' "${FAKE_DYNAMIC:-a}" "${FAKE_DYNAMIC:-a}" >"$output"
    printf '200|application/json;charset=UTF-8'
  elif [[ "$url" == *'/prod-api/'* ]]; then
    printf '{"code":%s,"msg":"dynamic","data":null}\n' "${FAKE_API_CODE:-401}" >"$output"
    printf '200|application/json;charset=UTF-8'
  else
    printf '<html></html>\n' >"$output"
    printf '200|text/html'
  fi
fi
FAKE_CURL
chmod 0755 "$FAKE_BIN/docker" "$FAKE_BIN/curl"

export PATH="$FAKE_BIN:$PATH"
export EVENT_LOG NACOS_USERNAME=test NACOS_PASSWORD=test INFO_SERVE_BASE_URL=http://test INFO_SERVE_HEALTH_BASE_URL=http://test

# Sourcing the common library must not execute an entrypoint.
source_output="$(bash -c "source '$COMMON'")"
assert_eq "$source_output" ''

# Canonical manifests support newline-containing filenames.
mkdir -p "$TEST_ROOT/manifest-tree"
printf 'a' >"$TEST_ROOT/manifest-tree/normal"
printf 'b' >"$TEST_ROOT/manifest-tree/line
break"
source "$COMMON"
metadata_fixture="$TEST_ROOT/metadata.env"
printf 'PACKAGE_VERSION=0.3.6\nDIST_MANIFEST_SHA256=%064d\n' 0 >"$metadata_fixture"
load_release_metadata "$metadata_fixture"
assert_eq "$PACKAGE_VERSION" '0.3.6'
canonical_manifest "$TEST_ROOT/manifest-tree" "$TEST_ROOT/tree.manifest"
verify_canonical_manifest "$TEST_ROOT/manifest-tree" "$TEST_ROOT/tree.manifest"

# A complete receipt is stable when dynamic API values change.
collect_baseline_receipt "$TARGET" "$TEST_ROOT/receipt-a.env"
FAKE_DYNAMIC=b collect_baseline_receipt "$TARGET" "$TEST_ROOT/receipt-b.env"
cmp -s "$TEST_ROOT/receipt-a.env" "$TEST_ROOT/receipt-b.env" || fail 'dynamic API fields changed receipt'

# Each evidence class changes the frozen receipt.
cp "$TEST_ROOT/receipt-a.env" "$TEST_ROOT/receipt-reference.env"
printf 'changed\n' >>"$TARGET/plus-ui/dist/index.html"
collect_baseline_receipt "$TARGET" "$TEST_ROOT/receipt-mutated.env"
cmp -s "$TEST_ROOT/receipt-reference.env" "$TEST_ROOT/receipt-mutated.env" && fail 'dist mutation was not detected'
printf '<html>old</html>\n' >"$TARGET/plus-ui/dist/index.html"

printf 'changed\n' >>"$TARGET/deploy/compose/services/portal-kernel.yml"
collect_baseline_receipt "$TARGET" "$TEST_ROOT/receipt-mutated.env"
cmp -s "$TEST_ROOT/receipt-reference.env" "$TEST_ROOT/receipt-mutated.env" && fail 'compose mutation was not detected'
printf 'services: portal-kernel.yml\n' >"$TARGET/deploy/compose/services/portal-kernel.yml"

FAKE_IMAGE_MUTATE=x collect_baseline_receipt "$TARGET" "$TEST_ROOT/receipt-mutated.env"
cmp -s "$TEST_ROOT/receipt-reference.env" "$TEST_ROOT/receipt-mutated.env" && fail 'image mutation was not detected'
FAKE_NACOS_MUTATE=x collect_baseline_receipt "$TARGET" "$TEST_ROOT/receipt-mutated.env"
cmp -s "$TEST_ROOT/receipt-reference.env" "$TEST_ROOT/receipt-mutated.env" && fail 'Nacos mutation was not detected'
FAKE_DB_MUTATE=x collect_baseline_receipt "$TARGET" "$TEST_ROOT/receipt-mutated.env"
cmp -s "$TEST_ROOT/receipt-reference.env" "$TEST_ROOT/receipt-mutated.env" && fail 'DB mutation was not detected'
FAKE_API_CODE=403 collect_baseline_receipt "$TARGET" "$TEST_ROOT/receipt-mutated.env"
cmp -s "$TEST_ROOT/receipt-reference.env" "$TEST_ROOT/receipt-mutated.env" && fail 'API capability mutation was not detected'

# Build a valid local package for apply/rollback behavior tests.
cp "$TEST_ROOT/receipt-reference.env" "$PACKAGE/BASELINE-RECEIPT.env"
new_dist_digest="$(canonical_tree_digest "$PACKAGE/payload/plus-ui/dist")"
old_dist_digest="$(awk -F= '$1=="DIST_MANIFEST_SHA256" {print $2}' "$PACKAGE/BASELINE-RECEIPT.env")"
cat >"$PACKAGE/RELEASE-METADATA.env" <<EOF
PACKAGE_VERSION=0.3.6
REQUIRED_BASE_VERSION=0.3.5
REQUIRED_BASE_COMMIT=f632f960b5d3fd1737b658e45fe43b15dd319e19
REQUIRED_BASELINE_ID=test-baseline
REQUIRED_BASE_DIST_MANIFEST_SHA256=$old_dist_digest
SOURCE_TAG=v0.3.6-rc.1
SOURCE_COMMIT=0000000000000000000000000000000000000000
SOURCE_SHORT_COMMIT=00000000
RELEASE_BRANCH_BASE_COMMIT=3f8b19d4bb87dcf02b36058a73be7991204350a8
NODE_VERSION=test
NPM_VERSION=test
PACKAGE_LOCK_SHA256=0000000000000000000000000000000000000000000000000000000000000000
DIST_MANIFEST_SHA256=$new_dist_digest
BUILD_ARCH=arm64
BUILD_TIME_UTC=2026-07-16T00:00:00Z
EOF
printf 'test\n' >"$PACKAGE/README.md"
canonical_manifest "$PACKAGE" "$TEST_ROOT/package.manifest"
mv "$TEST_ROOT/package.manifest" "$PACKAGE/MANIFEST.sha256"

# Failure injection happens after compose recreate and rolls back exactly once.
: >"$EVENT_LOG"
set +e
INFO_SERVE_ALLOW_FAILURE_INJECTION=1 "$PACKAGE/scripts/apply-update.sh" --target "$TARGET" --test-fail-after-switch >"$TEST_ROOT/apply-fail.log" 2>&1
apply_rc=$?
set -e
[[ "$apply_rc" -ne 0 ]] || fail 'failure injection unexpectedly succeeded'
assert_eq "$(tr -d '[:space:]' <"$TARGET/VERSION")" '0.3.5'
grep -q 'Failure injection enabled after nginx recreate' "$TEST_ROOT/apply-fail.log" || fail 'failure injection did not occur after recreate'
assert_eq "$(grep -c 'Automatic rollback completed' "$TEST_ROOT/apply-fail.log")" '1'
assert_eq "$(grep -c 'compose-recreate' "$EVENT_LOG")" '2'
assert_eq "$(cat "$TARGET/plus-ui/dist/index.html")" '<html>old</html>'

# Normal apply followed by standalone rollback restores the complete baseline.
"$PACKAGE/scripts/apply-update.sh" --target "$TARGET" >"$TEST_ROOT/apply-ok.log" 2>&1
assert_eq "$(tr -d '[:space:]' <"$TARGET/VERSION")" '0.3.6'
"$PACKAGE/scripts/rollback-update.sh" --target "$TARGET" --backup latest >"$TEST_ROOT/rollback.log" 2>&1
assert_eq "$(tr -d '[:space:]' <"$TARGET/VERSION")" '0.3.5'
assert_eq "$(cat "$TARGET/plus-ui/dist/index.html")" '<html>old</html>'

# Lock conflicts fail before changing the target.
mkdir "$TARGET/.update.lock"
set +e
"$PACKAGE/scripts/apply-update.sh" --target "$TARGET" >"$TEST_ROOT/lock.log" 2>&1
lock_rc=$?
set -e
rmdir "$TARGET/.update.lock"
[[ "$lock_rc" -ne 0 ]] || fail 'lock conflict unexpectedly succeeded'
assert_eq "$(tr -d '[:space:]' <"$TARGET/VERSION")" '0.3.5'

printf 'PASS: update package scripts\n'
