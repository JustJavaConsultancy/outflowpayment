package net.techcrunch.outflowPayment.merchant;

import net.techcrunch.outflowPayment.invoice.InvoiceDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * DTO for {@link Order}
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class OrderDTO implements Serializable {
    Long id;
    String merchantId;
    InvoiceDTO invoice;
    OffsetDateTime dateCreated;
    OffsetDateTime lastUpdated;
}