#!/usr/bin/env bash
set -euo pipefail
export LC_ALL=C

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
CATALOG="$ROOT_DIR/catalog.json"
PACKAGE_CODE=${1:-}
OUTPUT_ROOT=${2:-"$ROOT_DIR/../../releases/offline-apps"}
DEB_CACHE=${INFO_SERVE_DEB_CACHE:-"$HOME/.cache/info-serve-offline-debs"}

if [[ -z "$PACKAGE_CODE" ]]; then
  echo "用法: $0 <package-code> [output-root]" >&2
  exit 2
fi

[[ $(dpkg --print-architecture) == "arm64" ]] || { echo "只允许在 arm64 目标机构建" >&2; exit 1; }
grep -q '^ID=kylin' /etc/os-release || { echo "只允许在银河麒麟目标机构建" >&2; exit 1; }

case "$PACKAGE_CODE" in
  wps-office-arm64|wps-addins)
    echo "$PACKAGE_CODE 需要授权或经过审计的人工来源，不能自动下载" >&2
    exit 3
    ;;
  xournalpp-arm64) ROOT_PACKAGES=(xournalpp) ;;
  ocr-toolkit-zh-arm64) ROOT_PACKAGES=(ocrmypdf gimagereader tesseract-ocr-chi-sim) ;;
  vlc-arm64) ROOT_PACKAGES=(vlc) ;;
  gimp-arm64) ROOT_PACKAGES=(gimp) ;;
  inkscape-arm64) ROOT_PACKAGES=(inkscape) ;;
  keepassxc-arm64) ROOT_PACKAGES=(keepassxc) ;;
  flameshot-arm64) ROOT_PACKAGES=(flameshot) ;;
  remmina-arm64) ROOT_PACKAGES=(remmina) ;;
  filezilla-arm64) ROOT_PACKAGES=(filezilla) ;;
  *) echo "未知 package-code：$PACKAGE_CODE" >&2; exit 2 ;;
esac
VERSION=$(apt-cache policy "${ROOT_PACKAGES[0]}" | awk '/候选：|Candidate:/ {print $2; exit}')
[[ -n "$VERSION" && "$VERSION" != "(无)" && "$VERSION" != "(none)" ]] || { echo "无法解析 $PACKAGE_CODE 版本" >&2; exit 1; }

SAFE_VERSION=$(printf '%s' "$VERSION" | tr ':~+/' '____')
WORK_DIR=$(mktemp -d)
PACKAGE_DIR="$WORK_DIR/$PACKAGE_CODE-$SAFE_VERSION-kylin-v10sp1-arm64"
mkdir -p "$PACKAGE_DIR/packages" "$PACKAGE_DIR/LICENSES"
trap 'rm -rf "$WORK_DIR"' EXIT

{
  printf '%s\n' "${ROOT_PACKAGES[@]}"
  apt-cache depends --recurse --no-recommends --no-suggests --no-conflicts --no-breaks --no-replaces --no-enhances "${ROOT_PACKAGES[@]}" \
    | sed -n -E 's/^[[:space:]]*[|]?(Pre)?Depends:[[:space:]]*([^|]+).*$/\2/p' \
    | sed -E 's/[[:space:]]+$//; s/:any$//'
} | sort -u | while IFS= read -r package; do
  [[ "$package" == \<*\> ]] && continue
  if apt-cache show "$package" >/dev/null 2>&1; then
    printf '%s\n' "$package"
  fi
done | sort -u > "$PACKAGE_DIR/dependency-packages.txt"

pushd "$PACKAGE_DIR/packages" >/dev/null
mapfile -t ALL_PACKAGES < ../dependency-packages.txt
mkdir -p "$DEB_CACHE"
declare -A CACHED_PACKAGES=()
for deb in "$DEB_CACHE"/*.deb; do
  [[ -e "$deb" ]] || continue
  CACHED_PACKAGES["$(dpkg-deb -f "$deb" Package)"]="$deb"
done
MISSING_PACKAGES=()
for package in "${ALL_PACKAGES[@]}"; do
  if [[ -n "${CACHED_PACKAGES[$package]:-}" ]]; then
    cp "${CACHED_PACKAGES[$package]}" .
  else
    MISSING_PACKAGES+=("$package")
  fi
done
if (( ${#MISSING_PACKAGES[@]} > 0 )); then
  apt-get download "${MISSING_PACKAGES[@]}"
fi
find . -maxdepth 1 -type f -name '*.deb' -exec cp -n {} "$DEB_CACHE/" \;
popd >/dev/null
printf '%s\n' "${ROOT_PACKAGES[@]}" > "$PACKAGE_DIR/root-packages.txt"

find "$PACKAGE_DIR/packages" -type f -name '*.deb' -print0 | while IFS= read -r -d '' deb; do
  arch=$(dpkg-deb -f "$deb" Architecture)
  [[ "$arch" == "arm64" || "$arch" == "all" ]] || { echo "错误架构：$deb -> $arch" >&2; exit 1; }
done

cat > "$PACKAGE_DIR/install.sh" <<'SCRIPT'
#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
sha256sum -c SHA256SUMS
sudo install -m 0644 packages/*.deb /var/cache/apt/archives/
mapfile -t ROOT_PACKAGES < root-packages.txt
sudo apt-get install --no-download -y "${ROOT_PACKAGES[@]}"
SCRIPT

cat > "$PACKAGE_DIR/verify.sh" <<'SCRIPT'
#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
[[ $(dpkg --print-architecture) == "arm64" ]]
sha256sum -c SHA256SUMS
for deb in packages/*.deb; do
  arch=$(dpkg-deb -f "$deb" Architecture)
  [[ "$arch" == "arm64" || "$arch" == "all" ]]
done
echo "离线包架构与校验通过"
SCRIPT

cat > "$PACKAGE_DIR/install.md" <<EOF
# $PACKAGE_CODE 安装说明

- 目标：银河麒麟桌面操作系统 V10 SP1 / arm64
- 版本：$VERSION
- 来源：目标机已配置的银河麒麟软件源

安装前请关闭正在运行的同名软件。解压后执行：

\`\`\`bash
kysec_set -n exectl -v verified ./verify.sh
kysec_set -n exectl -v verified ./install.sh
bash ./verify.sh
bash ./install.sh
\`\`\`

安装脚本需要管理员 sudo 密码；应用中心不会自动执行系统命令。
EOF

chmod +x "$PACKAGE_DIR/install.sh" "$PACKAGE_DIR/verify.sh"
(cd "$PACKAGE_DIR" && sha256sum packages/*.deb > SHA256SUMS)
cp /etc/os-release "$PACKAGE_DIR/target-os-release"
ldd --version > "$PACKAGE_DIR/target-glibc.full" 2>&1
sed -n '1p' "$PACKAGE_DIR/target-glibc.full" > "$PACKAGE_DIR/target-glibc"
rm -f "$PACKAGE_DIR/target-glibc.full"

mkdir -p "$OUTPUT_ROOT/$PACKAGE_CODE"
ARCHIVE="$OUTPUT_ROOT/$PACKAGE_CODE/$PACKAGE_CODE-$SAFE_VERSION-kylin-v10sp1-arm64.tar.gz"
tar -C "$WORK_DIR" -czf "$ARCHIVE" "$(basename "$PACKAGE_DIR")"
(cd "$(dirname "$ARCHIVE")" && sha256sum "$(basename "$ARCHIVE")" > "$(basename "$ARCHIVE").sha256")
echo "$ARCHIVE"
