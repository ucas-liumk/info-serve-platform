# Task 2: RK（应知应会）试点拆出 —— 建立服务模板全链路

requiredknowledge 是零外部耦合 BC（不 import kernel、无 Dubbo、无通知、纯 `rk_*` 表），用它验证「新服务模板 + Nacos 配置 + 网关分流 + 镜像 + compose 单服务启停」整条链路。**本任务产出的结构就是 T7–T10 的模板。**

**Files:**
- Create: `source/ruoyi-modules/ruoyi-portal-requiredknowledge/pom.xml`
- Create: `source/ruoyi-modules/ruoyi-portal-requiredknowledge/src/main/java/org/dromara/portal/requiredknowledge/PortalRequiredKnowledgeApplication.java`
- Create: `source/ruoyi-modules/ruoyi-portal-requiredknowledge/src/main/resources/application.yml`
- Create: `source/ruoyi-modules/ruoyi-portal-requiredknowledge/src/main/resources/logback-plus.xml`
- Create: `source/script/config/nacos/portal-requiredknowledge.yml`
- Create: `deploy/scripts/nacos-publish.sh`
- Create: `deploy/compose/services/portal-requiredknowledge.yml`
- Create: `deploy/bin/svc.sh`
- Move: `source/ruoyi-modules/ruoyi-portal/src/main/java/org/dromara/portal/requiredknowledge/` → 新模块同包路径
- Modify: `source/ruoyi-modules/pom.xml`（modules）、`source/script/config/nacos/ruoyi-gateway.yml`（路由）、`deploy/scripts/generate-initdb.py:84`（new_configs）、`deploy/build-images.sh`、`deploy/.env`、`PORTS.md`

**Interfaces:**
- Consumes: 无（RK 零耦合；不依赖 T1 契约）。
- Produces: 服务模板结构（pom/yml/logback/App 类/compose 文件/svc.sh 用法），T7-T10 按同样式复制并按主文档 §端口命名总表替换参数。

- [ ] **Step 1: 新模块骨架 —— pom**

`source/ruoyi-modules/ruoyi-portal-requiredknowledge/pom.xml`：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>org.dromara</groupId>
        <artifactId>ruoyi-modules</artifactId>
        <version>${revision}</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>ruoyi-portal-requiredknowledge</artifactId>

    <description>portal-requiredknowledge 应知应会服务</description>

    <dependencies>
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-common-nacos</artifactId></dependency>
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-common-log</artifactId></dependency>
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-common-web</artifactId></dependency>
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-common-mybatis</artifactId></dependency>
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-common-doc</artifactId></dependency>
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-common-idempotent</artifactId></dependency>
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-common-tenant</artifactId></dependency>
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-common-security</artifactId></dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <finalName>${project.artifactId}</finalName>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>${spring-boot.version}</version>
                <executions>
                    <execution>
                        <goals><goal>repackage</goal></goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```
> 依赖集 = 原 ruoyi-portal 运行时依赖减去 RK 用不到的 `ruoyi-common-dubbo`/`ruoyi-common-oss`/`ruoyi-api-system`/`ruoyi-api-file`。若 Step 5 编译报缺符号，从原 portal pom 补对应条目（预期不需要）。

- [ ] **Step 2: 启动类 / application.yml / logback**

`PortalRequiredKnowledgeApplication.java`：
```java
package org.dromara.portal.requiredknowledge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

/** 应知应会服务 */
@SpringBootApplication
public class PortalRequiredKnowledgeApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(PortalRequiredKnowledgeApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
        System.out.println("应知应会服务启动成功");
    }
}
```

`application.yml`：
```yaml
# Tomcat
server:
  port: 8109

# Spring
spring:
  application:
    name: portal-requiredknowledge
  profiles:
    active: @profiles.active@

--- # nacos 配置
spring:
  cloud:
    nacos:
      server-addr: @nacos.server@
      username: @nacos.username@
      password: @nacos.password@
      discovery:
        group: @nacos.discovery.group@
        namespace: ${spring.profiles.active}
      config:
        group: @nacos.config.group@
        namespace: ${spring.profiles.active}
  config:
    import:
      - optional:nacos:application-common.yml
      - optional:nacos:datasource.yml
      - optional:nacos:${spring.application.name}.yml
```

`logback-plus.xml`（照抄 `ruoyi-portal/src/main/resources/logback-plus.xml`，仅改一行）：
```xml
<property name="log.path" value="logs/portal-requiredknowledge" />
```

- [ ] **Step 3: 整包搬迁（git mv，含测试目录若存在 rk 相关）**

```bash
cd /Users/macmini/windows-info-serve
mkdir -p source/ruoyi-modules/ruoyi-portal-requiredknowledge/src/main/java/org/dromara/portal
git mv source/ruoyi-modules/ruoyi-portal/src/main/java/org/dromara/portal/requiredknowledge \
       source/ruoyi-modules/ruoyi-portal-requiredknowledge/src/main/java/org/dromara/portal/requiredknowledge
```
包名不变（`org.dromara.portal.requiredknowledge`），启动类在包根，组件扫描天然覆盖。

- [ ] **Step 4: 注册模块 + Nacos data-id + initdb**

`source/ruoyi-modules/pom.xml` `<modules>` 追加 `<module>ruoyi-portal-requiredknowledge</module>`。

新建 `source/script/config/nacos/portal-requiredknowledge.yml`：
```yaml
spring:
  datasource:
    dynamic:
      primary: master
      datasource:
        master:
          type: ${spring.datasource.type}
          driver-class-name: org.postgresql.Driver
          url: ${datasource.system-master.url}
          username: ${datasource.system-master.username}
          password: ${datasource.system-master.password}
mybatis-plus:
  mapper-package: org.dromara.portal.requiredknowledge.**.mapper
  type-aliases-package: org.dromara.portal.requiredknowledge.**.domain
```

`deploy/scripts/generate-initdb.py` 第 84 行 `new_configs` 数组追加 `"portal-requiredknowledge.yml"`：
```python
new_configs = ["ruoyi-portal.yml", "ruoyi-file.yml", "portal-requiredknowledge.yml"]
```

- [ ] **Step 5: 编译 + ArchUnit 门禁**

```bash
cd /Users/macmini/windows-info-serve/source
mvn -o -ntp -Pdev -DskipTests -pl ruoyi-modules/ruoyi-portal-requiredknowledge -am compile
mvn -ntp -pl ruoyi-modules/ruoyi-portal -am -DskipTests=false test
```
预期：双双 `BUILD SUCCESS`（BcBoundaryTest 中 requiredknowledge 相关规则因包已搬空自动通过；ruoyi-portal 剩余代码无 rk import——若编译报错说明存在未侦察到的引用，停下按报错处理并回报）。

- [ ] **Step 6: 网关过渡路由**

`source/script/config/nacos/ruoyi-gateway.yml` 的 `routes:` 列表、`ruoyi-portal-appcenter` 条目**之前**插入：
```yaml
            - id: portal-requiredknowledge
              uri: lb://portal-requiredknowledge
              predicates:
                - Path=/infoservice/required-knowledge/**,/infoservice/portal/required-knowledge/**
              filters:
                - StripPrefix=1
              order: -5
```
原 `ruoyi-portal-infoservice` catch-all 保留（order 默认 0，具体路径 -5 先命中）。

- [ ] **Step 7: Nacos OpenAPI 发布脚本（一次创建，后续任务复用）**

`deploy/scripts/nacos-publish.sh`：
```bash
#!/usr/bin/env bash
# 把一个 Nacos 配置文件发布到指定 namespace（dev/prod）。
# 用法: nacos-publish.sh <namespace> <data-id> <文件路径>
# 环境变量: NACOS_ADDR(默认 127.0.0.1:8148) NACOS_USER(默认 nacos) NACOS_PASS(默认取 deploy/.env 的 NACOS_PASSWORD)
set -euo pipefail
[ $# -eq 3 ] || { echo "用法: $0 <namespace> <data-id> <文件路径>"; exit 1; }
NS="$1"; DATA_ID="$2"; FILE="$3"
[ -f "$FILE" ] || { echo "文件不存在: $FILE"; exit 1; }
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
NACOS_ADDR="${NACOS_ADDR:-127.0.0.1:8148}"
NACOS_USER="${NACOS_USER:-nacos}"
if [ -z "${NACOS_PASS:-}" ]; then
  NACOS_PASS="$(grep -E '^NACOS_PASSWORD=' "${SCRIPT_DIR}/../.env" | cut -d= -f2-)"
fi
TOKEN="$(curl -sf -X POST "http://${NACOS_ADDR}/nacos/v1/auth/login" \
  -d "username=${NACOS_USER}&password=${NACOS_PASS}" \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["accessToken"])')"
HTTP_CODE="$(curl -s -o /tmp/nacos-publish-resp.txt -w '%{http_code}' -X POST \
  "http://${NACOS_ADDR}/nacos/v1/cs/configs?accessToken=${TOKEN}" \
  --data-urlencode "dataId=${DATA_ID}" \
  --data-urlencode "group=DEFAULT_GROUP" \
  --data-urlencode "tenant=${NS}" \
  --data-urlencode "type=yaml" \
  --data-urlencode "content@${FILE}")"
if [ "${HTTP_CODE}" != "200" ] || ! grep -q true /tmp/nacos-publish-resp.txt; then
  echo "发布失败 HTTP=${HTTP_CODE} resp=$(cat /tmp/nacos-publish-resp.txt)"; exit 1
fi
echo "已发布 ${DATA_ID} → namespace=${NS}"
```
```bash
chmod +x deploy/scripts/nacos-publish.sh && bash -n deploy/scripts/nacos-publish.sh
```
> ⚠️ prod namespace 的内容与源文件有差异（`generate-initdb.py` 的 `normalize_nacos_content` 做 host 替换）。**发布 prod 时**先跑 `python3 deploy/scripts/generate-initdb.py`，比对产物中该 data-id 的 prod 内容；服务专属 data-id（如本任务的 `portal-requiredknowledge.yml`）不含 host 字面量，dev/prod 内容相同，可直接发布同一文件。`ruoyi-gateway.yml` 亦不含 host，同理。

- [ ] **Step 8: 发布配置到运行中的 Nacos（dev + prod 双 namespace）**

前提：本机基础设施已运行（`cd deploy && docker compose --env-file .env up -d mysql postgres redis rabbitmq nacos minio`）。
```bash
cd /Users/macmini/windows-info-serve
bash deploy/scripts/nacos-publish.sh dev  portal-requiredknowledge.yml source/script/config/nacos/portal-requiredknowledge.yml
bash deploy/scripts/nacos-publish.sh prod portal-requiredknowledge.yml source/script/config/nacos/portal-requiredknowledge.yml
bash deploy/scripts/nacos-publish.sh dev  ruoyi-gateway.yml source/script/config/nacos/ruoyi-gateway.yml
bash deploy/scripts/nacos-publish.sh prod ruoyi-gateway.yml source/script/config/nacos/ruoyi-gateway.yml
```
预期：4 行 `已发布 …`。网关配置支持动态刷新路由；如未生效，重启 gateway 容器。

- [ ] **Step 9: 镜像与 compose**

`deploy/build-images.sh`：在 portal 构建条目之后追加：
```bash
build_java_image "infosys/ruoyi-cloud-plus-portal-requiredknowledge:${RUOYI_CLOUD_VERSION}" \
  "${SOURCE_DIR}/ruoyi-modules/ruoyi-portal-requiredknowledge" \
  "target/ruoyi-portal-requiredknowledge.jar"
```
`deploy/.env` 追加：
```
PORTAL_REQUIREDKNOWLEDGE_IMAGE=infosys/ruoyi-cloud-plus-portal-requiredknowledge:2.6.2
```

新建 `deploy/compose/services/portal-requiredknowledge.yml`（**自包含**，不引用主文件锚点；网络为 external——由主 compose 先创建）：
```yaml
name: infosys-ruoyi-cloud-plus
services:
  portal-requiredknowledge:
    image: ${PORTAL_REQUIREDKNOWLEDGE_IMAGE}
    container_name: infosys-ruoyi-cloud-plus-portal-requiredknowledge
    restart: unless-stopped
    environment:
      TZ: Asia/Shanghai
      SPRING_PROFILES_ACTIVE: prod
      SERVER_PORT: "8109"
      SPRING_CLOUD_NACOS_SERVER_ADDR: nacos:8848
      SPRING_CLOUD_NACOS_USERNAME: ${NACOS_USERNAME}
      SPRING_CLOUD_NACOS_PASSWORD: ${NACOS_PASSWORD}
      JAVA_OPTS: >-
        -Xms256m -Xmx768m
        -Dspring.cloud.nacos.server-addr=nacos:8848
        -Dspring.cloud.nacos.username=${NACOS_USERNAME}
        -Dspring.cloud.nacos.password=${NACOS_PASSWORD}
    volumes:
      - ../../logs/portal-requiredknowledge:/app/logs
    networks:
      - infosys-ruoyi-cloud-plus

networks:
  infosys-ruoyi-cloud-plus:
    name: infosys-ruoyi-cloud-plus_infosys-ruoyi-cloud-plus
    external: true
```
> ⚠️ 先核对主 compose 的锚点 `x-ruoyi-env` 实际键值（`deploy/docker-compose.yml` 顶部）：JAVA_OPTS 覆盖串若与上文有出入，以主文件为准逐键复制，仅改 `SERVER_PORT` 与日志卷。网络实际名验证：`docker network ls | grep infosys`，若与上面 `name:` 不符，改成实际名。

- [ ] **Step 10: 单服务启停脚本 svc.sh（一次创建，管所有 BC 服务）**

`deploy/bin/svc.sh`：
```bash
#!/usr/bin/env bash
# 门户业务服务独立启停。用法: svc.sh <start|stop|restart|status|logs> <服务名>
# 服务名 = deploy/compose/services/ 下的文件名（不含 .yml），如 portal-requiredknowledge
set -euo pipefail
[ $# -ge 2 ] || { echo "用法: $0 <start|stop|restart|status|logs> <服务名>"; exit 1; }
ACTION="$1"; SVC="$2"
DEPLOY_DIR="$(cd "$(dirname "$0")/.." && pwd)"
FILE="${DEPLOY_DIR}/compose/services/${SVC}.yml"
[ -f "$FILE" ] || { echo "未知服务: ${SVC}（找不到 ${FILE}）"; exit 1; }
DC=(docker compose --env-file "${DEPLOY_DIR}/.env" -f "$FILE")
case "$ACTION" in
  start)
    docker network inspect infosys-ruoyi-cloud-plus_infosys-ruoyi-cloud-plus >/dev/null 2>&1 \
      || { echo "基础网络不存在：请先在 deploy/ 执行 docker compose --env-file .env up -d 启动基础设施"; exit 1; }
    "${DC[@]}" up -d ;;
  stop)    "${DC[@]}" stop ;;
  restart) "${DC[@]}" restart ;;
  status)  "${DC[@]}" ps ;;
  logs)    "${DC[@]}" logs --tail 200 -f ;;
  *) echo "不支持的操作: ${ACTION}"; exit 1 ;;
esac
```
```bash
chmod +x deploy/bin/svc.sh && bash -n deploy/bin/svc.sh
docker compose --env-file deploy/.env -f deploy/compose/services/portal-requiredknowledge.yml config --quiet
```
预期：语法检查与 compose config 均无输出（通过）。

- [ ] **Step 11: 运行时验证（试点核心步骤）**

```bash
cd /Users/macmini/windows-info-serve/source
# 全量打包本模块（Mac 内存紧则用 RUNBOOK 的 Docker Maven 命令替换）
mvn -o -ntp -Pprod -DskipTests -pl ruoyi-modules/ruoyi-portal-requiredknowledge -am package
cd ../deploy && ./build-images.sh   # 或只手工执行 Step 9 追加的那条 build_java_image
bash bin/svc.sh start portal-requiredknowledge
bash bin/svc.sh status portal-requiredknowledge
```
预期：容器 `Up`。日志确认注册成功：
```bash
bash bin/svc.sh logs portal-requiredknowledge | grep -m1 "启动成功"
```
经网关冒烟（登录 token 获取方式照 `deploy/scripts/appcenter-login.mjs` 或直接看 HTTP 状态非 404 即路由生效）：
```bash
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:7010/prod-api/infoservice/portal/required-knowledge/catalog
```
预期：`401`（路由已达新服务、被鉴权拦截，正常）或 `200`；**不能是 `404`/`503`**。
再验证老单体路径仍通：
```bash
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:7010/prod-api/infoservice/portal/stats
```
预期：`401` 或 `200`（仍由 ruoyi-portal 服务承接）。

- [ ] **Step 12: PORTS.md 登记 + 提交**

`PORTS.md` 后端服务端口表追加一行：
```
| Portal-RequiredKnowledge | `8109` | `source/ruoyi-modules/ruoyi-portal-requiredknowledge/src/main/resources/application.yml` | 应知应会服务（批次 A 自 portal 拆出） |
```
```bash
cd /Users/macmini/windows-info-serve
git add -A
git commit -m "feat: 应知应会拆分为独立服务 portal-requiredknowledge（试点）"
```
