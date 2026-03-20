package PSG.backEnd.repository;

import PSG.backEnd.model.entity.serviceSupplier.ServicePayment;
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
    List<ServicePayment> findByServiceSupplierIdAndDeletedFalse(Long serviceSupplierId);
    List<ServicePayment> findByBuildingIdAndDeletedFalse(Long buildingId);
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
            "WHERE sp.deleted = false " +
            "AND (CAST(:serviceSupplierId AS long) IS NULL OR sp.serviceSupplier.id = :serviceSupplierId) " +
            "AND (CAST(:buildingId AS long) IS NULL OR sp.building.id = :buildingId) " +
            "AND (CAST(:projectAreaId AS long) IS NULL OR sp.building.projectArea.id = :projectAreaId) " +
            "AND (:serviceType IS NULL OR sp.serviceType = :serviceType) " +
            "AND (CAST(:startDate AS date) IS NULL OR sp.paymentDate >= :startDate) " +
            "AND (CAST(:endDate AS date) IS NULL OR sp.paymentDate <= :endDate) " +
            "AND (CAST(:minAmount AS BigDecimal) IS NULL OR sp.amount >= :minAmount) " +
            "AND (CAST(:maxAmount AS BigDecimal) IS NULL OR sp.amount <= :maxAmount) " +
            "AND (:referenceNumber IS NULL OR LOWER(CAST(sp.referenceNumber AS string)) LIKE LOWER(CONCAT('%', CAST(:referenceNumber AS string), '%'))) " +
            "AND (:supplierName IS NULL OR LOWER(CAST(sp.serviceSupplier.supplier.legalName AS string)) LIKE LOWER(CONCAT('%', CAST(:supplierName AS string), '%')) " +
            "     OR :supplierName IS NULL OR LOWER(CAST(sp.serviceSupplier.supplier.tradeName AS string)) LIKE LOWER(CONCAT('%', CAST(:supplierName AS string), '%'))) " +
            "AND (:search IS NULL OR (LOWER(CAST(sp.serviceSupplier.supplier.legalName AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "     OR LOWER(CAST(sp.serviceSupplier.supplier.tradeName AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "     OR sp.serviceSupplier.supplier.cuit LIKE CONCAT('%', CAST(:search AS string), '%')))")
    Page<ServicePayment> findAllWithFilters(
            @Param("serviceSupplierId") Long serviceSupplierId,
            @Param("buildingId") Long buildingId,
            @Param("projectAreaId") Long projectAreaId,
            @Param("serviceType") ServiceType serviceType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("referenceNumber") String referenceNumber,
            @Param("supplierName") String supplierName,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT COUNT(sp) > 0 FROM ServicePayment sp " +
            "WHERE sp.serviceSupplier.id = :serviceSupplierId " +
            "AND sp.building.id = :buildingId " +
            "AND sp.serviceType = :serviceType " +
            "AND YEAR(sp.paymentDate) = :year " +
            "AND MONTH(sp.paymentDate) = :month " +
            "AND sp.deleted = false " +
            "AND (CAST(:excludePaymentId AS long) IS NULL OR sp.id != :excludePaymentId)")
    boolean existsPaymentForServiceInMonth(
            @Param("serviceSupplierId") Long serviceSupplierId,
            @Param("buildingId") Long buildingId,
            @Param("serviceType") ServiceType serviceType,
            @Param("year") int year,
            @Param("month") int month,
            @Param("excludePaymentId") Long excludePaymentId
    );
}

