package net.techcrunch.outflowPayment.transfer;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpTemplate;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SettlementResultPublisherTest {

    private final AmqpTemplate rabbitTemplate = mock(AmqpTemplate.class);
    private final SettlementResultPublisher publisher = new SettlementResultPublisher(
            rabbitTemplate,
            "flowable.message.exchange",
            "bluepay.settlement.result"
    );

    @Test
    void publishSendsTerminalSettlementResultToBluepay() {
        TransferInstruction instruction = new TransferInstruction();
        instruction.setInstructionType(TransferInstructionType.SETTLEMENT);
        instruction.setSettlementReference("set_20260711_abc");
        instruction.setSettlementBatchId("1000");
        instruction.setSettlementDate("2026-07-11");
        instruction.setSettlementItemCount(2);
        instruction.setAmount(new BigDecimal("1180.00"));
        instruction.setCorrelationId("corr-set-123");
        instruction.setProcessInstanceId("process-set-123");

        publisher.publish(
                instruction,
                TransferInstructionStatus.COMPLETED,
                Map.of("providerReference", "devout_set_20260711_abc")
        );

        verify(rabbitTemplate).convertAndSend(
                eq("flowable.message.exchange"),
                eq("bluepay.settlement.result"),
                org.mockito.ArgumentMatchers.<Object>argThat(argument -> {
                    Map<?, ?> message = (Map<?, ?>) argument;
                    Map<?, ?> payload = (Map<?, ?>) message.get("payload");
                    assertThat(message.get("eventType")).isEqualTo("OUTFLOW_SETTLEMENT_RESULT");
                    assertThat(message.get("sourceService")).isEqualTo("outflowpayment");
                    assertThat(message.get("reference")).isEqualTo("set_20260711_abc");
                    assertThat(payload.get("settlementStatus")).isEqualTo("SETTLED");
                    assertThat(payload.get("outflowStatus")).isEqualTo("COMPLETED");
                    assertThat(payload.get("settlementReference")).isEqualTo("set_20260711_abc");
                    return true;
                })
        );
    }
}
