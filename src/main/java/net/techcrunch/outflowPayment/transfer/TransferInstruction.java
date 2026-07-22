package net.techcrunch.outflowPayment.transfer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "transfer_instructions")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class TransferInstruction {

    @Id
    @Column(nullable = false, updatable = false)
    @SequenceGenerator(
            name = "primary_sequence",
            sequenceName = "primary_sequence",
            allocationSize = 1,
            initialValue = 10000
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "primary_sequence"
    )
    private Long id;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(unique = true)
    private String correlationId;

    @Column(nullable = false)
    private String merchantId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String beneficiaryAccount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransferInstructionType instructionType = TransferInstructionType.TRANSFER;

    @Column(unique = true)
    private String refundReference;

    private String originalTransactionId;

    @Enumerated(EnumType.STRING)
    private RefundLifecycleStatus refundStatus;

    @Column(unique = true)
    private String settlementReference;

    @Column(unique = true)
    private String payoutGroupReference;

    private String settlementBatchId;

    private String settlementDate;

    private Integer settlementItemCount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransferInstructionStatus status;

    private String processInstanceId;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> rawMessage;

    @CreatedDate
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    private OffsetDateTime lastUpdated;
}
