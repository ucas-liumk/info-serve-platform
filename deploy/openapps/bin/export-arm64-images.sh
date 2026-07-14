#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
LOCK_FILE="$ROOT_DIR/catalog.lock.tsv"
OUTPUT_DIR=${1:-"$ROOT_DIR/../../releases/openapps/arm64-$(date +%Y%m%d)"}
mkdir -p "$OUTPUT_DIR/images"

retry() {
  local attempt=1
  local max_attempts=5
  until "$@"; do
    if (( attempt >= max_attempts )); then
      return 1
    fi
    sleep $((attempt * 2))
    attempt=$((attempt + 1))
  done
}

grep -v '^#' "$LOCK_FILE" | while IFS=$'\t' read -r app_code upstream index_digest arm64_digest local_image; do
  archive="$OUTPUT_DIR/images/$app_code.tar.gz"
  if [[ -s "$archive" ]] && gzip -t "$archive" 2>/dev/null; then
    echo "复用已生成镜像包：$archive"
    continue
  fi
  rm -f "$archive"
  repository=${upstream%:*}
  retry docker pull --platform linux/arm64 "$repository@$arm64_digest"
  docker tag "$repository@$arm64_digest" "$local_image"
  [[ $(docker image inspect "$local_image" --format '{{.Architecture}}') == "arm64" ]]
  docker save "$local_image" | gzip -1 > "$archive"
  if [[ "${OPENAPPS_KEEP_LOCAL_IMAGES:-0}" != "1" ]]; then
    docker image rm "$local_image" "$repository@$arm64_digest" >/dev/null
  fi
done

cp "$LOCK_FILE" "$OUTPUT_DIR/catalog.lock.tsv"
cp "$ROOT_DIR/bin/load-images.sh" "$OUTPUT_DIR/load-images.sh"
(cd "$OUTPUT_DIR" && shasum -a 256 images/*.tar.gz catalog.lock.tsv load-images.sh > SHA256SUMS)
echo "ARM64 镜像包已生成：$OUTPUT_DIR"
