# Task 9: resources 物理拆出 → portal-resources 服务（LibreOffice 专用镜像）

resources 是头号故障源（文档转换），拆出后转换负载与其他模块彻底隔离。它是唯一需要 LibreOffice 的服务：原 `portal.Dockerfile` 改名给它专用，老 portal 改用标准镜像。

**Files:**
- Create: `source/ruoyi-modules/ruoyi-portal-resources/pom.xml`、`.../PortalResourcesApplication.java`、`.../application.yml`、`.../logback-plus.xml`
- Create: `source/script/config/nacos/portal-resources.yml`、`deploy/compose/services/portal-resources.yml`
- Move: `org/dromara/portal/resources/`（主代码+测试）→ 新模块；`deploy/docker/portal.Dockerfile` → `deploy/docker/resources.Dockerfile`
- Modify: `source/ruoyi-modules/pom.xml`、`source/script/config/nacos/ruoyi-gateway.yml`、`deploy/scripts/generate-initdb.py`、`deploy/build-images.sh`（resources 新条目 + portal 条目改标准 Dockerfile）、`deploy/.env`、`PORTS.md`

**Interfaces:**
- Consumes: T1 契约、T6 在 resources 包内落好的转换队列/转换器、T2 模板与脚本。
- Produces: 运行中的 `portal-resources`（8111），含 `portal-resources.convert` 队列消费者与 Dubbo group=resources 统计提供方。

- [ ] **Step 1: 新模块骨架**

`pom.xml` 同 T2 结构：artifactId `ruoyi-portal-resources`，description `portal-resources 资料共享服务`，dependencies：
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

`PortalResourcesApplication.java`（Dubbo 提供方，`@EnableDubbo`）：
```java
package org.dromara.portal.resources;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

/** 资料共享服务 */
@EnableDubbo
@SpringBootApplication
public class PortalResourcesApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(PortalResourcesApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
        System.out.println("资料共享服务启动成功");
    }
}
```

`application.yml`：T2 模板 + `server.port: 8111`、`spring.application.name: portal-resources`，并把老 portal `application.yml` 的转换配置块整体带上（原文照抄）：
```yaml
# 资料共享 PDF 预览转换缓存配置
infoservice:
  preview-cache:
    dir: ${INFOSERVICE_PREVIEW_CACHE_DIR:/ruoyi/portal/preview-cache}
    soffice: ${INFOSERVICE_PREVIEW_CACHE_SOFFICE:soffice}
    timeout-seconds: ${INFOSERVICE_PREVIEW_CACHE_TIMEOUT_SECONDS:120}
```
`logback-plus.xml`：`log.path` = `logs/portal-resources`。

- [ ] **Step 2: 整包搬迁 + 注册**

```bash
cd /Users/macmini/windows-info-serve
mkdir -p source/ruoyi-modules/ruoyi-portal-resources/src/main/java/org/dromara/portal
mkdir -p source/ruoyi-modules/ruoyi-portal-resources/src/test/java/org/dromara/portal
git mv source/ruoyi-modules/ruoyi-portal/src/main/java/org/dromara/portal/resources \
       source/ruoyi-modules/ruoyi-portal-resources/src/main/java/org/dromara/portal/resources
git mv source/ruoyi-modules/ruoyi-portal/src/test/java/org/dromara/portal/resources \
       source/ruoyi-modules/ruoyi-portal-resources/src/test/java/org/dromara/portal/resources
```
（测试目录含 T6 的 `ResourceConvertListenerTest`、`DocumentPreviewConverterTest`。）
`source/ruoyi-modules/pom.xml` `<modules>` 追加 `<module>ruoyi-portal-resources</module>`。

- [ ] **Step 3: Nacos data-id + initdb + 网关路由**

`source/script/config/nacos/portal-resources.yml`：T2 模板，包名两处 `org.dromara.portal.resources.**`。
`generate-initdb.py` `new_configs` 追加 `"portal-resources.yml"`。
`ruoyi-gateway.yml` 在 `portal-forum` 路由后插入：
```yaml
            - id: portal-resources
              uri: lb://portal-resources
              predicates:
                - Path=/infoservice/resource/**,/infoservice/portal/resources/**
              filters:
                - StripPrefix=1
              order: -5
```

- [ ] **Step 4: 编译双门禁**

```bash
cd /Users/macmini/windows-info-serve/source
mvn -o -ntp -Pdev -DskipTests -pl ruoyi-modules/ruoyi-portal-resources -am compile
mvn -ntp -pl ruoyi-modules/ruoyi-portal-resources -am -DskipTests=false test
mvn -ntp -pl ruoyi-modules/ruoyi-portal -am -DskipTests=false test
```
预期：全绿。
> ArchUnit 拆一删一：删除 `resources_should_not_depend_on_other_content_bcs`（主语包已空）。

- [ ] **Step 5: LibreOffice 专用镜像切换**

```bash
cd /Users/macmini/windows-info-serve
git mv deploy/docker/portal.Dockerfile deploy/docker/resources.Dockerfile
```
`deploy/build-images.sh`：
1. 原 portal 条目（`-f .../portal.Dockerfile` 那条 `docker build`）整条替换为标准构建（老 portal 已不含转换代码，不再需要 LibreOffice）：
```bash
build_java_image "infosys/ruoyi-cloud-plus-portal:2.6.2" \
  "${SOURCE_DIR}/ruoyi-modules/ruoyi-portal" \
  "target/ruoyi-portal.jar"
```
2. 追加 resources 条目（LibreOffice 镜像）：
```bash
docker build -t "infosys/ruoyi-cloud-plus-portal-resources:2.6.2" \
  --build-arg "JAR_FILE=target/ruoyi-portal-resources.jar" \
  -f "${ROOT_DIR}/deploy/docker/resources.Dockerfile" \
  "${SOURCE_DIR}/ruoyi-modules/ruoyi-portal-resources"
```
`.env` 追加 `PORTAL_RESOURCES_IMAGE=infosys/ruoyi-cloud-plus-portal-resources:2.6.2`。

- [ ] **Step 6: compose（带转换缓存卷）**

`deploy/compose/services/portal-resources.yml`：T2 Step 9 文件替换名字/镜像变量/端口（`8111`）之外，**追加**转换相关 env 与卷——键与值从 `deploy/docker-compose.yml` 现有 `ruoyi-portal` 服务条目照抄（相对路径多一层 `../`）：
```yaml
      INFOSERVICE_PREVIEW_CACHE_DIR: /ruoyi/portal/preview-cache
    volumes:
      - ../../logs/portal-resources:/app/logs
      - ../../data/preview-cache:/ruoyi/portal/preview-cache
```
> 以主 compose 里 ruoyi-portal 的 `INFOSERVICE_PREVIEW_CACHE_*` 实际键值为准逐键复制（可能还有 SOFFICE/TIMEOUT 两键）。
校验：`docker compose --env-file deploy/.env -f deploy/compose/services/portal-resources.yml config --quiet`。

- [ ] **Step 7: 配置发布 + 运行时冒烟**

```bash
bash deploy/scripts/nacos-publish.sh dev  portal-resources.yml source/script/config/nacos/portal-resources.yml
bash deploy/scripts/nacos-publish.sh prod portal-resources.yml source/script/config/nacos/portal-resources.yml
bash deploy/scripts/nacos-publish.sh dev  ruoyi-gateway.yml source/script/config/nacos/ruoyi-gateway.yml
bash deploy/scripts/nacos-publish.sh prod ruoyi-gateway.yml source/script/config/nacos/ruoyi-gateway.yml
cd /Users/macmini/windows-info-serve/source
mvn -o -ntp -Pprod -DskipTests -pl ruoyi-modules/ruoyi-portal,ruoyi-modules/ruoyi-portal-resources -am package
cd ../deploy && ./build-images.sh
docker compose --env-file .env up -d ruoyi-portal
bash bin/svc.sh start portal-resources
curl -s -o /dev/null -w 'resources %{http_code}\n' http://127.0.0.1:7010/prod-api/infoservice/portal/resources
```
预期：`401`/`200`。登录实测：上传一个 docx → RabbitMQ 管理页可见 `portal-resources.convert` 消费 → 点预览秒开（预热缓存命中）；`docker exec infosys-ruoyi-cloud-plus-portal-resources soffice --version` 确认 LibreOffice 在镜像内。

- [ ] **Step 8: PORTS.md + 提交**

`PORTS.md` 新增：
```
| Portal-Resources | `8111` | `source/ruoyi-modules/ruoyi-portal-resources/src/main/resources/application.yml` | 资料共享服务（批次 A 拆出，含 LibreOffice 转换） |
```
```bash
cd /Users/macmini/windows-info-serve
git add -A
git commit -m "feat: 资料共享拆分为独立服务 portal-resources 并独占 LibreOffice 镜像"
```
