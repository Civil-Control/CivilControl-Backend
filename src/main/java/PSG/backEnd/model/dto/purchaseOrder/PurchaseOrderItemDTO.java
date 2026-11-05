package PSG.backEnd.model.dto.purchaseOrder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PurchaseOrderItemDTO(
        @NotBlank @Size(max = 200) String name,
        @Positive BigDecimal quantity
) {}
