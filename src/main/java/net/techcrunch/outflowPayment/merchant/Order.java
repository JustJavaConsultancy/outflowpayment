package net.techcrunch.outflowPayment.merchant;

import net.techcrunch.outflowPayment.invoice.Invoice;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;

@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@Entity
@ToString
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
            generator = "primary_sequence")
    @SequenceGenerator(
            name = "primary_sequence",
            sequenceName = "primary_sequence",
            allocationSize = 1,
            initialValue = 10000)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "merchant_id")
    private String merchantId;

    @OneToOne(optional = false, orphanRemoval = true)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime lastUpdated;
}