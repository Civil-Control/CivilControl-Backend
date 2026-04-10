package PSG.backEnd.model.dto.vehicle;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

@Schema(description = "DTO for completing a repair order. " +
        "Closing the order creates a corresponding Repair record automatically.")
public record RepairOrderCompleteDTO(

        @Schema(description = "ID of the external supplier who performed the repair.",
                nullable = true)
        @Positive(message = "{validation.positive}")
        Long supplierId,

        @Schema(description = "Additional notes or description of the work performed. Maximum 1000 characters.",
                nullable = true)
        @Size(max = 1000, message = "{repair.description.size}")
        String description,

        @Schema(description = "Vehicle mileage (km) at the time of repair.",
                nullable = true)
        @Min(value = 0, message = "{repair.mileage.min}")
        Integer mileage,

        @Schema(description = "Repair items (materials and labor). At least one is required.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "{repair.items.required}")
        @Valid
        List<RepairItemDTO> items
) {}
