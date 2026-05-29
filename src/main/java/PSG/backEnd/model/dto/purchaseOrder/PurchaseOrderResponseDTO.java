package PSG.backEnd.model.dto.purchaseOrder;

import PSG.backEnd.model.dto.transactionalDocument.TransactionalDocumentSummaryDTO;
import PSG.backEnd.model.enums.PurchaseOrderCategory;
import PSG.backEnd.model.enums.PurchaseOrderPriority;
import PSG.backEnd.model.enums.PurchaseOrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Response DTO containing complete information about a purchase order.")
public record PurchaseOrderResponseDTO(
        Long id,
        Long orderNumber,
        LocalDate date,
        PurchaseOrderCategory category,
        String description,
        List<PurchaseOrderItemDTO> items,
        String requestedBy,
        BigDecimal estimatedAmount,
        PurchaseOrderPriority priority,
        PurchaseOrderStatus status,
        TransactionalDocumentSummaryDTO transactionalDocument,
        String createdByUserName
) {}
