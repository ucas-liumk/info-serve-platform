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
