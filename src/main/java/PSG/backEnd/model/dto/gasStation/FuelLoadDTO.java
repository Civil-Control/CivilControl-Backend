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
        @NotNull(message = "{fuelLoad.date.required}", groups = OnCreate.class)
        @PastOrPresent(message = "{fuelLoad.date.pastOrPresent}", groups = {OnCreate.class, OnUpdate.class})
        LocalDate date,

        @Schema(description = "Gas station branch code. Must be exactly 5 digits.",
                example = "12345",
                requiredMode = Schema.RequiredMode.REQUIRED,
                pattern = "\\d{5}")
        @NotBlank(message = "{fuelLoad.branchCode.required}", groups = OnCreate.class)
        @Pattern(regexp = "\\d{5}", message = "{fuelLoad.branchCode.size}", groups = {OnCreate.class, OnUpdate.class})
        String branchCode,

        @Schema(description = "Fuel purchase ticket number. Must be exactly 8 digits.",
                example = "87654321",
                requiredMode = Schema.RequiredMode.REQUIRED,
                pattern = "\\d{8}")
        @NotBlank(message = "{fuelLoad.ticketNumber.required}", groups = OnCreate.class)
        @Pattern(regexp = "\\d{8}", message = "{fuelLoad.ticketNumber.size}", groups = {OnCreate.class, OnUpdate.class})
        String ticketNumber,

        @Schema(description = "Type of fuel loaded. Valid values: NAFTA_SUPER (premium gasoline), NAFTA_COMUN (regular gasoline), " +
                "DIESEL (diesel fuel), GNC (compressed natural gas).",
                example = "NAFTA_SUPER",
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"NAFTA_SUPER", "NAFTA_COMUN", "DIESEL", "GNC"})
        @NotNull(message = "{fuelLoad.fuelType.required}", groups = OnCreate.class)
        FuelType fuelType,

        @Schema(description = "Amount of fuel loaded in liters. Must be between 0.01 and 5000.",
                example = "45.50",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minimum = "0.01",
                maximum = "5000")
        @NotNull(message = "{fuelLoad.liters.required}", groups = OnCreate.class)
        @DecimalMin(value = "0.01", message = "{fuelLoad.liters.positive}", groups = {OnCreate.class, OnUpdate.class})
        @DecimalMax(value = "5000", message = "{validation.max}", groups = {OnCreate.class, OnUpdate.class})
        Double liters,

        @Schema(description = "ID of the vehicle that received the fuel load.",
                example = "25",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{fuelLoad.vehicleId.required}", groups = OnCreate.class)
        @Positive(message = "{validation.positive}", groups = {OnCreate.class, OnUpdate.class})
        Long vehicleId,

        @Schema(description = "ID of the project area to which this fuel load is assigned. Optional field.",
                example = "10",
                nullable = true)
        @Positive(message = "{validation.positive}", groups = {OnCreate.class, OnUpdate.class})
        Long projectAreaId,

        @Schema(description = "ID of the gas station where the fuel load was made.",
                example = "5",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{gasStation.required}", groups = OnCreate.class)
        @Positive(message = "{validation.positive}", groups = {OnCreate.class, OnUpdate.class})
        Long gasStationId

) {}
