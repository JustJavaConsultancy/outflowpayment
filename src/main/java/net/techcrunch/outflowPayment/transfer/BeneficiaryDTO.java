package net.techcrunch.outflowPayment.transfer;

import lombok.*;
import java.io.Serializable;

/**
 * DTO for {@link Beneficiary}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class BeneficiaryDTO implements Serializable {
    String firstName;
    String lastName;
    String accountNumber;
    String bankName;
    String code;
}