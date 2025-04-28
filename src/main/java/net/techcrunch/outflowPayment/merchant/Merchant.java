package net.techcrunch.outflowPayment.merchant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "merchant")
public class Merchant {
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
    private String businessIdentity;
    private String businessName;
    private String businessAddress;
    private String mode;


}