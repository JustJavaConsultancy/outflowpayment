package net.techcrunch.outflowPayment.processes;

import net.techcrunch.outflowPayment.messaging.FailedInboundMessageService;
import net.techcrunch.outflowPayment.transfer.TransferInstruction;
import net.techcrunch.outflowPayment.transfer.TransferInstructionResult;
import net.techcrunch.outflowPayment.transfer.TransferInstructionService;
import net.techcrunch.outflowPayment.transfer.TransferInstructionStatus;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FlowableMessageListenerTest {

    private final RuntimeService runtimeService = mock(RuntimeService.class);
    private final TaskService taskService = mock(TaskService.class);
    private final TransferInstructionService transferInstructionService = mock(TransferInstructionService.class);
    private final FailedInboundMessageService failedInboundMessageService = mock(FailedInboundMessageService.class);
    private final FlowableMessageListener listener = new FlowableMessageListener(
            runtimeService,
            taskService,
            transferInstructionService,
            failedInboundMessageService
    );

    @Test
    void handlePaymentMessageStartsProcessForValidContractMessage() {
        Map<String, Object> message = transferMessage();
        Map<String, Object> processVariables = processVariables();
        TransferInstruction instruction = transferInstruction("trf-123");
        ProcessInstance processInstance = mock(ProcessInstance.class);

        when(transferInstructionService.prepareInstruction(message))
                .thenReturn(new TransferInstructionResult(instruction, processVariables, false, true));
        when(processInstance.getProcessInstanceId()).thenReturn("process-123");
        when(runtimeService.startProcessInstanceByMessage(
                "outflowPaymentMessage",
                "merchant-1",
                processVariables
        )).thenReturn(processInstance);

        listener.handlePaymentMessage(List.of(message));

        verify(runtimeService).startProcessInstanceByMessage(
                "outflowPaymentMessage",
                "merchant-1",
                processVariables
        );
        verify(transferInstructionService).markProcessStarted(instruction, "process-123");
    }

    @Test
    void handlePaymentMessageStartsProcessForSettlementContractMessage() {
        Map<String, Object> message = settlementMessage();
        Map<String, Object> processVariables = settlementProcessVariables();
        TransferInstruction instruction = transferInstruction("set_20260711_abc");
        instruction.setMerchantId("SETTLEMENT_BATCH");
        instruction.setBeneficiaryAccount("set_20260711_abc");
        ProcessInstance processInstance = mock(ProcessInstance.class);

        when(transferInstructionService.prepareInstruction(message))
                .thenReturn(new TransferInstructionResult(instruction, processVariables, false, true));
        when(processInstance.getProcessInstanceId()).thenReturn("process-set-123");
        when(runtimeService.startProcessInstanceByMessage(
                "outflowPaymentMessage",
                "SETTLEMENT_BATCH",
                processVariables
        )).thenReturn(processInstance);

        listener.handlePaymentMessage(List.of(message));

        verify(runtimeService).startProcessInstanceByMessage(
                "outflowPaymentMessage",
                "SETTLEMENT_BATCH",
                processVariables
        );
        verify(transferInstructionService).markProcessStarted(instruction, "process-set-123");
    }


    @Test
    void handlePaymentMessageSkipsDuplicateContractMessage() {
        Map<String, Object> message = transferMessage();
        TransferInstruction instruction = transferInstruction("trf-123");

        when(transferInstructionService.prepareInstruction(message))
                .thenReturn(new TransferInstructionResult(instruction, processVariables(), true, false));

        listener.handlePaymentMessage(List.of(message));

        verify(runtimeService, never()).startProcessInstanceByMessage(
                "outflowPaymentMessage",
                "merchant-1",
                processVariables()
        );
        verify(transferInstructionService, never()).markProcessStarted(instruction, "process-123");
    }

    @Test
    void handlePaymentMessageRejectsMalformedContractBeforeStartingProcess() {
        Map<String, Object> message = transferMessage();
        message.put("eventVersion", "2");

        when(transferInstructionService.prepareInstruction(anyMap()))
                .thenThrow(new IllegalArgumentException("Unsupported outflow eventVersion: 2"));

        assertThatThrownBy(() -> listener.handlePaymentMessage(List.of(message)))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verifyNoInteractions(runtimeService);
        verify(failedInboundMessageService).recordFailure(
                null,
                null,
                null,
                message,
                "IllegalArgumentException",
                "Unsupported outflow eventVersion: 2"
        );
    }

    @Test
    void handleTaskVerifierCompletionCompletesFlowableTaskWithRemainingVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("taskId", "task-123");
        variables.put("approved", true);

        listener.handleTaskVerifierCompletion(variables);

        verify(taskService).complete("task-123", Map.of("approved", true));
    }

    @Test
    void handleTaskAuthorizerCompletionCompletesFlowableTaskWithRemainingVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("taskId", "task-456");
        variables.put("authorized", true);

        listener.handleTaskAuthorizerCompletion(variables);

        verify(taskService).complete("task-456", Map.of("authorized", true));
    }

    private Map<String, Object> transferMessage() {
        Map<String, Object> message = new HashMap<>();
        message.put("eventType", "OUTFLOW_TRANSFER_REQUESTED");
        message.put("eventVersion", "1");
        message.put("sourceService", "bluepay");
        message.put("messageId", "msg-123");
        message.put("transferReference", "trf-123");
        message.put("correlationId", "corr-123");
        message.put("createdAt", OffsetDateTime.now().toString());
        message.put("payload", payload());
        return message;
    }

    private Map<String, Object> payload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("merchantId", "merchant-1");
        payload.put("amountToSend", "2500.00");
        payload.put("accNumber", "0123456789");
        return payload;
    }

    private Map<String, Object> processVariables() {
        Map<String, Object> variables = payload();
        variables.put("transferReference", "trf-123");
        variables.put("messageId", "msg-123");
        variables.put("correlationId", "corr-123");
        variables.put("sourceService", "bluepay");
        return variables;
    }

    private Map<String, Object> settlementMessage() {
        Map<String, Object> message = new HashMap<>();
        message.put("eventType", "OUTFLOW_SETTLEMENT_REQUESTED");
        message.put("eventVersion", "1");
        message.put("sourceService", "bluepay");
        message.put("messageId", "msg-set-123");
        message.put("transferReference", "set_20260711_abc");
        message.put("correlationId", "corr-set-123");
        message.put("createdAt", OffsetDateTime.now().toString());
        message.put("payload", settlementPayload());
        return message;
    }

    private Map<String, Object> settlementPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("batchId", 1000L);
        payload.put("batchReference", "set_20260711_abc");
        payload.put("settlementDate", "2026-07-11");
        payload.put("netAmount", "1180.00");
        payload.put("itemCount", 1);
        return payload;
    }

    private Map<String, Object> settlementProcessVariables() {
        Map<String, Object> variables = settlementPayload();
        variables.put("settlementReference", "set_20260711_abc");
        variables.put("transferReference", "set_20260711_abc");
        variables.put("merchantId", "SETTLEMENT_BATCH");
        variables.put("accNumber", "set_20260711_abc");
        variables.put("amountToSend", "1180.00");
        variables.put("instructionType", "SETTLEMENT");
        variables.put("messageId", "msg-set-123");
        variables.put("correlationId", "corr-set-123");
        variables.put("sourceService", "bluepay");
        return variables;
    }

    private TransferInstruction transferInstruction(String reference) {
        TransferInstruction instruction = new TransferInstruction();
        instruction.setReference(reference);
        instruction.setCorrelationId("corr-123");
        instruction.setMerchantId("merchant-1");
        instruction.setAmount(new BigDecimal("2500.00"));
        instruction.setBeneficiaryAccount("0123456789");
        instruction.setStatus(TransferInstructionStatus.RECEIVED);
        return instruction;
    }
}
