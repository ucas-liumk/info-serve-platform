# Task 8: forum 物理拆出 → portal-forum 服务

纯搬迁：forum 包经 T3（本地昵称解析器）、T4（MQ 通知）、T5（Dubbo 统计提供方）后对 kernel 零依赖。

**Files:**
- Create: `source/ruoyi-modules/ruoyi-portal-forum/pom.xml`、`.../PortalForumApplication.java`、`.../application.yml`、`.../logback-plus.xml`
- Create: `source/script/config/nacos/portal-forum.yml`、`deploy/compose/services/portal-forum.yml`
- Move: `org/dromara/portal/forum/` → 新模块
- Modify: `source/ruoyi-modules/pom.xml`、`source/script/config/nacos/ruoyi-gateway.yml`、`deploy/scripts/generate-initdb.py`、`deploy/build-images.sh`、`deploy/.env`、`PORTS.md`

**Interfaces:**
- Consumes: T1 契约（portal-event 发布器、stats 契约）、T2 模板与脚本、T7 后的 portal-kernel（运行时经 MQ/Dubbo 交互，无代码依赖）。
- Produces: 运行中的 `portal-forum`（8108），Dubbo group=forum 统计提供方随之跨进程可用。

- [ ] **Step 1: 新模块骨架**

`pom.xml` 同 T2 Step 1 结构：artifactId `ruoyi-portal-forum`，description `portal-forum 服务论坛服务`，dependencies：
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
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-common-portal-event</artifactId></dependency>
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-api-portal-kernel</artifactId></dependency>
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-api-system</artifactId></dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
```
build 段与 T2 一致。

`PortalForumApplication.java`（**Dubbo 提供方，需要 `@EnableDubbo`**）：
```java
package org.dromara.portal.forum;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

/** 服务论坛服务 */
@EnableDubbo
@SpringBootApplication
public class PortalForumApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(PortalForumApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
        System.out.println("服务论坛服务启动成功");
    }
}
```

`application.yml`：同 T2 模板，`server.port: 8108`、`spring.application.name: portal-forum`。
`logback-plus.xml`：`log.path` = `logs/portal-forum`。

- [ ] **Step 2: 整包搬迁 + 注册**

```bash
cd /Users/macmini/windows-info-serve
mkdir -p source/ruoyi-modules/ruoyi-portal-forum/src/main/java/org/dromara/portal
git mv source/ruoyi-modules/ruoyi-portal/src/main/java/org/dromara/portal/forum \
       source/ruoyi-modules/ruoyi-portal-forum/src/main/java/org/dromara/portal/forum
```
`source/ruoyi-modules/pom.xml` `<modules>` 追加 `<module>ruoyi-portal-forum</module>`。

- [ ] **Step 3: Nacos data-id + initdb + 网关路由**

`source/script/config/nacos/portal-forum.yml`：同 T2 模板，包名两处 `org.dromara.portal.forum.**`。
`generate-initdb.py` `new_configs` 追加 `"portal-forum.yml"`。
`ruoyi-gateway.yml` 在 `portal-requiredknowledge` 路由后插入：
```yaml
            - id: portal-forum
              uri: lb://portal-forum
              predicates:
                - Path=/infoservice/forum/**,/infoservice/portal/forum/**
              filters:
                - StripPrefix=1
              order: -5
```

- [ ] **Step 4: 编译双门禁**

```bash
cd /Users/macmini/windows-info-serve/source
mvn -o -ntp -Pdev -DskipTests -pl ruoyi-modules/ruoyi-portal-forum -am compile
mvn -ntp -pl ruoyi-modules/ruoyi-portal -am -DskipTests=false test
```
预期：双绿（老 portal 无 forum 残留引用——kernel 已在 T7 搬走，唯一曾引用 forum 的 `InfoPortalServiceImpl` 已 Dubbo 化）。

- [ ] **Step 5: 镜像 / compose / 配置发布**

`build-images.sh` 追加：
```bash
build_java_image "infosys/ruoyi-cloud-plus-portal-forum:${RUOYI_CLOUD_VERSION}" \
  "${SOURCE_DIR}/ruoyi-modules/ruoyi-portal-forum" \
  "target/ruoyi-portal-forum.jar"
```
`.env` 追加 `PORTAL_FORUM_IMAGE=infosys/ruoyi-cloud-plus-portal-forum:2.6.2`。
`deploy/compose/services/portal-forum.yml`：T2 Step 9 文件三处替换（名字→`portal-forum`、镜像变量→`${PORTAL_FORUM_IMAGE}`、端口→`"8108"`），并跑 `docker compose --env-file deploy/.env -f deploy/compose/services/portal-forum.yml config --quiet`。
配置发布：
```bash
bash deploy/scripts/nacos-publish.sh dev  portal-forum.yml source/script/config/nacos/portal-forum.yml
bash deploy/scripts/nacos-publish.sh prod portal-forum.yml source/script/config/nacos/portal-forum.yml
bash deploy/scripts/nacos-publish.sh dev  ruoyi-gateway.yml source/script/config/nacos/ruoyi-gateway.yml
bash deploy/scripts/nacos-publish.sh prod ruoyi-gateway.yml source/script/config/nacos/ruoyi-gateway.yml
```

- [ ] **Step 6: 运行时冒烟**

```bash
cd /Users/macmini/windows-info-serve/source
mvn -o -ntp -Pprod -DskipTests -pl ruoyi-modules/ruoyi-portal,ruoyi-modules/ruoyi-portal-forum -am package
cd ../deploy && ./build-images.sh
docker compose --env-file .env up -d ruoyi-portal
bash bin/svc.sh start portal-forum
curl -s -o /dev/null -w 'forum %{http_code}\n' http://127.0.0.1:7010/prod-api/infoservice/portal/forum/boards
curl -s -o /dev/null -w 'stats %{http_code}\n' http://127.0.0.1:7010/prod-api/infoservice/portal/stats
```
预期：均 `401`/`200`。登录门户实测：论坛发回复 → 消息中心出现通知（跨服务 MQ 链路 forum→kernel 首次真跨进程）；首页统计话题数正常（Dubbo group=forum 跨进程调用）。

- [ ] **Step 7: PORTS.md + 提交**

`PORTS.md` 新增：
```
| Portal-Forum | `8108` | `source/ruoyi-modules/ruoyi-portal-forum/src/main/resources/application.yml` | 服务论坛服务（批次 A 拆出） |
```
```bash
cd /Users/macmini/windows-info-serve
git add -A
git commit -m "feat: 服务论坛拆分为独立服务 portal-forum"
```
