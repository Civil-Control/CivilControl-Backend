package PSG.backEnd.repository;

import PSG.backEnd.model.entity.employee.SalaryPayment;
import PSG.backEnd.model.enums.documents.PaymentMethod;
import PSG.backEnd.model.enums.employee.SalaryFrecuency;
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
public interface SalaryPaymentRepository extends JpaRepository<SalaryPayment, Long> {
    List<SalaryPayment> findByEmployeeId(Long employeeId);
    Optional<SalaryPayment> findByIdAndEmployeeDeletedFalse(Long id);

    List<SalaryPayment> findByTransactionalDocumentId(Long transactionalDocumentId);

    @Query("SELECT sp FROM SalaryPayment sp " +
            "WHERE sp.employee.deleted = false " +
            "AND (CAST(:employeeId AS long) IS NULL OR sp.employee.id = :employeeId) " +
            "AND (:firstName IS NULL OR LOWER(CAST(sp.employee.name AS string)) LIKE LOWER(CONCAT('%', CAST(:firstName AS string), '%'))) " +
            "AND (:lastName IS NULL OR LOWER(CAST(sp.employee.lastName AS string)) LIKE LOWER(CONCAT('%', CAST(:lastName AS string), '%'))) " +
            "AND (:salaryFrequency IS NULL OR sp.salaryFrequency = :salaryFrequency) " +
            "AND (CAST(:projectAreaId AS long) IS NULL OR sp.projectArea.id = :projectAreaId) " +
            "AND (CAST(:paymentDateFrom AS date) IS NULL OR sp.paymentDate >= :paymentDateFrom) " +
            "AND (CAST(:paymentDateTo AS date) IS NULL OR sp.paymentDate <= :paymentDateTo) " +
            "AND (CAST(:minAmount AS BigDecimal) IS NULL OR sp.amount >= :minAmount) " +
            "AND (CAST(:maxAmount AS BigDecimal) IS NULL OR sp.amount <= :maxAmount) " +
            "AND (:paymentMethod IS NULL OR sp.paymentMethod = :paymentMethod) " +
            "AND (:search IS NULL OR (LOWER(CAST(sp.employee.name AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "     OR LOWER(CAST(sp.employee.lastName AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))) " +
            "AND (CAST(:transactionalDocumentId AS long) IS NULL OR sp.transactionalDocument.id = :transactionalDocumentId)")
    Page<SalaryPayment> findAllWithFilters(
            @Param("employeeId") Long employeeId,
            @Param("firstName") String firstName,
            @Param("lastName") String lastName,
            @Param("salaryFrequency") SalaryFrecuency salaryFrequency,
            @Param("projectAreaId") Long projectAreaId,
            @Param("paymentDateFrom") LocalDate paymentDateFrom,
            @Param("paymentDateTo") LocalDate paymentDateTo,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("paymentMethod") PaymentMethod paymentMethod,
            @Param("search") String search,
            @Param("transactionalDocumentId") Long transactionalDocumentId,
            Pageable pageable
    );

    @Query("SELECT COUNT(sp) > 0 FROM SalaryPayment sp " +
            "WHERE sp.employee.id = :employeeId " +
            "AND sp.salaryFrequency = 'MENSUAL' " +
            "AND YEAR(sp.paymentDate) = :year " +
            "AND MONTH(sp.paymentDate) = :month " +
            "AND sp.employee.deleted = false " +
            "AND (CAST(:excludePaymentId AS long) IS NULL OR sp.id != :excludePaymentId)")
    boolean existsMonthlyPaymentForEmployeeInMonth(
            @Param("employeeId") Long employeeId,
            @Param("year") int year,
            @Param("month") int month,
            @Param("excludePaymentId") Long excludePaymentId
    );

    @Query("SELECT COUNT(sp) FROM SalaryPayment sp " +
            "WHERE sp.employee.id = :employeeId " +
            "AND sp.salaryFrequency = 'QUINCENAL' " +
            "AND YEAR(sp.paymentDate) = :year " +
            "AND MONTH(sp.paymentDate) = :month " +
            "AND sp.employee.deleted = false " +
            "AND (CAST(:excludePaymentId AS long) IS NULL OR sp.id != :excludePaymentId)")
    long countBiweeklyPaymentsForEmployeeInMonth(
            @Param("employeeId") Long employeeId,
            @Param("year") int year,
            @Param("month") int month,
            @Param("excludePaymentId") Long excludePaymentId
    );
}

