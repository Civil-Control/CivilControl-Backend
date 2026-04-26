package PSG.backEnd.model.dto.report.supplierAccount;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Complete supplier current-account report. Three layers:
 * Layer 1 = status groups (PENDIENTE / CANCELADO),
 * Layer 2 = supplier groups (inside each status),
 * Layer 3 = chronological movements (inside each supplier group).
 */
@Builder
@Schema(description = "Complete supplier current-account report")
public record SupplierAccountReportDTO(
        SupplierAccountReportFilterDTO filters,
        List<SupplierAccountReportStatusGroupDTO> statusGroups,
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
