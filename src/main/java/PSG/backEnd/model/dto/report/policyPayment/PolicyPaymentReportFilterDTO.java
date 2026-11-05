package PSG.backEnd.model.dto.report.policyPayment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Filters for the policy payment report")
public record PolicyPaymentReportFilterDTO(

    @Schema(description = "Start date (inclusive)")
    LocalDate startDate,

    @Schema(description = "End date (inclusive)")
    LocalDate endDate,

    @Schema(description = "Filter by policy type enum names")
    List<String> policyTypes,

    @Schema(description = "Filter by specific insurance policy ID")
    Long insurancePolicyId,

    @Schema(description = "Minimum amount (inclusive)")
    BigDecimal minAmount,

    @Schema(description = "Maximum amount (inclusive)")
    BigDecimal maxAmount
) {}
