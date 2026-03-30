package PSG.backEnd.repository;

import PSG.backEnd.model.dto.contracts.CertificationStatsProjection;
import PSG.backEnd.model.entity.contracts.Certification;
import PSG.backEnd.model.enums.contracts.CertificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CertificationRepository extends JpaRepository<Certification, Long> {

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

    @Query("""
            SELECT c FROM Certification c
            WHERE c.deleted = false
            AND c.tenantId = :tenantId
            AND (:workContractId IS NULL OR c.contract.id = :workContractId)
            AND (:clientId IS NULL OR c.contract.client.id = :clientId)
            AND (:status IS NULL OR c.status = :status)
            AND (CAST(:dateFrom AS LocalDate) IS NULL OR c.certificationDate >= :dateFrom)
            AND (CAST(:dateTo AS LocalDate) IS NULL OR c.certificationDate <= :dateTo)
            AND (:hasInvoice IS NULL OR
                (:hasInvoice = TRUE AND c.salesDocument IS NOT NULL) OR
                (:hasInvoice = FALSE AND c.salesDocument IS NULL))
            AND (:salesDocumentId IS NULL OR c.salesDocument.id = :salesDocumentId)
            """)
    Page<Certification> findAllWithFilters(
            @Param("tenantId") Long tenantId,
            @Param("workContractId") Long workContractId,
            @Param("clientId") Long clientId,
            @Param("status") CertificationStatus status,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("hasInvoice") Boolean hasInvoice,
            @Param("salesDocumentId") Long salesDocumentId,
            Pageable pageable
    );
}
