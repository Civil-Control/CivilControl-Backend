package PSG.backEnd.model.dto.reference;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Extended reference item for employees, includes the employee's assigned
 * project area so that forms can auto-fill the sector/área field when an
 * employee is selected (e.g. salary payment form).
 */
@Schema(description = "Employee reference item with optional project area for form auto-fill")
public record EmployeeReferenceItem(
        @Schema(description = "Unique identifier", example = "5")
        Long id,
        @Schema(description = "Human-readable label for display", example = "García, Juan")
        String label,
        @Schema(description = "ID of the employee's assigned project area, null if not set", example = "3")
        Long projectAreaId
) {}
