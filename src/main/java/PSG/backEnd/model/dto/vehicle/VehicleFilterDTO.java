package PSG.backEnd.model.dto.vehicle;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Filter criteria for querying vehicles. All fields are optional and can be combined.")
public record VehicleFilterDTO(

        @Schema(description = "Filter by license plate. Partial matches are supported.",
                example = "ABC 123",
                nullable = true)
        String licensePlate,

        @Schema(description = "Filter by vehicle brand. Partial matches are supported.",
                example = "Toyota",
                nullable = true)
        String brand,

        @Schema(description = "Filter by vehicle model. Partial matches are supported.",
                example = "Corolla",
                nullable = true)
        String model,

        @Schema(description = "Filter by manufacturing year.",
                example = "2020",
                nullable = true)
        Integer year,

        @Schema(description = "Filter by vehicle color. Partial matches are supported.",
                example = "Blue",
                nullable = true)
        String color,

        @Schema(description = "Filter by vehicle nickname. Partial matches are supported.",
                example = "Blue Runner",
                nullable = true)
        String nickName,

        @Schema(description = "Filter by vehicle type.",
                example = "CAMION",
                allowableValues = {"CAMION", "CAMIONETA", "AUTO", "MOTO", "OTRO"},
                nullable = true)
        String vehicleType,

        @Schema(description = "Filter by project area name. Partial matches are supported.",
                example = "Operations",
                nullable = true)
        String projectAreaName,

        @Schema(description = "Filter by building name where the vehicle is stored. Partial matches are supported.",
                example = "Depósito",
                nullable = true)
        String buildingName,

        @Schema(description = "Filter by VTV expiration date. Exact match required.",
                example = "2025-12-31",
                nullable = true)
        LocalDate vtvExpirationDate,

        @Schema(description = "Filter by jurisdiction type.",
                example = "PROVINCIAL",
                allowableValues = {"PROVINCIAL", "MUNICIPAL", "NATIONAL"},
                nullable = true)
        String jurisdictionType,

        @Schema(description = "Filter by truck equipment type. Only applicable for vehicles of type CAMION.",
                example = "HIDROELEVADOR",
                allowableValues = {"NADA", "HIDROELEVADOR", "HIDROGRUA"},
                nullable = true)
        String truckEquipment,

        @Schema(description = "When true, includes deactivated (soft-deleted) vehicles in results. Defaults to false.",
                example = "false",
                nullable = true)
        Boolean includeInactive,

        @Schema(description = "Generic search across license plate, brand, model and nickname (case-insensitive partial match).",
                example = "Toyota",
                nullable = true)
        String search
) {}