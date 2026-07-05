#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_NAME="info-serve-portal-analytics"
PROJECT_SRC="${SCRIPT_DIR}/${PROJECT_NAME}"
HOP_PROJECTS_DIR="${HOP_PROJECTS_DIR:-/Users/macmini/DataPlatform/hop/projects}"
PROJECT_TARGET="${HOP_PROJECTS_DIR}/${PROJECT_NAME}"
HOP_CONTAINER="${HOP_CONTAINER:-dataplatform-hop-web}"

INFO_SERVE_DB_HOST="${INFO_SERVE_DB_HOST:-postgres}"
INFO_SERVE_DB_PORT="${INFO_SERVE_DB_PORT:-5432}"
INFO_SERVE_DB_NAME="${INFO_SERVE_DB_NAME:-ry-cloud}"
INFO_SERVE_DB_USER="${INFO_SERVE_DB_USER:-ruoyi}"
: "${INFO_SERVE_DB_PASSWORD:?Set INFO_SERVE_DB_PASSWORD before running this Hop pipeline.}"

mkdir -p "${PROJECT_TARGET}"
rsync -a --delete "${PROJECT_SRC}/" "${PROJECT_TARGET}/"
mkdir -p "${PROJECT_TARGET}/logs"

docker exec -i \
  -e HOP_PROJECT_NAME="${PROJECT_NAME}" \
  -e HOP_PROJECT_HOME="/files/${PROJECT_NAME}" \
  "${HOP_CONTAINER}" sh <<'HOP_REGISTER'
set -eu

config_file="/usr/local/tomcat/webapps/ROOT/config/hop-config.json"
project_name="${HOP_PROJECT_NAME}"
project_home="${HOP_PROJECT_HOME}"

PROJECT_NAME="${project_name}" perl -0pi -e '
  my $project_name = $ENV{"PROJECT_NAME"};
  s/"defaultProject"\s*:\s*"[^"]*"/"defaultProject" : "$project_name"/
    or die "defaultProject not found in hop-config.json\n";
' "${config_file}"

if ! grep -q "\"projectName\"[[:space:]]*:[[:space:]]*\"${project_name}\"" "${config_file}"; then
  PROJECT_NAME="${project_name}" PROJECT_HOME="${project_home}" perl -0pi -e '
    my $project_name = $ENV{"PROJECT_NAME"};
    my $project_home = $ENV{"PROJECT_HOME"};
    my $entry =
      "{\n" .
      "        \"projectName\" : \"$project_name\",\n" .
      "        \"projectHome\" : \"$project_home\",\n" .
      "        \"configFilename\" : \"project-config.json\"\n" .
      "      },\n      ";
    s/(\"projectConfigurations\"\s*:\s*\[\s*)/$1$entry/s
      or die "projectConfigurations not found in hop-config.json\n";
  ' "${config_file}"
fi

/usr/local/tomcat/webapps/ROOT/hop-conf.sh --projects-list --level=Basic
HOP_REGISTER

RUN_ID="$(date +%Y%m%d%H%M%S)"
LOG_FILE="/files/${PROJECT_NAME}/logs/refresh-portal-usage-analytics-${RUN_ID}.log"

docker exec \
  -e INFO_SERVE_DB_HOST="${INFO_SERVE_DB_HOST}" \
  -e INFO_SERVE_DB_PORT="${INFO_SERVE_DB_PORT}" \
  -e INFO_SERVE_DB_NAME="${INFO_SERVE_DB_NAME}" \
  -e INFO_SERVE_DB_USER="${INFO_SERVE_DB_USER}" \
  -e INFO_SERVE_DB_PASSWORD="${INFO_SERVE_DB_PASSWORD}" \
  "${HOP_CONTAINER}" sh -lc "
    HOP_OPTIONS='-Xms128m -Xmx1024m -XX:MaxMetaspaceSize=384m' \
    /usr/local/tomcat/webapps/ROOT/hop-run.sh \
      --project='${PROJECT_NAME}' \
      --runconfig=local \
      --file='/files/${PROJECT_NAME}/pipelines/refresh-portal-usage-analytics.hpl' \
      --level=Basic \
      --logfile='${LOG_FILE}' \
      --parameters-separator='|' \
      --parameters='INFO_SERVE_DB_HOST=${INFO_SERVE_DB_HOST}|INFO_SERVE_DB_PORT=${INFO_SERVE_DB_PORT}|INFO_SERVE_DB_NAME=${INFO_SERVE_DB_NAME}|INFO_SERVE_DB_USER=${INFO_SERVE_DB_USER}|INFO_SERVE_DB_PASSWORD=${INFO_SERVE_DB_PASSWORD}'
  "

echo "Hop pipeline completed. Log: ${PROJECT_TARGET}/logs/$(basename "${LOG_FILE}")"
