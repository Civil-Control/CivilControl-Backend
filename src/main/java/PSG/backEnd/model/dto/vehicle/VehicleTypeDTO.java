package PSG.backEnd.model.dto.vehicle;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "DTO for creating or updating a vehicle type.")
public record VehicleTypeDTO(

        @Schema(description = "Vehicle type name. Maximum 60 characters.",
                example = "Truck",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{vehicleType.name.required}", groups = OnCreate.class)
        @Size(max = 60, message = "{vehicleType.name.size}", groups = {OnCreate.class, OnUpdate.class})
        String name,

        @Schema(description = "Optional description of the vehicle type. Maximum 100 characters.",
                example = "Heavy cargo vehicle",
                nullable = true)
        @Size(max = 100, message = "{vehicleType.description.size}", groups = {OnCreate.class, OnUpdate.class})
        String description,

        @Schema(description = "Indicates whether this vehicle type requires specifying truck equipment (truckEquipment). " +
                "If true, the truckEquipment field is mandatory when registering a vehicle of this type. " +
                "If false, the truckEquipment field must not be specified.",
                example = "false",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{vehicleType.requiresTruckEquipment.required}", groups = OnCreate.class)
        Boolean requiresTruckEquipment
) {}
