package net.techcrunch.outflowPayment.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDTO {
    private String cardNumber;
    private String cardHolderName;
    private String cardExpirationDate;
    private String cardCvv;
    private BigDecimal amount;
    private String currency;
    private String channel;
    private Long invoiceId;
    private String payerPhoneNumber;
    private String payerEmail;



}
