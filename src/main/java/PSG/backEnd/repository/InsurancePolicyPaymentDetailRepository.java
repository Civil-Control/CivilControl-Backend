package PSG.backEnd.repository;

import PSG.backEnd.model.entity.insurance.InsurancePolicyPaymentDetail;
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
public interface InsurancePolicyPaymentDetailRepository extends JpaRepository<InsurancePolicyPaymentDetail, Long> {

    Optional<InsurancePolicyPaymentDetail> findByIdAndDeletedFalse(Long id);

    @Query("SELECT ippd FROM InsurancePolicyPaymentDetail ippd " +
            "JOIN FETCH ippd.paymentDetails pd " +
            "LEFT JOIN FETCH pd.cashPayment " +
            "LEFT JOIN FETCH pd.transferPayment " +
            "LEFT JOIN FETCH pd.checkPayment " +
            "WHERE ippd.id = :id AND ippd.deleted = false")
    Optional<InsurancePolicyPaymentDetail> findByIdWithPaymentDetails(@Param("id") Long id);

    @Query("SELECT ippd FROM InsurancePolicyPaymentDetail ippd " +
            "JOIN FETCH ippd.paymentDetails pd " +
            "LEFT JOIN FETCH pd.cashPayment " +
            "LEFT JOIN FETCH pd.transferPayment " +
            "LEFT JOIN FETCH pd.checkPayment " +
            "JOIN FETCH ippd.insurancePolicy ip " +
            "WHERE ippd.insurancePolicy.id = :policyId " +
            "AND ippd.deleted = false " +
            "ORDER BY ippd.periodFrom DESC")
    List<InsurancePolicyPaymentDetail> findByPolicyIdWithPaymentDetails(@Param("policyId") Long policyId);

    @Query("SELECT ippd FROM InsurancePolicyPaymentDetail ippd " +
            "JOIN FETCH ippd.paymentDetails pd " +
            "JOIN ippd.insurancePolicy ip " +
            "WHERE ippd.deleted = false " +
            "AND (CAST(:insurancePolicyId AS long) IS NULL OR ip.id = :insurancePolicyId) " +
            "AND (CAST(:dateFrom AS date) IS NULL OR pd.paymentDate >= :dateFrom) " +
            "AND (CAST(:dateTo AS date) IS NULL OR pd.paymentDate <= :dateTo) " +
            "AND (CAST(:minAmount AS big_decimal) IS NULL OR pd.amount >= :minAmount) " +
            "AND (CAST(:maxAmount AS big_decimal) IS NULL OR pd.amount <= :maxAmount)")
    Page<InsurancePolicyPaymentDetail> findAllWithFilters(
            @Param("insurancePolicyId") Long insurancePolicyId,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            Pageable pageable);

    @Query("SELECT ippd FROM InsurancePolicyPaymentDetail ippd " +
            "WHERE ippd.paymentDetails.id = :paymentDetailsId " +
            "AND ippd.deleted = false")
    Optional<InsurancePolicyPaymentDetail> findByPaymentDetailsId(@Param("paymentDetailsId") Long paymentDetailsId);
}
