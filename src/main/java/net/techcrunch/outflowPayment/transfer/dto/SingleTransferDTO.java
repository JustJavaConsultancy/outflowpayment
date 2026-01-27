package net.techcrunch.outflowPayment.transfer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class SingleTransferDTO implements Serializable {
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

//    extra fields
    @NotNull(message = "Status is required")
    private String status;

    @NotNull(message = "Merchant Name is required")
    private String merchantName;

    @NotNull(message = "Merchant Id is required")
    private String merchantId;

    @NotNull
    @Positive(message = "Transfer fee must be positive")
    @DecimalMin(value = "0.01", inclusive = true)
    @Digits(integer = 10, fraction = 2, message = "Transfer must have format 1000.00")
    private BigDecimal transferFee;

    @NotNull
    @Positive(message = "Balance must be positive")
    @DecimalMin(value = "0.00", inclusive = true, message = "Minimum Balance is ₦0.00")
    @Digits(integer = 10, fraction = 2, message = "Balance must have format 100.00")
    private BigDecimal balance;

    public static SingleTransferDTO fromSingleTransferRequest(
            @NonNull SingleTransferRequestDTO requestDTO,
            @NonNull String merchantId,
            @NonNull String merchantName,
            @NonNull BigDecimal transferFee,
            @NonNull BigDecimal balance,
            @NonNull String status
            ){

        return SingleTransferDTO.builder()
                .beneficiaryName(requestDTO.getBeneficiaryName())
                .bankName(requestDTO.getBankName())
                .accNumber(requestDTO.getAccNumber())
                .amountToSend(requestDTO.getAmountToSend())
                .duration(requestDTO.getDuration())
                .narration(requestDTO.getNarration())
                .confirmCode(requestDTO.getConfirmCode())
                .status(status)
                .merchantName(merchantName)
                .build();
    }
}
