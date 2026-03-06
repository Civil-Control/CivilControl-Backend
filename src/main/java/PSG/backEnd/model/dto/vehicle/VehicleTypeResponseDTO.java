package PSG.backEnd.model.dto.vehicle;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response DTO containing information about a vehicle type.")
public record VehicleTypeResponseDTO(

        @Schema(description = "Unique identifier of the vehicle type.", example = "1")
        Long id,

        @Schema(description = "Vehicle type name.", example = "Truck")
        String name,

        @Schema(description = "Vehicle type description.", example = "Heavy cargo vehicle")
        String description,

        @Schema(description = "Indicates whether vehicles of this type require specifying truck equipment (truckEquipment).",
                example = "false")
        boolean requiresTruckEquipment
) {}
