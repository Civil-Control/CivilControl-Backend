package PSG.backEnd.model.dto.purchaseOrder;

import PSG.backEnd.model.enums.PurchaseOrderCategory;
import PSG.backEnd.model.enums.PurchaseOrderPriority;
import PSG.backEnd.model.enums.PurchaseOrderStatus;

import java.time.LocalDate;

public record PurchaseOrderFilterDTO(
        LocalDate dateFrom,
        LocalDate dateTo,
        PurchaseOrderStatus status,
        PurchaseOrderCategory category,
        PurchaseOrderPriority priority,
        Long transactionalDocumentId,
        String search
) {}
