package PSG.backEnd.repository.recovery;

import PSG.backEnd.model.entity.recovery.RecoveryEvent;
import PSG.backEnd.model.enums.recovery.RecoveryEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RecoveryEventRepository extends JpaRepository<RecoveryEvent, Long> {

    List<RecoveryEvent> findByTransactionalDocumentIdOrderByOccurredAtAscIdAsc(Long transactionalDocumentId);

    /**
     * Net recovered amount per document (sum of every {@link RecoveryEvent#getRecoveredAmount()},
     * which already carries its sign — positive on GENERATED, negative on REVERSED /
     * ADJUSTED_CREDIT_NOTE). Used by reports to compute the real cash outflow of a recovery-sector
     * document ({@code total - recoveredAmount}) without reimplementing the recovery formula.
     */
    @Query("""
        SELECT e.transactionalDocument.id, COALESCE(SUM(e.recoveredAmount), 0)
        FROM RecoveryEvent e
        WHERE e.transactionalDocument.id IN :documentIds
        GROUP BY e.transactionalDocument.id
        """)
    List<Object[]> sumRecoveredAmountGroupedByDocumentIds(@Param("documentIds") Collection<Long> documentIds);

    /**
     * Full recovery ledger for a sector, across every supplier config, with optional filters.
     * Dated by the paired {@link PSG.backEnd.model.entity.treasury.CashBoxMovement#getMovementDate()}
     * (the operative date: the document's date on GENERATED, today on REVERSED, the credit note's
     * date on ADJUSTED_CREDIT_NOTE) rather than {@code occurredAt} (mere event-processing timestamp).
     */
    @Query("""
        SELECT e FROM RecoveryEvent e
        JOIN e.cashBoxMovement m
        WHERE e.supplierConfig.projectArea.id = :projectAreaId
          AND (:fromDate IS NULL OR m.movementDate >= :fromDate)
          AND (:toDate IS NULL OR m.movementDate <= :toDate)
          AND (:cashBoxId IS NULL OR e.snapshotCashBox.id = :cashBoxId)
          AND (:supplierId IS NULL OR e.supplierConfig.supplier.id = :supplierId)
          AND (:eventType IS NULL OR e.eventType = :eventType)
        """)
    Page<RecoveryEvent> findAllForSector(
            @Param("projectAreaId") Long projectAreaId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("cashBoxId") Long cashBoxId,
            @Param("supplierId") Long supplierId,
            @Param("eventType") RecoveryEventType eventType,
            Pageable pageable);

    /**
     * Returns the most recent {@link RecoveryEventType#GENERATED} event for a document
     * that has not yet been fully reversed by a subsequent {@code REVERSED} event.
     * Used by the service to know whether a reverse must be issued.
     */
    @Query("""
        SELECT e FROM RecoveryEvent e
        WHERE e.transactionalDocument.id = :documentId
          AND e.eventType = PSG.backEnd.model.enums.recovery.RecoveryEventType.GENERATED
          AND NOT EXISTS (
              SELECT 1 FROM RecoveryEvent r
              WHERE r.reversesEvent.id = e.id
                AND r.eventType = PSG.backEnd.model.enums.recovery.RecoveryEventType.REVERSED
          )
        ORDER BY e.occurredAt DESC, e.id DESC
        """)
    List<RecoveryEvent> findActiveGeneratedForDocument(@Param("documentId") Long documentId);

    default Optional<RecoveryEvent> findLastActiveGeneratedForDocument(Long documentId) {
        return findActiveGeneratedForDocument(documentId).stream().findFirst();
    }

    @Query("""
        SELECT COALESCE(SUM(e.recoveredAmount), 0) FROM RecoveryEvent e
        WHERE e.supplierConfig.id = :configId
        """)
    BigDecimal sumRecoveredAmountByConfig(@Param("configId") Long configId);

    int countBySupplierConfigId(Long configId);

    List<RecoveryEvent> findBySupplierConfigIdOrderByOccurredAtAscIdAsc(Long supplierConfigId);
}
