#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/.env"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.yml"

if [ ! -f "${ENV_FILE}" ]; then
  echo "Missing ${ENV_FILE}. Run start-budibase.sh first, or copy .env.example to .env."
  exit 1
fi

set -a
# shellcheck disable=SC1090
. "${ENV_FILE}"
set +a

docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" config --quiet
docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" ps

status_code="$(curl -fsS -o /dev/null -w '%{http_code}' --max-time 10 "http://127.0.0.1:${MAIN_PORT:-18100}/" || true)"
case "${status_code}" in
  200|301|302|303|307|308)
    echo "Budibase web is reachable at http://127.0.0.1:${MAIN_PORT:-18100}/ (${status_code})"
    ;;
  *)
    echo "Budibase web is not ready at http://127.0.0.1:${MAIN_PORT:-18100}/ (status: ${status_code:-none})"
    exit 1
    ;;
esac
