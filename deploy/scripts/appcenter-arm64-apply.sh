#!/usr/bin/env bash
set -euo pipefail

ASSET_DIR=${1:-"$HOME/info-serve-staging/appcenter-arm64"}
INSTALL_ROOT=${INFO_SERVE_ROOT:-"$HOME/info-serve"}
DEPLOY_DIR="$INSTALL_ROOT/deploy"
PG_CONTAINER=${APP_CENTER_PG_CONTAINER:-infosys-ruoyi-cloud-plus-postgres}
PUBLIC_HOST=${INFO_SERVE_PUBLIC_HOST:-$(hostname -I | awk '{print $1}')}
FILE_IMAGE=infosys/ruoyi-cloud-plus-file:2.6.2-appcenter-arm64-20260715
APPCENTER_IMAGE=infosys/ruoyi-cloud-plus-portal-appcenter:2.6.2-appcenter-arm64-20260715

require_file() {
  [[ -f "$1" ]] || { echo "缺少文件：$1" >&2; exit 1; }
}

set_env() {
  local key=$1 value=$2 file=$3
  if grep -q "^${key}=" "$file"; then
    sed -i "s|^${key}=.*|${key}=${value}|" "$file"
  else
    printf '%s=%s\n' "$key" "$value" >> "$file"
  fi
}

[[ $(uname -m) == "aarch64" ]] || { echo "只允许在 aarch64 目标机执行" >&2; exit 1; }
grep -q '^ID=kylin' /etc/os-release || { echo "只允许在银河麒麟目标机执行" >&2; exit 1; }
docker info >/dev/null 2>&1 || {
  echo "当前用户不能访问 Docker。请先把用户加入 docker 组并重新登录。" >&2
  exit 1
}
require_file "$ASSET_DIR/appcenter-upload-arm64-images.tar.gz"
require_file "$ASSET_DIR/core-images.sha256"
require_file "$ASSET_DIR/0.3.9-appcenter-arm64-catalog.sql"
require_file "$ASSET_DIR/dist/index.html"
require_file "$ASSET_DIR/openapps-images/SHA256SUMS"
require_file "$DEPLOY_DIR/.env"

(cd "$ASSET_DIR" && sha256sum -c core-images.sha256)
(cd "$ASSET_DIR/openapps-images" && shasum -a 256 -c SHA256SUMS)

gzip -dc "$ASSET_DIR/appcenter-upload-arm64-images.tar.gz" | docker load
[[ $(docker image inspect "$FILE_IMAGE" --format '{{.Architecture}}') == "arm64" ]]
[[ $(docker image inspect "$APPCENTER_IMAGE" --format '{{.Architecture}}') == "arm64" ]]

set_env FILE_IMAGE "$FILE_IMAGE" "$DEPLOY_DIR/.env"
set_env PORTAL_APPCENTER_IMAGE "$APPCENTER_IMAGE" "$DEPLOY_DIR/.env"
rsync -a --delete "$ASSET_DIR/dist/" "$INSTALL_ROOT/plus-ui/dist/"
rsync -a --delete "$ASSET_DIR/openapps/" "$DEPLOY_DIR/openapps/"
cp "$ASSET_DIR/0.3.9-appcenter-arm64-catalog.sql" "$DEPLOY_DIR/updates/"

find "$DEPLOY_DIR/openapps" -type f \( -name '*.sh' -o -name appctl \) -print | while read -r file; do
  if command -v kysec_set >/dev/null 2>&1; then
    kysec_set -n exectl -v verified "$file" >/dev/null
  fi
done
if command -v kysec_set >/dev/null 2>&1; then
  kysec_set -n exectl -v verified "$DEPLOY_DIR/bin/svc.sh" >/dev/null
  kysec_set -n exectl -v verified "$ASSET_DIR/openapps-images/load-images.sh" >/dev/null
fi

docker exec -i "$PG_CONTAINER" psql -v ON_ERROR_STOP=1 -U ruoyi -d ry-cloud \
  < "$ASSET_DIR/0.3.9-appcenter-arm64-catalog.sql"

(cd "$DEPLOY_DIR" && docker compose --env-file .env config --quiet)
(cd "$DEPLOY_DIR" && docker compose --env-file .env up -d --force-recreate ruoyi-file nginx-web)
bash "$DEPLOY_DIR/bin/svc.sh" start portal-appcenter

bash "$ASSET_DIR/openapps-images/load-images.sh"
for app in gitea searxng; do
  app_dir="$DEPLOY_DIR/openapps/$app"
  cp -n "$app_dir/.env.example" "$app_dir/.env"
done
set_env PUBLIC_BASE_URL "http://${PUBLIC_HOST}:18116/" "$DEPLOY_DIR/openapps/gitea/.env"
set_env PUBLIC_BASE_URL "http://${PUBLIC_HOST}:18119/" "$DEPLOY_DIR/openapps/searxng/.env"

echo "ARM64 资产应用完成。在线应用仍保持独立停止状态，请逐个执行 appctl start/verify/stop 回归。"
