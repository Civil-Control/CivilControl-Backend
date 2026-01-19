package PSG.backEnd.model.dto.vehicle;

import PSG.backEnd.model.enums.vehicle.JurisdictionType;
import PSG.backEnd.model.enums.vehicle.VehicleType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.model.validation.ValidTruckEquipment;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

@Schema(description = "Data Transfer Object for creating or updating a vehicle. " +
        "Represents the complete information of a vehicle including identification, specifications, and administrative details.")
@ValidTruckEquipment(groups = {OnCreate.class, OnUpdate.class})
public record VehicleDTO(

        @Schema(description = "Vehicle license plate number. Must follow specific format patterns: 'AA 123 BB' (old format) or 'AAA 123' (new format). " +
                "Use uppercase letters (A-Z) and digits only.",
                example = "ABC 123",
                requiredMode = Schema.RequiredMode.REQUIRED,
                pattern = "^([A-Z]{2}\\s?\\d{3}\\s?[A-Z]{2}|[A-Z]{3}\\s?\\d{3})$")
        @NotBlank(message = "License plate is required.", groups = OnCreate.class)
        @Size(min = 6, max = 10, message = "License plate length must be between 6 and 10 characters.", groups = {OnCreate.class, OnUpdate.class})
        @Pattern(
                regexp = "^([A-Z]{2}\\s?\\d{3}\\s?[A-Z]{2}|[A-Z]{3}\\s?\\d{3})$",
                message = "License plate format must be either 'AA 123 BB' or 'AAA 123'. Use uppercase letters (A–Z) and digits only."
                , groups = {OnCreate.class, OnUpdate.class})
        String licensePlate,

        @Schema(description = "Vehicle brand or manufacturer name. Maximum 60 characters.",
                example = "Toyota",
                maxLength = 60,
                nullable = true)
        @Size(max = 60, message = "Brand must be at most 60 characters.", groups = {OnCreate.class, OnUpdate.class})
        String brand,

        @Schema(description = "Vehicle model name. Maximum 60 characters.",
                example = "Corolla",
                maxLength = 60,
                nullable = true)
        @Size(max = 60, message = "Model must be at most 60 characters.", groups = {OnCreate.class, OnUpdate.class})
        String model,

        @Schema(description = "Manufacturing year of the vehicle. Valid range: 1950-2100.",
                example = "2020",
                minimum = "1950",
                maximum = "2100",
                nullable = true)
        @Min(value = 1950, message = "Year must be greater than or equal to 1950.", groups = {OnCreate.class, OnUpdate.class})
        @Max(value = 2100, message = "Year must be less than or equal to 2100.", groups = {OnCreate.class, OnUpdate.class})
        Integer year,

        @Schema(description = "Vehicle color. Maximum 40 characters.",
                example = "Blue",
                maxLength = 40,
                nullable = true)
        @Size(max = 40, message = "Color must be at most 40 characters.", groups = {OnCreate.class, OnUpdate.class})
        String color,

        @Schema(description = "Friendly nickname for the vehicle. Maximum 40 characters.",
                example = "The Blue Runner",
                maxLength = 40,
                nullable = true)
        @Size(max = 40, message = "Nickname must be at most 40 characters.", groups = {OnCreate.class, OnUpdate.class})
        String nickName,

        @Schema(description = "Type of vehicle. Valid values: CAMION (truck with cargo capacity), CAMIONETA (pickup truck), " +
                "AUTO (passenger car), MOTO (motorcycle), OTRO (other types).",
                example = "AUTO",
                allowableValues = {"CAMION", "CAMIONETA", "AUTO", "MOTO", "OTRO"},
                nullable = true)
        VehicleType vehicleType,

        @Schema(description = "ID of the project area to which this vehicle is assigned.",
                example = "5",
                nullable = true)
        Long projectAreaId,

        @Schema(description = "Physical location where the vehicle is stored or parked. Maximum 100 characters.",
                example = "Main Warehouse - Bay 3",
                maxLength = 100,
                nullable = true)
        @Size(max = 100, message = "Storage location must be at most 100 characters.", groups = {OnCreate.class, OnUpdate.class})
        String storedIn,

        @Schema(description = "VTV (Technical Vehicle Verification) expiration date. Must be today or in the future. " +
                "VTV is the mandatory periodic technical inspection required for vehicles.",
                example = "2025-12-31",
                nullable = true)
        @FutureOrPresent(message = "VTV expiration date must be today or in the future.", groups = {OnCreate.class, OnUpdate.class})
        LocalDate vtvExpirationDate,

        @Schema(description = "Type of jurisdiction where the vehicle is registered. Valid values: PROVINCIAL (provincial registration), " +
                "MUNICIPAL (municipal registration), NATIONAL (national registration).",
                example = "PROVINCIAL",
                allowableValues = {"PROVINCIAL", "MUNICIPAL", "NATIONAL"},
                nullable = true)
        JurisdictionType jurisdictionType,

        @Schema(description = "Type of truck equipment. ONLY for vehicles of type CAMION (required for trucks, must be null for other types). " +
                "Valid values: NADA (no special equipment), HIDROELEVADOR (hydraulic lift platform), HIDROGRUA (hydraulic crane). " +
                "This field is REQUIRED when vehicleType is CAMION and must NOT be specified for other vehicle types (AUTO, CAMIONETA, MOTO, OTRO).",
                example = "NADA",
                allowableValues = {"NADA", "HIDROELEVADOR", "HIDROGRUA"},
                nullable = true)
        String truckEquipment
) {}