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
            "LEFT JOIN gs.supplier gsS " +
            "WHERE (CAST(:dateFrom AS date) IS NULL OR fl.date >= :dateFrom) " +
            "AND (CAST(:dateTo AS date) IS NULL OR fl.date <= :dateTo) " +
            "AND (:branchCode IS NULL OR LOWER(CAST(fl.branchCode AS string)) LIKE LOWER(CONCAT('%', CAST(:branchCode AS string), '%'))) " +
            "AND (:ticketNumber IS NULL OR LOWER(CAST(fl.ticketNumber AS string)) LIKE LOWER(CONCAT('%', CAST(:ticketNumber AS string), '%'))) " +
            "AND (:fuelType IS NULL OR LOWER(CAST(fl.fuelType AS string)) LIKE LOWER(CONCAT('%', CAST(:fuelType AS string), '%'))) " +
            "AND (CAST(:vehicleId AS long) IS NULL OR fl.vehicle.id = :vehicleId) " +
            "AND (:vehicleLicensePlate IS NULL OR LOWER(CAST(v.licensePlate AS string)) LIKE LOWER(CONCAT('%', CAST(:vehicleLicensePlate AS string), '%'))) " +
            "AND (CAST(:projectAreaId AS long) IS NULL OR fl.projectArea.id = :projectAreaId) " +
            "AND (:projectAreaName IS NULL OR LOWER(CAST(pa.name AS string)) LIKE LOWER(CONCAT('%', CAST(:projectAreaName AS string), '%'))) " +
            "AND (CAST(:gasStationId AS long) IS NULL OR fl.gasStation.id = :gasStationId) " +
            "AND (:gasStationName IS NULL OR LOWER(CAST(gsS.legalName AS string)) LIKE LOWER(CONCAT('%', CAST(:gasStationName AS string), '%')) " +
            "     OR :gasStationName IS NULL OR LOWER(CAST(gsS.tradeName AS string)) LIKE LOWER(CONCAT('%', CAST(:gasStationName AS string), '%'))) " +
            "AND (CAST(:totalAmountMin AS big_decimal) IS NULL OR fl.totalAmount >= :totalAmountMin) " +
            "AND (CAST(:totalAmountMax AS big_decimal) IS NULL OR fl.totalAmount <= :totalAmountMax) " +
            "AND (CAST(:transactionalDocumentId AS long) IS NULL OR fl.transactionalDocument.id = :transactionalDocumentId) " +
            "AND (:search IS NULL OR (LOWER(CAST(v.licensePlate AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "     OR LOWER(CAST(fl.fuelType AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))))")
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
            @Param("search") String search,
            @Param("gasStationName") String gasStationName,
            @Param("totalAmountMin") java.math.BigDecimal totalAmountMin,
            @Param("totalAmountMax") java.math.BigDecimal totalAmountMax,
            @Param("transactionalDocumentId") Long transactionalDocumentId,
            Pageable pageable
    );
}
