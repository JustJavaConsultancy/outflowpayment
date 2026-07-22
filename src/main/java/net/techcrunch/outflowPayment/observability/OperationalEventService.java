package net.techcrunch.outflowPayment.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class OperationalEventService {

    private static final Logger log = LoggerFactory.getLogger(OperationalEventService.class);
    private static final String SERVICE_NAME = "outflowpayment";

    private final OperationalEventRepository operationalEventRepository;

    public OperationalEventService(OperationalEventRepository operationalEventRepository) {
        this.operationalEventRepository = operationalEventRepository;
    }

    public OperationalEvent record(String eventType,
                                   OperationalEventSeverity severity,
                                   String referenceType,
                                   String reference,
                                   String summary,
                                   Map<String, Object> details) {
        Map<String, Object> safeDetails = details == null ? Map.of() : new HashMap<>(details);
        OperationalEvent event = new OperationalEvent();
        event.setServiceName(SERVICE_NAME);
        event.setEventType(eventType);
        event.setSeverity(severity);
        event.setReferenceType(referenceType);
        event.setReference(reference);
        event.setCorrelationId(stringValue(safeDetails.get("correlationId")));
        event.setMessageId(stringValue(safeDetails.get("messageId")));
        event.setMerchantId(stringValue(safeDetails.get("merchantId")));
        event.setProcessInstanceId(stringValue(safeDetails.get("processInstanceId")));
        event.setFailureClass(stringValue(safeDetails.get("failureClass")));
        event.setSummary(summary);
        event.setDetails(safeDetails);

        log.info(
                "operational_event service={} eventType={} severity={} referenceType={} reference={} correlationId={} messageId={} merchantId={} processInstanceId={} summary={}",
                SERVICE_NAME,
                eventType,
                severity,
                referenceType,
                reference,
                event.getCorrelationId(),
                event.getMessageId(),
                event.getMerchantId(),
                event.getProcessInstanceId(),
                summary
        );
        return operationalEventRepository.save(event);
    }

    public OperationalEvent info(String eventType,
                                 String referenceType,
                                 String reference,
                                 String summary,
                                 Map<String, Object> details) {
        return record(eventType, OperationalEventSeverity.INFO, referenceType, reference, summary, details);
    }

    public OperationalEvent warning(String eventType,
                                    String referenceType,
                                    String reference,
                                    String summary,
                                    Map<String, Object> details) {
        return record(eventType, OperationalEventSeverity.WARNING, referenceType, reference, summary, details);
    }

    public OperationalEvent error(String eventType,
                                  String referenceType,
                                  String reference,
                                  String summary,
                                  Map<String, Object> details) {
        return record(eventType, OperationalEventSeverity.ERROR, referenceType, reference, summary, details);
    }

    private String stringValue(Object value) {
        return value == null || value.toString().isBlank() ? null : value.toString();
    }
}
