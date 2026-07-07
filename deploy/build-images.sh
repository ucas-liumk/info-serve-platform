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

  build_java_image "infosys/ruoyi-cloud-plus-file:2.6.2" \
    "${SOURCE_DIR}/ruoyi-modules/ruoyi-file" \
    "target/ruoyi-file.jar"

  build_java_image "infosys/ruoyi-cloud-plus-monitor:2.6.2" \
    "${SOURCE_DIR}/ruoyi-visual/ruoyi-monitor" \
    "target/ruoyi-monitor.jar"

  build_java_image "infosys/ruoyi-cloud-plus-portal-appcenter:2.6.2" \
    "${SOURCE_DIR}/ruoyi-modules/ruoyi-portal-appcenter" \
    "target/ruoyi-portal-appcenter.jar"

  build_java_image "infosys/ruoyi-cloud-plus-portal-requiredknowledge:2.6.2" \
    "${SOURCE_DIR}/ruoyi-modules/ruoyi-portal-requiredknowledge" \
    "target/ruoyi-portal-requiredknowledge.jar"

  build_java_image "infosys/ruoyi-cloud-plus-portal-kernel:2.6.2" \
    "${SOURCE_DIR}/ruoyi-modules/ruoyi-portal-kernel" \
    "target/ruoyi-portal-kernel.jar"

  build_java_image "infosys/ruoyi-cloud-plus-portal-forum:2.6.2" \
    "${SOURCE_DIR}/ruoyi-modules/ruoyi-portal-forum" \
    "target/ruoyi-portal-forum.jar"

  # portal-resources 含文档转 PDF 预览，需 LibreOffice + CJK 字体，使用专用 Dockerfile
  docker build \
    -t "infosys/ruoyi-cloud-plus-portal-resources:2.6.2" \
    --build-arg "JAR_FILE=target/ruoyi-portal-resources.jar" \
    -f "${ROOT_DIR}/deploy/docker/resources.Dockerfile" \
    "${SOURCE_DIR}/ruoyi-modules/ruoyi-portal-resources"
} 2>&1 | tee "${LOG_DIR}/docker-build-images.log"
