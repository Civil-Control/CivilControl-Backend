package PSG.backEnd.repository;

import PSG.backEnd.model.entity.contracts.WorkContract;
import PSG.backEnd.model.enums.contracts.Currency;
import PSG.backEnd.model.enums.contracts.WorkContractStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkContractRepository extends JpaRepository<WorkContract, Long> {

    Optional<WorkContract> findByIdAndDeletedFalse(Long id);

    List<WorkContract> findByDeletedFalse();

    List<WorkContract> findAllByDeletedFalseAndStatus(WorkContractStatus status);

    @Query("""
            SELECT wc FROM WorkContract wc
            WHERE wc.deleted = false
            AND wc.tenantId = :tenantId
            AND (:clientId IS NULL OR wc.client.id = :clientId)
            AND (:clientBusinessName IS NULL OR LOWER(CAST(wc.client.businessName AS string)) LIKE LOWER(CONCAT('%', CAST(:clientBusinessName AS string), '%')))
            AND (:contractNumber IS NULL OR LOWER(CAST(wc.contractNumber AS string)) LIKE LOWER(CONCAT('%', CAST(:contractNumber AS string), '%')))
            AND (:projectAreaId IS NULL OR wc.projectArea.id = :projectAreaId)
            AND (:status IS NULL OR wc.status = :status)
            AND (:currency IS NULL OR wc.currency = :currency)
            AND (CAST(:contractDateFrom AS LocalDate) IS NULL OR wc.contractDate >= :contractDateFrom)
            AND (CAST(:contractDateTo AS LocalDate) IS NULL OR wc.contractDate <= :contractDateTo)
            AND (CAST(:minContractedAmount AS BigDecimal) IS NULL OR wc.contractedAmount >= :minContractedAmount)
            AND (CAST(:maxContractedAmount AS BigDecimal) IS NULL OR wc.contractedAmount <= :maxContractedAmount)
            AND (:search IS NULL OR (
                LOWER(CAST(wc.contractNumber AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                OR LOWER(CAST(wc.description AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
            ))
            """)
    Page<WorkContract> findAllWithFilters(
            @Param("tenantId") Long tenantId,
            @Param("clientId") Long clientId,
            @Param("clientBusinessName") String clientBusinessName,
            @Param("contractNumber") String contractNumber,
            @Param("projectAreaId") Long projectAreaId,
            @Param("status") WorkContractStatus status,
            @Param("currency") Currency currency,
            @Param("contractDateFrom") LocalDate contractDateFrom,
            @Param("contractDateTo") LocalDate contractDateTo,
            @Param("minContractedAmount") BigDecimal minContractedAmount,
            @Param("maxContractedAmount") BigDecimal maxContractedAmount,
            @Param("search") String search,
            Pageable pageable
    );

    boolean existsByContractNumberAndTenantIdAndDeletedFalse(String contractNumber, Long tenantId);

    boolean existsByContractNumberAndTenantIdAndDeletedFalseAndIdNot(
            String contractNumber, Long tenantId, Long excludeId);
}
