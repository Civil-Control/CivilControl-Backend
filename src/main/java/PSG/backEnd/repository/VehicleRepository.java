package PSG.backEnd.repository;

import PSG.backEnd.model.entity.vehicle.Vehicle;
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
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByDeletedFalse();
    Optional<Vehicle> findByIdAndDeletedFalse(Long id);
    Optional<Vehicle> findByLicensePlateAndDeletedTrue(String licensePlate);
    boolean existsByLicensePlateAndDeletedFalse(String licensePlate);
    boolean existsByIdAndDeletedFalse(Long id);

    @Query("SELECT v FROM Vehicle v " +
            "LEFT JOIN v.projectArea pa " +
            "LEFT JOIN v.vehicleType vt " +
            "LEFT JOIN v.storedInBuilding b " +
            "WHERE (:licensePlate IS NULL OR LOWER(CAST(v.licensePlate AS string)) LIKE LOWER(CONCAT('%', CAST(:licensePlate AS string), '%'))) " +
            "AND (:brand IS NULL OR LOWER(CAST(v.brand AS string)) LIKE LOWER(CONCAT('%', CAST(:brand AS string), '%'))) " +
            "AND (:model IS NULL OR LOWER(CAST(v.model AS string)) LIKE LOWER(CONCAT('%', CAST(:model AS string), '%'))) " +
            "AND (CAST(:year AS integer) IS NULL OR v.year = :year) " +
            "AND (:color IS NULL OR LOWER(CAST(v.color AS string)) LIKE LOWER(CONCAT('%', CAST(:color AS string), '%'))) " +
            "AND (:nickName IS NULL OR LOWER(CAST(v.nickName AS string)) LIKE LOWER(CONCAT('%', CAST(:nickName AS string), '%'))) " +
            "AND (:vehicleType IS NULL OR LOWER(CAST(vt.name AS string)) LIKE LOWER(CONCAT('%', CAST(:vehicleType AS string), '%'))) " +
            "AND (:projectAreaName IS NULL OR LOWER(CAST(pa.name AS string)) LIKE LOWER(CONCAT('%', CAST(:projectAreaName AS string), '%'))) " +
            "AND (:buildingName IS NULL OR LOWER(CAST(b.name AS string)) LIKE LOWER(CONCAT('%', CAST(:buildingName AS string), '%'))) " +
            "AND (CAST(:vtvExpirationDate AS date) IS NULL OR v.vtvExpirationDate = :vtvExpirationDate) " +
            "AND (:jurisdictionType IS NULL OR LOWER(CAST(v.jurisdictionType AS string)) LIKE LOWER(CONCAT('%', CAST(:jurisdictionType AS string), '%'))) " +
            "AND (:truckEquipment IS NULL OR LOWER(CAST(v.truckEquipment AS string)) LIKE LOWER(CONCAT('%', CAST(:truckEquipment AS string), '%'))) " +
            "AND v.deleted = false " +
            "AND (:includeInactive = true OR v.active = true)")
    Page<Vehicle> findAllWithFilters(
            @Param("licensePlate") String licensePlate,
            @Param("brand") String brand,
            @Param("model") String model,
            @Param("year") Integer year,
            @Param("color") String color,
            @Param("nickName") String nickName,
            @Param("vehicleType") String vehicleType,
            @Param("projectAreaName") String projectAreaName,
            @Param("buildingName") String buildingName,
            @Param("vtvExpirationDate") LocalDate vtvExpirationDate,
            @Param("jurisdictionType") String jurisdictionType,
            @Param("truckEquipment") String truckEquipment,
            @Param("includeInactive") Boolean includeInactive,
            Pageable pageable
    );
}
