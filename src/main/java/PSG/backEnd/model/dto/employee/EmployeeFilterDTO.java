package PSG.backEnd.model.dto.employee;

import PSG.backEnd.model.enums.employee.EmployeeRole;
import PSG.backEnd.model.enums.employee.EmployeeStatus;
import PSG.backEnd.model.enums.employee.EmploymentType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Filter Data Transfer Object for employees. " +
        "Used to filter and search employees by multiple criteria including personal information, employment details, and location.")
public record EmployeeFilterDTO(
    @Schema(description = "Filter by employee first name. Supports partial matching (contains search).",
            example = "Juan",
            nullable = true)
    String name,

    @Schema(description = "Filter by employee last name. Supports partial matching (contains search).",
            example = "García",
            nullable = true)
    String lastName,

    @Schema(description = "Filter by DNI number. Exact match required.",
            example = "12345678",
            nullable = true)
    String dni,

    @Schema(description = "Filter by CUIL number. Exact match required.",
            example = "20-12345678-9",
            nullable = true)
    String cuil,

    @Schema(description = "Filter by project area ID. Returns employees assigned to the specified project area.",
            example = "3",
            nullable = true)
    Long projectAreaId,

    @Schema(description = "Filter by city of residence. Supports partial matching (contains search).",
            example = "Buenos Aires",
            nullable = true)
    String city,

    @Schema(description = "Filter by employment type. Values: TIEMPO_COMPLETO, MEDIO_TIEMPO, CONTRATO_TEMPORAL, PASANTIA, SUB_CONTRATADO.",
            example = "TIEMPO_COMPLETO",
            nullable = true)
    EmploymentType employmentType,

    @Schema(description = "Filter by employee status. Values: ACTIVO, LICENCIA, SUSPENDIDO.",
            example = "ACTIVO",
            nullable = true)
    EmployeeStatus employeeStatus,

    @Schema(description = "Filter by employee role. Filters employees by their job position. Values: CHOFER, OFICIAL, AYUDANTE, ADMINISTRATIVO, LIMPIEZA, MECANICO, OTRO.",
            example = "CHOFER",
            nullable = true)
    EmployeeRole employeeRole,

    @Schema(description = "Filter by minimum hire date. Returns employees hired from this date onwards.",
            example = "2023-01-01",
            nullable = true)
    LocalDate hireDateFrom,

    @Schema(description = "Filter by maximum hire date. Returns employees hired up to this date.",
            example = "2023-12-31",
            nullable = true)
    LocalDate hireDateTo
) {}

