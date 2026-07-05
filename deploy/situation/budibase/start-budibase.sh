#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/.env"
ENV_EXAMPLE="${SCRIPT_DIR}/.env.example"
NETWORK_NAME="${BUDIBASE_NETWORK_NAME:-infosys-ruoyi-cloud-plus}"

random_secret() {
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -hex 32
    return
  fi
  if command -v uuidgen >/dev/null 2>&1; then
    printf '%s%s' "$(uuidgen | tr -d '-')" "$(uuidgen | tr -d '-')"
    return
  fi
  printf '%s%s' "$(date +%s%N)" "$$"
}

set_env_value() {
  local key="$1"
  local value="$2"
  KEY="$key" VALUE="$value" perl -0pi -e '
    my $key = $ENV{"KEY"};
    my $value = $ENV{"VALUE"};
    my $seen = 0;
    my @lines = split(/\n/, $_, -1);
    for my $line (@lines) {
      if ($line =~ /^\Q$key\E=/) {
        $line = "$key=$value";
        $seen = 1;
      }
    }
    die "Missing env key: $key\n" unless $seen;
    $_ = join("\n", @lines);
  ' "${ENV_FILE}"
}

if [ ! -f "${ENV_FILE}" ]; then
  cp "${ENV_EXAMPLE}" "${ENV_FILE}"
  set_env_value API_ENCRYPTION_KEY "$(random_secret)"
  set_env_value JWT_SECRET "$(random_secret)"
  set_env_value MINIO_SECRET_KEY "$(random_secret)"
  set_env_value COUCH_DB_PASSWORD "$(random_secret)"
  set_env_value REDIS_PASSWORD "$(random_secret)"
  set_env_value INTERNAL_API_KEY "$(random_secret)"
  set_env_value LITELLM_MASTER_KEY "sk-$(random_secret)"
  set_env_value LITELLM_SALT_KEY "$(random_secret)"
  set_env_value LITELLM_DB_PASSWORD "$(random_secret)"
  chmod 600 "${ENV_FILE}"
  echo "Generated local Budibase env file: ${ENV_FILE}"
fi

if ! docker network inspect "${NETWORK_NAME}" >/dev/null 2>&1; then
  docker network create "${NETWORK_NAME}" >/dev/null
fi

docker compose --env-file "${ENV_FILE}" -f "${SCRIPT_DIR}/docker-compose.yml" up -d --remove-orphans
docker compose --env-file "${ENV_FILE}" -f "${SCRIPT_DIR}/docker-compose.yml" stop litellm-service litellm-db >/dev/null 2>&1 || true
docker compose --env-file "${ENV_FILE}" -f "${SCRIPT_DIR}/docker-compose.yml" rm -f litellm-service litellm-db >/dev/null 2>&1 || true

echo "Budibase is starting. Run verify-budibase.sh to check readiness."
