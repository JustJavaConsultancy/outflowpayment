package net.techcrunch.outflowPayment.accounting;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO for {@link Account}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class AccountDTO implements Serializable {
    Long id;
    String name;
    String code;
    String type;
    String currency;
    BigDecimal balance;
    private String ownerId;
    private String accNumber;
}