package PSG.backEnd.model.dto.report.issuedPayment;

import PSG.backEnd.model.enums.documents.PaymentMethod;
import PSG.backEnd.model.enums.report.IssuedPaymentReportGroupBy;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Root DTO for the Issued Payments Report (Feature 16).
 */
@Builder
@Schema(description = "Issued Payments Report root DTO")
public record IssuedPaymentReportDTO(
    IssuedPaymentReportFilterDTO filters,
    IssuedPaymentReportGroupBy groupBy,
    List<IssuedPaymentReportPrimaryGroupDTO> primaryGroups,
    BigDecimal totalAmount,
    int totalCount,
    Map<PaymentMethod, BigDecimal> totalsByMethod,
    Map<PaymentMethod, Integer> countsByMethod,
    CheckSummaryDTO checkSummary,
    LocalDateTime generatedAt,
    String reportName,
    String periodDescription
) {}
