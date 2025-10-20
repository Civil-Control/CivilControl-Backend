package PSG.backEnd.model.dto.gasStation;

import PSG.backEnd.model.enums.vehicle.FuelType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

@Schema(description = "Data Transfer Object for creating or updating a fuel load transaction. " +
        "Represents a fuel purchase made at a gas station for a specific vehicle.")
public record FuelLoadDTO(

        @Schema(description = "Date when the fuel load was made. Must be today or in the past.",
                example = "2024-10-15",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Date is required.", groups = OnCreate.class)
        @PastOrPresent(message = "Date must be today or in the past.", groups = {OnCreate.class, OnUpdate.class})
        LocalDate date,

        @Schema(description = "Gas station branch code. Must be exactly 5 digits.",
                example = "12345",
                requiredMode = Schema.RequiredMode.REQUIRED,
                pattern = "\\d{5}")
        @NotBlank(message = "Branch code is required", groups = OnCreate.class)
        @Pattern(regexp = "\\d{5}", message = "Branch code must be exactly 5 digits", groups = {OnCreate.class, OnUpdate.class})
        String branchCode,

        @Schema(description = "Fuel purchase ticket number. Must be exactly 8 digits.",
                example = "87654321",
                requiredMode = Schema.RequiredMode.REQUIRED,
                pattern = "\\d{8}")
        @NotBlank(message = "Ticket number is required", groups = OnCreate.class)
        @Pattern(regexp = "\\d{8}", message = "Ticket number must be exactly 8 digits", groups = {OnCreate.class, OnUpdate.class})
        String ticketNumber,

        @Schema(description = "Type of fuel loaded. Valid values: NAFTA_SUPER (premium gasoline), NAFTA_COMUN (regular gasoline), " +
                "DIESEL (diesel fuel), GNC (compressed natural gas).",
                example = "NAFTA_SUPER",
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"NAFTA_SUPER", "NAFTA_COMUN", "DIESEL", "GNC"})
        @NotNull(message = "Fuel type is required.", groups = OnCreate.class)
        FuelType fuelType,

        @Schema(description = "Amount of fuel loaded in liters. Must be between 0.01 and 5000.",
                example = "45.50",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minimum = "0.01",
                maximum = "5000")
        @NotNull(message = "Liters is required.", groups = OnCreate.class)
        @DecimalMin(value = "0.01", message = "Liters must be greater than 0.", groups = {OnCreate.class, OnUpdate.class})
        @DecimalMax(value = "5000", message = "Liters must be less than or equal to 5000.", groups = {OnCreate.class, OnUpdate.class})
        Double liters,

        @Schema(description = "ID of the vehicle that received the fuel load.",
                example = "25",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Vehicle id is required.", groups = OnCreate.class)
        @Positive(message = "Vehicle id must be a positive number.", groups = {OnCreate.class, OnUpdate.class})
        Long vehicleId,

        @Schema(description = "ID of the project area to which this fuel load is assigned. Optional field.",
                example = "10",
                nullable = true)
        @Positive(message = "Project area id must be a positive number.", groups = {OnCreate.class, OnUpdate.class})
        Long projectAreaId,

        @Schema(description = "ID of the gas station where the fuel load was made.",
                example = "5",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Gas station id is required.", groups = OnCreate.class)
        @Positive(message = "Gas station id must be a positive number.", groups = {OnCreate.class, OnUpdate.class})
        Long gasStationId

) {}
