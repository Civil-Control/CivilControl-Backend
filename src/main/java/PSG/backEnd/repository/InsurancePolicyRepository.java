package PSG.backEnd.repository;

import PSG.backEnd.model.entity.insurance.InsurancePolicy;
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
public interface InsurancePolicyRepository extends JpaRepository<InsurancePolicy, Long> {

    List<InsurancePolicy> findAllByOrderByIdDesc();
    Optional<InsurancePolicy> findByIdAndDeletedFalse(Long id);
    Optional<InsurancePolicy> findByPolicyNumberAndDeletedFalse(String policyNumber);
    Optional<InsurancePolicy> findByPolicyNumberAndDeletedTrue(String policyNumber);
    boolean existsByPolicyNumberAndDeletedFalse(String policyNumber);
    boolean existsByIdAndDeletedFalse(Long id);

    @Query("SELECT ip FROM InsurancePolicy ip " +
            "WHERE (:policyNumber IS NULL OR LOWER(CAST(ip.policyNumber AS string)) LIKE LOWER(CONCAT('%', CAST(:policyNumber AS string), '%'))) " +
            "AND (:termNumber IS NULL OR LOWER(CAST(ip.termNumber AS string)) LIKE LOWER(CONCAT('%', CAST(:termNumber AS string), '%'))) " +
            "AND (:policyType IS NULL OR CAST(ip.policyType AS string) = :policyType) " +
            "AND (:policyStatus IS NULL OR CAST(ip.policyStatus AS string) = :policyStatus) " +
            "AND (:paymentFrequency IS NULL OR CAST(ip.paymentFrequency AS string) = :paymentFrequency) " +
            "AND (CAST(:issueDateFrom AS date) IS NULL OR ip.issueDate >= :issueDateFrom) " +
            "AND (CAST(:issueDateTo AS date) IS NULL OR ip.issueDate <= :issueDateTo) " +
            "AND (CAST(:effectiveFromStart AS date) IS NULL OR ip.effectiveFrom >= :effectiveFromStart) " +
            "AND (CAST(:effectiveFromEnd AS date) IS NULL OR ip.effectiveFrom <= :effectiveFromEnd) " +
            "AND (CAST(:effectiveToStart AS date) IS NULL OR ip.effectiveTo >= :effectiveToStart) " +
            "AND (CAST(:effectiveToEnd AS date) IS NULL OR ip.effectiveTo <= :effectiveToEnd) " +
            "AND (CAST(:isCancelled AS boolean) IS NULL OR " +
            "     (:isCancelled = true AND ip.cancellationDate IS NOT NULL) OR " +
            "     (:isCancelled = false AND ip.cancellationDate IS NULL)) " +
            "AND ip.deleted = false " +
            "ORDER BY ip.id DESC")
    Page<InsurancePolicy> findAllWithFilters(
            @Param("policyNumber") String policyNumber,
            @Param("termNumber") String termNumber,
            @Param("policyType") String policyType,
            @Param("policyStatus") String policyStatus,
            @Param("paymentFrequency") String paymentFrequency,
            @Param("issueDateFrom") LocalDate issueDateFrom,
            @Param("issueDateTo") LocalDate issueDateTo,
            @Param("effectiveFromStart") LocalDate effectiveFromStart,
            @Param("effectiveFromEnd") LocalDate effectiveFromEnd,
            @Param("effectiveToStart") LocalDate effectiveToStart,
            @Param("effectiveToEnd") LocalDate effectiveToEnd,
            @Param("isCancelled") Boolean isCancelled,
            Pageable pageable
    );
}
