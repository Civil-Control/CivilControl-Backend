package PSG.backEnd.model.dto.laborIncident;

import PSG.backEnd.model.enums.employee.LaborIncidentStatus;
import PSG.backEnd.model.enums.employee.LaborIncidentType;

import java.time.LocalDate;

public record LaborIncidentFilterDTO(
    Long employeeId,
    String employeeSearch,
    LaborIncidentType incidentType,
    LaborIncidentStatus status,
    LocalDate incidentDateFrom,
    LocalDate incidentDateTo,
    Boolean hasFinancialImpact
) {}
