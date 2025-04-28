package net.techcrunch.outflowPayment.merchant;

import lombok.*;

import java.io.Serializable;

/**
 * DTO for {@link Merchant}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class MerchantDto implements Serializable {
    Long id;
    String businessIdentity;
    String businessName;
    String businessAddress;
    String mode;
}