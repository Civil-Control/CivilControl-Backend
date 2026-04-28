package PSG.backEnd.model.dto.recovery;

import PSG.backEnd.model.enums.recovery.RecoveryBase;
import PSG.backEnd.model.enums.recovery.RecoveryEventType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Read model for a {@link PSG.backEnd.model.entity.recovery.RecoveryEvent}.
 * The {@code recoveredAmount} carries the sign already applied to the cash box
 * (positive on GENERATED, negative on REVERSED / ADJUSTED_CREDIT_NOTE) so the UI can
 * render it directly with the correct color cues.
 */
public record RecoveryEventResponseDTO(
    Long id,
    RecoveryEventType eventType,
    Long transactionalDocumentId,
    String transactionalDocumentReference,
    Long creditNoteDocumentId,
    String creditNoteDocumentReference,
    Long reversesEventId,
    Long supplierId,
    String supplierName,
    BigDecimal snapshotPercentage,
    RecoveryBase snapshotBase,
    Long snapshotCashBoxId,
    String snapshotCashBoxName,
    BigDecimal documentNet,
    BigDecimal documentIva,
    BigDecimal recoveredAmount,
    Long cashBoxMovementId,
    LocalDateTime occurredAt,
    Long triggeredByUserId,
    String triggeredByUserName
) {}
