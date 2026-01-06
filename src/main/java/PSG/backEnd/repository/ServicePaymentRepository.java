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
            "AND (:serviceSupplierId IS NULL OR sp.serviceSupplier.id = :serviceSupplierId) " +
            "AND (:buildingId IS NULL OR sp.building.id = :buildingId) " +
            "AND (:serviceType IS NULL OR sp.serviceType = :serviceType) " +
            "AND (:startDate IS NULL OR sp.paymentDate >= :startDate) " +
            "AND (:endDate IS NULL OR sp.paymentDate <= :endDate) " +
            "AND (:minAmount IS NULL OR sp.amount >= :minAmount) " +
            "AND (:maxAmount IS NULL OR sp.amount <= :maxAmount) " +
            "AND (:referenceNumber IS NULL OR LOWER(CAST(sp.referenceNumber AS string)) LIKE LOWER(CONCAT('%', CAST(:referenceNumber AS string), '%')))")
    Page<ServicePayment> findAllWithFilters(
            @Param("serviceSupplierId") Long serviceSupplierId,
            @Param("buildingId") Long buildingId,
            @Param("serviceType") ServiceType serviceType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("referenceNumber") String referenceNumber,
            Pageable pageable
    );

    @Query("SELECT COUNT(sp) > 0 FROM ServicePayment sp " +
            "WHERE sp.serviceSupplier.id = :serviceSupplierId " +
            "AND sp.building.id = :buildingId " +
            "AND sp.serviceType = :serviceType " +
            "AND YEAR(sp.paymentDate) = :year " +
            "AND MONTH(sp.paymentDate) = :month " +
            "AND sp.deleted = false " +
            "AND (:excludePaymentId IS NULL OR sp.id != :excludePaymentId)")
    boolean existsPaymentForServiceInMonth(
            @Param("serviceSupplierId") Long serviceSupplierId,
            @Param("buildingId") Long buildingId,
            @Param("serviceType") ServiceType serviceType,
            @Param("year") int year,
            @Param("month") int month,
            @Param("excludePaymentId") Long excludePaymentId
    );
}

