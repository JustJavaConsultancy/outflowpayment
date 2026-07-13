package net.techcrunch.outflowPayment.observability;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/operational-events")
public class OperationalEventController {

    private final OperationalEventRepository operationalEventRepository;

    public OperationalEventController(OperationalEventRepository operationalEventRepository) {
        this.operationalEventRepository = operationalEventRepository;
    }

    @GetMapping("/{reference}")
    public List<OperationalEvent> getEventsByReference(@PathVariable String reference) {
        return operationalEventRepository.findByReferenceOrderByDateCreatedAsc(reference);
    }
}
