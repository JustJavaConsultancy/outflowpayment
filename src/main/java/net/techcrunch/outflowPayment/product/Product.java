package net.techcrunch.outflowPayment.product;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "Products")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Product {

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

    /* ================= Identification ================= */

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 300)
    private String description;

    /* ================= Pricing ================= */

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /* ================= Inventory ================= */

    /**
     * Null or 0 means "not stock-tracked"
     * (used for government services & digital goods)
     */
    private Integer quantityInStock;
    private boolean unlimited = false;

    /**
     * TRUE  → physical goods
     * FALSE → services / digital / government
     */
    @Column(nullable = false)
    private Boolean containsPhysicalGoods = true;

    private Integer quantitySold = 0;

    /* ================= Ownership ================= */

    /**
     * Present → merchant product
     * Null    → government product
     */
    @Column(name = "merchant_id")
    private String merchantId;

    /**
     * Revenue settlement account
     * - Merchant bank or ledger account
     * - Government treasury mapping
     */
    @Column(name = "account_id", nullable = false)
    private Long accountId;

    /* ================= Government / IGR ================= */

    /**
     * Official Nigerian revenue code
     * e.g. TIN, BPL, LUC, MKT
     */
    @Column(name = "revenue_code", length = 20)
    private String revenueCode;

    private String category = "";

    /* ================= Subscription ================= */
    private Boolean subscribe = false;

    /* ================= Media ================= */

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "product_media",
            joinColumns = @JoinColumn(name = "product_id")
    )
    @Column(name = "media_url")
    private List<String> media = new ArrayList<>();

    /* ================= Audit ================= */

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime lastUpdated;
}
