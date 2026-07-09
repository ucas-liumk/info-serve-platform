package org.dromara.portal.resources.mq;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.portal.api.event.PortalEventConstants;
import org.dromara.portal.resources.service.IInfoResourceService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@Component
public class ResourceConvertListener {

    public static final String PENDING_KEY_PREFIX = "resources:convert:pending:";

    private final IInfoResourceService resourceService;

    @RabbitListener(queues = PortalEventConstants.QUEUE_RESOURCES_CONVERT,
                    containerFactory = "resourcesConvertContainerFactory")
    public void onConvert(ResourceConvertMessage message, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            resourceService.ensurePreviewConverted(message.getResourceId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("资料转换失败，转入死信 message={}", message, e);
            channel.basicNack(deliveryTag, false, false);
        } finally {
            RedisUtils.deleteObject(PENDING_KEY_PREFIX + message.getResourceId());
        }
    }
}
