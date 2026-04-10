package PSG.backEnd.repository;

import PSG.backEnd.model.entity.vehicle.Repair;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface RepairRepository extends JpaRepository<Repair, Long> {

    List<Repair> findByVehicleId(Long vehicleId);

    List<Repair> findBySupplierId(Long supplierId);

    @Query("SELECT DISTINCT r FROM Repair r " +
            "LEFT JOIN r.vehicle v " +
            "LEFT JOIN r.supplier s " +
            "LEFT JOIN r.items i " +
            "WHERE (CAST(:dateFrom AS date) IS NULL OR r.date >= :dateFrom) " +
            "AND (CAST(:dateTo AS date) IS NULL OR r.date <= :dateTo) " +
            "AND (CAST(:vehicleId AS long) IS NULL OR r.vehicle.id = :vehicleId) " +
            "AND (:licensePlate IS NULL OR LOWER(CAST(v.licensePlate AS string)) LIKE LOWER(CONCAT('%', CAST(:licensePlate AS string), '%'))) " +
            "AND (CAST(:projectAreaId AS long) IS NULL OR v.projectArea.id = :projectAreaId) " +
            "AND (CAST(:minCost AS BigDecimal) IS NULL OR (SELECT COALESCE(SUM(ri.amount), 0) FROM RepairItem ri WHERE ri.repair = r) >= :minCost) " +
            "AND (CAST(:maxCost AS BigDecimal) IS NULL OR (SELECT COALESCE(SUM(ri.amount), 0) FROM RepairItem ri WHERE ri.repair = r) <= :maxCost) " +
            "AND (CAST(:supplierId AS long) IS NULL OR r.supplier.id = :supplierId) " +
            "AND (:supplierName IS NULL OR LOWER(CAST(s.legalName AS string)) LIKE LOWER(CONCAT('%', CAST(:supplierName AS string), '%'))) " +
            "AND (:itemDescription IS NULL OR LOWER(CAST(i.description AS string)) LIKE LOWER(CONCAT('%', CAST(:itemDescription AS string), '%'))) " +
            "AND (CAST(:minMileage AS int) IS NULL OR r.mileage >= :minMileage) " +
            "AND (CAST(:maxMileage AS int) IS NULL OR r.mileage <= :maxMileage) " +
            "AND (:search IS NULL OR (LOWER(CAST(v.licensePlate AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "     OR LOWER(CAST(s.legalName AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "     OR LOWER(CAST(i.description AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))) " +
            "AND (CAST(:transactionalDocumentId AS long) IS NULL OR i.transactionalDocument.id = :transactionalDocumentId) " +
            "AND (:unlinked = false OR EXISTS (SELECT 1 FROM RepairItem ri2 WHERE ri2.repair = r AND ri2.transactionalDocument IS NULL))")
    Page<Repair> findAllWithFilters(
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("vehicleId") Long vehicleId,
            @Param("licensePlate") String licensePlate,
            @Param("projectAreaId") Long projectAreaId,
            @Param("minCost") BigDecimal minCost,
            @Param("maxCost") BigDecimal maxCost,
            @Param("supplierId") Long supplierId,
            @Param("supplierName") String supplierName,
            @Param("itemDescription") String itemDescription,
            @Param("minMileage") Integer minMileage,
            @Param("maxMileage") Integer maxMileage,
            @Param("search") String search,
            @Param("transactionalDocumentId") Long transactionalDocumentId,
            @Param("unlinked") boolean unlinked,
            Pageable pageable
    );
}
