package PSG.backEnd.model.dto.crewAssignment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

@Schema(description = "An assignment within a crew report save request.")
public record CrewReportAssignmentDTO(
    @Schema(description = "Employee ID.", example = "25")
    @NotNull(message = "{validation.required}")
    Long employeeId,

    @Schema(description = "Vehicle ID.", example = "10")
    @NotNull(message = "{validation.required}")
    Long vehicleId,

    @Schema(description = "Project area ID for this assignment.", example = "3")
    @NotNull(message = "{validation.required}")
    Long projectAreaId,

    @Schema(description = "Project area task ID. Optional.", nullable = true)
    Long projectAreaTaskId,

    @Schema(description = "Whether the employee is the driver.", example = "false")
    Boolean isDriver,

    @Schema(description = "Optional observation.", nullable = true)
    @Size(max = 500, message = "{validation.size.max}")
    String observation,

    @Schema(description = "Vehicle mileage.", example = "45000", nullable = true)
    Integer km,

    @Schema(description = "Departure time override for this assignment.", nullable = true)
    LocalTime departureTime,

    @Schema(description = "Return time override for this assignment.", nullable = true)
    LocalTime returnTime
) {}
