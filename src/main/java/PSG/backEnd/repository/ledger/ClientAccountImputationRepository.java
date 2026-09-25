package PSG.backEnd.repository.ledger;

import PSG.backEnd.model.entity.ledger.ClientAccountImputation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

/**
 * Client-side counterpart of {@code AccountImputationRepository}. Only the subset actually used
 * (no on-account queries — there is no client-side payment/on-account flow).
 */
@Repository
public interface ClientAccountImputationRepository extends JpaRepository<ClientAccountImputation, Long> {

    @Query("SELECT COALESCE(SUM(i.amountApplied), 0) FROM ClientAccountImputation i WHERE i.destinationMovement.id = :destinationMovementId")
    BigDecimal sumAppliedToDestination(@Param("destinationMovementId") Long destinationMovementId);

    void deleteByOriginMovement_Id(Long originMovementId);
}
