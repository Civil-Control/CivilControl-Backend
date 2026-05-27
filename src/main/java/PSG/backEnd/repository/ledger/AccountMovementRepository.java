package PSG.backEnd.repository.ledger;

import PSG.backEnd.model.entity.ledger.AccountMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountMovementRepository extends JpaRepository<AccountMovement, Long> {

    List<AccountMovement> findBySupplier_IdOrderByMovementDateAscIdAsc(Long supplierId);

    Optional<AccountMovement> findBySourceDocument_Id(Long documentId);

    Optional<AccountMovement> findBySourcePayment_Id(Long paymentId);

    @Query("SELECT COALESCE(SUM(m.amount), 0) FROM AccountMovement m WHERE m.supplier.id = :supplierId AND m.tenantId = :tenantId")
    BigDecimal sumBalanceBySupplier(@Param("supplierId") Long supplierId, @Param("tenantId") Long tenantId);
}
