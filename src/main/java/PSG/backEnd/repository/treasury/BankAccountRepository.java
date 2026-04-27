package PSG.backEnd.repository.treasury;

import PSG.backEnd.model.entity.treasury.BankAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    Optional<BankAccount> findByIdAndDeletedFalse(Long id);

    @Query("SELECT b FROM BankAccount b WHERE b.deleted = false " +
            "AND (:active IS NULL OR b.active = :active) " +
            "AND (:search IS NULL OR LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "                    OR LOWER(b.bankName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "                    OR LOWER(b.accountNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<BankAccount> findAllWithFilters(@Param("active") Boolean active,
                                         @Param("search") String search,
                                         Pageable pageable);

    boolean existsByAccountNumberAndBankNameIgnoreCaseAndDeletedFalse(String accountNumber, String bankName);

    boolean existsByAccountNumberAndBankNameIgnoreCaseAndIdNotAndDeletedFalse(String accountNumber, String bankName, Long id);
}
