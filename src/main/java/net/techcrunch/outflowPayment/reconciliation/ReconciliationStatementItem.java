package net.techcrunch.outflowPayment.reconciliation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "reconciliation_statement_items")
@Getter
@Setter
public class ReconciliationStatementItem {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "primary_sequence")
    @SequenceGenerator(name = "primary_sequence", sequenceName = "primary_sequence", allocationSize = 1, initialValue = 10000)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private ReconciliationStatementImport importBatch;

    @Column(nullable = false)
    private int rowNumber;

    @Column(length = 160)
    private String transactionReference;

    @Column(length = 160)
    private String externalReference;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(length = 8)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReconciliationStatementDirection direction;

    private OffsetDateTime transactionDate;
    private OffsetDateTime valueDate;

    @Column(length = 80)
    private String providerStatus;

    @Column(length = 1000)
    private String narration;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> rawPayload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ReconciliationStatementMatchStatus matchStatus = ReconciliationStatementMatchStatus.UNMATCHED;

    @Column(length = 160)
    private String matchedReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReconciliationMatchConfidence matchConfidence = ReconciliationMatchConfidence.NONE;

    @Column(length = 80)
    private String matchStrategy;

    @Column(length = 80)
    private String matchedReferenceType;

    private OffsetDateTime matchedAt;

    @Column(length = 1000)
    private String mismatchReason;
}
