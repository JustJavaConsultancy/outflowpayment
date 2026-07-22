package net.techcrunch.outflowPayment.transfer;

import java.util.Map;

public record TransferInstructionResult(
        TransferInstruction instruction,
        Map<String, Object> variables,
        boolean duplicate,
        boolean shouldStartProcess
) {
}
