package PSG.backEnd.model.dto.vehicle;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "DTO para crear o actualizar un tipo de vehículo.")
public record VehicleTypeDTO(

        @Schema(description = "Nombre del tipo de vehículo. Máximo 60 caracteres.",
                example = "Camión",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{vehicleType.name.required}")
        @Size(max = 60, message = "{vehicleType.name.size}")
        String name,

        @Schema(description = "Descripción opcional del tipo de vehículo. Máximo 100 caracteres.",
                example = "Vehículo pesado de carga",
                nullable = true)
        @Size(max = 100, message = "{vehicleType.description.size}")
        String description,

        @Schema(description = "Indica si este tipo de vehículo requiere especificar equipamiento de camión (truckEquipment). " +
                "Si es true, el campo truckEquipment será obligatorio al registrar un vehículo de este tipo. " +
                "Si es false, el campo truckEquipment no debe especificarse.",
                example = "false",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{vehicleType.requiresTruckEquipment.required}")
        Boolean requiresTruckEquipment
) {}

