package PSG.backEnd.model.dto.reference;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Extended reference item for vehicles, includes the vehicle's assigned
 * project area so that forms can auto-fill the sector/área field when a
 * vehicle is selected (e.g. fuel-load form).
 */
@Schema(description = "Vehicle reference item with optional project area for form auto-fill")
public record VehicleReferenceItem(
        @Schema(description = "Unique identifier", example = "7")
        Long id,
        @Schema(description = "Human-readable label for display", example = "ABC-123 · Toyota Corolla")
        String label,
        @Schema(description = "ID of the vehicle's assigned project area, null if not set", example = "2")
        Long projectAreaId
) {}
