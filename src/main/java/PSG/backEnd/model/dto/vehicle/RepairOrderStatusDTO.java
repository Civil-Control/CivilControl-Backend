package PSG.backEnd.model.dto.vehicle;

import PSG.backEnd.model.enums.vehicle.RepairOrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "DTO for updating the status of a repair order.")
public record RepairOrderStatusDTO(

        @Schema(description = "New status to apply to the repair order.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{repairOrder.status.required}")
        RepairOrderStatus status
) {}
