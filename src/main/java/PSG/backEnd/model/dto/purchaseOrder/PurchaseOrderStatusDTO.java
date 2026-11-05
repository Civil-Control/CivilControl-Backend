package PSG.backEnd.model.dto.purchaseOrder;

import PSG.backEnd.model.enums.PurchaseOrderStatus;
import jakarta.validation.constraints.NotNull;

public record PurchaseOrderStatusDTO(
        @NotNull(message = "{purchaseOrder.status.required}")
        PurchaseOrderStatus status
) {}
