package net.techcrunch.outflowPayment.processes;

import org.flowable.engine.RuntimeService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
@Component
public class FlowableMessageListener {
    @Autowired
    private RuntimeService runtimeService;

    @RabbitListener(
            queues = "outflowPayment.queue",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handlePaymentMessage(Map<String, Object> variables) {
        System.out.println("\nReceived variables: " + variables);
//        runtimeService.startProcessInstanceByMessage(
//                "outflowPaymentMessage",
//                variables
//        );
    }
}
