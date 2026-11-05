package PSG.backEnd.model.dto.employee;

import PSG.backEnd.model.enums.employee.MovementType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "Data Transfer Object for creating or updating an attendance record. " +
        "Represents a single clock-in or clock-out event for an employee.")
public record AttendanceRecordDTO(
    @Schema(description = "Unique identifier of the employee registering the movement.",
            example = "25",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{validation.required}", groups = OnCreate.class)
    Long employeeId,

    @Schema(description = "Date of the attendance movement. Cannot be in the future.",
            example = "2026-04-01",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{validation.required}", groups = OnCreate.class)
    @PastOrPresent(message = "{validation.pastOrPresent}", groups = {OnCreate.class, OnUpdate.class})
    LocalDate date,

    @Schema(description = "Time of the attendance movement in HH:mm format.",
            example = "08:30",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{validation.required}", groups = OnCreate.class)
    LocalTime time,

    @Schema(description = "Type of movement: ENTRADA (clock-in) or SALIDA (clock-out).",
            example = "ENTRADA",
            allowableValues = {"ENTRADA", "SALIDA"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{validation.required}", groups = OnCreate.class)
    MovementType movementType,

    @Schema(description = "Optional building ID where the movement was registered.",
            example = "3",
            nullable = true)
    @Positive(message = "{validation.positive}", groups = {OnCreate.class, OnUpdate.class})
    Long buildingId,

    @Schema(description = "Optional observation or note about this attendance record.",
            example = "Llegó tarde por lluvia",
            nullable = true)
    @Size(max = 500, message = "{validation.size.max}", groups = {OnCreate.class, OnUpdate.class})
    String observation
) {}
