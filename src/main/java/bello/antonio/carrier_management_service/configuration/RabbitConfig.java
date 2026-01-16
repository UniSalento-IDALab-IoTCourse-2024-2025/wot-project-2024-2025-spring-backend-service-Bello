package bello.antonio.carrier_management_service.configuration;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String ARTIST_REQUESTS_QUEUE = "artist-request-queue";
    public static final String ARTIST_APPROVAL_QUEUE = "artist-approval-queue";
    @Bean
    public Queue artistRequestQueue() {
        return new Queue(ARTIST_REQUESTS_QUEUE, false); // false = non persistente
    }

    @Bean
    public Queue artistApprovalQueue() {
        return new Queue(ARTIST_APPROVAL_QUEUE, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }


}
