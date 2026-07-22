package net.techcrunch.outflowPayment.messaging;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/failed-messages")
public class FailedInboundMessageController {

    private final FailedInboundMessageService failedInboundMessageService;

    public FailedInboundMessageController(FailedInboundMessageService failedInboundMessageService) {
        this.failedInboundMessageService = failedInboundMessageService;
    }

    @GetMapping
    public List<FailedInboundMessage> listFailed() {
        return failedInboundMessageService.listFailed();
    }

    @PostMapping("/{id}/replay")
    public FailedInboundMessage replay(@PathVariable Long id) {
        return failedInboundMessageService.replay(id);
    }

    @PostMapping("/{id}/ignore")
    public FailedInboundMessage ignore(@PathVariable Long id) {
        return failedInboundMessageService.ignore(id);
    }
}
