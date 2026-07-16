#!/usr/bin/env bash

# Shared, side-effect-free helpers for info-serve update scripts.
# This file intentionally has no main entrypoint.

UPDATE_COMMON_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

info_serve_die() {
  printf 'ERROR: %s\n' "$*" >&2
  return 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || info_serve_die "required command not found: $1"
}

sha256_file() {
  sha256sum <"$1" | awk '{print $1}'
}

sha256_stdin() {
  sha256sum | awk '{print $1}'
}

load_release_metadata() {
  local metadata="$1"
  local key value
  [[ -f "$metadata" ]] || info_serve_die "release metadata not found: $metadata"
  while IFS='=' read -r key value; do
    [[ "$key" =~ ^[A-Z][A-Z0-9_]*$ ]] || info_serve_die "invalid metadata key: $key"
    [[ "$value" != *$'\n'* && "$value" != *$'\r'* ]] || info_serve_die "invalid metadata value for $key"
    case "$key" in
      PACKAGE_VERSION|REQUIRED_BASE_VERSION|REQUIRED_BASE_COMMIT|REQUIRED_BASELINE_ID|REQUIRED_BASE_DIST_MANIFEST_SHA256|SOURCE_TAG|SOURCE_COMMIT|SOURCE_SHORT_COMMIT|RELEASE_BRANCH_BASE_COMMIT|NODE_VERSION|NPM_VERSION|PACKAGE_LOCK_SHA256|DIST_MANIFEST_SHA256|BUILD_ARCH|BUILD_TIME_UTC)
        printf -v "$key" '%s' "$value"
        export "$key"
        ;;
      *) info_serve_die "unknown metadata key: $key" ;;
    esac
  done <"$metadata"
  [[ "${PACKAGE_VERSION:-}" == 0.3.6 && -n "${DIST_MANIFEST_SHA256:-}" ]] || info_serve_die "release metadata is incomplete"
}

canonical_manifest() {
  local root="$1"
  local output="$2"
  local temp

  [[ -d "$root" ]] || info_serve_die "manifest root does not exist: $root"
  temp="$(mktemp "${TMPDIR:-/tmp}/info-serve-manifest.XXXXXX")"
  (
    cd "$root"
    while IFS= read -r -d '' file; do
      sha256sum -z "$file"
    done < <(find . -type f -print0 | LC_ALL=C sort -z)
  ) >"$temp"
  mv "$temp" "$output"
}

verify_canonical_manifest() {
  local root="$1"
  local manifest="$2"
  local record expected relative actual

  [[ -f "$manifest" ]] || info_serve_die "manifest not found: $manifest"
  while IFS= read -r -d '' record; do
    expected="${record%% *}"
    relative="${record#*  }"
    [[ "$relative" == ./* ]] || info_serve_die "invalid manifest path: $relative"
    [[ -f "$root/${relative#./}" ]] || info_serve_die "manifest file missing: $relative"
    actual="$(sha256_file "$root/${relative#./}")"
    [[ "$actual" == "$expected" ]] || info_serve_die "manifest mismatch: $relative"
  done <"$manifest"
}

canonical_tree_digest() {
  local root="$1"
  local temp digest
  temp="$(mktemp "${TMPDIR:-/tmp}/info-serve-tree.XXXXXX")"
  canonical_manifest "$root" "$temp"
  digest="$(sha256_file "$temp")"
  rm -f "$temp"
  printf '%s\n' "$digest"
}

validate_target_root() {
  local target="$1"
  [[ -n "$target" && "$target" == /* && "$target" != "/" ]] || info_serve_die "target must be a non-root absolute path"
  [[ -d "$target/plus-ui/dist" ]] || info_serve_die "target dist not found: $target/plus-ui/dist"
  [[ -f "$target/VERSION" ]] || info_serve_die "target VERSION not found"
  [[ -f "$target/deploy/docker-compose.yml" && -f "$target/deploy/.env" ]] || info_serve_die "target compose files not found"
}

http_probe() {
  local url="$1"
  local body_file="$2"
  curl -fsS -o "$body_file" -w '%{http_code}|%{content_type}' "$url"
}

normalize_api_capabilities() {
  local base_url="${INFO_SERVE_BASE_URL:-http://127.0.0.1:7010}"
  local temp_dir="$1"
  local output="$2"
  local probe http content_type code value_type path key

  : >"$output"

  probe="$(http_probe "$base_url/prod-api/auth/code" "$temp_dir/auth.json")"
  IFS='|' read -r http content_type <<<"$probe"
  code="$(sed -n 's/.*"code":\([0-9][0-9]*\).*/\1/p' "$temp_dir/auth.json" | head -1)"
  grep -Eq '"captchaEnabled":(true|false)' "$temp_dir/auth.json" || info_serve_die "captchaEnabled is not boolean"
  value_type=boolean
  [[ -n "$code" ]] || info_serve_die "auth API code missing"
  printf 'auth.code=%s|%s|%s|%s\n' "$http" "${content_type%%;*}" "$code" "$value_type" >>"$output"

  for key in categories page tree; do
    case "$key" in
      categories) path='/prod-api/portal/resources/categories' ;;
      page) path='/prod-api/portal/resources/page' ;;
      tree) path='/prod-api/resource/category/tree' ;;
    esac
    probe="$(http_probe "$base_url$path" "$temp_dir/$key.json")"
    IFS='|' read -r http content_type <<<"$probe"
    code="$(sed -n 's/.*"code":\([0-9][0-9]*\).*/\1/p' "$temp_dir/$key.json" | head -1)"
    [[ -n "$code" ]] || info_serve_die "$key API code missing"
    printf '%s=%s|%s|%s\n' "$key" "$http" "${content_type%%;*}" "$code" >>"$output"
  done

  for key in resources preview; do
    case "$key" in
      resources) path='/portal/resources' ;;
      preview) path='/portal/resources/preview/1' ;;
    esac
    probe="$(http_probe "$base_url$path" "$temp_dir/$key.html")"
    IFS='|' read -r http content_type <<<"$probe"
    printf '%s=%s|%s\n' "$key" "$http" "${content_type%%;*}" >>"$output"
  done

  LC_ALL=C sort -o "$output" "$output"
}

collect_baseline_receipt() {
  local target="$1"
  local output="$2"
  local temp_dir temp_output token content data_id namespace key container image_id git_present git_head
  local db_markers api_capabilities compose_file compose_key db_sql
  local nacos_base_url="${NACOS_BASE_URL:-http://127.0.0.1:8148/nacos}"
  local nacos_username="${NACOS_USERNAME:-}"
  local nacos_password="${NACOS_PASSWORD:-}"

  require_command curl
  require_command docker
  require_command sha256sum
  validate_target_root "$target"
  [[ -n "$nacos_username" && -n "$nacos_password" ]] || info_serve_die "NACOS_USERNAME and NACOS_PASSWORD are required"

  temp_dir="$(mktemp -d "${TMPDIR:-/tmp}/info-serve-receipt.XXXXXX")"
  temp_output="$temp_dir/receipt.env"
  db_markers="$temp_dir/db-markers.txt"
  api_capabilities="$temp_dir/api-capabilities.txt"
  trap 'rm -rf "$temp_dir"' RETURN

  printf 'RECEIPT_SCHEMA_VERSION=1\n' >"$temp_output"
  printf 'BASE_VERSION=%s\n' "$(tr -d '[:space:]' <"$target/VERSION")" >>"$temp_output"

  if [[ -d "$target/.git" || -f "$target/.git" ]]; then
    git_present=1
    git_head="$(git -C "$target" rev-parse HEAD)"
  else
    git_present=0
    git_head=ABSENT
  fi
  printf 'TARGET_GIT_PRESENT=%s\nTARGET_GIT_HEAD=%s\n' "$git_present" "$git_head" >>"$temp_output"
  printf 'DIST_MANIFEST_SHA256=%s\n' "$(canonical_tree_digest "$target/plus-ui/dist")" >>"$temp_output"

  for compose_key in KERNEL APPCENTER FORUM REQUIREDKNOWLEDGE RESOURCES; do
    case "$compose_key" in
      KERNEL) compose_file='portal-kernel.yml' ;;
      APPCENTER) compose_file='portal-appcenter.yml' ;;
      FORUM) compose_file='portal-forum.yml' ;;
      REQUIREDKNOWLEDGE) compose_file='portal-requiredknowledge.yml' ;;
      RESOURCES) compose_file='portal-resources.yml' ;;
    esac
    [[ -f "$target/deploy/compose/services/$compose_file" ]] || info_serve_die "compose baseline file missing: $compose_file"
    printf 'COMPOSE_PORTAL_%s_SHA256=%s\n' "$compose_key" "$(sha256_file "$target/deploy/compose/services/$compose_file")" >>"$temp_output"
  done

  for key in GATEWAY PORTAL_KERNEL PORTAL_APPCENTER PORTAL_FORUM PORTAL_REQUIREDKNOWLEDGE PORTAL_RESOURCES; do
    case "$key" in
      GATEWAY) container='infosys-ruoyi-cloud-plus-gateway' ;;
      PORTAL_KERNEL) container='infosys-ruoyi-cloud-plus-portal-kernel' ;;
      PORTAL_APPCENTER) container='infosys-ruoyi-cloud-plus-portal-appcenter' ;;
      PORTAL_FORUM) container='infosys-ruoyi-cloud-plus-portal-forum' ;;
      PORTAL_REQUIREDKNOWLEDGE) container='infosys-ruoyi-cloud-plus-portal-requiredknowledge' ;;
      PORTAL_RESOURCES) container='infosys-ruoyi-cloud-plus-portal-resources' ;;
    esac
    image_id="$(docker inspect --format '{{.Image}}' "$container")"
    [[ "$image_id" == sha256:* ]] || info_serve_die "invalid image ID for $container"
    printf 'IMAGE_%s_ID=%s\n' "$key" "$image_id" >>"$temp_output"
  done

  token="$(curl -fsS -X POST "$nacos_base_url/v1/auth/login" --data-urlencode "username=$nacos_username" --data-urlencode "password=$nacos_password" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')"
  [[ -n "$token" ]] || info_serve_die "Nacos login did not return an access token"
  for namespace in dev prod; do
    for data_id in portal-kernel.yml portal-appcenter.yml portal-forum.yml portal-requiredknowledge.yml portal-resources.yml ruoyi-gateway.yml; do
      content="$temp_dir/nacos-$namespace-$data_id"
      curl -fsS -G "$nacos_base_url/v1/cs/configs" \
        --data-urlencode "accessToken=$token" \
        --data-urlencode "dataId=$data_id" \
        --data-urlencode 'group=DEFAULT_GROUP' \
        --data-urlencode "tenant=$namespace" >"$content"
      [[ -s "$content" ]] || info_serve_die "empty Nacos config: $namespace/$data_id"
      key="NACOS_$(printf '%s_%s' "$namespace" "$data_id" | tr '[:lower:].-' '[:upper:]__')_SHA256"
      printf '%s=%s\n' "$key" "$(sha256_file "$content")" >>"$temp_output"
    done
  done

  db_sql="SELECT marker FROM (
    SELECT 1 AS ord, 'table.portal_module=' || COALESCE(to_regclass('public.portal_module')::text, 'MISSING') AS marker
    UNION ALL SELECT 2, 'table.info_resource_note=' || COALESCE(to_regclass('public.info_resource_note')::text, 'MISSING')
    UNION ALL SELECT 3, 'table.info_resource_view_record=' || COALESCE(to_regclass('public.info_resource_view_record')::text, 'MISSING')
    UNION ALL SELECT 4, 'table.info_resource_category_link=' || COALESCE(to_regclass('public.info_resource_category_link')::text, 'MISSING')
    UNION ALL SELECT 5, 'column.info_resource_category.parent_id=' || COALESCE((
      SELECT data_type || ':' || is_nullable FROM information_schema.columns
      WHERE table_schema='public' AND table_name='info_resource_category' AND column_name='parent_id'
    ), 'MISSING')
  ) markers ORDER BY ord;"
  docker exec infosys-ruoyi-cloud-plus-postgres sh -lc 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -At -c "$1"' _ "$db_sql" >"$db_markers"
  [[ "$(wc -l <"$db_markers" | tr -d ' ')" == 5 ]] || info_serve_die "database marker collection incomplete"
  printf 'DB_SCHEMA_MARKERS_SHA256=%s\n' "$(sha256_file "$db_markers")" >>"$temp_output"

  normalize_api_capabilities "$temp_dir" "$api_capabilities"
  printf 'API_CAPABILITIES_SHA256=%s\n' "$(sha256_file "$api_capabilities")" >>"$temp_output"

  LC_ALL=C sort -o "$temp_output" "$temp_output"
  mkdir -p "$(dirname "$output")"
  mv "$temp_output" "$output"
  trap - RETURN
  rm -rf "$temp_dir"
}

acquire_update_lock() {
  local target="$1"
  UPDATE_LOCK_DIR="$target/.update.lock"
  mkdir "$UPDATE_LOCK_DIR" 2>/dev/null || info_serve_die "another update or rollback is already running"
  printf '%s\n' "$$" >"$UPDATE_LOCK_DIR/pid"
}

release_update_lock() {
  if [[ -n "${UPDATE_LOCK_DIR:-}" && -d "$UPDATE_LOCK_DIR" ]]; then
    rm -f "$UPDATE_LOCK_DIR/pid"
    rmdir "$UPDATE_LOCK_DIR"
  fi
}

recreate_nginx_web() {
  local target="$1"
  (
    cd "$target/deploy"
    docker compose --env-file .env -f docker-compose.yml up -d --no-deps --force-recreate nginx-web
  )
}

official_health_check() {
  local base_url="${INFO_SERVE_HEALTH_BASE_URL:-http://127.0.0.1:7010}"
  local attempt root_code auth_code running
  for attempt in $(seq 1 30); do
    root_code="$(curl -s -o /dev/null -w '%{http_code}' "$base_url/" || true)"
    auth_code="$(curl -s -o /dev/null -w '%{http_code}' "$base_url/prod-api/auth/code" || true)"
    running="$(docker inspect --format '{{.State.Running}}' infosys-ruoyi-cloud-plus-web 2>/dev/null || true)"
    if [[ "$root_code" =~ ^(2|3)[0-9][0-9]$ && "$auth_code" == 200 && "$running" == true ]]; then
      return 0
    fi
    [[ "$attempt" == 30 ]] || sleep 1
  done
  info_serve_die "health check timed out: root=$root_code auth=$auth_code running=$running"
}

verify_backup_payload() {
  local backup="$1"
  [[ -f "$backup/BACKUP_COMPLETE" ]] || info_serve_die "backup is incomplete: $backup"
  verify_canonical_manifest "$backup/payload" "$backup/PAYLOAD-MANIFEST.sha256z"
}

restore_from_backup() {
  local target="$1"
  local backup="$2"
  local operation_id="$3"
  local stage="$target/plus-ui/.dist-rollback-stage-$operation_id"
  local failed="$target/plus-ui/.dist-failed-$operation_id"
  local release_info_present

  verify_backup_payload "$backup"
  [[ ! -e "$stage" && ! -e "$failed" ]] || info_serve_die "rollback staging path already exists"
  cp -a "$backup/payload/plus-ui/dist" "$stage"
  mv "$target/plus-ui/dist" "$failed"
  mv "$stage" "$target/plus-ui/dist"
  cp "$backup/payload/VERSION" "$target/.VERSION.rollback-$operation_id"
  mv "$target/.VERSION.rollback-$operation_id" "$target/VERSION"

  release_info_present="$(awk -F= '$1=="RELEASE_INFO_PRESENT" {print $2}' "$backup/BACKUP-METADATA.env")"
  if [[ "$release_info_present" == 1 ]]; then
    cp "$backup/payload/.release-info" "$target/.release-info.rollback-$operation_id"
    mv "$target/.release-info.rollback-$operation_id" "$target/.release-info"
  else
    rm -f "$target/.release-info"
  fi

  recreate_nginx_web "$target" || return $?
  official_health_check || return $?
  rm -rf "$failed"
}
