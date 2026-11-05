package PSG.backEnd.model.dto.contracts;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

@Schema(description = "Optional data sent when linking a certification from the sales-document side.")
public record CertificationLinkDTO(
    @Schema(description = "IVA rate applied to the certified amount on the sales-document.", example = "21")
    @DecimalMin(value = "0", message = "{validation.decimalMin}")
    @DecimalMax(value = "100", message = "{validation.max}")
    BigDecimal ivaPercentage
) {}
