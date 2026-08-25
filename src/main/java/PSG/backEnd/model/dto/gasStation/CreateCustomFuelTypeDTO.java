package PSG.backEnd.model.dto.gasStation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "DTO for creating a tenant-defined custom fuel type.")
public record CreateCustomFuelTypeDTO(

        @Schema(description = "Display label for the new fuel type.",
                example = "Aceite para motos",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{gasStation.customFuelType.label.required}")
        @Size(max = 100, message = "{gasStation.customFuelType.label.required}")
        String label
) {}
