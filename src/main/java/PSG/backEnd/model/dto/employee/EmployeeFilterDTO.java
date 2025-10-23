package PSG.backEnd.model.dto.employee;

import PSG.backEnd.model.enums.employee.EmployeeRole;
import PSG.backEnd.model.enums.employee.EmployeeStatus;
import PSG.backEnd.model.enums.employee.EmploymentType;

import java.time.LocalDate;

public record EmployeeFilterDTO(
    String name,
    String lastName,
    String dni,
    String cuil,
    Long projectAreaId,
    String city,
    EmploymentType employmentType,
    EmployeeStatus employeeStatus,
    EmployeeRole employeeRole,
    LocalDate hireDateFrom,
    LocalDate hireDateTo
) {}

