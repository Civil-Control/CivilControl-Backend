package PSG.backEnd.model.dto.reference;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Extended reference item for service assignments. Includes the subject's
 * project area so that the payment form can auto-fill the sector field.
 */
@Schema(description = "Service assignment reference item with project area for form auto-fill")
public record ServiceAssignmentReferenceItem(
        @Schema(description = "Unique identifier", example = "5")
        Long id,
        @Schema(description = "Human-readable label for display", example = "Proveedor X · LUZ · Edificio Central")
        String label,
        @Schema(description = "ID of the subject's project area (building or vehicle), null if not set", example = "2")
        Long projectAreaId
) {}
