package PSG.backEnd.model.dto.vehicle;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "DTO for completing a repair order. " +
        "Closing the order creates a corresponding Repair record automatically.")
public record RepairOrderCompleteDTO(

        @Schema(description = "Types of repair performed. At least one type is required.",
                example = "[\"ARRANQUE\", \"SISTEMA_ELECTRICO\"]",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "{repair.repairType.required}")
        List<String> repairTypes,

        @Schema(description = "Name of the internal employee who performed the repair. " +
                "Omit if the repair was performed by an external supplier.",
                example = "Juan Lopez",
                maxLength = 100,
                nullable = true)
        @Size(max = 100, message = "{repair.employee.size}")
        String employee,

        @Schema(description = "ID of the external supplier who performed the repair. " +
                "Omit if performed by an internal employee.",
                example = "8",
                nullable = true)
        @Positive(message = "{validation.positive}")
        Long supplierId,

        @Schema(description = "Total cost of the repair. Required if an external supplier is specified.",
                example = "1500.50",
                nullable = true)
        @DecimalMin(value = "0.01", message = "{repair.cost.min}")
        @Digits(integer = 10, fraction = 2, message = "{validation.digits}")
        BigDecimal cost,

        @Schema(description = "Additional notes or description of the work performed. Maximum 1000 characters.",
                example = "Se reemplazo la bomba de combustible y se limpiaron los inyectores.",
                maxLength = 1000,
                nullable = true)
        @Size(max = 1000, message = "{repair.description.size}")
        String description
) {}
