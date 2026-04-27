package PSG.backEnd.model.dto.report.issuedPayment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * Aggregated check-status statistics for the Issued Payments Report.
 *
 * <p>Always present in the response (even when no checks match the filters,
 * in which case all counters are zero) so the frontend can render KPI cards
 * deterministically. Counts and amounts use the <i>effective</i> status
 * (i.e. {@code PENDIENTE} with past dueDate is reported under "overdue", not
 * "pending").
 */
@Builder
@Schema(description = "Aggregated counts/amounts of checks by effective status")
public record CheckSummaryDTO(
    int totalChecks,
    BigDecimal totalChecksAmount,
    int pendingCount,    BigDecimal pendingAmount,
    int overdueCount,    BigDecimal overdueAmount,
    int settledCount,    BigDecimal settledAmount,
    int rejectedCount,   BigDecimal rejectedAmount,
    int cancelledCount,  BigDecimal cancelledAmount
) {}
