package PSG.backEnd.model.dto.laborIncident;

import PSG.backEnd.model.enums.employee.LaborIncidentStatus;
import PSG.backEnd.model.enums.employee.LaborIncidentType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record LaborIncidentResponseDTO(
    Long id,
    LaborIncidentType incidentType,
    LocalDate incidentDate,
    String description,
    BigDecimal financialImpact,
    String affectedAsset,
    LaborIncidentStatus status,
    LocalDate resolvedDate,
    String notes,
    List<EmployeeRef> employees
) {
    public record EmployeeRef(Long id, String name, String lastName) {}
}
