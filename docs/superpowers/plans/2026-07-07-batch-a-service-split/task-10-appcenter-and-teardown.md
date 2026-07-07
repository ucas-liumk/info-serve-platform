# Task 10: appcenter 物理拆出 + 删除 ruoyi-portal 单体 + 网关终态路由

最后一个 BC 拆出后，老单体清空退役。本任务结束时网关路由达到 00-plan 的终态表，仓库内不再存在 `ruoyi-portal` 模块。

**Files:**
- Create: `source/ruoyi-modules/ruoyi-portal-appcenter/pom.xml`、`.../PortalAppcenterApplication.java`、`.../application.yml`、`.../logback-plus.xml`
- Create: `source/script/config/nacos/portal-appcenter.yml`、`deploy/compose/services/portal-appcenter.yml`
- Move: `org/dromara/portal/appcenter/` → 新模块
- Delete: `source/ruoyi-modules/ruoyi-portal/`（整目录，含 `RuoYiPortalApplication`、`BcBoundaryTest`、application.yml）、`source/script/config/nacos/ruoyi-portal.yml`
- Modify: `source/ruoyi-modules/pom.xml`（+appcenter，−portal）、`ruoyi-gateway.yml`（终态路由）、`generate-initdb.py`（new_configs +appcenter −ruoyi-portal）、`build-images.sh`（+appcenter，−portal）、`deploy/.env`（+APPCENTER，−PORTAL_IMAGE）、`deploy/docker-compose.yml`（删 ruoyi-portal 服务块）、`PORTS.md`

**Interfaces:**
- Consumes: T1-T9 全部产出。
- Produces: 五服务终态；`lb://ruoyi-portal` 从网关消失。

- [ ] **Step 1: 新模块骨架**

`pom.xml` 同 T2 结构：artifactId `ruoyi-portal-appcenter`，description `portal-appcenter 应用中心服务`，dependencies（appcenter 用 OSS 下载离线包、api-file 传输文件、api-system 的 LoginUser/RoleDTO）：
```xml
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-common-nacos</artifactId></dependency>
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-common-log</artifactId></dependency>
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-common-web</artifactId></dependency>
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-common-mybatis</artifactId></dependency>
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-common-doc</artifactId></dependency>
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-common-idempotent</artifactId></dependency>
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-common-tenant</artifactId></dependency>
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-common-security</artifactId></dependency>
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-common-dubbo</artifactId></dependency>
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-common-oss</artifactId></dependency>
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-common-redis</artifactId></dependency>
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-common-portal-event</artifactId></dependency>
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-api-portal-kernel</artifactId></dependency>
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-api-system</artifactId></dependency>
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-api-file</artifactId></dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
```
build 段与 T2 一致。

`PortalAppcenterApplication.java`（Dubbo 提供方，`@EnableDubbo`）：
```java
package org.dromara.portal.appcenter;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

/** 应用中心服务 */
@EnableDubbo
@SpringBootApplication
public class PortalAppcenterApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(PortalAppcenterApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
        System.out.println("应用中心服务启动成功");
    }
}
```
`application.yml`：T2 模板，`server.port: 8106`、`spring.application.name: portal-appcenter`。
`logback-plus.xml`：`log.path` = `logs/portal-appcenter`。

- [ ] **Step 2: 搬迁 + 删除单体**

```bash
cd /Users/macmini/windows-info-serve
mkdir -p source/ruoyi-modules/ruoyi-portal-appcenter/src/main/java/org/dromara/portal
git mv source/ruoyi-modules/ruoyi-portal/src/main/java/org/dromara/portal/appcenter \
       source/ruoyi-modules/ruoyi-portal-appcenter/src/main/java/org/dromara/portal/appcenter
# 此刻 ruoyi-portal 只剩启动类/配置/BcBoundaryTest（使命完成，物理隔离取代之），整体删除：
git rm -r source/ruoyi-modules/ruoyi-portal
git rm source/script/config/nacos/ruoyi-portal.yml
```
`source/ruoyi-modules/pom.xml`：`<modules>` 追加 `<module>ruoyi-portal-appcenter</module>`，删除 `<module>ruoyi-portal</module>`。

- [ ] **Step 3: Nacos data-id + initdb + 网关终态**

`source/script/config/nacos/portal-appcenter.yml`：T2 模板，包名两处 `org.dromara.portal.appcenter.**`。
`generate-initdb.py` `new_configs`：追加 `"portal-appcenter.yml"`、**删除** `"ruoyi-portal.yml"`。终态：
```python
new_configs = ["ruoyi-file.yml", "portal-requiredknowledge.yml", "portal-kernel.yml",
               "portal-forum.yml", "portal-resources.yml", "portal-appcenter.yml"]
```
`ruoyi-gateway.yml`：**删除** `ruoyi-portal-appcenter`、`ruoyi-portal-infoservice` 两条旧路由，在 T2-T9 各路由之后追加 appcenter 兜底路由：
```yaml
            - id: portal-appcenter
              uri: lb://portal-appcenter
              predicates:
                - Path=/appcenter/**
              filters:
                - StripPrefix=1
              order: 0
```
核对终态与 00-plan 路由表逐条一致（6 条 portal 路由 + auth/system/file 原样）。

- [ ] **Step 4: 编译门禁 + 全仓残留清查**

```bash
cd /Users/macmini/windows-info-serve/source
mvn -o -ntp -Pdev -DskipTests -pl ruoyi-modules/ruoyi-portal-appcenter -am compile
grep -rn "ruoyi-portal[^-]" --include="*.xml" --include="*.yml" --include="*.py" --include="*.sh" \
  /Users/macmini/windows-info-serve/source /Users/macmini/windows-info-serve/deploy && echo "有残留，逐一处理" || echo "残留清查 OK"
```
预期：编译绿；grep 无输出（`ruoyi-portal-xxx` 新模块名因 `[^-]` 被排除）。若有残留（如 RUNBOOK/AGENTS 文档引用），文档类留给 T11，配置/脚本类现在处理。

- [ ] **Step 5: 镜像 / compose / .env 收尾**

`build-images.sh`：删除 portal 条目（T9 已改为标准构建的那条），追加：
```bash
build_java_image "infosys/ruoyi-cloud-plus-portal-appcenter:2.6.2" \
  "${SOURCE_DIR}/ruoyi-modules/ruoyi-portal-appcenter" \
  "target/ruoyi-portal-appcenter.jar"
```
`.env`：追加 `PORTAL_APPCENTER_IMAGE=infosys/ruoyi-cloud-plus-portal-appcenter:2.6.2`，删除 `PORTAL_IMAGE=` 行。
`deploy/docker-compose.yml`：删除 `ruoyi-portal:` 整个服务块（image/container/env/volumes/depends_on）。
`deploy/compose/services/portal-appcenter.yml`：T2 Step 9 文件三处替换（名字/`${PORTAL_APPCENTER_IMAGE}`/端口 `"8106"`）。
校验：
```bash
cd /Users/macmini/windows-info-serve/deploy
docker compose --env-file .env config --quiet
docker compose --env-file .env -f compose/services/portal-appcenter.yml config --quiet
```

- [ ] **Step 6: 配置发布 + 停老起新 + 冒烟**

```bash
bash deploy/scripts/nacos-publish.sh dev  portal-appcenter.yml source/script/config/nacos/portal-appcenter.yml
bash deploy/scripts/nacos-publish.sh prod portal-appcenter.yml source/script/config/nacos/portal-appcenter.yml
bash deploy/scripts/nacos-publish.sh dev  ruoyi-gateway.yml source/script/config/nacos/ruoyi-gateway.yml
bash deploy/scripts/nacos-publish.sh prod ruoyi-gateway.yml source/script/config/nacos/ruoyi-gateway.yml
cd /Users/macmini/windows-info-serve/source
mvn -o -ntp -Pprod -DskipTests -pl ruoyi-modules/ruoyi-portal-appcenter -am package
cd ../deploy && ./build-images.sh
docker compose --env-file .env rm -sf ruoyi-portal   # 停删老单体容器
bash bin/svc.sh start portal-appcenter
for p in "appcenter/portal/apps" "appcenter/application/list" "infoservice/portal/stats" "appcenter/portal/messages"; do
  curl -s -o /dev/null -w "$p %{http_code}\n" "http://127.0.0.1:7010/prod-api/$p"
done
```
预期：四个均 `401`/`200`（apps/application→portal-appcenter，stats/messages→portal-kernel）。
> Nacos 里遗留的 `ruoyi-portal.yml` data-id 与服务注册项会随实例下线自然消失；data-id 可在控制台手工删除（可选清理）。

- [ ] **Step 7: PORTS.md + 提交**

`PORTS.md`：删除原 Portal(8107=ruoyi-portal) 行，新增：
```
| Portal-Appcenter | `8106` | `source/ruoyi-modules/ruoyi-portal-appcenter/src/main/resources/application.yml` | 应用中心服务（批次 A 拆出，回收原 appcenter 端口） |
```
```bash
cd /Users/macmini/windows-info-serve
git add -A
git commit -m "feat: 应用中心拆分为独立服务并退役 ruoyi-portal 单体"
```
