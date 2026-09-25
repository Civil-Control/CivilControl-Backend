package PSG.backEnd.repository.ledger;

import PSG.backEnd.model.entity.ledger.ClientAccountMovement;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Client-side counterpart of {@code AccountMovementRepository}. Only the subset actually used
 * (no payment-movement queries — there is no client-side Payment entity).
 */
@Repository
public interface ClientAccountMovementRepository extends JpaRepository<ClientAccountMovement, Long> {

    @Query("SELECT m FROM ClientAccountMovement m WHERE m.sourceDocument.id = :documentId AND m.movementType <> PSG.backEnd.model.enums.ledger.ClientAccountMovementType.REVERSAL")
    Optional<ClientAccountMovement> findOriginalBySourceDocument(@Param("documentId") Long documentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM ClientAccountMovement m WHERE m.sourceDocument.id = :documentId AND m.movementType <> PSG.backEnd.model.enums.ledger.ClientAccountMovementType.REVERSAL")
    Optional<ClientAccountMovement> findOriginalBySourceDocumentWithLock(@Param("documentId") Long documentId);

    @Query("SELECT COALESCE(SUM(m.amount), 0) FROM ClientAccountMovement m WHERE m.client.id = :clientId AND m.tenantId = :tenantId")
    BigDecimal sumBalanceByClient(@Param("clientId") Long clientId, @Param("tenantId") Long tenantId);

    @Modifying
    @Query("DELETE FROM ClientAccountMovement m WHERE m.sourceDocument.id = :documentId AND m.movementType = PSG.backEnd.model.enums.ledger.ClientAccountMovementType.REVERSAL")
    void deleteReversalsBySourceDocument(@Param("documentId") Long documentId);
}
