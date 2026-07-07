#!/usr/bin/env bash
# 门户业务服务独立启停。用法: svc.sh <start|stop|restart|status|logs> <服务名>
# 服务名 = deploy/compose/services/ 下的文件名（不含 .yml），如 portal-requiredknowledge
set -euo pipefail
[ $# -ge 2 ] || { echo "用法: $0 <start|stop|restart|status|logs> <服务名>"; exit 1; }
ACTION="$1"; SVC="$2"
DEPLOY_DIR="$(cd "$(dirname "$0")/.." && pwd)"
FILE="${DEPLOY_DIR}/compose/services/${SVC}.yml"
[ -f "$FILE" ] || { echo "未知服务: ${SVC}（找不到 ${FILE}）"; exit 1; }
DC=(docker compose --project-directory "${DEPLOY_DIR}" --env-file "${DEPLOY_DIR}/.env" -f "$FILE")
case "$ACTION" in
  start)
    docker network inspect infosys-ruoyi-cloud-plus >/dev/null 2>&1 \
      || { echo "基础网络不存在：请先在 deploy/ 执行 docker compose --env-file .env up -d 启动基础设施"; exit 1; }
    "${DC[@]}" up -d ;;
  stop)    "${DC[@]}" stop ;;
  restart) "${DC[@]}" restart ;;
  status)  "${DC[@]}" ps ;;
  logs)    "${DC[@]}" logs --tail 200 -f ;;
  *) echo "不支持的操作: ${ACTION}"; exit 1 ;;
esac
