package net.techcrunch.outflowPayment.accounting;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@ToString
@Table(name = "account")
public class Account {
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

    @Column(name = "name", nullable = false)
    private String name;
    private String code;
    private String type;
    private String description;
    private String currency;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal  balance;
    private String ownerId;

    private String accNumber;

}