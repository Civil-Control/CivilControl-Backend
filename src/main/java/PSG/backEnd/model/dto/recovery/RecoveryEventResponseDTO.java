package PSG.backEnd.model.dto.recovery;

import PSG.backEnd.model.enums.recovery.RecoveryBase;
import PSG.backEnd.model.enums.recovery.RecoveryEventType;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    /**
     * Operative date of the movement: the document's date on GENERATED, today on REVERSED, the
     * credit note's date on ADJUSTED_CREDIT_NOTE. This is what the UI should show as "Fecha" —
     * {@code occurredAt} below is only the audit timestamp of when the system processed it.
     */
    LocalDate movementDate,
    LocalDateTime occurredAt,
    Long triggeredByUserId,
    String triggeredByUserName
) {}
