package net.techcrunch.outflowPayment.transfer;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/transfers")
public class TransferInstructionController {

    private final TransferInstructionService transferInstructionService;

    public TransferInstructionController(TransferInstructionService transferInstructionService) {
        this.transferInstructionService = transferInstructionService;
    }

    @GetMapping("/status/{transferReference}")
    public TransferInstructionStatusResponse getTransferStatus(
            @PathVariable("transferReference") String transferReference
    ) {
        try {
            return TransferInstructionStatusResponse.from(
                    transferInstructionService.getByReference(transferReference)
            );
        } catch (IllegalStateException exc) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exc.getMessage(), exc);
        }
    }
}
