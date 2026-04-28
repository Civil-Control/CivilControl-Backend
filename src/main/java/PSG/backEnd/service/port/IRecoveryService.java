package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.recovery.RecoveryEventResponseDTO;
import PSG.backEnd.model.dto.recovery.RecoverySupplierConfigDTO;
import PSG.backEnd.model.dto.recovery.RecoverySupplierConfigResponseDTO;
import PSG.backEnd.model.entity.TransactionalDocument;
import PSG.backEnd.model.entity.recovery.RecoveryEvent;

import java.util.List;
import java.util.Optional;

/**
 * Service port for the hidden Value Recovery feature (Feature 18).
 *
 * <p>Owns the orchestration that turns a Factura A associated to the recovery sector
 * into a positive {@link PSG.backEnd.model.entity.treasury.CashBoxMovement}, and
 * keeps the cash box in sync when the originating document is edited, deleted or
 * partially reversed by a credit note.
 *
 * <p>All operations are no-ops for documents that do not satisfy the eligibility
 * rules (sector flag, fiscal letter A, configured supplier).
 */
public interface IRecoveryService {

    /**
     * Validates that the {@code document} is eligible for recovery and creates a
     * {@code GENERATED} event plus the matching cash-box inflow. Idempotent in the
     * sense that it does not create duplicate events for the same triggering action
     * — callers should pair {@link #reverseForDocument(TransactionalDocument)} before
     * regenerating when totals or supplier change.
     *
     * @return the created event, or {@link Optional#empty()} if the document is not
     *         eligible (different sector, no recovery flag, etc.).
     * @throws IllegalArgumentException when the sector is the recovery sector but the
     *         document does not satisfy the hard rules (not Factura A, no supplier,
     *         supplier not configured).
     */
    Optional<RecoveryEvent> generateForDocument(TransactionalDocument document);

    /**
     * Reverses (full) the most recent active {@code GENERATED} event for the document.
     * No-op when no active event exists.
     */
    Optional<RecoveryEvent> reverseForDocument(TransactionalDocument document);

    /**
     * Convenience helper used after edits: reverses the active event (if any) and then
     * regenerates a fresh one when the document is still eligible.
     */
    Optional<RecoveryEvent> regenerateIfNeeded(TransactionalDocument document);

    /**
     * Applies a partial reverse for a credit note that targets one or more invoices
     * with previously generated recoveries. The credit note's net + IVA are distributed
     * proportionally across its credit applications and the snapshot percentage of
     * each original event is used to compute the adjustment.
     */
    void adjustForCreditNote(TransactionalDocument creditNote);

    // ── Management API (configs + read models) ─────────────────────────────

    /** Returns the (single, optional) recovery sector for the current tenant. */
    Optional<Long> findActiveRecoverySectorId();

    /**
     * Lists all non-deleted supplier configs for the given recovery sector, ordered by supplier
     * legal name. Includes derived counters (events count, accumulated recovered amount).
     */
    List<RecoverySupplierConfigResponseDTO> listConfigsForArea(Long projectAreaId);

    /**
     * Creates a supplier config for the given recovery sector. The {@code projectAreaId} must
     * point to an area whose {@code isRecoverySector} flag is {@code true}; the supplier must
     * not already be configured for that sector.
     */
    RecoverySupplierConfigResponseDTO createConfig(Long projectAreaId, RecoverySupplierConfigDTO dto);

    /**
     * Updates an existing supplier config (percentage, cash box, active). The supplier itself
     * cannot be re-pointed; create a new config and deactivate the previous one for that case.
     */
    RecoverySupplierConfigResponseDTO updateConfig(Long configId, RecoverySupplierConfigDTO dto);

    /** Soft-deletes a supplier config. Past events keep their snapshot references. */
    void deleteConfig(Long configId);

    /** Returns the full event timeline for a recovery-eligible document, oldest first. */
    List<RecoveryEventResponseDTO> listEventsForDocument(Long documentId);

    /** Returns the full event timeline for a supplier config, oldest first. */
    List<RecoveryEventResponseDTO> listEventsForConfig(Long configId);
}