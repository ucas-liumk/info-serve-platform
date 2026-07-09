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
