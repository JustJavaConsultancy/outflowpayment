package net.techcrunch.outflowPayment;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue flowableMessageQueue() {
        return new Queue("outflowPayment.queue", true);
    }

    @Bean
    public DirectExchange flowableMessageExchange() {
        return new DirectExchange("flowable.message.exchange");
    }

    @Bean
    public Binding binding(Queue queue, DirectExchange exchange) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with("outflowPayment.routingKey");
    }
}
