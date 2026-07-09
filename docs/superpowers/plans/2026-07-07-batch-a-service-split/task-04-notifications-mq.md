# Task 4: 单体内解耦② —— 通知 MQ 化（4 发布点 + kernel 消费者 + DLQ + 幂等）

四个通知写入点（appcenter×2 / forum / resources）从进程内调用 `IPortalNotificationService` 改为发布 `PortalNotificationEvent` 到 RabbitMQ；kernel 起消费者落 `app_message`。broker、账密、`spring.rabbitmq` 公共配置已在 `application-common.yml` 就位，本任务零 Nacos 配置变更。

**Files:**
- Test: `source/ruoyi-modules/ruoyi-portal/src/test/java/org/dromara/portal/kernel/mq/PortalNotificationListenerTest.java`（新建）
- Create: `.../ruoyi-portal/src/main/java/org/dromara/portal/kernel/mq/KernelMqConfig.java`
- Create: `.../kernel/mq/PortalNotificationListener.java`
- Modify: `source/ruoyi-modules/ruoyi-portal/pom.xml`（+`ruoyi-common-portal-event`，含传递的 amqp）
- Modify: `.../appcenter/service/impl/AppDemandServiceImpl.java`（:153 附近）
- Modify: `.../appcenter/service/impl/AppApplicationServiceImpl.java`（:373 附近）
- Modify: `.../forum/service/impl/InfoForumServiceImpl.java`（:266 附近）
- Modify: `.../resources/service/impl/InfoResourceServiceImpl.java`（:698 附近）
- Modify: `.../arch/BcBoundaryTest.java`（收紧规则 6 至终态）
- Modify: `deploy/docker-compose.yml`（ruoyi-portal 增加 rabbitmq 健康依赖）

**Interfaces:**
- Consumes: T1 的 `PortalEventPublisher`、`PortalNotificationEvent`、`PortalEventConstants`；T3 后的 kernel 内部 `IPortalNotificationService`（listener 复用其落库逻辑，接口不再对 BC 暴露）。
- Produces: 运行中的通知链路（exchange `portal.topic`、队列 `portal-kernel.notifications`+DLQ、幂等键前缀 `portal:event:`）。T7 时 `kernel/mq/*` 随包搬迁。

- [ ] **Step 1: portal 加依赖**

`source/ruoyi-modules/ruoyi-portal/pom.xml` `<dependencies>` 追加：
```xml
        <dependency>
            <groupId>org.dromara</groupId>
            <artifactId>ruoyi-common-portal-event</artifactId>
        </dependency>
```

- [ ] **Step 2: 写失败测试（RED）**

`src/test/java/org/dromara/portal/kernel/mq/PortalNotificationListenerTest.java`：
```java
package org.dromara.portal.kernel.mq;

import com.rabbitmq.client.Channel;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.portal.api.event.PortalNotificationEvent;
import org.dromara.portal.kernel.service.IPortalNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PortalNotificationListenerTest {

    @Mock
    private IPortalNotificationService notificationService;

    @Mock
    private Channel channel;

    @InjectMocks
    private PortalNotificationListener listener;

    private PortalNotificationEvent event(String targetType) {
        PortalNotificationEvent e = PortalNotificationEvent.toUsers("forum", "标题", "内容", List.of(2L));
        e.setEventId("evt-1");
        e.setTargetType(targetType);
        return e;
    }

    @Test
    void first_delivery_dispatches_and_acks() throws Exception {
        try (MockedStatic<RedisUtils> redis = mockStatic(RedisUtils.class)) {
            redis.when(() -> RedisUtils.setObjectIfAbsent(anyString(), any(), any(Duration.class))).thenReturn(true);
            listener.onNotification(event(PortalNotificationEvent.TARGET_USERS), channel, 7L);
            verify(notificationService).sendToUsers(List.of(2L), "标题", "内容", "forum");
            verify(channel).basicAck(7L, false);
        }
    }

    @Test
    void duplicate_delivery_skips_and_acks() throws Exception {
        try (MockedStatic<RedisUtils> redis = mockStatic(RedisUtils.class)) {
            redis.when(() -> RedisUtils.setObjectIfAbsent(anyString(), any(), any(Duration.class))).thenReturn(false);
            listener.onNotification(event(PortalNotificationEvent.TARGET_USERS), channel, 8L);
            verify(notificationService, never()).sendToUsers(any(), anyString(), anyString(), anyString());
            verify(channel).basicAck(8L, false);
        }
    }

    @Test
    void broadcast_event_uses_sendToAllUsers() throws Exception {
        try (MockedStatic<RedisUtils> redis = mockStatic(RedisUtils.class)) {
            redis.when(() -> RedisUtils.setObjectIfAbsent(anyString(), any(), any(Duration.class))).thenReturn(true);
            listener.onNotification(event(PortalNotificationEvent.TARGET_ALL), channel, 9L);
            verify(notificationService).sendToAllUsers("标题", "内容", "forum");
            verify(channel).basicAck(9L, false);
        }
    }

    @Test
    void failure_nacks_to_dlq() throws Exception {
        try (MockedStatic<RedisUtils> redis = mockStatic(RedisUtils.class)) {
            redis.when(() -> RedisUtils.setObjectIfAbsent(anyString(), any(), any(Duration.class))).thenReturn(true);
            doThrow(new RuntimeException("db down")).when(notificationService)
                .sendToAllUsers(anyString(), anyString(), anyString());
            listener.onNotification(event(PortalNotificationEvent.TARGET_ALL), channel, 10L);
            verify(channel).basicNack(10L, false, false);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }
    }
}
```

- [ ] **Step 3: 跑测试确认失败**

```bash
cd /Users/macmini/windows-info-serve/source
mvn -ntp -pl ruoyi-modules/ruoyi-portal -am -DskipTests=false test
```
预期：编译失败（`PortalNotificationListener` 不存在）。

- [ ] **Step 4: kernel 消费者实现**

`kernel/mq/KernelMqConfig.java`：
```java
package org.dromara.portal.kernel.mq;

import org.dromara.portal.api.event.PortalEventConstants;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** kernel 通知消费端拓扑：队列 + DLQ + 四条 routing key 绑定 + 手动 ack 容器工厂 */
@Configuration
public class KernelMqConfig {

    @Bean
    public DirectExchange portalDlxExchange() {
        return ExchangeBuilder.directExchange(PortalEventConstants.DLX).durable(true).build();
    }

    @Bean
    public Queue kernelNotificationsQueue() {
        return QueueBuilder.durable(PortalEventConstants.QUEUE_KERNEL_NOTIFICATIONS)
            .deadLetterExchange(PortalEventConstants.DLX)
            .deadLetterRoutingKey(PortalEventConstants.QUEUE_KERNEL_NOTIFICATIONS_DLQ)
            .build();
    }

    @Bean
    public Queue kernelNotificationsDlq() {
        return QueueBuilder.durable(PortalEventConstants.QUEUE_KERNEL_NOTIFICATIONS_DLQ).build();
    }

    @Bean
    public Binding kernelNotificationsDlqBinding() {
        return BindingBuilder.bind(kernelNotificationsDlq()).to(portalDlxExchange())
            .with(PortalEventConstants.QUEUE_KERNEL_NOTIFICATIONS_DLQ);
    }

    @Bean
    public Binding bindDemandReplied(TopicExchange portalTopicExchange) {
        return BindingBuilder.bind(kernelNotificationsQueue()).to(portalTopicExchange)
            .with(PortalEventConstants.RK_APPCENTER_DEMAND_REPLIED);
    }

    @Bean
    public Binding bindAppPublished(TopicExchange portalTopicExchange) {
        return BindingBuilder.bind(kernelNotificationsQueue()).to(portalTopicExchange)
            .with(PortalEventConstants.RK_APPCENTER_APP_PUBLISHED);
    }

    @Bean
    public Binding bindForumReplyCreated(TopicExchange portalTopicExchange) {
        return BindingBuilder.bind(kernelNotificationsQueue()).to(portalTopicExchange)
            .with(PortalEventConstants.RK_FORUM_REPLY_CREATED);
    }

    @Bean
    public Binding bindResourcePublished(TopicExchange portalTopicExchange) {
        return BindingBuilder.bind(kernelNotificationsQueue()).to(portalTopicExchange)
            .with(PortalEventConstants.RK_RESOURCES_ITEM_PUBLISHED);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory portalManualAckContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter portalEventMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(portalEventMessageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(5);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
```
（`portalTopicExchange`、`portalEventMessageConverter` 两个 bean 由 T1 的 `PortalEventAutoConfiguration` 提供。）

`kernel/mq/PortalNotificationListener.java`：
```java
package org.dromara.portal.kernel.mq;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.portal.api.event.PortalEventConstants;
import org.dromara.portal.api.event.PortalNotificationEvent;
import org.dromara.portal.kernel.service.IPortalNotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

/** 通知事件消费者：eventId Redis 幂等，成功 ack，失败 nack 直入 DLQ（不重回队列） */
@Slf4j
@RequiredArgsConstructor
@Component
public class PortalNotificationListener {

    private static final String DEDUP_KEY_PREFIX = "portal:event:";
    private static final Duration DEDUP_TTL = Duration.ofDays(7);

    private final IPortalNotificationService notificationService;

    @RabbitListener(queues = PortalEventConstants.QUEUE_KERNEL_NOTIFICATIONS,
                    containerFactory = "portalManualAckContainerFactory")
    public void onNotification(PortalNotificationEvent event, Channel channel,
                               @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            if (event == null || StringUtils.isBlank(event.getEventId())) {
                log.warn("丢弃非法通知事件: {}", event);
                channel.basicAck(deliveryTag, false);
                return;
            }
            boolean first = RedisUtils.setObjectIfAbsent(DEDUP_KEY_PREFIX + event.getEventId(), "1", DEDUP_TTL);
            if (!first) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            dispatch(event);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("通知事件处理失败，转入死信 event={}", event, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private void dispatch(PortalNotificationEvent event) {
        if (PortalNotificationEvent.TARGET_ALL.equals(event.getTargetType())) {
            notificationService.sendToAllUsers(event.getTitle(), event.getContent(), event.getScene());
        } else {
            notificationService.sendToUsers(event.getTargetUserIds(), event.getTitle(), event.getContent(), event.getScene());
        }
    }
}
```
> 若 `ruoyi-common-redis` 不在 portal 传递依赖里（编译报 `RedisUtils` 缺失），在 portal pom 显式加 `<dependency><groupId>org.dromara</groupId><artifactId>ruoyi-common-redis</artifactId></dependency>`。

- [ ] **Step 5: 四个发布点替换（逐一）**

通用改法：每个 impl 类删 import `org.dromara.portal.kernel.service.IPortalNotificationService` 与字段 `private final IPortalNotificationService notificationService;`，新增字段 `private final PortalEventPublisher eventPublisher;` 与 imports：
```java
import org.dromara.common.portalevent.publisher.PortalEventPublisher;
import org.dromara.portal.api.event.PortalEventConstants;
import org.dromara.portal.api.event.PortalNotificationEvent;
```

1. `AppDemandServiceImpl.java` 的 `sendHandleMessage` 方法体替换为：
```java
    private void sendHandleMessage(AppDemand demand, String handleRemark) {
        eventPublisher.publishNotification(
            PortalEventConstants.RK_APPCENTER_DEMAND_REPLIED,
            PortalNotificationEvent.toUsers(
                "demand",
                "需求反馈已回复",
                "你提交的“" + StringUtils.blankToDefault(demand.getAppName(), "需求反馈") + "”已有管理员回复：" + handleRemark,
                java.util.List.of(demand.getRequesterId())
            ));
    }
```

2. `AppApplicationServiceImpl.java` 的 `sendAppOnlineNotification` 方法体中 `notificationService.sendToAllUsers(...)` 调用替换为：
```java
        eventPublisher.publishNotification(
            PortalEventConstants.RK_APPCENTER_APP_PUBLISHED,
            PortalNotificationEvent.toAll(
                "app",
                "应用上架：" + app.getAppName(),
                "应用中心已上架“" + app.getAppName() + "”" + version + "，可前往应用中心打开使用。"
            ));
```

3. `InfoForumServiceImpl.java` 的 `sendForumReplyNotification` 中 `notificationService.sendToUsers(...)` 调用替换为：
```java
        eventPublisher.publishNotification(
            PortalEventConstants.RK_FORUM_REPLY_CREATED,
            PortalNotificationEvent.toUsers(
                "forum",
                "论坛有新回复：" + topic.getTitle(),
                author + " 回复了话题“" + topic.getTitle() + "”：" + excerpt,
                java.util.List.copyOf(recipients)
            ));
```

4. `InfoResourceServiceImpl.java` 的 `sendResourceCreatedNotification` 中 `notificationService.sendToAllUsers(...)` 调用替换为：
```java
        eventPublisher.publishNotification(
            PortalEventConstants.RK_RESOURCES_ITEM_PUBLISHED,
            PortalNotificationEvent.toAll(
                "resource",
                "新增资源：" + resource.getTitle(),
                uploader + " 上传了新资源“" + resource.getTitle() + "”（" + fileName + "），可前往资料共享中查看。"
            ));
```
文案字符串与原文逐字一致（保持用户可见行为不变）。

- [ ] **Step 6: ArchUnit 收紧至终态（内容 BC 对 kernel 零依赖）**

`BcBoundaryTest.java` 中 T3 的 `content_bcs_should_only_touch_kernel_service_interfaces` 替换为：
```java
    @Test
    void content_bcs_should_not_depend_on_kernel_at_all() {
        noClasses().that().resideInAnyPackage(
                "org.dromara.portal.appcenter..", "org.dromara.portal.resources..",
                "org.dromara.portal.forum..", "org.dromara.portal.requiredknowledge..")
            .should().dependOnClassesThat().resideInAPackage("org.dromara.portal.kernel..")
            .check(classes);
    }
```

- [ ] **Step 7: 跑测试（GREEN）**

```bash
cd /Users/macmini/windows-info-serve/source
mvn -ntp -pl ruoyi-modules/ruoyi-portal -am -DskipTests=false test
```
预期：`BUILD SUCCESS`，Listener 4 用例 + Message 3 用例 + ArchUnit 全绿。

- [ ] **Step 8: compose 依赖补齐 + 端到端手验**

`deploy/docker-compose.yml` 的 `ruoyi-portal` 服务 `depends_on` 增加：
```yaml
      rabbitmq:
        condition: service_healthy
```
```bash
cd /Users/macmini/windows-info-serve/deploy && docker compose --env-file .env config --quiet
```
端到端手验（基础设施已起、重建并重启 portal 后）：在门户发一条论坛回复 → RabbitMQ 管理页 `http://127.0.0.1:8173`（ruoyi/ruoyi123）应见 `portal.topic` exchange 与 `portal-kernel.notifications` 队列曲线；消息中心铃铛出现新通知。此验证亦可推迟到 T12 统一做，但队列/exchange 声明正确性建议现在就看一眼。

- [ ] **Step 9: 提交**

```bash
cd /Users/macmini/windows-info-serve
git add -A
git commit -m "feat: 门户通知改走 RabbitMQ 事件，内核消费落库并支持死信与幂等"
```
