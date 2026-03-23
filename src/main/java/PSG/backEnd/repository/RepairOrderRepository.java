package PSG.backEnd.repository;

import PSG.backEnd.model.entity.vehicle.RepairOrder;
import PSG.backEnd.model.enums.vehicle.RepairOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface RepairOrderRepository extends JpaRepository<RepairOrder, Long> {

    boolean existsByIdAndDeletedFalse(Long id);

    @Query("SELECT ro FROM RepairOrder ro " +
            "LEFT JOIN ro.vehicle v " +
            "WHERE ro.deleted = false " +
            "AND (CAST(:dateFrom AS date) IS NULL OR ro.date >= :dateFrom) " +
            "AND (CAST(:dateTo AS date) IS NULL OR ro.date <= :dateTo) " +
            "AND (CAST(:vehicleId AS long) IS NULL OR ro.vehicle.id = :vehicleId) " +
            "AND (:vehicleLicensePlate IS NULL OR LOWER(CAST(v.licensePlate AS string)) LIKE LOWER(CONCAT('%', CAST(:vehicleLicensePlate AS string), '%'))) " +
            "AND (:status IS NULL OR ro.status = :status) " +
            "AND (:search IS NULL OR (LOWER(CAST(ro.description AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "     OR LOWER(CAST(ro.reportedBy AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "     OR LOWER(CAST(v.licensePlate AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))))")
    Page<RepairOrder> findAllWithFilters(
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("vehicleId") Long vehicleId,
            @Param("vehicleLicensePlate") String vehicleLicensePlate,
            @Param("status") RepairOrderStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT ro FROM RepairOrder ro " +
            "LEFT JOIN ro.vehicle v " +
            "WHERE ro.deleted = false " +
            "AND ro.createdByUser.id = :userId " +
            "AND (CAST(:dateFrom AS date) IS NULL OR ro.date >= :dateFrom) " +
            "AND (CAST(:dateTo AS date) IS NULL OR ro.date <= :dateTo) " +
            "AND (CAST(:vehicleId AS long) IS NULL OR ro.vehicle.id = :vehicleId) " +
            "AND (:vehicleLicensePlate IS NULL OR LOWER(CAST(v.licensePlate AS string)) LIKE LOWER(CONCAT('%', CAST(:vehicleLicensePlate AS string), '%'))) " +
            "AND (:status IS NULL OR ro.status = :status) " +
            "AND (:search IS NULL OR (LOWER(CAST(ro.description AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "     OR LOWER(CAST(ro.reportedBy AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "     OR LOWER(CAST(v.licensePlate AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))))")
    Page<RepairOrder> findAllByCreatedByUserWithFilters(
            @Param("userId") Long userId,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("vehicleId") Long vehicleId,
            @Param("vehicleLicensePlate") String vehicleLicensePlate,
            @Param("status") RepairOrderStatus status,
            @Param("search") String search,
            Pageable pageable
    );
}
