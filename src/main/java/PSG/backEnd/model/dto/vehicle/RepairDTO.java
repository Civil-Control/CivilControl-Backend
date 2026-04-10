package PSG.backEnd.model.dto.vehicle;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Data Transfer Object for creating or updating a vehicle repair.")
public record RepairDTO(

        @Schema(description = "Date when the repair was performed. Must be today or in the past.",
                example = "2024-10-01",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{repair.date.required}", groups = OnCreate.class)
        @PastOrPresent(message = "{repair.date.pastOrPresent}", groups = {OnCreate.class, OnUpdate.class})
        LocalDate date,

        @Schema(description = "ID of the vehicle that received the repair.",
                example = "15",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{repair.vehicleId.required}", groups = OnCreate.class)
        @Positive(message = "{validation.positive}", groups = {OnCreate.class, OnUpdate.class})
        Long vehicleId,

        @Schema(description = "Detailed description of the repair work performed. Maximum 1000 characters.",
                example = "Replaced brake pads and rotors on front wheels.",
                maxLength = 1000,
                nullable = true)
        @Size(max = 1000, message = "{repair.description.size}", groups = {OnCreate.class, OnUpdate.class})
        String description,

        @Schema(description = "Vehicle mileage (km) at the time of repair.",
                example = "45000",
                nullable = true)
        @Min(value = 0, message = "{repair.mileage.min}", groups = {OnCreate.class, OnUpdate.class})
        Integer mileage,

        @Schema(description = "ID of the external supplier who performed the repair.",
                example = "8",
                nullable = true)
        @Positive(message = "{validation.positive}", groups = {OnCreate.class, OnUpdate.class})
        Long supplierId,

        @Schema(description = "Repair items (materials and labor). At least one is required on create.")
        @NotEmpty(message = "{repair.items.required}", groups = OnCreate.class)
        @Valid
        List<RepairItemDTO> items
) {}
