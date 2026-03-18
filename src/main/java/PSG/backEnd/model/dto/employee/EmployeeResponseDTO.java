package PSG.backEnd.model.dto.employee;

import PSG.backEnd.model.dto.address.AddressResponseDTO;
import PSG.backEnd.model.dto.projectArea.ProjectAreaResponseDTO;
import PSG.backEnd.model.enums.employee.EmployeeRole;
import PSG.backEnd.model.enums.employee.EmployeeStatus;
import PSG.backEnd.model.enums.employee.EmploymentType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Response Data Transfer Object for employee. " +
        "Contains complete employee information including personal details, employment data, and associated entities.")
public record EmployeeResponseDTO(
    @Schema(description = "Unique identifier of the employee.",
            example = "25")
    Long id,

    @Schema(description = "First name of the employee.",
            example = "Juan Carlos")
    String name,

    @Schema(description = "Last name of the employee.",
            example = "García Pérez")
    String lastName,

    @Schema(description = "Argentine national identity document number (DNI).",
            example = "12345678")
    String dni,

    @Schema(description = "Argentine tax identification number (CUIL) in format XX-XXXXXXXX-X.",
            example = "20-12345678-9")
    String cuil,

    @Schema(description = "Complete information about the project area where the employee is assigned.")
    ProjectAreaResponseDTO projectArea,

    @Schema(description = "Complete residential address of the employee.")
    AddressResponseDTO address,

    @Schema(description = "Date of birth of the employee.",
            example = "1990-05-15")
    LocalDate birthDate,

    @Schema(description = "Primary phone number of the employee.",
            example = "+54 9 11 1234-5678")
    String phoneNumber,

    @Schema(description = "Email address of the employee.",
            example = "juan.garcia@example.com")
    String email,

    @Schema(description = "Emergency contact information for the employee.")
    EmergencyContactResponseDTO emergencyContact,

    @Schema(description = "Type of employment contract. Values: TIEMPO_COMPLETO, MEDIO_TIEMPO, CONTRATO_TEMPORAL, PASANTIA, SUB_CONTRATADO.",
            example = "TIEMPO_COMPLETO")
    EmploymentType employmentType,

    @Schema(description = "Current employment status. Values: ACTIVO, LICENCIA, SUSPENDIDO.",
            example = "ACTIVO")
    EmployeeStatus employeeStatus,

    @Schema(description = "Employee's roles or job positions in the organization.",
            example = "[\"CHOFER\", \"OFICIAL\"]")
    List<EmployeeRole> employeeRoles,

    @Schema(description = "Date when the employee was hired.",
            example = "2023-01-15")
    LocalDate hireDate,

    @Schema(description = "Date when the employee's employment ended. Null for currently employed personnel.",
            example = "2025-12-31",
            nullable = true)
    LocalDate endDate
) {}

