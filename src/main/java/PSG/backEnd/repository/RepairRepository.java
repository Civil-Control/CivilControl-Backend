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

    @Query("SELECT r FROM Repair r " +
            "LEFT JOIN r.vehicle v " +
            "LEFT JOIN r.supplier s " +
            "WHERE (:dateFrom IS NULL OR r.date >= :dateFrom) " +
            "AND (:dateTo IS NULL OR r.date <= :dateTo) " +
            "AND (:vehicleId IS NULL OR r.vehicle.id = :vehicleId) " +
            "AND (:licensePlate IS NULL OR LOWER(CAST(v.licensePlate AS string)) LIKE LOWER(CONCAT('%', CAST(:licensePlate AS string), '%'))) " +
            "AND (:minCost IS NULL OR r.cost >= :minCost) " +
            "AND (:maxCost IS NULL OR r.cost <= :maxCost) " +
            "AND (:employee IS NULL OR LOWER(CAST(r.employee AS string)) LIKE LOWER(CONCAT('%', CAST(:employee AS string), '%'))) " +
            "AND (:supplierId IS NULL OR r.supplier.id = :supplierId) " +
            "AND (:supplierName IS NULL OR LOWER(CAST(s.legalName AS string)) LIKE LOWER(CONCAT('%', CAST(:supplierName AS string), '%'))) " +
            "AND (:repairType IS NULL OR LOWER(CAST(r.repairType AS string)) LIKE LOWER(CONCAT('%', CAST(:repairType AS string), '%')))")
    Page<Repair> findAllWithFilters(
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("vehicleId") Long vehicleId,
            @Param("licensePlate") String licensePlate,
            @Param("minCost") BigDecimal minCost,
            @Param("maxCost") BigDecimal maxCost,
            @Param("employee") String employee,
            @Param("supplierId") Long supplierId,
            @Param("supplierName") String supplierName,
            @Param("repairType") String repairType,
            Pageable pageable
    );
}

