# 银河麒麟 ARM64 离线软件包

本目录只保存构建与验证清单，不提交二进制。实际产物输出到仓库根目录 `releases/offline-apps/`，并保持在 Git 之外。

已确认目标机：银河麒麟桌面操作系统 V10 SP1、aarch64、glibc 2.31、Debian/Ubuntu 兼容软件包体系。

## 构建

构建必须在与交付目标一致、已配置可信麒麟软件源的机器执行：

```bash
deploy/offline-apps/build-kylin-package.sh xournalpp-arm64 /path/to/output
```

脚本会解析依赖闭包、复用 `~/.cache/info-serve-offline-debs` 中的已验证依赖、下载缺失的 `.deb`、生成安装说明和 SHA256，最后输出单一 `.tar.gz`。安装时仅请求目标软件，依赖包通过本机 APT 缓存离线解析，不会强制重装整个依赖闭包。WPS 与 WPS 加载项属于授权/自有来源，不能由脚本从公共软件源抓取。

## 验证

每个归档必须从干净快照开始，在断开外网软件源后执行：

```bash
tar -xzf <package>.tar.gz
cd <package-directory>
kysec_set -n exectl -v verified ./verify.sh
kysec_set -n exectl -v verified ./install.sh
bash ./verify.sh
bash ./install.sh
```

`install.sh` 是用户手工执行的安装脚本，应用中心不会自动调用 sudo。
