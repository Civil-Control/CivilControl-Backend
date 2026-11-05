package PSG.backEnd.model.dto.transactionalDocument;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record LinkPurchaseOrderDTO(
        @NotNull
        @Positive
        Long purchaseOrderId
) {}
