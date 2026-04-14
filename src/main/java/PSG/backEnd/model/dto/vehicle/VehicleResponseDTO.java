package PSG.backEnd.model.dto.vehicle;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Response DTO containing complete information about a vehicle, " +
        "including related project area details.")
public record VehicleResponseDTO(

        @Schema(description = "Unique identifier of the vehicle.",
                example = "15")
        Long id,

        @Schema(description = "Vehicle license plate number.",
                example = "ABC 123")
        String licensePlate,

        @Schema(description = "Vehicle brand or manufacturer name.",
                example = "Toyota")
        String brand,

        @Schema(description = "Vehicle model name.",
                example = "Corolla")
        String model,

        @Schema(description = "Manufacturing year of the vehicle.",
                example = "2020")
        Integer year,

        @Schema(description = "Vehicle color.",
                example = "Blue")
        String color,

        @Schema(description = "Friendly nickname for the vehicle.",
                example = "The Blue Runner")
        String nickName,

        @Schema(description = "Whether the vehicle has been soft-deleted (record preservation only — not for operational status).",
                example = "false")
        boolean deleted,

        @Schema(description = "Whether the vehicle is currently in service. False means temporarily inactive (not the same as soft-deleted).",
                example = "true")
        boolean active,

        @Schema(description = "ID del tipo de vehículo.",
                example = "1")
        Long vehicleTypeId,

        @Schema(description = "Nombre del tipo de vehículo.",
                example = "Camión")
        String vehicleTypeName,

        @Schema(description = "Name of the project area to which this vehicle is assigned.",
                example = "Operations Department")
        String projectAreaName,

        @Schema(description = "Color of the project area to which this vehicle is assigned.",
                example = "#3b82f6",
                nullable = true)
        String projectAreaColor,

        @Schema(description = "ID of the project area to which this vehicle is assigned.",
                example = "5",
                nullable = true)
        Long projectAreaId,

        @Schema(description = "ID of the project area task (sub-task).", nullable = true)
        Long projectAreaTaskId,

        @Schema(description = "Name of the project area task (sub-task).", nullable = true)
        String projectAreaTaskName,

        @Schema(description = "ID of the building where the vehicle is stored.",
                example = "5",
                nullable = true)
        Long buildingId,

        @Schema(description = "Name of the building where the vehicle is stored.",
                example = "Depósito Central")
        String buildingName,

        @Schema(description = "VTV (Technical Vehicle Verification) expiration date.",
                example = "2025-12-31")
        LocalDate vtvExpirationDate,

        @Schema(description = "Type of jurisdiction where the vehicle is registered.",
                example = "PROVINCIAL",
                allowableValues = {"PROVINCIAL", "MUNICIPAL", "NATIONAL"})
        String jurisdictionType,

        @Schema(description = "Type of truck equipment. Only present for vehicles of type CAMION.",
                example = "HIDROELEVADOR",
                allowableValues = {"NADA", "HIDROELEVADOR", "HIDROGRUA"})
        String truckEquipment,

        @Schema(description = "Current mileage of the vehicle (km). Synced from the latest crew assignment.",
                example = "45000",
                nullable = true)
        Integer km
) {}
