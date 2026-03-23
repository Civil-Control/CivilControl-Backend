package PSG.backEnd.repository;

import PSG.backEnd.model.entity.vehicle.Repair;
import PSG.backEnd.model.enums.vehicle.RepairType;
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

    List<Repair> findByTransactionalDocumentId(Long transactionalDocumentId);

    @Query("SELECT r FROM Repair r " +
            "LEFT JOIN r.vehicle v " +
            "LEFT JOIN r.supplier s " +
            "WHERE (CAST(:dateFrom AS date) IS NULL OR r.date >= :dateFrom) " +
            "AND (CAST(:dateTo AS date) IS NULL OR r.date <= :dateTo) " +
            "AND (CAST(:vehicleId AS long) IS NULL OR r.vehicle.id = :vehicleId) " +
            "AND (:licensePlate IS NULL OR LOWER(CAST(v.licensePlate AS string)) LIKE LOWER(CONCAT('%', CAST(:licensePlate AS string), '%'))) " +
            "AND (CAST(:projectAreaId AS long) IS NULL OR v.projectArea.id = :projectAreaId) " +
            "AND (CAST(:minCost AS BigDecimal) IS NULL OR r.cost >= :minCost) " +
            "AND (CAST(:maxCost AS BigDecimal) IS NULL OR r.cost <= :maxCost) " +
            "AND (:employee IS NULL OR LOWER(CAST(r.employee AS string)) LIKE LOWER(CONCAT('%', CAST(:employee AS string), '%'))) " +
            "AND (CAST(:supplierId AS long) IS NULL OR r.supplier.id = :supplierId) " +
            "AND (:supplierName IS NULL OR LOWER(CAST(s.legalName AS string)) LIKE LOWER(CONCAT('%', CAST(:supplierName AS string), '%'))) " +
            "AND (:repairType IS NULL OR :repairType MEMBER OF r.repairTypes) " +
            "AND (:search IS NULL OR (LOWER(CAST(v.licensePlate AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "     OR LOWER(CAST(r.employee AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "     OR LOWER(CAST(s.legalName AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))) " +
            "AND (CAST(:transactionalDocumentId AS long) IS NULL OR r.transactionalDocument.id = :transactionalDocumentId)")
    Page<Repair> findAllWithFilters(
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("vehicleId") Long vehicleId,
            @Param("licensePlate") String licensePlate,
            @Param("projectAreaId") Long projectAreaId,
            @Param("minCost") BigDecimal minCost,
            @Param("maxCost") BigDecimal maxCost,
            @Param("employee") String employee,
            @Param("supplierId") Long supplierId,
            @Param("supplierName") String supplierName,
            @Param("repairType") RepairType repairType,
            @Param("search") String search,
            @Param("transactionalDocumentId") Long transactionalDocumentId,
            Pageable pageable
    );
}

