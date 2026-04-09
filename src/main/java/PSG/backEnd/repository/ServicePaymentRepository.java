package PSG.backEnd.repository;

import PSG.backEnd.model.entity.serviceSupplier.ServicePayment;
import PSG.backEnd.model.enums.PaymentSubjectType;
import PSG.backEnd.model.enums.ServiceType;
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
public interface ServicePaymentRepository extends JpaRepository<ServicePayment, Long> {

    List<ServicePayment> findByDeletedFalse();
    Optional<ServicePayment> findByIdAndDeletedFalse(Long id);
    boolean existsByIdAndDeletedFalse(Long id);
    List<ServicePayment> findByServiceAssignmentIdAndDeletedFalse(Long serviceAssignmentId);
    boolean existsByReferenceNumberAndDeletedFalse(String referenceNumber);

    @Query("SELECT COUNT(sp) > 0 FROM ServicePayment sp " +
            "WHERE sp.referenceNumber = :referenceNumber " +
            "AND sp.deleted = false " +
            "AND (:excludePaymentId IS NULL OR sp.id != :excludePaymentId)")
    boolean existsByReferenceNumberAndDeletedFalseExcludingId(
            @Param("referenceNumber") String referenceNumber,
            @Param("excludePaymentId") Long excludePaymentId
    );

    @Query("SELECT sp FROM ServicePayment sp " +
            "LEFT JOIN sp.serviceAssignment sa " +
            "LEFT JOIN sa.serviceSupplier ss " +
            "LEFT JOIN ss.supplier sup " +
            "LEFT JOIN sa.building b " +
            "LEFT JOIN sp.vehicle v " +
            "WHERE sp.deleted = false " +
            // ——— Subject type filter ———
            "AND (:subjectType IS NULL OR sp.subjectType = :subjectType) " +
            // ——— Building-based filters ———
            "AND (CAST(:serviceAssignmentId AS long) IS NULL OR sa.id = :serviceAssignmentId) " +
            "AND (CAST(:serviceSupplierId AS long) IS NULL OR ss.id = :serviceSupplierId) " +
            "AND (CAST(:buildingId AS long) IS NULL OR b.id = :buildingId) " +
            "AND (:serviceType IS NULL OR sa.serviceType = :serviceType) " +
            // ——— Vehicle-based filters ———
            "AND (CAST(:vehicleId AS long) IS NULL OR v.id = :vehicleId) " +
            // ——— Common filters ———
            "AND (CAST(:projectAreaId AS long) IS NULL OR sp.projectArea.id = :projectAreaId) " +
            "AND (CAST(:year AS integer) IS NULL OR sp.year = :year) " +
            "AND (CAST(:period AS integer) IS NULL OR sp.period = :period) " +
            "AND (CAST(:startDate AS date) IS NULL OR sp.paymentDate >= :startDate) " +
            "AND (CAST(:endDate AS date) IS NULL OR sp.paymentDate <= :endDate) " +
            "AND (CAST(:minAmount AS BigDecimal) IS NULL OR sp.amount >= :minAmount) " +
            "AND (CAST(:maxAmount AS BigDecimal) IS NULL OR sp.amount <= :maxAmount) " +
            "AND (:referenceNumber IS NULL OR LOWER(CAST(sp.referenceNumber AS string)) LIKE LOWER(CONCAT('%', CAST(:referenceNumber AS string), '%'))) " +
            "AND (:supplierName IS NULL OR LOWER(CAST(sup.legalName AS string)) LIKE LOWER(CONCAT('%', CAST(:supplierName AS string), '%')) " +
            "     OR :supplierName IS NULL OR LOWER(CAST(sup.tradeName AS string)) LIKE LOWER(CONCAT('%', CAST(:supplierName AS string), '%'))) " +
            // ——— Global search: searches across building-based and vehicle-based fields ———
            "AND (:search IS NULL OR (" +
            "     LOWER(CAST(sup.legalName AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "     OR LOWER(CAST(sup.tradeName AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "     OR CAST(sup.cuit AS string) LIKE CONCAT('%', CAST(:search AS string), '%') " +
            "     OR LOWER(CAST(b.name AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "     OR LOWER(CAST(v.licensePlate AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "))")
    Page<ServicePayment> findAllWithFilters(
            @Param("subjectType") PaymentSubjectType subjectType,
            @Param("serviceAssignmentId") Long serviceAssignmentId,
            @Param("serviceSupplierId") Long serviceSupplierId,
            @Param("buildingId") Long buildingId,
            @Param("projectAreaId") Long projectAreaId,
            @Param("serviceType") ServiceType serviceType,
            @Param("vehicleId") Long vehicleId,
            @Param("year") Integer year,
            @Param("period") Integer period,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("referenceNumber") String referenceNumber,
            @Param("supplierName") String supplierName,
            @Param("search") String search,
            Pageable pageable
    );
}

