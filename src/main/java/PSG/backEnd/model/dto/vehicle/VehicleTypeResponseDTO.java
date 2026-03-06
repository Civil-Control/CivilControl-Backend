package PSG.backEnd.model.dto.vehicle;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response DTO con la información de un tipo de vehículo.")
public record VehicleTypeResponseDTO(

        @Schema(description = "Identificador único del tipo de vehículo.", example = "1")
        Long id,

        @Schema(description = "Nombre del tipo de vehículo.", example = "Camión")
        String name,

        @Schema(description = "Descripción del tipo de vehículo.", example = "Vehículo pesado de carga")
        String description,

        @Schema(description = "Indica si los vehículos de este tipo requieren especificar equipamiento de camión (truckEquipment).",
                example = "false")
        boolean requiresTruckEquipment
) {}

