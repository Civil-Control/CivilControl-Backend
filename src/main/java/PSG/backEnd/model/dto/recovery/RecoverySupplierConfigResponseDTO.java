package PSG.backEnd.model.dto.recovery;

import java.math.BigDecimal;

/**
 * Read model for a {@link PSG.backEnd.model.entity.recovery.RecoverySupplierConfig}.
 * Includes derived counters (events count, accumulated recovered amount) so the UI can
 * surface usage without follow-up calls.
 */
public record RecoverySupplierConfigResponseDTO(
    Long id,
    Long projectAreaId,
    String projectAreaName,
    Long supplierId,
    String supplierName,
    String supplierCuit,
    BigDecimal recoveryPercentage,
    Long cashBoxId,
    String cashBoxName,
    BigDecimal cashBoxCurrentBalance,
    Boolean active,
    int totalRecoveryEventsCount,
    BigDecimal totalRecoveredAmount
) {}
