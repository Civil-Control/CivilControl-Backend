package PSG.backEnd.repository.treasury;

import PSG.backEnd.model.entity.treasury.BankAccountMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface BankAccountMovementRepository extends JpaRepository<BankAccountMovement, Long> {

    Page<BankAccountMovement> findByBankAccountIdOrderByMovementDateDescIdDesc(Long bankAccountId, Pageable pageable);

    int countByBankAccountId(Long bankAccountId);

    @Query("SELECT MAX(m.createdAt) FROM BankAccountMovement m WHERE m.bankAccount.id = :bankAccountId")
    LocalDateTime findLastMovementAt(@Param("bankAccountId") Long bankAccountId);
}
