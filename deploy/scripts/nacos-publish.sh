#!/usr/bin/env bash
# 把一个 Nacos 配置文件发布到指定 namespace（dev/prod）。
# 用法: nacos-publish.sh <namespace> <data-id> <文件路径>
# 环境变量: NACOS_ADDR(默认 127.0.0.1:8148) NACOS_USER(默认 nacos) NACOS_PASS(默认取 deploy/.env 的 NACOS_PASSWORD)
set -euo pipefail
[ $# -eq 3 ] || { echo "用法: $0 <namespace> <data-id> <文件路径>"; exit 1; }
NS="$1"; DATA_ID="$2"; FILE="$3"
[ -f "$FILE" ] || { echo "文件不存在: $FILE"; exit 1; }
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
NACOS_ADDR="${NACOS_ADDR:-127.0.0.1:8148}"
NACOS_USER="${NACOS_USER:-nacos}"
if [ -z "${NACOS_PASS:-}" ]; then
  NACOS_PASS="$(grep -E '^NACOS_PASSWORD=' "${SCRIPT_DIR}/../.env" | cut -d= -f2-)"
fi
TOKEN="$(curl -sf -X POST "http://${NACOS_ADDR}/nacos/v1/auth/login" \
  -d "username=${NACOS_USER}&password=${NACOS_PASS}" \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["accessToken"])')"
HTTP_CODE="$(curl -s -o /tmp/nacos-publish-resp.txt -w '%{http_code}' -X POST \
  "http://${NACOS_ADDR}/nacos/v1/cs/configs?accessToken=${TOKEN}" \
  --data-urlencode "dataId=${DATA_ID}" \
  --data-urlencode "group=DEFAULT_GROUP" \
  --data-urlencode "tenant=${NS}" \
  --data-urlencode "type=yaml" \
  --data-urlencode "content@${FILE}")"
if [ "${HTTP_CODE}" != "200" ] || ! grep -q true /tmp/nacos-publish-resp.txt; then
  echo "发布失败 HTTP=${HTTP_CODE} resp=$(cat /tmp/nacos-publish-resp.txt)"; exit 1
fi
echo "已发布 ${DATA_ID} → namespace=${NS}"
