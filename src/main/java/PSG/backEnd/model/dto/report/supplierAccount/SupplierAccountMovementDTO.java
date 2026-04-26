package PSG.backEnd.model.dto.report.supplierAccount;

import PSG.backEnd.model.enums.report.SupplierAccountMovementType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single chronological entry in a supplier's current-account timeline.
 * Either {@link #debit()} or {@link #credit()} is non-zero (never both).
 * {@link #accumulatedBalance()} is computed by the backend starting from the
 * supplier's previous balance.
 */
@Schema(description = "A single movement in a supplier current-account timeline (debit or credit, with running balance)")
public record SupplierAccountMovementDTO(
        LocalDate date,
        SupplierAccountMovementType type,
        String reference,
        String description,
        BigDecimal debit,
        BigDecimal credit,
        BigDecimal accumulatedBalance,
        String paymentMethod,
        Long sourceId
) {}
