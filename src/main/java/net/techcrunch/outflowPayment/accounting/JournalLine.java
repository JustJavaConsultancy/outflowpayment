package net.techcrunch.outflowPayment.accounting;

import net.techcrunch.outflowPayment.transaction.Transaction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@ToString
@EntityListeners(AuditingEntityListener.class)
@Table(name = "journal_line")
public class JournalLine {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    /* ==============================
       ACCOUNTING CORE
       ============================== */

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_entry_type", nullable = false)
    private AccountEntryType accountEntryType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "current_balance", precision = 19, scale = 2)
    private BigDecimal currentBalance;

    /* ==============================
       BUSINESS CONTEXT
       ============================== */

    @ManyToOne
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @Column(name = "external_reference", length = 100)
    private String externalReference; // payment / refund reference

    @Column(length = 255)
    private String description;

    @Column(length = 255)
    private String narration;

    /* ==============================
       TSA / TREASURY SUPPORT
       ============================== */

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(length = 3, nullable = false)
    private String currency; // e.g. NGN

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_category", nullable = false)
    private JournalEntryCategory entryCategory;

    @CreatedDate
    @Column(name = "posted_at", nullable = false)
    private OffsetDateTime postedAt;

}