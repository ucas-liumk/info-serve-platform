# info-serve 0.3.6 单机 UTM 正式增量包

本包只适用于 `BASELINE-RECEIPT.env` 对应的单台 UTM/Kylin 现场快照。其他 0.3.5 环境会在写入前被拒绝。

## 使用顺序

1. **解压前**对照 GitHub Release 公布的 SHA-256，执行：

   ```bash
   sha256sum -c info-serve-update-0.3.6-*.tar.gz.sha256
   ```

2. 解压并执行内部只读校验：

   ```bash
   tar -xzf info-serve-update-0.3.6-*.tar.gz
   cd info-serve-update-0.3.6
   sudo kysec_set -r -n exectl -v verified scripts
   ./scripts/verify-package.sh
   ```

3. 设置 Nacos 只读采集凭据并升级：

   ```bash
   export NACOS_USERNAME='<用户名>'
   export NACOS_PASSWORD='<密码>'
   sudo -E ./scripts/apply-update.sh --target /home/kylin/info-serve
   ```

4. 若需人工回退：

   ```bash
   sudo -E ./scripts/rollback-update.sh --target /home/kylin/info-serve --backup latest
   ```

`kysec_set` 只在外部 SHA-256 校验通过后，将本包脚本从麒麟的 `unknown` 执行标签改为 `verified`；不会关闭 KYSEC。

升级脚本会保留备份与审计日志。不要删除 `/home/kylin/info-serve/.update-backups` 和 `.update-logs`，直到版本验收结束。
