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
            queues = "${message.ouflowPayment.queue}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handlePaymentMessage(Map<String, Object> variables) {
        runtimeService.startProcessInstanceByMessage(
                "outflowPaymentMessage",
                variables
        );
    }
}
