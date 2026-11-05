package PSG.backEnd.model.dto.recovery;

import java.time.LocalDate;

/**
 * Optional filters for {@code GET /recovery/sectors/{projectAreaId}/events}, the sector-wide
 * recovery ledger (as opposed to {@code listEventsForConfig}, which is scoped to one supplier).
 * All fields are optional — {@code null} means "no filter on this field".
 *
 * <p>This listing only ever returns active {@code GENERATED} events (one per document, at most)
 * — REVERSED / ADJUSTED_CREDIT_NOTE rows and superseded GENERATED rows are deliberately excluded
 * so the table reads as "what's currently recovered", not a raw audit log. That full history
 * still exists in the ledger and remains reachable per-document via
 * {@code GET /recovery/documents/{documentId}/events}.
 */
public record RecoveryEventFilterDTO(
    LocalDate fromDate,
    LocalDate toDate,
    Long cashBoxId,
    Long supplierId
) {}
