#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SOURCE_DIR="${ROOT_DIR}/source"
LOG_DIR="${ROOT_DIR}/logs"
mkdir -p "${LOG_DIR}"

build_java_image() {
  local image="$1"
  local context="$2"
  local jar="$3"

  docker build \
    -t "${image}" \
    --build-arg "JAR_FILE=${jar}" \
    -f "${ROOT_DIR}/deploy/docker/java-service.Dockerfile" \
    "${context}"
}

{
  build_java_image "infosys/ruoyi-cloud-plus-nacos:2.6.2" \
    "${SOURCE_DIR}/ruoyi-visual/ruoyi-nacos" \
    "target/ruoyi-nacos.jar"

  build_java_image "infosys/ruoyi-cloud-plus-gateway:2.6.2" \
    "${SOURCE_DIR}/ruoyi-gateway" \
    "target/ruoyi-gateway.jar"

  build_java_image "infosys/ruoyi-cloud-plus-auth:2.6.2" \
    "${SOURCE_DIR}/ruoyi-auth" \
    "target/ruoyi-auth.jar"

  build_java_image "infosys/ruoyi-cloud-plus-system:2.6.2" \
    "${SOURCE_DIR}/ruoyi-modules/ruoyi-system" \
    "target/ruoyi-system.jar"

  build_java_image "infosys/ruoyi-cloud-plus-gen:2.6.2" \
    "${SOURCE_DIR}/ruoyi-modules/ruoyi-gen" \
    "target/ruoyi-gen.jar"

  build_java_image "infosys/ruoyi-cloud-plus-job:2.6.2" \
    "${SOURCE_DIR}/ruoyi-modules/ruoyi-job" \
    "target/ruoyi-job.jar"

  build_java_image "infosys/ruoyi-cloud-plus-resource:2.6.2" \
    "${SOURCE_DIR}/ruoyi-modules/ruoyi-resource" \
    "target/ruoyi-resource.jar"

  build_java_image "infosys/ruoyi-cloud-plus-workflow:2.6.2" \
    "${SOURCE_DIR}/ruoyi-modules/ruoyi-workflow" \
    "target/ruoyi-workflow.jar"

  build_java_image "infosys/ruoyi-cloud-plus-snailjob:2.6.2" \
    "${SOURCE_DIR}/ruoyi-visual/ruoyi-snailjob-server" \
    "target/ruoyi-snailjob-server.jar"

  build_java_image "infosys/ruoyi-cloud-plus-monitor:2.6.2" \
    "${SOURCE_DIR}/ruoyi-visual/ruoyi-monitor" \
    "target/ruoyi-monitor.jar"
} 2>&1 | tee "${LOG_DIR}/docker-build-images.log"
