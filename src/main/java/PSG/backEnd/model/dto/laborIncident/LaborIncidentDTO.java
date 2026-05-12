package PSG.backEnd.model.dto.laborIncident;

import PSG.backEnd.model.enums.employee.LaborIncidentStatus;
import PSG.backEnd.model.enums.employee.LaborIncidentType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record LaborIncidentDTO(
    @Schema(description = "IDs de los empleados involucrados en el incidente. Mínimo uno requerido.")
    @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
    @NotEmpty(message = "{validation.notEmpty}", groups = OnCreate.class)
    List<Long> employeeIds,

    @Schema(description = "Categoría del incidente.")
    @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
    LaborIncidentType incidentType,

    @Schema(description = "Fecha en que ocurrió el incidente. No puede ser futura.")
    @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
    @PastOrPresent(message = "{laborIncident.incidentDate.pastOrPresent}", groups = {OnCreate.class, OnUpdate.class})
    LocalDate incidentDate,

    @Schema(description = "Descripción detallada del evento. Mínimo 10 caracteres.", minLength = 10, maxLength = 1000)
    @NotBlank(message = "{validation.notBlank}", groups = OnCreate.class)
    @Size(min = 10, max = 1000, message = "{laborIncident.description.size}", groups = {OnCreate.class, OnUpdate.class})
    String description,

    @Schema(description = "Costo económico para la empresa. Nulo si no aplica.", nullable = true)
    @DecimalMin(value = "0.00", message = "{laborIncident.financialImpact.negative}", groups = {OnCreate.class, OnUpdate.class})
    BigDecimal financialImpact,

    @Schema(description = "Bien involucrado (ej: Camión F-350 patente AB123).", nullable = true)
    @Size(max = 500, message = "{laborIncident.affectedAsset.size}", groups = {OnCreate.class, OnUpdate.class})
    String affectedAsset,

    @Schema(description = "Estado actual del incidente.")
    @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
    LaborIncidentStatus status,

    @Schema(description = "Fecha de resolución. Solo válida cuando status es RESUELTO.", nullable = true)
    LocalDate resolvedDate,

    @Schema(description = "Observaciones adicionales.", nullable = true)
    @Size(max = 1000, message = "{laborIncident.notes.size}", groups = {OnCreate.class, OnUpdate.class})
    String notes
) {}
