package PSG.backEnd.repository.treasury;

import PSG.backEnd.model.entity.treasury.CashBoxMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface CashBoxMovementRepository extends JpaRepository<CashBoxMovement, Long> {

    Page<CashBoxMovement> findByCashBoxIdOrderByMovementDateDescIdDesc(Long cashBoxId, Pageable pageable);

    int countByCashBoxId(Long cashBoxId);

    @Query("SELECT MAX(m.createdAt) FROM CashBoxMovement m WHERE m.cashBox.id = :cashBoxId")
    LocalDateTime findLastMovementAt(@Param("cashBoxId") Long cashBoxId);
}
