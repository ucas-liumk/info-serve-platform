# Task 7: kernel 物理拆出 → portal-kernel 服务

T3-T6 后 kernel↔BC 双向零代码依赖，本任务为纯搬迁。portal-kernel 承载：门户统计聚合、模块注册表、消息中心、通知 MQ 消费者。结构照 T2 模板，参数按 00-plan 总表。

**Files:**
- Create: `source/ruoyi-modules/ruoyi-portal-kernel/pom.xml`、`.../PortalKernelApplication.java`、`.../application.yml`、`.../logback-plus.xml`
- Create: `source/script/config/nacos/portal-kernel.yml`、`deploy/compose/services/portal-kernel.yml`
- Move: `org/dromara/portal/kernel/` 主代码包 + 3 个 kernel 测试类 → 新模块
- Modify: `source/ruoyi-modules/pom.xml`、`source/script/config/nacos/ruoyi-gateway.yml`、`deploy/scripts/generate-initdb.py`、`deploy/build-images.sh`、`deploy/.env`、`PORTS.md`

**Interfaces:**
- Consumes: T1 契约与 T3-T5 在 kernel 包内落好的全部实现；T2 建立的 svc.sh / nacos-publish.sh。
- Produces: 运行中的 `portal-kernel` 服务（8107）；老 `ruoyi-portal` 从此不再含 kernel 包（消息/统计/模块注册表路由全部由新服务承接）。

- [ ] **Step 1: 新模块骨架**

`pom.xml`（结构同 T2 Step 1，改 artifactId 与依赖集）：artifactId `ruoyi-portal-kernel`，description `portal-kernel 门户内核服务`，dependencies：
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
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-common-redis</artifactId></dependency>
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-common-portal-event</artifactId></dependency>
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-api-portal-kernel</artifactId></dependency>
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-api-system</artifactId></dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
```
build 段与 T2 完全一致（finalName + spring-boot repackage）。

`PortalKernelApplication.java`（kernel 只是 Dubbo 消费方，不加 `@EnableDubbo`——与拆分前 portal 纯消费时的既有模式一致）：
```java
package org.dromara.portal.kernel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

/** 门户内核服务 */
@SpringBootApplication
public class PortalKernelApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(PortalKernelApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
        System.out.println("门户内核服务启动成功");
    }
}
```

`application.yml`：同 T2 Step 2 模板，仅 `server.port: 8107`、`spring.application.name: portal-kernel`（config import 三行同款保留 `datasource.yml`）。
`logback-plus.xml`：同 T2，`log.path` 改 `logs/portal-kernel`。

- [ ] **Step 2: 整包搬迁（主代码 + 测试）**

```bash
cd /Users/macmini/windows-info-serve
mkdir -p source/ruoyi-modules/ruoyi-portal-kernel/src/main/java/org/dromara/portal
mkdir -p source/ruoyi-modules/ruoyi-portal-kernel/src/test/java/org/dromara/portal
git mv source/ruoyi-modules/ruoyi-portal/src/main/java/org/dromara/portal/kernel \
       source/ruoyi-modules/ruoyi-portal-kernel/src/main/java/org/dromara/portal/kernel
git mv source/ruoyi-modules/ruoyi-portal/src/test/java/org/dromara/portal/kernel \
       source/ruoyi-modules/ruoyi-portal-kernel/src/test/java/org/dromara/portal/kernel
```
（测试目录含 T3/T4/T5 的 `PortalMessageServiceTest`、`PortalNotificationListenerTest`、`InfoPortalServiceStatsTest`，随包整体走。）

`source/ruoyi-modules/pom.xml` `<modules>` 追加 `<module>ruoyi-portal-kernel</module>`。

- [ ] **Step 3: Nacos data-id + initdb + 网关路由**

新建 `source/script/config/nacos/portal-kernel.yml`（同 T2 Step 4 模板，两处包名改为）：
```yaml
mybatis-plus:
  mapper-package: org.dromara.portal.kernel.**.mapper
  type-aliases-package: org.dromara.portal.kernel.**.domain
```
`generate-initdb.py` `new_configs` 追加 `"portal-kernel.yml"`。

`ruoyi-gateway.yml` 在 T2 加入的 `portal-requiredknowledge` 路由**之前**插入两条：
```yaml
            - id: portal-kernel-messages
              uri: lb://portal-kernel
              predicates:
                - Path=/appcenter/portal/messages/**
              filters:
                - StripPrefix=1
              order: -10
            - id: portal-kernel
              uri: lb://portal-kernel
              predicates:
                - Path=/infoservice/portal/stats,/infoservice/portal/modules/**,/infoservice/module/**
              filters:
                - StripPrefix=1
              order: -10
```

- [ ] **Step 4: 编译 + 测试双门禁**

```bash
cd /Users/macmini/windows-info-serve/source
mvn -o -ntp -Pdev -DskipTests -pl ruoyi-modules/ruoyi-portal-kernel -am compile
mvn -ntp -pl ruoyi-modules/ruoyi-portal-kernel -am -DskipTests=false test
mvn -ntp -pl ruoyi-modules/ruoyi-portal -am -DskipTests=false test
```
预期：三条全 `BUILD SUCCESS`（kernel 模块 9 个用例绿；老 portal 编译通过证明零残留依赖）。若老 portal 报 kernel 符号缺失，说明 T3-T6 有漏网调用，回到对应任务补断。
> ArchUnit 拆一删一（T2 实证空规则会失败，failOnEmptyShould 默认 true）：kernel 包搬走后，`BcBoundaryTest` 中以 kernel 为主语的 `kernel_should_not_depend_on_content_bcs` 删除；`content_bcs_should_not_depend_on_kernel_at_all` 保留（主语仍非空，且继续防守残留引用）。

- [ ] **Step 5: 镜像 / compose / 配置发布**

`deploy/build-images.sh` 追加：
```bash
build_java_image "infosys/ruoyi-cloud-plus-portal-kernel:2.6.2" \
  "${SOURCE_DIR}/ruoyi-modules/ruoyi-portal-kernel" \
  "target/ruoyi-portal-kernel.jar"
```
`deploy/.env` 追加 `PORTAL_KERNEL_IMAGE=infosys/ruoyi-cloud-plus-portal-kernel:2.6.2`。

新建 `deploy/compose/services/portal-kernel.yml`：内容 = T2 Step 9 的文件，替换三处——`portal-requiredknowledge`→`portal-kernel`（服务名/容器名/日志卷）、`${PORTAL_REQUIREDKNOWLEDGE_IMAGE}`→`${PORTAL_KERNEL_IMAGE}`、`SERVER_PORT: "8109"`→`"8107"`。
```bash
docker compose --env-file deploy/.env -f deploy/compose/services/portal-kernel.yml config --quiet
```

发布配置（基础设施运行中）：
```bash
bash deploy/scripts/nacos-publish.sh dev  portal-kernel.yml source/script/config/nacos/portal-kernel.yml
bash deploy/scripts/nacos-publish.sh prod portal-kernel.yml source/script/config/nacos/portal-kernel.yml
bash deploy/scripts/nacos-publish.sh dev  ruoyi-gateway.yml source/script/config/nacos/ruoyi-gateway.yml
bash deploy/scripts/nacos-publish.sh prod ruoyi-gateway.yml source/script/config/nacos/ruoyi-gateway.yml
```

- [ ] **Step 6: 运行时冒烟（重建老 portal 镜像 + 起新服务）**

老 portal 代码已变（kernel 包移除），必须同批重建：
```bash
cd /Users/macmini/windows-info-serve/source
mvn -o -ntp -Pprod -DskipTests -pl ruoyi-modules/ruoyi-portal,ruoyi-modules/ruoyi-portal-kernel -am package
cd ../deploy && ./build-images.sh
docker compose --env-file .env up -d ruoyi-portal   # 重建老 portal 容器
bash bin/svc.sh start portal-kernel
```
冒烟（`404`/`503` 即失败）：
```bash
curl -s -o /dev/null -w 'stats %{http_code}\n'    http://127.0.0.1:7010/prod-api/infoservice/portal/stats
curl -s -o /dev/null -w 'modules %{http_code}\n'  http://127.0.0.1:7010/prod-api/infoservice/portal/modules
curl -s -o /dev/null -w 'messages %{http_code}\n' http://127.0.0.1:7010/prod-api/appcenter/portal/messages
curl -s -o /dev/null -w 'apps %{http_code}\n'     http://127.0.0.1:7010/prod-api/appcenter/portal/apps
```
预期：四个都是 `401` 或 `200`——前三个由 portal-kernel 承接、最后一个仍由老 portal 承接。

- [ ] **Step 7: PORTS.md 登记 + 提交**

`PORTS.md`：Portal 行说明改为「ruoyi-portal（拆分过渡中，仅剩 appcenter/forum/resources）」，新增一行：
```
| Portal-Kernel | `8107` | `source/ruoyi-modules/ruoyi-portal-kernel/src/main/resources/application.yml` | 门户内核服务（统计/消息/模块注册表；原 ruoyi-portal 8107 移交） |
```
> 过渡期老 ruoyi-portal 与 portal-kernel 都写 8107：容器网络内不冲突；本地 dev 同时起两者时给老 portal 传 `--server.port=8117` 临时错开（T10 删老 portal 后此问题消失）。

```bash
cd /Users/macmini/windows-info-serve
git add -A
git commit -m "feat: 门户内核拆分为独立服务 portal-kernel"
```
