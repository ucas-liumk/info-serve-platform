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
