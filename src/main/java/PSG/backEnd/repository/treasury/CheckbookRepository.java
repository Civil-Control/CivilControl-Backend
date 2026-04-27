package PSG.backEnd.repository.treasury;

import PSG.backEnd.model.entity.treasury.Checkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CheckbookRepository extends JpaRepository<Checkbook, Long> {

    Optional<Checkbook> findByIdAndDeletedFalse(Long id);

    int countByBankAccountIdAndActiveTrueAndDeletedFalse(Long bankAccountId);

    @Query("SELECT c FROM Checkbook c WHERE c.deleted = false " +
            "AND (:bankAccountId IS NULL OR c.bankAccount.id = :bankAccountId) " +
            "AND (:active IS NULL OR c.active = :active) " +
            "AND (:search IS NULL OR LOWER(CAST(c.name AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "                    OR LOWER(CAST(c.checkbookNumber AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<Checkbook> findAllWithFilters(@Param("bankAccountId") Long bankAccountId,
                                       @Param("active") Boolean active,
                                       @Param("search") String search,
                                       Pageable pageable);

    /**
     * Returns checkbooks of the given bank account whose [rangeFrom,rangeTo] overlaps the given pair.
     * Used to enforce the "no overlap within the same bank account" rule.
     */
    @Query("SELECT c FROM Checkbook c " +
            "WHERE c.deleted = false AND c.bankAccount.id = :bankAccountId " +
            "AND (:excludeId IS NULL OR c.id <> :excludeId) " +
            "AND c.rangeFrom <= :rangeTo AND c.rangeTo >= :rangeFrom")
    List<Checkbook> findOverlappingRanges(@Param("bankAccountId") Long bankAccountId,
                                          @Param("rangeFrom") Long rangeFrom,
                                          @Param("rangeTo") Long rangeTo,
                                          @Param("excludeId") Long excludeId);

    /** Numeric check numbers already consumed by check_payments using this checkbook (any state). */
    @Query("SELECT cp.checkNumber FROM CheckPayment cp " +
            "WHERE cp.checkbook.id = :checkbookId AND cp.checkNumber IS NOT NULL")
    List<String> findUsedCheckNumberStrings(@Param("checkbookId") Long checkbookId);

    boolean existsByCheckbookNumberIgnoreCaseAndBankAccountIdAndDeletedFalse(String checkbookNumber, Long bankAccountId);

    boolean existsByCheckbookNumberIgnoreCaseAndBankAccountIdAndIdNotAndDeletedFalse(String checkbookNumber, Long bankAccountId, Long id);
}
