package PSG.backEnd.model.dto.recovery;

import PSG.backEnd.model.enums.recovery.RecoveryEventType;

import java.time.LocalDate;

/**
 * Optional filters for {@code GET /recovery/sectors/{projectAreaId}/events}, the sector-wide
 * recovery ledger (as opposed to {@code listEventsForConfig}, which is scoped to one supplier).
 * All fields are optional — {@code null} means "no filter on this field".
 */
public record RecoveryEventFilterDTO(
    LocalDate fromDate,
    LocalDate toDate,
    Long cashBoxId,
    Long supplierId,
    RecoveryEventType eventType
) {}
