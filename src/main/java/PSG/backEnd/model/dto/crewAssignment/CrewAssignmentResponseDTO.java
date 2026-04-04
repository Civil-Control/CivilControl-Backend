package PSG.backEnd.model.dto.crewAssignment;

import PSG.backEnd.model.enums.employee.EmployeeRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Response DTO for a crew assignment with resolved entity names.")
public record CrewAssignmentResponseDTO(
    @Schema(description = "Assignment ID.", example = "150")
    Long id,

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

    @Schema(description = "Vehicle ID.", example = "10")
    Long vehicleId,
    @Schema(description = "Vehicle license plate.", example = "ABC-123")
    String vehicleLicensePlate,
    @Schema(description = "Vehicle nickname.", example = "La Ranger", nullable = true)
    String vehicleNickName,
    @Schema(description = "Vehicle brand.", example = "Ford", nullable = true)
    String vehicleBrand,
    @Schema(description = "Vehicle model.", example = "Ranger", nullable = true)
    String vehicleModel,

    @Schema(description = "Project area ID.", example = "3")
    Long projectAreaId,
    @Schema(description = "Project area name.", example = "Zona Norte")
    String projectAreaName,
    @Schema(description = "Project area color.", example = "#FF5733", nullable = true)
    String projectAreaColor,

    @Schema(description = "Assignment date.", example = "2026-04-04")
    LocalDate date,
    @Schema(description = "Whether the employee is the driver.", example = "true")
    Boolean isDriver,
    @Schema(description = "Optional observation.", nullable = true)
    String observation
) {}
