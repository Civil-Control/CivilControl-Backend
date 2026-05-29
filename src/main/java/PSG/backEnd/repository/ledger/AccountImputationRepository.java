package PSG.backEnd.repository.ledger;

import PSG.backEnd.model.entity.ledger.AccountImputation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface AccountImputationRepository extends JpaRepository<AccountImputation, Long> {

    List<AccountImputation> findByOriginMovement_Id(Long originMovementId);

    List<AccountImputation> findByDestinationMovement_Id(Long destinationMovementId);

    @Query("SELECT COALESCE(SUM(i.amountApplied), 0) FROM AccountImputation i WHERE i.destinationMovement.id = :destinationMovementId")
    BigDecimal sumAppliedToDestination(@Param("destinationMovementId") Long destinationMovementId);

    @Query("SELECT COALESCE(SUM(i.amountApplied), 0) FROM AccountImputation i WHERE i.originMovement.id = :originMovementId")
    BigDecimal sumAppliedFromOrigin(@Param("originMovementId") Long originMovementId);

    void deleteByOriginMovement_Id(Long originMovementId);

    void deleteByOriginMovement_IdAndOnAccountFalse(Long originMovementId);

    @Query("SELECT COALESCE(SUM(i.amountApplied), 0) FROM AccountImputation i WHERE i.originMovement.id = :originMovementId AND i.onAccount = true")
    BigDecimal sumOnAccountAppliedFromOrigin(@Param("originMovementId") Long originMovementId);

    List<AccountImputation> findByDestinationMovement_IdAndOnAccountTrue(Long destinationMovementId);

    boolean existsByOriginMovement_IdAndOnAccountTrue(Long originMovementId);

    @Query("SELECT COALESCE(SUM(i.amountApplied), 0) FROM AccountImputation i WHERE i.destinationMovement.id = :destinationMovementId AND i.onAccount = true")
    BigDecimal sumOnAccountAppliedToDestination(@Param("destinationMovementId") Long destinationMovementId);
}
