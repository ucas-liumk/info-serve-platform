package org.dromara.portal.resources.mq;

import org.dromara.portal.api.event.PortalEventConstants;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 资料转换工作队列拓扑：prefetch=1 串行化 LibreOffice，失败入死信 */
@Configuration
public class ResourcesMqConfig {

    @Bean
    public DirectExchange resourcesDlxExchange() {
        return ExchangeBuilder.directExchange(PortalEventConstants.DLX).durable(true).build();
    }

    @Bean
    public Queue resourcesConvertQueue() {
        return QueueBuilder.durable(PortalEventConstants.QUEUE_RESOURCES_CONVERT)
            .deadLetterExchange(PortalEventConstants.DLX)
            .deadLetterRoutingKey(PortalEventConstants.QUEUE_RESOURCES_CONVERT_DLQ)
            .build();
    }

    @Bean
    public Queue resourcesConvertDlq() {
        return QueueBuilder.durable(PortalEventConstants.QUEUE_RESOURCES_CONVERT_DLQ).build();
    }

    @Bean
    public Binding resourcesConvertDlqBinding() {
        return BindingBuilder.bind(resourcesConvertDlq()).to(resourcesDlxExchange())
            .with(PortalEventConstants.QUEUE_RESOURCES_CONVERT_DLQ);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory resourcesConvertContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter portalEventMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(portalEventMessageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(1);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
