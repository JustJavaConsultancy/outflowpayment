package net.techcrunch.outflowPayment.product;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO implements Serializable {
    private Long id;

    /* ================= Identification ================= */

    @Size(max = 50)
    private String code;

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 300)
    private String description;

    /* ================= Pricing ================= */
    @NotNull
    @Digits(integer = 13, fraction = 2)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", example = "7500.00")
    private BigDecimal amount;

    /* ================= Inventory ================= */

    private Integer quantityInStock;
    private boolean unlimited = false;

    private Boolean containsPhysicalGoods = true;

    @Min(0)
    private Integer quantitySold = 0;

    /* ================= Subscription ================= */
    private Boolean subscribe = false;

    /* ================= Ownership ================= */

    /**
     * Null for government products
     */
    private String merchantId;

    @NotNull
    private Long accountId;

    /* ================= Government / IGR ================= */

    /**
     * Mandatory for government services
     */
    private String revenueCode;

    private String category = "";

    /* ================= Media ================= */

    private List<String> media = new ArrayList<>();

    /* ================= Audit ================= */

    private OffsetDateTime dateCreated;

    /* ================= Convenience ================= */

    public String getPrimaryMedia() {
        return media == null || media.isEmpty() ? "" : media.get(0);
    }

    public BigDecimal getRevenue(){
        return this.amount.multiply( new BigDecimal(this.quantitySold));
    }
}
