package PSG.backEnd.repository.recovery;

import PSG.backEnd.model.entity.recovery.RecoveryEvent;
import PSG.backEnd.model.enums.recovery.RecoveryEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface RecoveryEventRepository extends JpaRepository<RecoveryEvent, Long> {

    List<RecoveryEvent> findByTransactionalDocumentIdOrderByOccurredAtAscIdAsc(Long transactionalDocumentId);

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
