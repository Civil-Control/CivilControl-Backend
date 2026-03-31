package PSG.backEnd.repository;

import PSG.backEnd.model.entity.insurance.PolicyPayment;
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
public interface PolicyPaymentRepository extends JpaRepository<PolicyPayment, Long> {

    Optional<PolicyPayment> findByIdAndDeletedFalse(Long id);

    List<PolicyPayment> findByInsurancePolicyIdAndDeletedFalseOrderByPeriodFromDesc(Long insurancePolicyId);

    @Query("SELECT pp FROM PolicyPayment pp " +
            "WHERE pp.insurancePolicy.id = :policyId " +
            "AND pp.deleted = false " +
            "AND pp.periodFrom <= :periodEnd " +
            "AND pp.periodTo >= :periodStart")
    List<PolicyPayment> findByPolicyIdAndPeriodOverlap(
            @Param("policyId") Long policyId,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd);

    @Query("SELECT pp FROM PolicyPayment pp " +
            "JOIN pp.insurancePolicy ip " +
            "WHERE pp.deleted = false " +
            "AND (CAST(:insurancePolicyId AS long) IS NULL OR pp.insurancePolicy.id = :insurancePolicyId) " +
            "AND (CAST(:dateFrom AS date) IS NULL OR pp.paymentDate >= :dateFrom) " +
            "AND (CAST(:dateTo AS date) IS NULL OR pp.paymentDate <= :dateTo) " +
            "AND (CAST(:minAmount AS big_decimal) IS NULL OR pp.amount >= :minAmount) " +
            "AND (CAST(:maxAmount AS big_decimal) IS NULL OR pp.amount <= :maxAmount) " +
            "AND (:policyNumber IS NULL OR LOWER(CAST(ip.policyNumber AS string)) LIKE LOWER(CONCAT('%', CAST(:policyNumber AS string), '%')))")
    Page<PolicyPayment> findAllWithFilters(
            @Param("insurancePolicyId") Long insurancePolicyId,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("minAmount") java.math.BigDecimal minAmount,
            @Param("maxAmount") java.math.BigDecimal maxAmount,
            @Param("policyNumber") String policyNumber,
            Pageable pageable);
}
