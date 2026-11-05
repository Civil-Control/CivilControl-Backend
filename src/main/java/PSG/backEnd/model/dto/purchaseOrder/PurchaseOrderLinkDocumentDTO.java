package PSG.backEnd.model.dto.purchaseOrder;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PurchaseOrderLinkDocumentDTO(
        @NotNull
        @Positive
        Long transactionalDocumentId
) {}
