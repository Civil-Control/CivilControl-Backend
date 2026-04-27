package PSG.backEnd.model.dto.vehicle;

import PSG.backEnd.model.dto.transactionalDocument.TransactionalDocumentSummaryDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Response DTO for a repair item with resolved document info.")
public record RepairItemResponseDTO(

        @Schema(description = "Unique identifier of the repair item.")
        Long id,

        @Schema(description = "Type of item: MATERIAL or MANO_DE_OBRA.")
        String itemType,

        @Schema(description = "Description of the item.")
        String description,

        @Schema(description = "Amount for this item.", nullable = true)
        BigDecimal amount,

        @Schema(description = "IVA percentage applied to this item.", example = "21.00")
        BigDecimal ivaPercentage,

        @Schema(description = "IVA amount applied to this item. Only populated when the item is linked " +
                "to a transactional document (the IVA reflects what was actually invoiced); null otherwise.",
                nullable = true)
        BigDecimal ivaAmount,

        @Schema(description = "Linked transactional document summary, if any.", nullable = true)
        TransactionalDocumentSummaryDTO transactionalDocument,

        @Schema(description = "Sort order of this item within the repair.")
        Integer sortOrder
) {}
