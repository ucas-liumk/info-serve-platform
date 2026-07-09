## 变更内容

-

## 影响范围

- BC / 模块：
- 数据库变更：无 / 有，见：
- Nacos / 配置变更：无 / 有，见：
- Dubbo API 变更：无 / 有，需同批重建：

## 验证结果

- [ ] `git diff --check`
- [ ] 前端：`cd plus-ui && npm run build:prod`
- [ ] 前端样式：`cd plus-ui && npm run design:audit`
- [ ] 后端：`cd source && mvn -o -ntp -Pdev -DskipTests -pl <模块> -am compile`
- [ ] 门户模块单测（改动 portal-* 时）：`cd source && mvn -ntp -pl ruoyi-modules/<模块> -am -DskipTests=false test`
- [ ] deploy：`docker compose --env-file .env config --quiet`
- [ ] 行为冒烟 / curl：

不适用项说明：

## 截图 / 日志

-

## 风险与回滚

-

## Issue

Closes #
