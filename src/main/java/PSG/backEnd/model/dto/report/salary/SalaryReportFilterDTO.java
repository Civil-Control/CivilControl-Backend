package PSG.backEnd.model.dto.report.salary;

import PSG.backEnd.model.enums.documents.PaymentMethod;
import PSG.backEnd.model.enums.employee.SalaryFrecuency;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Filter criteria for the salary report")
public record SalaryReportFilterDTO(

    @Schema(description = "Start date of the report period (inclusive)", example = "2026-01-01")
    LocalDate startDate,

    @Schema(description = "End date of the report period (inclusive)", example = "2026-12-31")
    LocalDate endDate,

    @Schema(description = "List of project area IDs to filter by. Empty or null = all areas")
    List<Long> projectAreaIds,

    @Schema(description = "Filter by salary frequency", nullable = true)
    SalaryFrecuency salaryFrequency,

    @Schema(description = "Filter by payment method", nullable = true)
    PaymentMethod paymentMethod,

    @Schema(description = "Minimum payment amount (inclusive)", nullable = true)
    BigDecimal minAmount,

    @Schema(description = "Maximum payment amount (inclusive)", nullable = true)
    BigDecimal maxAmount,

    @Schema(description = "When true, include records with no project area assigned alongside any selected area filter")
    Boolean includeUnassigned
) {}
