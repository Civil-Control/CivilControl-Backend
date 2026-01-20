package PSG.backEnd.model.dto.vehicle;

import PSG.backEnd.model.enums.vehicle.RepairType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Data Transfer Object for creating or updating a vehicle repair. " +
        "A repair can be performed either by an internal employee or an external supplier.")
public record RepairDTO(

        @Schema(description = "Date when the repair was performed. Must be today or in the past.",
                example = "2024-10-01",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Date is required.", groups = OnCreate.class)
        @PastOrPresent(message = "Date must be today or in the past.", groups = {OnCreate.class, OnUpdate.class})
        LocalDate date,

        @Schema(description = "ID of the vehicle that received the repair.",
                example = "15",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Vehicle ID is required.", groups = OnCreate.class)
        @Positive(message = "Vehicle ID must be a positive number.", groups = {OnCreate.class, OnUpdate.class})
        Long vehicleId,

        @Schema(description = "Total cost of the repair. Can be null if the repair was performed by an internal employee. " +
                "Must be greater than zero if provided.",
                example = "1500.50",
                nullable = true)
        @DecimalMin(value = "0.01", message = "Cost must be greater than zero.", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 10, fraction = 2, message = "Cost must have at most 10 integer digits and 2 decimal places.", groups = {OnCreate.class, OnUpdate.class})
        BigDecimal cost,

        @Schema(description = "Detailed description of the repair work performed. Maximum 1000 characters.",
                example = "Replaced brake pads and rotors on front wheels. Performed full brake system inspection.",
                maxLength = 1000,
                nullable = true)
        @Size(max = 1000, message = "Description must be at most 1000 characters.", groups = {OnCreate.class, OnUpdate.class})
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

        @Schema(description = "Type of repair performed. Valid values: PREVENTIVE (scheduled maintenance), " +
                "CORRECTIVE (fixing a problem), PREDICTIVE (based on diagnostics).",
                example = "CORRECTIVE",
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"PREVENTIVE", "CORRECTIVE", "PREDICTIVE"})
        @NotNull(message = "{repair.repairType.required}", groups = OnCreate.class)
        RepairType repairType
) {}
