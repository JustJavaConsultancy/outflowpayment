package net.techcrunch.outflowPayment.processes;

import net.techcrunch.outflowPayment.messaging.FailedInboundMessageService;
import net.techcrunch.outflowPayment.transfer.TransferInstructionResult;
import net.techcrunch.outflowPayment.transfer.TransferInstructionService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class FlowableMessageListener {
    private final static Logger log = LoggerFactory.getLogger(FlowableMessageListener.class);

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final TransferInstructionService transferInstructionService;
    private final FailedInboundMessageService failedInboundMessageService;

    @Value("${message.outflowPayment.queue}")
    private String outflowQueue;

    @Value("${message.outflowPayment-routing-key}")
    private String outflowRoutingKey;

    @Value("${message.flowable.message.exchange}")
    private String flowableMessageExchange;

    public FlowableMessageListener(RuntimeService runtimeService,
                                   TaskService taskService,
                                   TransferInstructionService transferInstructionService,
                                   FailedInboundMessageService failedInboundMessageService){
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.transferInstructionService = transferInstructionService;
        this.failedInboundMessageService = failedInboundMessageService;
    }

    @RabbitListener(
            queues = "${message.outflowPayment.queue}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handlePaymentMessage(List<Map<String, Object>> variables) {
        log.info("\nReceived variables: {}", variables);
        for (Map<String, Object> variable : variables) {
            try {
                TransferInstructionResult instructionResult = transferInstructionService.prepareInstruction(variable);
                Map<String, Object> transferDTO = instructionResult.variables();
                log.info("transferDTO:: {}", transferDTO);

                if (!instructionResult.shouldStartProcess()) {
                    log.info(
                            "Skipping duplicate outflow transfer instruction: {}",
                            instructionResult.instruction().getReference()
                    );
                    continue;
                }

                String businessKey = (String) transferDTO.get("merchantId");
                ProcessInstance processInstance = runtimeService.startProcessInstanceByMessage(
                        "outflowPaymentMessage",
                        businessKey,
                        transferDTO
                );
                transferInstructionService.markProcessStarted(
                        instructionResult.instruction(),
                        processInstance.getProcessInstanceId()
                );
            } catch (RuntimeException exception) {
                failedInboundMessageService.recordFailure(
                        outflowQueue,
                        flowableMessageExchange,
                        outflowRoutingKey,
                        variable,
                        exception.getClass().getSimpleName(),
                        exception.getMessage()
                );
                throw new AmqpRejectAndDontRequeueException("Outflow message failed and was recorded for replay", exception);
            }
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
