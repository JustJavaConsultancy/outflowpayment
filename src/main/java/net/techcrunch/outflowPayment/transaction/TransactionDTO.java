package net.techcrunch.outflowPayment.transaction;

import com.fasterxml.jackson.annotation.JsonFormat;
import net.techcrunch.outflowPayment.invoice.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class TransactionDTO {

    private Long id;

    @NotNull
    @Size(max = 255)
    @TransactionReferenceUnique
    private String reference;

    @NotNull
    @Size(max = 255)
    @TransactionExternalReferenceUnique
    private String externalReference;

    @NotNull
    @Digits(integer = 10, fraction = 2)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", example = "92.08")
    private BigDecimal amount;

    @NotNull
    @Size(max = 255)
    private String beneficiaryAccount;

    @NotNull
    @Size(max = 255)
    @TransactionSourceAccountUnique
    private String sourceAccount;

    @NotNull
    private Status status;

    @NotNull
    private PaymentType paymentType;

    private String channel;
    private String transactionOwner;

//    recent changes
    @LastModifiedDate
    private OffsetDateTime lastUpdated;

    @CreatedDate
    private OffsetDateTime dateCreated;

//    private InvoiceDTO invoice;
    private Map<String, Object> transactionDetails;
}
