package PSG.backEnd.model.dto.contracts;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "New certified amount for a certification.")
public record CertificationAmountDTO(
    @Schema(description = "Amount certified (net of IVA).", example = "2500000.00")
    @NotNull(message = "{validation.notNull}")
    @DecimalMin(value = "0.01", message = "{validation.decimalMin}")
    BigDecimal certifiedAmount
) {}
