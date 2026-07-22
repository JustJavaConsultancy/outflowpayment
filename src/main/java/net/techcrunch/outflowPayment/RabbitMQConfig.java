package net.techcrunch.outflowPayment;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
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

    @Value("${message.flowable.message.dlx:${message.flowable.message.exchange}.dlx}")
    private String deadLetterExchange;

    @Value("${message.outflowPayment.task.verifier}")
    private String outflowTaskVerifier;

    @Value("${message.outflowPayment.task.authorizer}")
    private String outflowTaskAuthorizer;

    @Bean
    public Queue flowableMessageQueue() {
        return durableQueue(outflowQueue, outflowRoutingKey);
    }

    @Bean
    public Queue flowableVerifierTaskQueue(){
        return durableQueue(outflowTaskVerifierQueue, outflowTaskVerifier);
    }

    @Bean
    public Queue flowableAuthorizerTaskQueue(){
        return durableQueue(outflowTaskAuthorizerQueue, outflowTaskAuthorizer);
    }

    @Bean
    public DirectExchange flowableMessageExchange() {
        return new DirectExchange(flowableMessageExchange);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(deadLetterExchange);
    }

    @Bean
    public Queue outflowDeadLetterQueue() {
        return new Queue(outflowQueue + ".dlq", true);
    }

    @Bean
    public Queue verifierDeadLetterQueue() {
        return new Queue(outflowTaskVerifierQueue + ".dlq", true);
    }

    @Bean
    public Queue authorizerDeadLetterQueue() {
        return new Queue(outflowTaskAuthorizerQueue + ".dlq", true);
    }

    @Bean
    public Binding binding(@Qualifier("flowableMessageQueue") Queue queue,
                           @Qualifier("flowableMessageExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with(outflowRoutingKey);
    }

    @Bean
    public Binding verifierBinding(
            @Qualifier("flowableVerifierTaskQueue") Queue queue,
            @Qualifier("flowableMessageExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with(outflowTaskVerifier);
    }

    @Bean
    public Binding authorizerBinding(
            @Qualifier("flowableAuthorizerTaskQueue") Queue queue,
            @Qualifier("flowableMessageExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with(outflowTaskAuthorizer);
    }

    @Bean
    public Binding outflowDeadLetterBinding(@Qualifier("outflowDeadLetterQueue") Queue queue,
                                            @Qualifier("deadLetterExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(outflowRoutingKey + ".dlq");
    }

    @Bean
    public Binding verifierDeadLetterBinding(@Qualifier("verifierDeadLetterQueue") Queue queue,
                                             @Qualifier("deadLetterExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(outflowTaskVerifier + ".dlq");
    }

    @Bean
    public Binding authorizerDeadLetterBinding(@Qualifier("authorizerDeadLetterQueue") Queue queue,
                                               @Qualifier("deadLetterExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(outflowTaskAuthorizer + ".dlq");
    }

    @Bean
    public RabbitListenerContainerFactory<SimpleMessageListenerContainer> rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(1000, 2.0, 5000)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build());
        return factory;
    }

    private Queue durableQueue(String queueName, String routingKey) {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", deadLetterExchange)
                .withArgument("x-dead-letter-routing-key", routingKey + ".dlq")
                .build();
    }
}
