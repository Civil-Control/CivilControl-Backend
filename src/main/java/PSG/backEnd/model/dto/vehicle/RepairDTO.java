package PSG.backEnd.model.dto.vehicle;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Data Transfer Object for creating or updating a vehicle repair. " +
        "A repair can be performed either by an internal employee or an external supplier.")
public record RepairDTO(

        @Schema(description = "Date when the repair was performed. Must be today or in the past.",
                example = "2024-10-01",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{repair.date.required}", groups = OnCreate.class)
        @PastOrPresent(message = "{repair.date.pastOrPresent}", groups = {OnCreate.class, OnUpdate.class})
        LocalDate date,

        @Schema(description = "ID of the vehicle that received the repair.",
                example = "15",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{repair.vehicleId.required}", groups = OnCreate.class)
        @Positive(message = "{validation.positive}", groups = {OnCreate.class, OnUpdate.class})
        Long vehicleId,

        @Schema(description = "Total cost of the repair. Can be null if the repair was performed by an internal employee. " +
                "Must be greater than zero if provided.",
                example = "1500.50",
                nullable = true)
        @DecimalMin(value = "0.01", message = "{repair.cost.min}", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 10, fraction = 2, message = "{validation.digits}", groups = {OnCreate.class, OnUpdate.class})
        BigDecimal cost,

        @Schema(description = "Detailed description of the repair work performed. Maximum 1000 characters.",
                example = "Replaced brake pads and rotors on front wheels. Performed full brake system inspection.",
                maxLength = 1000,
                nullable = true)
        @Size(max = 1000, message = "{repair.description.size}", groups = {OnCreate.class, OnUpdate.class})
        String description,

        @Schema(description = "Name of the internal employee who performed the repair. " +
                "Use this field for internal repairs. Do not provide both employee and supplierId.",
                example = "John Smith",
                maxLength = 100,
                nullable = true)
        @Size(max = 100, message = "{repair.employee.size}", groups = {OnCreate.class, OnUpdate.class})
        String employee,

        @Schema(description = "ID of the external supplier who performed the repair. " +
                "Use this field for external repairs. Do not provide both supplierId and employee.",
                example = "8",
                nullable = true)
        @Positive(message = "{validation.positive}", groups = {OnCreate.class, OnUpdate.class})
        Long supplierId,

        @Schema(description = "Types of repair performed. At least one type is required on create.",
                example = "[\"ARRANQUE\", \"SISTEMA_ELECTRICO\"]",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "{repair.repairType.required}", groups = OnCreate.class)
        List<String> repairTypes
) {}
