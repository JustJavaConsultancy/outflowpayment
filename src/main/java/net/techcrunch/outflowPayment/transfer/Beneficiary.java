package net.techcrunch.outflowPayment.transfer;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Beneficiary {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(nullable = false)
    private Long id;

    private String firstName;
    private String lastName;
    private String accountNumber;
    private String bankName;
    private String code;

}
