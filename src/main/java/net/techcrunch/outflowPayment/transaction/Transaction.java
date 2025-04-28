package net.techcrunch.outflowPayment.transaction;

import net.techcrunch.outflowPayment.invoice.Status;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;


@Entity
@Table(name = "Transactions")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@ToString
public class Transaction implements Serializable {

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

    @Column(nullable = false, unique = true)
    private String externalReference;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String beneficiaryAccount;

    @Column(nullable = false)
    private String sourceAccount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;

    @Column(nullable = true)
    private String channel;

    @Column(nullable = true)
    private String transactionOwner;

    @CreatedDate
//    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateCreated;

//    @ToString.Exclude
//    @OneToOne(orphanRemoval = true)
//    @JoinColumn(name = "invoice_id")
//    private Invoice invoice;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> transactionDetails;

    @LastModifiedDate
//    @Column(nullable = false)
    private OffsetDateTime lastUpdated;



}
