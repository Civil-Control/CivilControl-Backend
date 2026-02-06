package PSG.backEnd.repository;

import PSG.backEnd.model.entity.vehicle.LicencePlatePayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface LicencePlatePaymentRepository extends JpaRepository<LicencePlatePayment, Long> {

    Optional<LicencePlatePayment> findByVehicleIdAndYearAndPeriod(Long vehicleId, Integer year, Integer period);

    @Query("SELECT lpp FROM LicencePlatePayment lpp " +
            "LEFT JOIN Vehicle vehicle ON lpp.vehicleId = vehicle.id " +
            "WHERE (CAST(:dateFrom AS date) IS NULL OR lpp.date >= :dateFrom) " +
            "AND (CAST(:dateTo AS date) IS NULL OR lpp.date <= :dateTo) " +
            "AND (CAST(:vehicleId AS long) IS NULL OR lpp.vehicleId = :vehicleId) " +
            "AND (:vehicleLicensePlate IS NULL OR LOWER(CAST(vehicle.licensePlate AS string)) LIKE LOWER(CONCAT('%', CAST(:vehicleLicensePlate AS string), '%'))) " +
            "AND (CAST(:projectAreaId AS long) IS NULL OR vehicle.projectArea.id = :projectAreaId) " +
            "AND (CAST(:minAmount AS BigDecimal) IS NULL OR lpp.amount >= :minAmount) " +
            "AND (CAST(:maxAmount AS BigDecimal) IS NULL OR lpp.amount <= :maxAmount) " +
            "AND (CAST(:year AS integer) IS NULL OR lpp.year = :year) " +
            "AND (CAST(:period AS integer) IS NULL OR lpp.period = :period) " +
            "AND (:jurisdictionType IS NULL OR LOWER(CAST(lpp.jurisdictionType AS string)) LIKE LOWER(CONCAT('%', CAST(:jurisdictionType AS string), '%')))")
    Page<LicencePlatePayment> findAllWithFilters(
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("vehicleId") Long vehicleId,
            @Param("vehicleLicensePlate") String vehicleLicensePlate,
            @Param("projectAreaId") Long projectAreaId,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("year") Integer year,
            @Param("period") Integer period,
            @Param("jurisdictionType") String jurisdictionType,
            Pageable pageable
    );
}
