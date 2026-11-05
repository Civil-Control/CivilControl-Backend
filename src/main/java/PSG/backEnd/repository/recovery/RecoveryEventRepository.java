package PSG.backEnd.repository.recovery;

import PSG.backEnd.model.entity.recovery.RecoveryEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RecoveryEventRepository extends JpaRepository<RecoveryEvent, Long>, JpaSpecificationExecutor<RecoveryEvent> {

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

    /**
     * Ids of documents left with more than one active (un-reversed) {@code GENERATED} event —
     * the data-integrity gap {@code RecoveryService.generateForDocument} now prevents going
     * forward. Feeds the one-time cleanup ({@code dedupeDuplicateGeneratedEvents}).
     */
    @Query("""
        SELECT e.transactionalDocument.id
        FROM RecoveryEvent e
        WHERE e.eventType = PSG.backEnd.model.enums.recovery.RecoveryEventType.GENERATED
          AND NOT EXISTS (
              SELECT 1 FROM RecoveryEvent r
              WHERE r.reversesEvent.id = e.id
                AND r.eventType = PSG.backEnd.model.enums.recovery.RecoveryEventType.REVERSED
          )
        GROUP BY e.transactionalDocument.id
        HAVING COUNT(e) > 1
        """)
    List<Long> findDocumentIdsWithDuplicateActiveGeneratedEvents();

    @Query("""
        SELECT COALESCE(SUM(e.recoveredAmount), 0) FROM RecoveryEvent e
        WHERE e.supplierConfig.id = :configId
        """)
    BigDecimal sumRecoveredAmountByConfig(@Param("configId") Long configId);

    int countBySupplierConfigId(Long configId);

    List<RecoveryEvent> findBySupplierConfigIdOrderByOccurredAtAscIdAsc(Long supplierConfigId);
}
