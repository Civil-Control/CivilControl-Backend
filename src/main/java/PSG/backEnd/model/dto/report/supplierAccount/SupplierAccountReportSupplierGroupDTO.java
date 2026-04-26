package PSG.backEnd.model.dto.report.supplierAccount;

import PSG.backEnd.model.enums.report.SupplierAccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * Per-supplier aggregate of a current-account report:
 * previous balance, period totals, final balance, status, and the chronological
 * list of movements within the period.
 */
@Builder
@Schema(description = "Per-supplier current-account group (Layer 1 of the report)")
public record SupplierAccountReportSupplierGroupDTO(
        Long supplierId,
        String supplierLegalName,
        String supplierTradeName,
        String supplierCuit,
        BigDecimal previousBalance,
        BigDecimal totalDebited,
        BigDecimal totalPaid,
        BigDecimal totalCreditNotes,
        BigDecimal finalBalance,
        SupplierAccountStatus status,
        int movementCount,
        List<SupplierAccountMovementDTO> movements
) {}
