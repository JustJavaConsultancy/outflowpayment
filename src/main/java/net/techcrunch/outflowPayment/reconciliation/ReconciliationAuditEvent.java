package net.techcrunch.outflowPayment.reconciliation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Getter
@Setter
public class ReconciliationAuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "primary_sequence")
    @SequenceGenerator(name = "primary_sequence", sequenceName = "primary_sequence", allocationSize = 1, initialValue = 10000)
    private Long id;

    private Long reconciliationItemId;
    private Long runId;
    private String reference;

    @Column(nullable = false, length = 80)
    private String eventType;

    @Column(nullable = false, length = 120)
    private String actor = "system";

    @Column(length = 1000)
    private String note;

    @Enumerated(EnumType.STRING)
    private ReconciliationItemStatus previousStatus;

    @Enumerated(EnumType.STRING)
    private ReconciliationItemStatus newStatus;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> details;

    @Column(nullable = false)
    private OffsetDateTime dateCreated = OffsetDateTime.now();
}
