package PSG.backEnd.model.dto.report.supplierAccount;

import PSG.backEnd.model.enums.report.SupplierAccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * Status-level grouping inside the supplier current-account report.
 *
 * <p>Each report contains exactly two status groups: one for {@link SupplierAccountStatus#PENDIENTE}
 * and one for {@link SupplierAccountStatus#CANCELADO}. Suppliers inside each group are sorted by
 * descending final balance and then alphabetically by legal name.</p>
 */
@Builder
@Schema(description = "Status-level grouping inside the supplier current-account report")
public record SupplierAccountReportStatusGroupDTO(
        SupplierAccountStatus status,
        int supplierCount,
        BigDecimal subtotalPreviousBalance,
        BigDecimal subtotalDebited,
        BigDecimal subtotalPaid,
        BigDecimal subtotalCreditNotes,
        BigDecimal subtotalFinalBalance,
        List<SupplierAccountReportSupplierGroupDTO> supplierGroups
) {}
