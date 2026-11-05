package PSG.backEnd.model.dto.crewAssignment;

import PSG.backEnd.model.enums.employee.EmployeeRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;
import java.util.List;

@Schema(description = "A crew member within a vehicle assignment.")
public record CrewMemberDTO(
    @Schema(description = "Assignment ID.", example = "150")
    Long assignmentId,
    @Schema(description = "Employee ID.", example = "25")
    Long employeeId,
    @Schema(description = "Employee first name.", example = "Juan Carlos")
    String employeeName,
    @Schema(description = "Employee last name.", example = "García Pérez")
    String employeeLastName,
    @Schema(description = "Employee DNI.", example = "35123456")
    String employeeDni,
    @Schema(description = "Employee roles.")
    List<EmployeeRole> employeeRoles,
    @Schema(description = "Whether this member is the driver.", example = "true")
    Boolean isDriver,
    @Schema(description = "Optional observation.", nullable = true)
    String observation,
    @Schema(description = "Departure time for this member.", nullable = true)
    LocalTime departureTime,
    @Schema(description = "Return time for this member.", nullable = true)
    LocalTime returnTime
) {}
