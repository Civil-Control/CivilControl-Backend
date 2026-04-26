package PSG.backEnd.model.dto.report.supplierAccount;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Complete supplier current-account report. Two layers only:
 * Layer 1 = supplier groups, Layer 2 = chronological movements (inside each group).
 */
@Builder
@Schema(description = "Complete supplier current-account report")
public record SupplierAccountReportDTO(
        SupplierAccountReportFilterDTO filters,
        List<SupplierAccountReportSupplierGroupDTO> supplierGroups,
        int supplierCount,
        int pendingSupplierCount,
        int settledSupplierCount,
        BigDecimal totalPreviousBalance,
        BigDecimal totalDebited,
        BigDecimal totalCredited,
        BigDecimal totalPendingBalance,
        LocalDateTime generatedAt,
        String reportName,
        String periodDescription
) {}
