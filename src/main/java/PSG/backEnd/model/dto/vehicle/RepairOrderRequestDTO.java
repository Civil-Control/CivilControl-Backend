package PSG.backEnd.model.dto.vehicle;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Data Transfer Object for creating or updating a repair order. " +
        "A repair order is submitted by a field operator to report a vehicle failure.")
public record RepairOrderRequestDTO(

        @Schema(description = "Date when the failure was reported. Must be today or in the past.",
                example = "2024-10-01",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{repairOrder.date.required}", groups = OnCreate.class)
        @PastOrPresent(message = "{repairOrder.date.pastOrPresent}", groups = {OnCreate.class, OnUpdate.class})
        LocalDate date,

        @Schema(description = "ID of the vehicle that has the failure.",
                example = "15",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{repairOrder.vehicleId.required}", groups = OnCreate.class)
        @Positive(message = "{validation.positive}", groups = {OnCreate.class, OnUpdate.class})
        Long vehicleId,

        @Schema(description = "Description of the failure or issue observed. Maximum 1000 characters.",
                example = "El motor hace un ruido extrano al arrancar y pierde potencia en pendiente.",
                maxLength = 1000,
                nullable = true)
        @Size(max = 1000, message = "{repairOrder.description.size}", groups = {OnCreate.class, OnUpdate.class})
        String description,

        @Schema(description = "List of items/tasks to be addressed in this repair order. At least one is required.",
                example = "[\"Cambio de aceite\", \"Revisión de frenos\"]",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "{repairOrder.items.required}", groups = OnCreate.class)
        List<@NotBlank(message = "{repairOrder.items.itemBlank}") @Size(max = 200, message = "{repairOrder.items.itemSize}") String> items,

        @Schema(description = "Name of the field operator who reported the failure. Maximum 100 characters.",
                example = "Carlos Perez",
                maxLength = 100,
                nullable = true)
        @Size(max = 100, message = "{repairOrder.reportedBy.size}", groups = {OnCreate.class, OnUpdate.class})
        String reportedBy
) {}
