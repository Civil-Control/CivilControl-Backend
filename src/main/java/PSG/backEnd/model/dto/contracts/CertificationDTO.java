package PSG.backEnd.model.dto.contracts;

import PSG.backEnd.model.enums.contracts.CertificationStatus;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "DTO for creating or updating a certification.")
public record CertificationDTO(

    @Schema(description = "ID of the work contract this certification belongs to.")
    @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
    @Positive(message = "{validation.positive}", groups = {OnCreate.class, OnUpdate.class})
    Long workContractId,

    @Schema(description = "Date the certification was issued.", example = "2025-04-15")
    @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
    LocalDate certificationDate,

    @Schema(description = "Amount certified in this period.", example = "2500000.00")
    @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
    @DecimalMin(value = "0.01", message = "{validation.decimalMin}", groups = {OnCreate.class, OnUpdate.class})
    BigDecimal certifiedAmount,

    @Schema(description = "Optional ID of the linked sales document (comprobante de venta).")
    Long salesDocumentId,

    @Schema(description = "Certification status (no transition constraints).", example = "PRESENTADO")
    @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
    CertificationStatus status,

    @Schema(description = "Optional comment.", example = "Avance medido según acta N° 3")
    @Size(max = 500, message = "{validation.size}", groups = {OnCreate.class, OnUpdate.class})
    String comment
) {}
