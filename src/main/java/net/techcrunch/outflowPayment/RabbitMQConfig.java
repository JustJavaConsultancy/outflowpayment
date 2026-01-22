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

    @Value("${message.outflowPayment.queue}")
    private String outflowQueue;

    @Value("${message.outflowPayment.task.verifier.queue}")
    private String outflowTaskVerifierQueue;

    @Value("${message.outflowPayment.task.authorizer.queue}")
    private String outflowTaskAuthorizerQueue;

    @Value("${message.flowable.message.exchange}")
    private String flowableMessageExchange;

    @Value("${message.outflowPayment.task.verifier}")
    private String outflowTaskVerifier;

    @Value("${message.outflowPayment.task.authorizer}")
    private String outflowTaskAuthorizer;

    @Bean
    public Queue flowableMessageQueue() {
        return new Queue(outflowQueue, true);
    }

    @Bean
    public Queue flowableVerifierTaskQueue(){
        return new Queue(outflowTaskVerifierQueue, true);
    }

    @Bean
    public Queue flowableAuthorizerTaskQueue(){
        return new Queue(outflowTaskAuthorizerQueue,true);
    }

    @Bean
    public DirectExchange flowableMessageExchange() {
        return new DirectExchange(flowableMessageExchange);
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
                .with(outflowTaskVerifier);
    }

    @Bean
    public Binding authorizerBinding(
            @Qualifier("flowableAuthorizerTaskQueue") Queue queue,
            DirectExchange exchange) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with(outflowTaskAuthorizer);
    }
}
