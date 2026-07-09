# Task 1: 契约模块 ruoyi-api-portal-kernel + MQ 公共模块 ruoyi-common-portal-event

**Files:**
- Create: `source/ruoyi-api/ruoyi-api-portal-kernel/pom.xml`
- Create: `source/ruoyi-api/ruoyi-api-portal-kernel/src/main/java/org/dromara/portal/api/RemoteModuleStatsService.java`
- Create: `source/ruoyi-api/ruoyi-api-portal-kernel/src/main/java/org/dromara/portal/api/PortalStatsMetrics.java`
- Create: `source/ruoyi-api/ruoyi-api-portal-kernel/src/main/java/org/dromara/portal/api/domain/vo/RemoteModuleStatsVo.java`
- Create: `source/ruoyi-api/ruoyi-api-portal-kernel/src/main/java/org/dromara/portal/api/event/PortalEventConstants.java`
- Create: `source/ruoyi-api/ruoyi-api-portal-kernel/src/main/java/org/dromara/portal/api/event/PortalNotificationEvent.java`
- Create: `source/ruoyi-common/ruoyi-common-portal-event/pom.xml`
- Create: `source/ruoyi-common/ruoyi-common-portal-event/src/main/java/org/dromara/common/portalevent/config/PortalEventAutoConfiguration.java`
- Create: `source/ruoyi-common/ruoyi-common-portal-event/src/main/java/org/dromara/common/portalevent/publisher/PortalEventPublisher.java`
- Create: `source/ruoyi-common/ruoyi-common-portal-event/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Modify: `source/ruoyi-api/pom.xml`（modules 注册）
- Modify: `source/ruoyi-api/ruoyi-api-bom/pom.xml`（dependencyManagement 注册）
- Modify: `source/ruoyi-common/pom.xml`（modules 注册）
- Modify: `source/ruoyi-common/ruoyi-common-bom/pom.xml`（dependencyManagement 注册）

**Interfaces:**
- Consumes: 无（首任务）。
- Produces（后续任务按此签名消费，不得改动）:
  - `RemoteModuleStatsService.stats() → RemoteModuleStatsVo`；`RemoteModuleStatsVo{String moduleCode; Map<String,Long> metrics}`
  - `PortalStatsMetrics.APP_COUNT|TOPIC_COUNT|ACTIVE_AUTHOR_COUNT|RESOURCE_COUNT|VISIT_COUNT`
  - `PortalEventConstants.EXCHANGE|DLX|QUEUE_KERNEL_NOTIFICATIONS(_DLQ)|QUEUE_RESOURCES_CONVERT(_DLQ)|RK_*` 四个 routing key、`STATS_GROUP_APPCENTER|FORUM|RESOURCES`
  - `PortalNotificationEvent{eventId,occurredAt,scene,title,content,targetType,targetUserIds}`，`TARGET_ALL="ALL"`、`TARGET_USERS="USERS"`
  - `PortalEventPublisher.publishNotification(String routingKey, PortalNotificationEvent event)`（内部捕获异常仅记日志——**通知属尽力而为，不得让业务操作因 broker 故障失败**）

- [ ] **Step 1: 开工分支**

```bash
cd /Users/macmini/windows-info-serve
git fetch origin && git switch -c feature/batch-a-service-split origin/main
```
预期：新分支创建成功。若分支已存在（多任务共用同一分支），直接 `git switch feature/batch-a-service-split`。

- [ ] **Step 2: 创建 api 模块 pom**

`source/ruoyi-api/ruoyi-api-portal-kernel/pom.xml`：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>org.dromara</groupId>
        <artifactId>ruoyi-api</artifactId>
        <version>${revision}</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>ruoyi-api-portal-kernel</artifactId>

    <description>ruoyi-api-portal-kernel 门户内核契约（Dubbo 统计接口 + 门户事件体）</description>

    <dependencies>
        <dependency>
            <groupId>org.dromara</groupId>
            <artifactId>ruoyi-common-core</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 3: 契约源码（5 个文件）**

`RemoteModuleStatsService.java`（纯 POJO 接口，与 ruoyi-api-system 的 RemoteDictService 同风格，无注解）：
```java
package org.dromara.portal.api;

import org.dromara.portal.api.domain.vo.RemoteModuleStatsVo;

/**
 * 门户模块统计契约：各内容 BC 以 @DubboService(group=模块名) 提供，kernel 聚合首页统计。
 * 指标键见 {@link PortalStatsMetrics}。
 */
public interface RemoteModuleStatsService {

    RemoteModuleStatsVo stats();
}
```

`PortalStatsMetrics.java`：
```java
package org.dromara.portal.api;

/** 门户统计指标键（跨服务契约，勿改字面值） */
public final class PortalStatsMetrics {

    public static final String APP_COUNT = "appCount";
    public static final String TOPIC_COUNT = "topicCount";
    public static final String ACTIVE_AUTHOR_COUNT = "activeAuthorCount";
    public static final String RESOURCE_COUNT = "resourceCount";
    public static final String VISIT_COUNT = "visitCount";

    private PortalStatsMetrics() {
    }
}
```

`domain/vo/RemoteModuleStatsVo.java`：
```java
package org.dromara.portal.api.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

@Data
public class RemoteModuleStatsVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 模块编码：appcenter / forum / resources */
    private String moduleCode;

    /** 指标键 → 数值，键见 PortalStatsMetrics */
    private Map<String, Long> metrics;
}
```

`event/PortalEventConstants.java`：
```java
package org.dromara.portal.api.event;

/** 门户事件契约常量（exchange / routing key / 队列名 / Dubbo group），跨服务勿改字面值 */
public final class PortalEventConstants {

    public static final String EXCHANGE = "portal.topic";
    public static final String DLX = "portal.dlx";

    public static final String QUEUE_KERNEL_NOTIFICATIONS = "portal-kernel.notifications";
    public static final String QUEUE_KERNEL_NOTIFICATIONS_DLQ = "portal-kernel.notifications.dlq";
    public static final String QUEUE_RESOURCES_CONVERT = "portal-resources.convert";
    public static final String QUEUE_RESOURCES_CONVERT_DLQ = "portal-resources.convert.dlq";

    public static final String RK_APPCENTER_DEMAND_REPLIED = "appcenter.demand.replied";
    public static final String RK_APPCENTER_APP_PUBLISHED = "appcenter.application.published";
    public static final String RK_FORUM_REPLY_CREATED = "forum.reply.created";
    public static final String RK_RESOURCES_ITEM_PUBLISHED = "resources.item.published";

    public static final String STATS_GROUP_APPCENTER = "appcenter";
    public static final String STATS_GROUP_FORUM = "forum";
    public static final String STATS_GROUP_RESOURCES = "resources";

    private PortalEventConstants() {
    }
}
```

`event/PortalNotificationEvent.java`：
```java
package org.dromara.portal.api.event;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 门户通知事件（BC → RabbitMQ → kernel 落 app_message）。
 * scene 即历史 msgType 取值：demand / app / forum / resource / system。
 */
@Data
public class PortalNotificationEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final String TARGET_ALL = "ALL";
    public static final String TARGET_USERS = "USERS";

    private String eventId;
    private Long occurredAt;
    private String scene;
    private String title;
    private String content;
    private String targetType;
    private List<Long> targetUserIds;

    public static PortalNotificationEvent toAll(String scene, String title, String content) {
        PortalNotificationEvent e = new PortalNotificationEvent();
        e.setScene(scene);
        e.setTitle(title);
        e.setContent(content);
        e.setTargetType(TARGET_ALL);
        return e;
    }

    public static PortalNotificationEvent toUsers(String scene, String title, String content, List<Long> userIds) {
        PortalNotificationEvent e = toAll(scene, title, content);
        e.setTargetType(TARGET_USERS);
        e.setTargetUserIds(userIds);
        return e;
    }
}
```

- [ ] **Step 4: 注册 api 模块（两处）**

`source/ruoyi-api/pom.xml` 的 `<modules>` 内、`<module>ruoyi-api-file</module>` 之后追加：
```xml
        <module>ruoyi-api-portal-kernel</module>
```
`source/ruoyi-api/ruoyi-api-bom/pom.xml` 的 `<dependencyManagement><dependencies>` 内追加：
```xml
            <dependency>
                <groupId>org.dromara</groupId>
                <artifactId>ruoyi-api-portal-kernel</artifactId>
                <version>${revision}</version>
            </dependency>
```

- [ ] **Step 5: 编译验证 api 模块**

```bash
cd /Users/macmini/windows-info-serve/source
mvn -o -ntp -Pdev -DskipTests -pl ruoyi-api/ruoyi-api-portal-kernel -am compile
```
预期：`BUILD SUCCESS`。

- [ ] **Step 6: 创建 MQ 公共模块 pom**

`source/ruoyi-common/ruoyi-common-portal-event/pom.xml`：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>org.dromara</groupId>
        <artifactId>ruoyi-common</artifactId>
        <version>${revision}</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>ruoyi-common-portal-event</artifactId>

    <description>ruoyi-common-portal-event 门户事件发布公共封装（RabbitMQ）</description>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>
        <dependency>
            <groupId>org.dromara</groupId>
            <artifactId>ruoyi-api-portal-kernel</artifactId>
        </dependency>
    </dependencies>
</project>
```
> 参照物：`ruoyi-common-bus/pom.xml` 的 parent 写法。`spring-boot-starter-amqp` 版本由 spring-boot BOM 管理，不写版本号。

- [ ] **Step 7: 自动装配 + 发布器**

`config/PortalEventAutoConfiguration.java`：
```java
package org.dromara.common.portalevent.config;

import org.dromara.common.portalevent.publisher.PortalEventPublisher;
import org.dromara.portal.api.event.PortalEventConstants;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class PortalEventAutoConfiguration {

    @Bean
    public MessageConverter portalEventMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange portalTopicExchange() {
        return ExchangeBuilder.topicExchange(PortalEventConstants.EXCHANGE).durable(true).build();
    }

    @Bean
    public PortalEventPublisher portalEventPublisher(RabbitTemplate rabbitTemplate) {
        return new PortalEventPublisher(rabbitTemplate);
    }
}
```

`publisher/PortalEventPublisher.java`：
```java
package org.dromara.common.portalevent.publisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.portal.api.event.PortalEventConstants;
import org.dromara.portal.api.event.PortalNotificationEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.UUID;

/**
 * 门户事件发布器。通知属尽力而为：broker 异常只记日志（含完整事件便于人工补发），
 * 绝不向业务调用方抛出——业务操作不得因通知失败而回滚。
 */
@Slf4j
@RequiredArgsConstructor
public class PortalEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishNotification(String routingKey, PortalNotificationEvent event) {
        if (event.getEventId() == null) {
            event.setEventId(UUID.randomUUID().toString());
        }
        if (event.getOccurredAt() == null) {
            event.setOccurredAt(System.currentTimeMillis());
        }
        try {
            rabbitTemplate.convertAndSend(PortalEventConstants.EXCHANGE, routingKey, event);
        } catch (Exception e) {
            log.error("门户事件发布失败 routingKey={} event={}", routingKey, event, e);
        }
    }
}
```

`src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`（单行）：
```
org.dromara.common.portalevent.config.PortalEventAutoConfiguration
```

- [ ] **Step 8: 注册 common 模块（两处）**

`source/ruoyi-common/pom.xml` 的 `<modules>` 追加 `<module>ruoyi-common-portal-event</module>`；
`source/ruoyi-common/ruoyi-common-bom/pom.xml` 的 `<dependencyManagement><dependencies>` 追加：
```xml
            <dependency>
                <groupId>org.dromara</groupId>
                <artifactId>ruoyi-common-portal-event</artifactId>
                <version>${revision}</version>
            </dependency>
```
> 两个文件的现有条目风格照抄（对齐缩进、位置按字母序附近即可）。

- [ ] **Step 9: 编译验证 + 提交**

```bash
cd /Users/macmini/windows-info-serve/source
mvn -o -ntp -Pdev -DskipTests -pl ruoyi-common/ruoyi-common-portal-event -am compile
```
预期：`BUILD SUCCESS`。

```bash
cd /Users/macmini/windows-info-serve
git add source/ruoyi-api source/ruoyi-common
git commit -m "feat: 新增门户内核契约模块与门户事件发布公共模块"
```
