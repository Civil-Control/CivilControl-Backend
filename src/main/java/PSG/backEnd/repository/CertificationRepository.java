package PSG.backEnd.repository;

import PSG.backEnd.model.dto.contracts.CertificationStatsProjection;
import PSG.backEnd.model.entity.contracts.Certification;
import PSG.backEnd.model.enums.contracts.CertificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CertificationRepository extends JpaRepository<Certification, Long>, JpaSpecificationExecutor<Certification> {

    List<Certification> findBySalesDocumentIdAndDeletedFalse(Long salesDocumentId);

    Optional<Certification> findByIdAndDeletedFalse(Long id);

    @Query("""
            SELECT COALESCE(MAX(c.certificationNumber), 0)
            FROM Certification c
            WHERE c.contract.id = :contractId
              AND c.tenantId = :tenantId
              AND c.deleted = false
            """)
    Integer findMaxCertificationNumber(@Param("contractId") Long contractId,
                                       @Param("tenantId") Long tenantId);

    @Query("""
            SELECT new PSG.backEnd.model.dto.contracts.CertificationStatsProjection(
                COALESCE(SUM(c.certifiedAmount), 0),
                COALESCE(SUM(CASE WHEN c.salesDocument IS NOT NULL THEN c.certifiedAmount ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN c.status = 'COBRADO' THEN c.certifiedAmount ELSE 0 END), 0)
            )
            FROM Certification c
            WHERE c.contract.id = :contractId
              AND c.deleted = false
              AND c.tenantId = :tenantId
            """)
    CertificationStatsProjection getStatsByContractId(@Param("contractId") Long contractId,
                                                      @Param("tenantId") Long tenantId);

    /**
     * Find all certifications linked to any of the given sales document ids,
     * eagerly fetching the contract for label/description rendering.
     */
    @Query("SELECT c FROM Certification c " +
           "LEFT JOIN FETCH c.contract " +
           "WHERE c.deleted = false AND c.salesDocument.id IN :salesDocumentIds")
    List<Certification> findBySalesDocumentIdInForReport(@Param("salesDocumentIds") Collection<Long> salesDocumentIds);

    /**
     * Find orphan certifications (without sales document) for the sales report.
     * Eagerly fetches the contract, its client and projectArea.
     * Each parameter has a corresponding boolean guard to allow an empty filter.
     */
    @Query("SELECT DISTINCT c FROM Certification c " +
           "JOIN FETCH c.contract wc " +
           "LEFT JOIN FETCH wc.client " +
           "LEFT JOIN FETCH wc.projectArea " +
           "WHERE c.deleted = false " +
           "AND c.salesDocument IS NULL " +
           "AND (CAST(:dateFrom AS LocalDate) IS NULL OR c.certificationDate >= :dateFrom) " +
           "AND (CAST(:dateTo AS LocalDate) IS NULL OR c.certificationDate <= :dateTo) " +
           "AND (:status IS NULL OR c.status = :status) " +
           "AND (CAST(:minAmount AS BigDecimal) IS NULL OR c.certifiedAmount >= :minAmount) " +
           "AND (CAST(:maxAmount AS BigDecimal) IS NULL OR c.certifiedAmount <= :maxAmount) " +
           "AND (:workContractId IS NULL OR wc.id = :workContractId) " +
           "AND (:hasAreaFilter = false OR wc.projectArea.id IN :projectAreaIds) " +
           "AND (:hasClientFilter = false OR wc.client.id IN :clientIds)")
    List<Certification> findOrphansForReport(
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("status") CertificationStatus status,
            @Param("minAmount") java.math.BigDecimal minAmount,
            @Param("maxAmount") java.math.BigDecimal maxAmount,
            @Param("workContractId") Long workContractId,
            @Param("hasAreaFilter") boolean hasAreaFilter,
            @Param("projectAreaIds") Collection<Long> projectAreaIds,
            @Param("hasClientFilter") boolean hasClientFilter,
            @Param("clientIds") Collection<Long> clientIds
    );
}
