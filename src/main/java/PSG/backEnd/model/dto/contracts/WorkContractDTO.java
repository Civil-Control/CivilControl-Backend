package PSG.backEnd.model.dto.contracts;

import PSG.backEnd.model.enums.contracts.Currency;
import PSG.backEnd.model.enums.contracts.WorkContractStatus;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "DTO for creating or updating a work contract.")
public record WorkContractDTO(

    @Schema(description = "Unique contract number within the tenant.", example = "2025-001")
    @NotBlank(message = "{validation.notBlank}", groups = OnCreate.class)
    @Size(max = 50, message = "{validation.size}", groups = {OnCreate.class, OnUpdate.class})
    String contractNumber,

    @Schema(description = "ID of the client (customer) for this contract.")
    @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
    @Positive(message = "{validation.positive}", groups = {OnCreate.class, OnUpdate.class})
    Long clientId,

    @Schema(description = "Optional project area ID.")
    Long projectAreaId,

    @Schema(description = "Optional project area task (sub-task) ID.")
    Long projectAreaTaskId,

    @Schema(description = "Description of the contracted work.", example = "Pavimentación calle Belgrano tramo 3")
    @NotBlank(message = "{validation.notBlank}", groups = OnCreate.class)
    @Size(max = 1000, message = "{validation.size}", groups = {OnCreate.class, OnUpdate.class})
    String description,

    @Schema(description = "Contract signing or start date.", example = "2025-03-15")
    @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
    LocalDate contractDate,

    @Schema(description = "Optional contract expiry date.", example = "2025-12-31")
    LocalDate endDate,

    @Schema(description = "Total contracted amount.", example = "10000000.00")
    @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
    @DecimalMin(value = "0.01", message = "{validation.decimalMin}", groups = {OnCreate.class, OnUpdate.class})
    BigDecimal contractedAmount,

    @Schema(description = "Currency: ARS or USD.", example = "ARS")
    @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
    Currency currency,

    @Schema(description = "Contract status.", example = "ACTIVO")
    @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
    WorkContractStatus status,

    @Schema(description = "Optional comment.", example = "Contrato con municipio, sujeto a auditoría.")
    @Size(max = 500, message = "{validation.size}", groups = {OnCreate.class, OnUpdate.class})
    String comment,

    @Schema(description = "Whether this contract is soft-deleted.")
    Boolean deleted
) {}
