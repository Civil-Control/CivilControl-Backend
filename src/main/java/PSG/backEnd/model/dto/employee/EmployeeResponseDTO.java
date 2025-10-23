package PSG.backEnd.model.dto.employee;

import PSG.backEnd.model.dto.address.AddressResponseDTO;
import PSG.backEnd.model.dto.projectArea.ProjectAreaResponseDTO;
import PSG.backEnd.model.enums.employee.EmployeeRole;
import PSG.backEnd.model.enums.employee.EmployeeStatus;
import PSG.backEnd.model.enums.employee.EmploymentType;

import java.time.LocalDate;

public record EmployeeResponseDTO(
    Long id,
    String name,
    String lastName,
    String dni,
    String cuil,
    ProjectAreaResponseDTO projectArea,
    AddressResponseDTO address,
    LocalDate birthDate,
    String phoneNumber,
    String email,
    EmergencyContactResponseDTO emergencyContact,
    EmploymentType employmentType,
    EmployeeStatus employeeStatus,
    EmployeeRole employeeRole,
    LocalDate hireDate,
    LocalDate endDate
) {}

