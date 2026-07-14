#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
cd "$ROOT_DIR"
shasum -a 256 -c SHA256SUMS

if docker info >/dev/null 2>&1; then
  DOCKER=(docker)
else
  DOCKER=(sudo docker)
fi

for archive in images/*.tar.gz; do
  echo "导入 $archive"
  gzip -dc "$archive" | "${DOCKER[@]}" load
done

grep -v '^#' catalog.lock.tsv | while IFS=$'\t' read -r _ _ _ _ image; do
  arch=$("${DOCKER[@]}" image inspect "$image" --format '{{.Architecture}}')
  [[ "$arch" == "arm64" ]] || { echo "$image 架构错误：$arch" >&2; exit 1; }
done

echo "全部 ARM64 镜像导入完成"
