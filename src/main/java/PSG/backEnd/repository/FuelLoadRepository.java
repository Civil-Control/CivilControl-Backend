package PSG.backEnd.repository;

import PSG.backEnd.model.entity.gasStation.FuelLoad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FuelLoadRepository extends JpaRepository<FuelLoad, Long> {
    List<FuelLoad> findAll();
    Optional<FuelLoad> findById(Long id);

    List<FuelLoad> findByVehicleId(Long vehicleId);
    List<FuelLoad> findByProjectAreaId(Long projectAreaId);
    List<FuelLoad> findByGasStationId(Long gasStationId);

    boolean existsByTicketNumberAndBranchCode(String ticketNumber, String branchCode);

    @Query("SELECT fl FROM FuelLoad fl " +
            "LEFT JOIN fl.vehicle v " +
            "LEFT JOIN fl.projectArea pa " +
            "LEFT JOIN fl.gasStation gs " +
            "WHERE (:dateFrom IS NULL OR fl.date >= :dateFrom) " +
            "AND (:dateTo IS NULL OR fl.date <= :dateTo) " +
            "AND (:branchCode IS NULL OR LOWER(fl.branchCode) LIKE LOWER(CONCAT('%', :branchCode, '%'))) " +
            "AND (:ticketNumber IS NULL OR LOWER(fl.ticketNumber) LIKE LOWER(CONCAT('%', :ticketNumber, '%'))) " +
            "AND (:fuelType IS NULL OR LOWER(CAST(fl.fuelType AS string)) LIKE LOWER(CONCAT('%', :fuelType, '%'))) " +
            "AND (:vehicleId IS NULL OR fl.vehicle.id = :vehicleId) " +
            "AND (:vehicleLicensePlate IS NULL OR LOWER(v.licensePlate) LIKE LOWER(CONCAT('%', :vehicleLicensePlate, '%'))) " +
            "AND (:projectAreaId IS NULL OR fl.projectArea.id = :projectAreaId) " +
            "AND (:projectAreaName IS NULL OR LOWER(pa.name) LIKE LOWER(CONCAT('%', :projectAreaName, '%'))) " +
            "AND (:gasStationId IS NULL OR fl.gasStation.id = :gasStationId)")
    Page<FuelLoad> findAllWithFilters(
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("branchCode") String branchCode,
            @Param("ticketNumber") String ticketNumber,
            @Param("fuelType") String fuelType,
            @Param("vehicleId") Long vehicleId,
            @Param("vehicleLicensePlate") String vehicleLicensePlate,
            @Param("projectAreaId") Long projectAreaId,
            @Param("projectAreaName") String projectAreaName,
            @Param("gasStationId") Long gasStationId,
            Pageable pageable
    );
}
