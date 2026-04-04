package PSG.backEnd.model.dto.crewAssignment;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Data Transfer Object for creating or updating a crew assignment.")
public record CrewAssignmentDTO(
    @Schema(description = "Employee ID to assign.", example = "25")
    @NotNull(message = "{validation.required}", groups = OnCreate.class)
    Long employeeId,

    @Schema(description = "Vehicle ID to assign the employee to.", example = "10")
    @NotNull(message = "{validation.required}", groups = OnCreate.class)
    Long vehicleId,

    @Schema(description = "Project area ID for this assignment.", example = "3")
    @NotNull(message = "{validation.required}", groups = OnCreate.class)
    Long projectAreaId,

    @Schema(description = "Date of the assignment.", example = "2026-04-04")
    @NotNull(message = "{validation.required}", groups = OnCreate.class)
    LocalDate date,

    @Schema(description = "Whether the employee is the driver of the vehicle.", example = "false")
    Boolean isDriver,

    @Schema(description = "Optional observation.", example = "Sale temprano", nullable = true)
    @Size(max = 500, message = "{validation.size.max}")
    String observation
) {}
