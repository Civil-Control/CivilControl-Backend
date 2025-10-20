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
                example = "CAR",
                allowableValues = {"CAR", "TRUCK", "VAN", "MOTORCYCLE", "BUS"},
                nullable = true)
        String vehicleType,

        @Schema(description = "Filter by project area name. Partial matches are supported.",
                example = "Operations",
                nullable = true)
        String projectAreaName,

        @Schema(description = "Filter by storage location. Partial matches are supported.",
                example = "Warehouse",
                nullable = true)
        String storedIn,

        @Schema(description = "Filter by VTV expiration date. Exact match required.",
                example = "2025-12-31",
                nullable = true)
        LocalDate vtvExpirationDate,

        @Schema(description = "Filter by jurisdiction type.",
                example = "PROVINCIAL",
                allowableValues = {"PROVINCIAL", "MUNICIPAL", "NATIONAL"},
                nullable = true)
        String jurisdictionType
) {}