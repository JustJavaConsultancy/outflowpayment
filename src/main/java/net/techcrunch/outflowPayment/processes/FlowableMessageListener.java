package net.techcrunch.outflowPayment.processes;

import net.techcrunch.outflowPayment.transfer.DurationType;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
public class FlowableMessageListener {
    private final static Logger log = LoggerFactory.getLogger(FlowableMessageListener.class);

    private final RuntimeService runtimeService;
    private final TaskService taskService;

    public FlowableMessageListener(RuntimeService runtimeService, TaskService taskService){
        this.runtimeService = runtimeService;
        this.taskService = taskService;
    }

    @RabbitListener(
            queues = "${message.outflowPayment.queue}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handlePaymentMessage(List<Map<String, Object>> variables) {
        log.info("\nReceived variables: {}", variables);
        int i = 1;
        for (Map<String, Object> variable : variables) {
            Map<String, Object> transferDTO = (Map<String, Object>) variable.get("TransferDTO");
            log.info("transferDTO:: {}", transferDTO);

            if (transferDTO.get("duration") != null && !transferDTO.get("duration").toString().isEmpty()) {
                DurationType period = DurationType.fromValue(transferDTO.get("duration").toString()).orElse(null);
                if (period != null) {
                    LocalDate nextDate = period.nextBillingDate(LocalDate.now());
                    log.info("nextBillingDate = {}",nextDate);
                    transferDTO.put(
                            "nextBillingDate",
                            nextDate.toString()
                    );
                    transferDTO.put("isRecurrent", true);
                }
            }

            transferDTO.putIfAbsent("isRecurrent", false);
            String businessKey = (String) transferDTO.get("merchantId");
            runtimeService.startProcessInstanceByMessage(
                    "outflowPaymentMessage",
                    businessKey,
                    transferDTO
            );
        }
    }

    @RabbitListener(
            queues = "${message.outflowPayment.task.verifier.queue}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleTaskVerifierCompletion(Map<String, Object> variables) {
        String taskId = (String) variables.get("taskId");
        variables.remove("taskId");
        taskService.complete(taskId, variables);
    }

    @RabbitListener(
            queues = "${message.outflowPayment.task.authorizer.queue}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleTaskAuthorizerCompletion(Map<String, Object> variables){
        String taskId = (String) variables.get("taskId");
        variables.remove("taskId");
        taskService.complete(taskId, variables);
    }
}
