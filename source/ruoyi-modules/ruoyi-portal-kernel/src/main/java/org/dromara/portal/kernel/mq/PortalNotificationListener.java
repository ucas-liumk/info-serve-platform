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
