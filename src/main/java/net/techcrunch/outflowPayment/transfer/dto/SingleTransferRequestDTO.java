package net.techcrunch.outflowPayment.transfer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class SingleTransferRequestDTO implements Serializable {

    @NotBlank(message = "Beneficiary Name is required")
    @Size(min = 1, max = 50, message = "Beneficiary Name must be between 1 and 50 characters")
    private String beneficiaryName;

    @NotBlank(message = "Bank Name is required")
    @Size(min = 1, max = 50, message = "Bank Name must be between 1 and 50 characters")
    private String bankName;

    @NotBlank(message = "Account Name is required")
    @Pattern(regexp = "^\\d{10}$", message = "Account number must be exactly 10 digits")
    private String accNumber;

    @NotNull
    @Positive(message = "Amount must be positive")
    @DecimalMin(value = "100.00", inclusive = true, message = "Minimum amount is ₦100.00")
    @Digits(integer = 10, fraction = 2, message = "Amount must have format 1000.00")
    @JsonProperty("amountToSend")
    private BigDecimal amountToSend;

    private String duration = "";

    @NotBlank(message = "Narration is required")
    private String narration;

    @NotBlank(message = "Confirm Code is required")
    private String confirmCode;
}
