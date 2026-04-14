package PSG.backEnd.model.dto.vehicle;

import PSG.backEnd.model.enums.vehicle.JurisdictionType;
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

        @Schema(description = "Vehicle license plate number. Supported formats: 'AB 123 CD' (new), 'AAA 123' (old), '101AB123CD' or '101AAA123' (trailer), 'A001ABC' (moto). " +
                "Use uppercase letters (A-Z) and digits only.",
                example = "ABC 123",
                requiredMode = Schema.RequiredMode.REQUIRED,
                pattern = "^([A-Z]{2}\\s?\\d{3}\\s?[A-Z]{2}|[A-Z]{3}\\s?\\d{3}|101[A-Z]{2}\\d{3}[A-Z]{2}|101[A-Z]{3}\\d{3}|[A-Z]\\d{3}[A-Z]{3})$")
        @NotBlank(message = "{vehicle.licensePlate.required}", groups = OnCreate.class)
        @Size(min = 6, max = 10, message = "{validation.size}", groups = {OnCreate.class, OnUpdate.class})
        @Pattern(
                regexp = "^([A-Z]{2}\\s?\\d{3}\\s?[A-Z]{2}|[A-Z]{3}\\s?\\d{3}|101[A-Z]{2}\\d{3}[A-Z]{2}|101[A-Z]{3}\\d{3}|[A-Z]\\d{3}[A-Z]{3})$",
                message = "{validation.pattern}"
                , groups = {OnCreate.class, OnUpdate.class})
        String licensePlate,

        @Schema(description = "Vehicle brand or manufacturer name. Maximum 60 characters.",
                example = "Toyota",
                maxLength = 60,
                nullable = true)
        @Size(max = 60, message = "{vehicle.brand.size}", groups = {OnCreate.class, OnUpdate.class})
        String brand,

        @Schema(description = "Vehicle model name. Maximum 60 characters.",
                example = "Corolla",
                maxLength = 60,
                nullable = true)
        @Size(max = 60, message = "{vehicle.model.size}", groups = {OnCreate.class, OnUpdate.class})
        String model,

        @Schema(description = "Manufacturing year of the vehicle. Valid range: 1950-2100.",
                example = "2020",
                minimum = "1950",
                maximum = "2100",
                nullable = true)
        @Min(value = 1950, message = "{vehicle.year.min}", groups = {OnCreate.class, OnUpdate.class})
        @Max(value = 2100, message = "{vehicle.year.max}", groups = {OnCreate.class, OnUpdate.class})
        Integer year,

        @Schema(description = "Vehicle color. Maximum 40 characters.",
                example = "Blue",
                maxLength = 40,
                nullable = true)
        @Size(max = 40, message = "{vehicle.color.size}", groups = {OnCreate.class, OnUpdate.class})
        String color,

        @Schema(description = "Friendly nickname for the vehicle. Maximum 40 characters.",
                example = "The Blue Runner",
                maxLength = 40,
                nullable = true)
        @Size(max = 40, message = "{vehicle.nickName.size}", groups = {OnCreate.class, OnUpdate.class})
        String nickName,

        @Schema(description = "ID del tipo de vehículo.",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{vehicle.vehicleType.required}", groups = OnCreate.class)
        Long vehicleTypeId,

        @Schema(description = "ID of the project area to which this vehicle is assigned.",
                example = "5",
                nullable = true)
        Long projectAreaId,

        @Schema(description = "ID of the project area task (sub-task). Optional.",
                nullable = true)
        Long projectAreaTaskId,

        @Schema(description = "ID of the building where the vehicle is stored.",
                example = "5",
                nullable = true)
        Long buildingId,

        @Schema(description = "VTV (Technical Vehicle Verification) expiration date. " +
                "VTV is the mandatory periodic technical inspection required for vehicles.",
                example = "2025-12-31",
                nullable = true)
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