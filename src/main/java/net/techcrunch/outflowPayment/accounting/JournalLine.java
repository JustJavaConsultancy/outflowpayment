package net.techcrunch.outflowPayment.accounting;

import net.techcrunch.outflowPayment.transaction.Transaction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@ToString
@Table(name = "journal_line")
public class JournalLine {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;
    private BigDecimal currentBalance;
    private BigDecimal amount;
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_entry_type")
    private AccountEntryType accountEntryType;

    @ManyToOne
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

}