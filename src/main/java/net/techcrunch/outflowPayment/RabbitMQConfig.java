package net.techcrunch.outflowPayment;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${message.outflowPayment-routing-key}")
    String outflowRoutingKey;

//    @Value("${message.outflowPayment.task.complete}")
//    private String outflowTaskRoutingKey;

    @Value("${message.ouflowPayment.queue}")
    private String outflowQueue;

//    @Value("${message.outflowPayment.task.complete.queue}")
//    private String outflowTaskQueue;

    @Bean
    public Queue flowableMessageQueue() {
        return new Queue(outflowQueue, true);
    }

    @Bean
    public Queue flowableVerifierTaskQueue(){
        return new Queue("outflowPayment.task.verifier.queue", true);
    }

    @Bean
    public Queue flowableAuthorizerTaskQueue(){
        return new Queue("outflowPayment.task.authorizer.queue" ,true);
    }

    @Bean
    public DirectExchange flowableMessageExchange() {
        return new DirectExchange("flowable.message.exchange");
    }

    @Bean
    public Binding binding(@Qualifier("flowableMessageQueue") Queue queue,
                           DirectExchange exchange) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with(outflowRoutingKey);
    }

    @Bean
    public Binding verifierBinding(
            @Qualifier("flowableVerifierTaskQueue") Queue queue,
            DirectExchange exchange) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with("outflowPayment.task.verifier");
    }

    @Bean
    public Binding authorizerBinding(
            @Qualifier("flowableAuthorizerTaskQueue") Queue queue,
            DirectExchange exchange) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with("outflowPayment.task.authorizer");
    }
}
