package net.techcrunch.outflowPayment.reconciliation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "reconciliation_statement_rejected_items")
@Getter
@Setter
public class ReconciliationStatementRejectedItem {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "primary_sequence")
    @SequenceGenerator(name = "primary_sequence", sequenceName = "primary_sequence", allocationSize = 1, initialValue = 10000)
    private Long id;

    @ManyToOne(optional = false)
    private ReconciliationStatementImport importBatch;

    @Column(nullable = false)
    private int rowNumber;

    @Column(length = 4000)
    private String rawLine;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> rawPayload;

    @Column(nullable = false)
    private OffsetDateTime dateCreated = OffsetDateTime.now();
}
