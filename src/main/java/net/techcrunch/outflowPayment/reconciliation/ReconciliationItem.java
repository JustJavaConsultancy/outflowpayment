package net.techcrunch.outflowPayment.reconciliation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
public class ReconciliationItem {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "primary_sequence")
    @SequenceGenerator(name = "primary_sequence", sequenceName = "primary_sequence", allocationSize = 1, initialValue = 10000)
    private Long id;

    @ManyToOne(optional = false)
    private ReconciliationRun run;

    @Column(nullable = false)
    private String reference;

    @Column(nullable = false)
    private String referenceType;

    private BigDecimal expectedAmount;
    private BigDecimal actualAmount;
    private String expectedStatus;
    private String actualStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReconciliationIssueType issueType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReconciliationItemStatus resolutionStatus;

    @Column(length = 1000)
    private String details;

    private String assignedTo;
    private OffsetDateTime assignedAt;

    @Column(length = 1000)
    private String resolutionNote;

    private String resolvedBy;
    private OffsetDateTime resolvedAt;

    private String reopenedBy;
    private OffsetDateTime reopenedAt;

    private String lastActionBy;
    private OffsetDateTime lastActionAt;
}
